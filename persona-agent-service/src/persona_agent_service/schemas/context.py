from datetime import datetime
from typing import Any

from pydantic import BaseModel, ConfigDict, Field


class ContextBehaviorEvent(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    event_id: str | None = Field(default=None, alias="eventId")
    event_type: str | None = Field(default=None, alias="eventType")
    source_module: str | None = Field(default=None, alias="sourceModule")
    object_type: str | None = Field(default=None, alias="objectType")
    object_id: int | None = Field(default=None, alias="objectId")
    keyword: str | None = None
    sku_id: int | None = Field(default=None, alias="skuId")
    spu_id: int | None = Field(default=None, alias="spuId")
    category_id: int | None = Field(default=None, alias="categoryId")
    order_id: int | None = Field(default=None, alias="orderId")
    amount: float | None = None
    payload_json: str | None = Field(default=None, alias="payloadJson")
    occurred_at: datetime | None = Field(default=None, alias="occurredAt")


class ContextTargetSummary(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    target_type: str | None = Field(default=None, alias="targetType")
    target_id: int | None = Field(default=None, alias="targetId")
    count: int = 0
    last_occurred_at: datetime | None = Field(default=None, alias="lastOccurredAt")


class ContextDemandSignal(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    event_id: str | None = Field(default=None, alias="eventId")
    event_type: str | None = Field(default=None, alias="eventType")
    sku_id: int | None = Field(default=None, alias="skuId")
    spu_id: int | None = Field(default=None, alias="spuId")
    category_id: int | None = Field(default=None, alias="categoryId")
    order_id: int | None = Field(default=None, alias="orderId")
    amount: float | None = None
    preference_confirmed: bool = Field(default=False, alias="preferenceConfirmed")
    fulfilled: bool = False
    complement_trigger: bool = Field(default=False, alias="complementTrigger")
    repeat_recommendation_suppressed: bool = Field(default=False, alias="repeatRecommendationSuppressed")
    occurred_at: datetime | None = Field(default=None, alias="occurredAt")


class ContextEvidence(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    event_id: str | None = Field(default=None, alias="eventId")
    event_type: str | None = Field(default=None, alias="eventType")
    reason: str | None = None
    occurred_at: datetime | None = Field(default=None, alias="occurredAt")


class AgentProfileContext(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    user_id: int = Field(alias="userId")
    recent_events: list[ContextBehaviorEvent] = Field(default_factory=list, alias="recentEvents")
    event_type_counts: dict[str, int] = Field(default_factory=dict, alias="eventTypeCounts")
    recent_keywords: list[str] = Field(default_factory=list, alias="recentKeywords")
    top_categories: list[ContextTargetSummary] = Field(default_factory=list, alias="topCategories")
    viewed_products: list[ContextTargetSummary] = Field(default_factory=list, alias="viewedProducts")
    cart_signals: list[ContextDemandSignal] = Field(default_factory=list, alias="cartSignals")
    order_signals: list[ContextDemandSignal] = Field(default_factory=list, alias="orderSignals")
    paid_signals: list[ContextDemandSignal] = Field(default_factory=list, alias="paidSignals")
    canceled_signals: list[ContextDemandSignal] = Field(default_factory=list, alias="canceledSignals")
    fulfilled_needs: list[ContextDemandSignal] = Field(default_factory=list, alias="fulfilledNeeds")
    evidence_event_ids: list[str] = Field(default_factory=list, alias="evidenceEventIds")
    evidence: list[ContextEvidence] = Field(default_factory=list)
    generated_at: datetime | None = Field(default=None, alias="generatedAt")

    @property
    def has_payment_success(self) -> bool:
        return self.event_type_counts.get("PAYMENT_SUCCESS", 0) > 0 or any(
            signal.event_type == "PAYMENT_SUCCESS" for signal in self.paid_signals
        )

    @property
    def all_evidence_event_ids(self) -> list[str]:
        event_ids = [event_id for event_id in self.evidence_event_ids if event_id]
        for event in self.recent_events:
            if event.event_id:
                event_ids.append(event.event_id)
        for signal in (
            self.cart_signals
            + self.order_signals
            + self.paid_signals
            + self.canceled_signals
            + self.fulfilled_needs
        ):
            if signal.event_id:
                event_ids.append(signal.event_id)
        return list(dict.fromkeys(event_ids))

    def model_dump_public(self) -> dict[str, Any]:
        return self.model_dump(by_alias=True, mode="json")
