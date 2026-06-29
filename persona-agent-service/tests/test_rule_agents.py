from persona_agent_service.agents.behavior_agent import BehaviorAgent
from persona_agent_service.agents.intent_agent import IntentAgent
from persona_agent_service.agents.profile_builder_critic import ProfileBuilderCritic
from persona_agent_service.agents.profile_manager import ProfileManager
from persona_agent_service.agents.trend_agent import TrendAgent
from persona_agent_service.schemas.context import AgentProfileContext
from persona_agent_service.schemas.enums import DemandState


def rich_context() -> AgentProfileContext:
    return AgentProfileContext.model_validate(
        {
            "userId": 10001,
            "recentEvents": [
                {
                    "eventId": "event-search-001",
                    "eventType": "PRODUCT_SEARCH",
                    "keyword": "keyboard",
                    "categoryId": 201,
                    "occurredAt": "2026-06-29T10:00:00Z",
                },
                {
                    "eventId": "event-view-001",
                    "eventType": "PRODUCT_VIEW",
                    "skuId": 30001,
                    "spuId": 20001,
                    "categoryId": 201,
                    "occurredAt": "2026-06-29T10:01:00Z",
                },
                {
                    "eventId": "event-cart-001",
                    "eventType": "CART_ADD",
                    "skuId": 30001,
                    "spuId": 20001,
                    "categoryId": 201,
                    "occurredAt": "2026-06-29T10:02:00Z",
                },
                {
                    "eventId": "event-paid-001",
                    "eventType": "PAYMENT_SUCCESS",
                    "skuId": 30001,
                    "spuId": 20001,
                    "categoryId": 201,
                    "orderId": 50001,
                    "amount": 918.00,
                    "occurredAt": "2026-06-29T10:03:00Z",
                },
                {
                    "eventId": "event-cancel-001",
                    "eventType": "ORDER_CANCELED",
                    "skuId": 30002,
                    "spuId": 20002,
                    "categoryId": 202,
                    "orderId": 50002,
                    "occurredAt": "2026-06-29T10:04:00Z",
                },
            ],
            "eventTypeCounts": {
                "PRODUCT_SEARCH": 1,
                "PRODUCT_VIEW": 1,
                "CART_ADD": 1,
                "PAYMENT_SUCCESS": 1,
                "ORDER_CANCELED": 1,
            },
            "recentKeywords": ["keyboard"],
            "topCategories": [{"targetType": "CATEGORY", "targetId": 201, "count": 4}],
            "viewedProducts": [{"targetType": "PRODUCT", "targetId": 20001, "count": 1}],
            "cartSignals": [{"eventId": "event-cart-001", "eventType": "CART_ADD", "skuId": 30001}],
            "orderSignals": [],
            "paidSignals": [
                {
                    "eventId": "event-paid-001",
                    "eventType": "PAYMENT_SUCCESS",
                    "skuId": 30001,
                    "spuId": 20001,
                    "categoryId": 201,
                    "orderId": 50001,
                    "amount": 918.00,
                    "preferenceConfirmed": True,
                    "fulfilled": True,
                    "complementTrigger": True,
                    "repeatRecommendationSuppressed": True,
                }
            ],
            "canceledSignals": [{"eventId": "event-cancel-001", "eventType": "ORDER_CANCELED"}],
            "fulfilledNeeds": [],
            "evidenceEventIds": ["event-search-001", "event-paid-001"],
            "evidence": [
                {
                    "eventId": "event-paid-001",
                    "eventType": "PAYMENT_SUCCESS",
                    "reason": "fulfilled_need_and_complement_trigger",
                }
            ],
            "generatedAt": "2026-06-29T10:05:00Z",
        }
    )


def test_behavior_agent_extracts_behavior_facts():
    report = BehaviorAgent().build_report(rich_context(), "workflow-001")

    assert report.facts
    assert report.recent_keywords == ["keyboard"]
    assert report.event_type_counts["PAYMENT_SUCCESS"] == 1
    assert report.top_categories[0]["targetId"] == 201
    assert report.paid_signals[0]["eventId"] == "event-paid-001"
    assert "event-paid-001" in report.evidence_event_ids


def test_intent_agent_recognizes_fulfilled_and_does_not_continue_same_sku():
    report = IntentAgent().build_report(rich_context(), "workflow-001")

    assert report.fulfilled_needs
    fulfilled = report.fulfilled_needs[0]
    assert fulfilled.demand_state == DemandState.FULFILLED
    assert fulfilled.preference_confirmed is True
    assert fulfilled.fulfilled is True
    assert fulfilled.complement_trigger is True
    assert fulfilled.repeat_recommendation_suppressed is True
    assert report.current_intents == []
    assert report.complement_opportunities
    assert not any(opportunity.label.lower().startswith("same sku") for opportunity in report.complement_opportunities)


def test_trend_agent_outputs_rising_declining_and_noise_fields():
    report = TrendAgent().build_report(rich_context(), "workflow-001")

    assert report.rising_interests
    assert report.declining_interests
    assert report.stable_interests
    assert report.rising[0].trend == "rising"

    empty_report = TrendAgent().build_report(
        AgentProfileContext.model_validate({"userId": 10001}),
        "workflow-empty",
    )
    assert empty_report.noise_events


def test_profile_builder_outputs_summary_complements_do_not_recommend_and_audit():
    context = rich_context()
    behavior = BehaviorAgent().build_report(context, "workflow-001")
    intent = IntentAgent().build_report(context, "workflow-001")
    trend = TrendAgent().build_report(context, "workflow-001")

    draft, audit, version = ProfileBuilderCritic().build(context, "workflow-001", behavior, intent, trend)

    assert "Fulfilled purchase detected" in draft.summary
    assert draft.complement_opportunities
    assert draft.do_not_recommend[0]["skuId"] == 30001
    assert draft.audit_result == "passed"
    assert audit.passed is True
    assert version.profile["profileSummary"] == draft.summary
    assert version.profile["doNotRecommend"][0]["skuId"] == 30001
    assert version.profile["fulfilledNeeds"][0]["repeatRecommendationSuppressed"] is True


def test_profile_builder_checks_missing_evidence_ids():
    context = AgentProfileContext.model_validate({"userId": 10001})
    result = ProfileManager().build_profile(context)

    assert result.profile.profile["profileSummary"].startswith("No sufficient behavior evidence")
    assert result.audit_report.passed is False
    assert "missing evidenceEventIds" in result.audit_report.issues


def test_profile_manager_returns_complete_rule_based_workflow_result():
    result = ProfileManager().build_profile(rich_context())

    assert result.workflow_id.startswith("workflow-")
    assert result.behavior_fact_report.artifact_type == "BehaviorFactReport"
    assert result.intent_report.artifact_type == "IntentReport"
    assert result.trend_report.artifact_type == "TrendReport"
    assert result.profile.artifact_type == "UserProfileVersion"
    assert result.profile.profile["complementOpportunities"]
