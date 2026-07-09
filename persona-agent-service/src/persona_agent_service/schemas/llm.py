from pydantic import BaseModel, ConfigDict, Field

from persona_agent_service.schemas.enums import DemandState


class DeepSeekDemandState(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    target: str
    state: DemandState
    reason: str
    evidence_event_ids: list[str] = Field(default_factory=list, alias="evidenceEventIds")


class DeepSeekFulfilledNeed(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    sku_id: int | None = Field(default=None, alias="skuId")
    spu_id: int | None = Field(default=None, alias="spuId")
    reason: str
    evidence_event_ids: list[str] = Field(default_factory=list, alias="evidenceEventIds")


class DeepSeekDoNotRecommendItem(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    sku_id: int | None = Field(default=None, alias="skuId")
    spu_id: int | None = Field(default=None, alias="spuId")
    reason: str


class DeepSeekComplementOpportunity(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    label: str
    category: str | None = None
    reason: str
    source: str
    evidence_event_ids: list[str] = Field(default_factory=list, alias="evidenceEventIds")
    score: float = Field(default=0.0, ge=0.0, le=1.0)


class DeepSeekRiskChecks(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    recommends_fulfilled_sku: bool = Field(default=False, alias="recommendsFulfilledSku")
    has_evidence: bool = Field(default=False, alias="hasEvidence")
    notes: list[str] = Field(default_factory=list)


class DeepSeekRecommendationInsight(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    summary: str
    demand_stage: DemandState = Field(alias="demandStage")
    preference_tags: list[str] = Field(default_factory=list, alias="preferenceTags")
    demand_states: list[DeepSeekDemandState] = Field(default_factory=list, alias="demandStates")
    fulfilled_needs: list[DeepSeekFulfilledNeed] = Field(default_factory=list, alias="fulfilledNeeds")
    do_not_recommend: list[DeepSeekDoNotRecommendItem] = Field(default_factory=list, alias="doNotRecommend")
    complement_opportunities: list[DeepSeekComplementOpportunity] = Field(
        default_factory=list,
        alias="complementOpportunities",
    )
    recommendation_reasons: list[str] = Field(default_factory=list, alias="recommendationReasons")
    risk_checks: DeepSeekRiskChecks = Field(default_factory=DeepSeekRiskChecks, alias="riskChecks")
    confidence: float = Field(default=0.0, ge=0.0, le=1.0)

