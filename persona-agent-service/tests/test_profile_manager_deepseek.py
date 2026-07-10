from persona_agent_service.agents.deepseek_recommendation_agent import (
    GENERATION_DEEPSEEK_ENHANCED,
    GENERATION_FALLBACK_RULE_BASED,
    GENERATION_RULE_BASED,
    DeepSeekRecommendationAgent,
)
from persona_agent_service.agents.profile_manager import ProfileManager
from persona_agent_service.config.settings import AgentSettings
from persona_agent_service.llm.exceptions import DeepSeekRequestError
from persona_agent_service.schemas.context import AgentProfileContext


class StubDeepSeekClient:
    def __init__(self, response=None, exception: Exception | None = None):
        self.response = response
        self.exception = exception

    def complete_json(self, system_prompt: str, user_prompt: str):
        if self.exception:
            raise self.exception
        return self.response


def enabled_settings() -> AgentSettings:
    return AgentSettings(
        deepseek_enabled=True,
        deepseek_api_key="test-api-key",
        deepseek_model="deepseek-test",
    )


def sample_context() -> AgentProfileContext:
    return AgentProfileContext.model_validate(
        {
            "userId": 10001,
            "recentEvents": [
                {
                    "eventId": "event-paid-001",
                    "eventType": "PAYMENT_SUCCESS",
                    "skuId": 30001,
                    "spuId": 20001,
                    "categoryId": 201,
                    "orderId": 50001,
                    "amount": 918.0,
                }
            ],
            "eventTypeCounts": {"PAYMENT_SUCCESS": 1},
            "recentKeywords": ["keyboard"],
            "topCategories": [{"targetType": "CATEGORY", "targetId": 201, "count": 3}],
            "paidSignals": [
                {
                    "eventId": "event-paid-001",
                    "eventType": "PAYMENT_SUCCESS",
                    "skuId": 30001,
                    "spuId": 20001,
                    "categoryId": 201,
                    "preferenceConfirmed": True,
                    "fulfilled": True,
                    "complementTrigger": True,
                    "repeatRecommendationSuppressed": True,
                }
            ],
            "evidenceEventIds": ["event-paid-001"],
            "evidence": [
                {
                    "eventId": "event-paid-001",
                    "eventType": "PAYMENT_SUCCESS",
                    "reason": "fulfilled_need_and_complement_trigger",
                }
            ],
        }
    )


def valid_insight() -> dict:
    return {
        "summary": "用户已完成键盘购买，当前需求已满足，后续更适合看鼠标、腕托和桌垫等配套机会。",
        "demandStage": "FULFILLED",
        "preferenceTags": ["keyboard", "office"],
        "demandStates": [
            {
                "target": "keyboard purchase",
                "state": "FULFILLED",
                "reason": "PAYMENT_SUCCESS confirms this need.",
                "evidenceEventIds": ["event-paid-001"],
            }
        ],
        "fulfilledNeeds": [
            {
                "skuId": 30001,
                "spuId": 20001,
                "reason": "Paid order fulfilled the concrete keyboard need.",
                "evidenceEventIds": ["event-paid-001"],
            }
        ],
        "doNotRecommend": [
            {
                "skuId": 30001,
                "spuId": 20001,
                "reason": "fulfilled need, suppress repeat recommendation",
            }
        ],
        "complementOpportunities": [
            {
                "label": "wireless mouse",
                "category": "office peripherals",
                "reason": "Complement after keyboard purchase.",
                "source": "PAYMENT_SUCCESS",
                "evidenceEventIds": ["event-paid-001"],
                "score": 0.82,
            }
        ],
        "recommendationReasons": ["购买成功代表当前键盘需求已满足，下一步更适合补充配套外设。"],
        "riskChecks": {"recommendsFulfilledSku": False, "hasEvidence": True, "notes": []},
        "confidence": 0.86,
    }


def test_profile_manager_deepseek_disabled_keeps_rule_based_profile():
    result = ProfileManager().build_profile(sample_context())

    assert result.profile.profile["generationMode"] == GENERATION_RULE_BASED
    assert result.profile.profile["fulfilledNeeds"][0]["skuId"] == 30001
    assert result.profile.profile["doNotRecommend"][0]["skuId"] == 30001


def test_profile_manager_deepseek_success_keeps_java_compatible_response():
    deepseek_agent = DeepSeekRecommendationAgent(
        settings=enabled_settings(),
        client=StubDeepSeekClient(valid_insight()),
    )

    result = ProfileManager(deepseek_recommendation_agent=deepseek_agent).build_profile(sample_context())
    body = result.model_dump(by_alias=True, mode="json")

    assert body["workflowId"].startswith("workflow-")
    assert body["behaviorFactReport"]["artifactType"] == "BehaviorFactReport"
    assert body["intentReport"]["artifactType"] == "IntentReport"
    assert body["trendReport"]["artifactType"] == "TrendReport"
    assert body["profile"]["artifactType"] == "UserProfileVersion"
    assert body["profile"]["profile"]["generationMode"] == GENERATION_DEEPSEEK_ENHANCED
    assert body["profile"]["profile"]["doNotRecommend"][0]["skuId"] == 30001


def test_profile_manager_deepseek_failure_does_not_fail_build():
    deepseek_agent = DeepSeekRecommendationAgent(
        settings=enabled_settings(),
        client=StubDeepSeekClient(exception=DeepSeekRequestError("service unavailable")),
    )

    result = ProfileManager(deepseek_recommendation_agent=deepseek_agent).build_profile(sample_context())

    assert result.profile.profile["generationMode"] == GENERATION_FALLBACK_RULE_BASED
    assert result.profile.profile["fulfilledNeeds"][0]["repeatRecommendationSuppressed"] is True
    assert "service unavailable" in result.profile.profile["llmError"]


def test_profile_manager_unexpected_deepseek_failure_does_not_fail_build():
    deepseek_agent = DeepSeekRecommendationAgent(
        settings=enabled_settings(),
        client=StubDeepSeekClient(exception=RuntimeError("unexpected provider error")),
    )

    result = ProfileManager(deepseek_recommendation_agent=deepseek_agent).build_profile(sample_context())

    assert result.profile.profile["generationMode"] == GENERATION_FALLBACK_RULE_BASED
    assert result.profile.profile["fulfilledNeeds"][0]["skuId"] == 30001
    assert result.profile.profile["doNotRecommend"][0]["skuId"] == 30001
    assert result.profile.profile["complementOpportunities"]
    assert "RuntimeError" in result.profile.profile["llmError"]


def test_profile_manager_profile_does_not_expose_sensitive_terms():
    result = ProfileManager().build_profile(sample_context())
    profile_text = str(result.profile.profile).lower()

    assert "password" not in profile_text
    assert "jwt" not in profile_text
    assert "recipientphone" not in profile_text
