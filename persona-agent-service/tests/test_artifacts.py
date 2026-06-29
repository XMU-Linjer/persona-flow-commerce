from persona_agent_service.schemas.artifacts import (
    BehaviorFact,
    BehaviorFactReport,
    ComplementOpportunity,
    CurrentIntent,
    EvidenceRef,
    IntentReport,
    ProfileAuditReport,
    ProfileDraft,
    ScoreSet,
    TrendReport,
    TrendSignal,
    UserProfileVersion,
    payment_success_fulfilled_need,
)
from persona_agent_service.schemas.enums import ArtifactType, DemandState


def test_behavior_fact_report_keeps_evidence_event_ids():
    report = BehaviorFactReport(
        workflowId="workflow-001",
        userId=10001,
        confidence=0.8,
        evidenceEventIds=["event-view-001"],
        facts=[
            BehaviorFact(
                eventId="event-view-001",
                eventType="PRODUCT_VIEW",
                skuId=30001,
                spuId=20001,
                categoryId=201,
            )
        ],
    )

    assert report.artifact_type == ArtifactType.BEHAVIOR_FACT_REPORT
    assert report.evidence_event_ids == ["event-view-001"]
    assert report.facts[0].event_type == "PRODUCT_VIEW"


def test_intent_report_can_express_fulfilled_payment_success():
    fulfilled_need = payment_success_fulfilled_need(
        event_id="event-payment-001",
        sku_id=30001,
        spu_id=20001,
        category_id=201,
    )
    report = IntentReport(
        workflowId="workflow-001",
        userId=10001,
        confidence=0.85,
        evidenceEventIds=["event-payment-001"],
        fulfilledNeeds=[fulfilled_need],
        complementOpportunities=[
            ComplementOpportunity(
                label="desk accessories",
                relatedFulfilledSkuId=30001,
                complementScore=0.76,
                evidence=fulfilled_need.evidence,
            )
        ],
    )

    assert report.fulfilled_needs[0].demand_state == DemandState.FULFILLED
    assert report.fulfilled_needs[0].preference_confirmed is True
    assert report.fulfilled_needs[0].fulfilled is True
    assert report.fulfilled_needs[0].complement_trigger is True
    assert report.fulfilled_needs[0].repeat_recommendation_suppressed is True
    assert report.complement_opportunities[0].related_fulfilled_sku_id == 30001


def test_trend_report_can_express_rising_declining_and_noise():
    report = TrendReport(
        workflowId="workflow-001",
        userId=10001,
        confidence=0.7,
        rising=[TrendSignal(label="keyboard accessories", trend="rising", score=0.8)],
        declining=[TrendSignal(label="same keyboard sku", trend="declining", score=0.6)],
        noise=[TrendSignal(label="one-off browse", trend="noise", score=0.2)],
    )

    assert report.rising[0].trend == "rising"
    assert report.declining[0].trend == "declining"
    assert report.noise[0].trend == "noise"


def test_profile_draft_contains_fulfilled_needs_and_complement_opportunities():
    evidence = EvidenceRef(
        eventId="event-payment-001",
        eventType="PAYMENT_SUCCESS",
        reason="preference_confirmed_need_fulfilled_complement_trigger",
    )
    current_intent = CurrentIntent(
        label="keyboard accessories",
        demandState=DemandState.INTENT,
        scores=ScoreSet(preferenceScore=0.6, intentScore=0.7, fulfilledScore=0.1, complementScore=0.8),
        evidence=[evidence],
    )
    fulfilled_need = payment_success_fulfilled_need("event-payment-001", sku_id=30001)
    draft = ProfileDraft(
        workflowId="workflow-001",
        userId=10001,
        confidence=0.82,
        evidenceEventIds=["event-payment-001"],
        summary="User bought a keyboard; recommend complements, not the same SKU.",
        preferenceTags=["office gear"],
        currentIntents=[current_intent],
        fulfilledNeeds=[fulfilled_need],
        complementOpportunities=[
            ComplementOpportunity(
                label="wrist rest",
                relatedFulfilledSkuId=30001,
                complementScore=0.79,
                evidence=[evidence],
            )
        ],
    )

    assert draft.fulfilled_needs[0].repeat_recommendation_suppressed is True
    assert draft.complement_opportunities[0].label == "wrist rest"
    assert "not the same SKU" in draft.summary


def test_profile_audit_report_and_user_profile_version_are_structured():
    audit = ProfileAuditReport(
        workflowId="workflow-001",
        userId=10001,
        confidence=0.9,
        passed=True,
        selfCheckResult="No contradiction found.",
    )
    version = UserProfileVersion(
        workflowId="workflow-001",
        userId=10001,
        confidence=0.88,
        versionNo=1,
        summary="Structured profile skeleton.",
        profile={"fulfilledNeeds": [{"skuId": 30001}]},
    )

    assert audit.passed is True
    assert version.version_no == 1
    assert version.profile["fulfilledNeeds"][0]["skuId"] == 30001
