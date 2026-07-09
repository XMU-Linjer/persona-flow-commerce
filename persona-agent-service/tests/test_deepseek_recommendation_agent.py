from persona_agent_service.agents.behavior_agent import BehaviorAgent
from persona_agent_service.agents.deepseek_recommendation_agent import (
    GENERATION_DEEPSEEK_ENHANCED,
    GENERATION_FALLBACK_RULE_BASED,
    GENERATION_RULE_BASED,
    DeepSeekRecommendationAgent,
)
from persona_agent_service.agents.intent_agent import IntentAgent
from persona_agent_service.agents.profile_builder_critic import ProfileBuilderCritic
from persona_agent_service.agents.trend_agent import TrendAgent
from persona_agent_service.config.settings import AgentSettings
from persona_agent_service.llm.exceptions import DeepSeekRequestError
from persona_agent_service.schemas.context import AgentProfileContext


class StubDeepSeekClient:
    def __init__(self, response=None, exception: Exception | None = None):
        self.response = response
        self.exception = exception
        self.called = False

    def complete_json(self, system_prompt: str, user_prompt: str):
        self.called = True
        assert "只输出合法 JSON" in system_prompt
        assert "allowedEventIds" in user_prompt
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


def baseline(context: AgentProfileContext):
    workflow_id = "workflow-001"
    behavior = BehaviorAgent().build_report(context, workflow_id)
    intent = IntentAgent().build_report(context, workflow_id)
    trend = TrendAgent().build_report(context, workflow_id)
    draft, audit, profile = ProfileBuilderCritic().build(context, workflow_id, behavior, intent, trend)
    return behavior, intent, trend, profile


def test_deepseek_disabled_does_not_call_client():
    context = sample_context()
    behavior, intent, trend, profile = baseline(context)
    client = StubDeepSeekClient(response=valid_insight())
    agent = DeepSeekRecommendationAgent(
        settings=AgentSettings(deepseek_enabled=False, deepseek_api_key="test-api-key"),
        client=client,
    )

    result = agent.enhance_profile(context, behavior, intent, trend, profile)

    assert client.called is False
    assert result.profile["generationMode"] == GENERATION_RULE_BASED
    assert result.profile["fulfilledNeeds"][0]["skuId"] == 30001


def test_missing_api_key_does_not_call_client():
    context = sample_context()
    behavior, intent, trend, profile = baseline(context)
    client = StubDeepSeekClient(response=valid_insight())
    agent = DeepSeekRecommendationAgent(
        settings=AgentSettings(deepseek_enabled=True, deepseek_api_key=""),
        client=client,
    )

    result = agent.enhance_profile(context, behavior, intent, trend, profile)

    assert client.called is False
    assert result.profile["generationMode"] == GENERATION_RULE_BASED


def test_deepseek_success_returns_enhanced_profile():
    context = sample_context()
    behavior, intent, trend, profile = baseline(context)
    agent = DeepSeekRecommendationAgent(settings=enabled_settings(), client=StubDeepSeekClient(valid_insight()))

    result = agent.enhance_profile(context, behavior, intent, trend, profile)

    assert result.profile["generationMode"] == GENERATION_DEEPSEEK_ENHANCED
    assert result.profile["llmProvider"] == "deepseek"
    assert result.profile["llmModel"] == "deepseek-test"
    assert result.profile["fulfilledNeeds"][0]["skuId"] == 30001
    assert result.profile["doNotRecommend"][0]["skuId"] == 30001
    assert result.profile["complementOpportunities"][0]["evidenceEventIds"] == ["event-paid-001"]
    assert "wireless mouse" in result.profile["complementOpportunities"][0]["label"]


def test_deepseek_failure_falls_back_to_rule_based_profile():
    context = sample_context()
    behavior, intent, trend, profile = baseline(context)
    agent = DeepSeekRecommendationAgent(
        settings=enabled_settings(),
        client=StubDeepSeekClient(exception=DeepSeekRequestError("boom")),
    )

    result = agent.enhance_profile(context, behavior, intent, trend, profile)

    assert result.profile["generationMode"] == GENERATION_FALLBACK_RULE_BASED
    assert result.profile["profileSummary"] == profile.profile["profileSummary"]
    assert result.profile["fulfilledNeeds"][0]["repeatRecommendationSuppressed"] is True
    assert "boom" in result.profile["llmError"]


def test_deepseek_output_recommending_fulfilled_sku_is_blocked_by_critic():
    insight = valid_insight()
    insight["riskChecks"]["recommendsFulfilledSku"] = True
    context = sample_context()
    behavior, intent, trend, profile = baseline(context)
    agent = DeepSeekRecommendationAgent(settings=enabled_settings(), client=StubDeepSeekClient(insight))

    result = agent.enhance_profile(context, behavior, intent, trend, profile)

    assert result.profile["generationMode"] == GENERATION_FALLBACK_RULE_BASED
    assert "recommends a fulfilled SKU" in result.profile["llmError"]


def test_deepseek_output_without_complement_evidence_is_blocked_by_critic():
    insight = valid_insight()
    insight["complementOpportunities"][0]["evidenceEventIds"] = []
    context = sample_context()
    behavior, intent, trend, profile = baseline(context)
    agent = DeepSeekRecommendationAgent(settings=enabled_settings(), client=StubDeepSeekClient(insight))

    result = agent.enhance_profile(context, behavior, intent, trend, profile)

    assert result.profile["generationMode"] == GENERATION_FALLBACK_RULE_BASED
    assert "missing evidenceEventIds" in result.profile["llmError"]
