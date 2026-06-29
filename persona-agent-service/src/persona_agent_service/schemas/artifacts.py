from datetime import datetime, timezone
from typing import Any
from uuid import uuid4

from pydantic import BaseModel, ConfigDict, Field

from persona_agent_service.schemas.enums import ArtifactType, DemandState


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


class EvidenceRef(BaseModel):
    event_id: str = Field(alias="eventId")
    event_type: str = Field(alias="eventType")
    reason: str

    model_config = ConfigDict(populate_by_name=True)


class ScoreSet(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    preference_score: float = Field(default=0.0, ge=0.0, le=1.0, alias="preferenceScore")
    intent_score: float = Field(default=0.0, ge=0.0, le=1.0, alias="intentScore")
    fulfilled_score: float = Field(default=0.0, ge=0.0, le=1.0, alias="fulfilledScore")
    complement_score: float = Field(default=0.0, ge=0.0, le=1.0, alias="complementScore")


class ArtifactBase(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    artifact_id: str = Field(default_factory=lambda: str(uuid4()), alias="artifactId")
    workflow_id: str = Field(alias="workflowId")
    user_id: int = Field(alias="userId")
    artifact_type: ArtifactType = Field(alias="artifactType")
    created_at: datetime = Field(default_factory=utc_now, alias="createdAt")
    confidence: float = Field(default=0.0, ge=0.0, le=1.0)
    evidence_event_ids: list[str] = Field(default_factory=list, alias="evidenceEventIds")


class BehaviorFact(BaseModel):
    event_id: str = Field(alias="eventId")
    event_type: str = Field(alias="eventType")
    sku_id: int | None = Field(default=None, alias="skuId")
    spu_id: int | None = Field(default=None, alias="spuId")
    category_id: int | None = Field(default=None, alias="categoryId")
    keyword: str | None = None

    model_config = ConfigDict(populate_by_name=True)


class BehaviorFactReport(ArtifactBase):
    artifact_type: ArtifactType = Field(default=ArtifactType.BEHAVIOR_FACT_REPORT, alias="artifactType")
    facts: list[BehaviorFact] = Field(default_factory=list)


class FulfilledNeed(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    sku_id: int | None = Field(default=None, alias="skuId")
    spu_id: int | None = Field(default=None, alias="spuId")
    category_id: int | None = Field(default=None, alias="categoryId")
    demand_state: DemandState = Field(default=DemandState.FULFILLED, alias="demandState")
    preference_confirmed: bool = Field(default=True, alias="preferenceConfirmed")
    fulfilled: bool = True
    complement_trigger: bool = Field(default=True, alias="complementTrigger")
    repeat_recommendation_suppressed: bool = Field(default=True, alias="repeatRecommendationSuppressed")
    scores: ScoreSet = Field(default_factory=lambda: ScoreSet(
        preferenceScore=0.8,
        intentScore=0.2,
        fulfilledScore=0.9,
        complementScore=0.7,
    ))
    evidence: list[EvidenceRef] = Field(default_factory=list)


class CurrentIntent(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    label: str
    demand_state: DemandState = Field(alias="demandState")
    scores: ScoreSet
    evidence: list[EvidenceRef] = Field(default_factory=list)


class ComplementOpportunity(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    label: str
    related_fulfilled_sku_id: int | None = Field(default=None, alias="relatedFulfilledSkuId")
    complement_score: float = Field(default=0.0, ge=0.0, le=1.0, alias="complementScore")
    evidence: list[EvidenceRef] = Field(default_factory=list)


class IntentReport(ArtifactBase):
    artifact_type: ArtifactType = Field(default=ArtifactType.INTENT_REPORT, alias="artifactType")
    current_intents: list[CurrentIntent] = Field(default_factory=list, alias="currentIntents")
    fulfilled_needs: list[FulfilledNeed] = Field(default_factory=list, alias="fulfilledNeeds")
    complement_opportunities: list[ComplementOpportunity] = Field(
        default_factory=list,
        alias="complementOpportunities",
    )


class TrendSignal(BaseModel):
    label: str
    trend: str
    score: float = Field(default=0.0, ge=0.0, le=1.0)
    evidence: list[EvidenceRef] = Field(default_factory=list)


class TrendReport(ArtifactBase):
    artifact_type: ArtifactType = Field(default=ArtifactType.TREND_REPORT, alias="artifactType")
    rising: list[TrendSignal] = Field(default_factory=list)
    declining: list[TrendSignal] = Field(default_factory=list)
    noise: list[TrendSignal] = Field(default_factory=list)


class ProfileDraft(ArtifactBase):
    artifact_type: ArtifactType = Field(default=ArtifactType.PROFILE_DRAFT, alias="artifactType")
    summary: str
    preference_tags: list[str] = Field(default_factory=list, alias="preferenceTags")
    current_intents: list[CurrentIntent] = Field(default_factory=list, alias="currentIntents")
    fulfilled_needs: list[FulfilledNeed] = Field(default_factory=list, alias="fulfilledNeeds")
    complement_opportunities: list[ComplementOpportunity] = Field(
        default_factory=list,
        alias="complementOpportunities",
    )


class ProfileAuditReport(ArtifactBase):
    artifact_type: ArtifactType = Field(default=ArtifactType.PROFILE_AUDIT_REPORT, alias="artifactType")
    passed: bool
    issues: list[str] = Field(default_factory=list)
    self_check_result: str = Field(alias="selfCheckResult")


class UserProfileVersion(ArtifactBase):
    artifact_type: ArtifactType = Field(default=ArtifactType.USER_PROFILE_VERSION, alias="artifactType")
    version_no: int = Field(alias="versionNo")
    summary: str
    profile: dict[str, Any] = Field(default_factory=dict)


def payment_success_fulfilled_need(
    event_id: str,
    sku_id: int | None = None,
    spu_id: int | None = None,
    category_id: int | None = None,
) -> FulfilledNeed:
    evidence = EvidenceRef(
        eventId=event_id,
        eventType="PAYMENT_SUCCESS",
        reason="preference_confirmed_need_fulfilled_complement_trigger",
    )
    return FulfilledNeed(
        skuId=sku_id,
        spuId=spu_id,
        categoryId=category_id,
        evidence=[evidence],
    )
