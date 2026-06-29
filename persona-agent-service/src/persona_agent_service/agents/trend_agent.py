from persona_agent_service.schemas.artifacts import EvidenceRef, TrendReport, TrendSignal
from persona_agent_service.schemas.context import AgentProfileContext


class TrendAgent:
    def build_report(self, context: AgentProfileContext, workflow_id: str) -> TrendReport:
        evidence = self._evidence_refs(context)
        stable = [
            TrendSignal(label=f"category:{category.target_id}", trend="stable", score=min(category.count / 5, 1.0), evidence=evidence[:2])
            for category in context.top_categories
            if category.target_id is not None and category.count >= 2
        ]
        rising = [
            TrendSignal(label=keyword, trend="rising", score=0.65, evidence=evidence[:2])
            for keyword in context.recent_keywords[:5]
        ]
        if context.cart_signals or context.order_signals:
            rising.append(TrendSignal(label="purchase intent", trend="rising", score=0.78, evidence=evidence[:3]))

        declining = []
        if context.canceled_signals or context.event_type_counts.get("ORDER_CANCELED", 0) > 0:
            declining.append(TrendSignal(label="current purchase path", trend="declining", score=0.62, evidence=evidence[:3]))

        burst = []
        if context.event_type_counts.get("CART_ADD", 0) + context.event_type_counts.get("ORDER_CREATED", 0) >= 2:
            burst.append(TrendSignal(label="short term buying intent", trend="burst", score=0.74, evidence=evidence[:3]))

        noise = []
        if not context.recent_events and not context.recent_keywords:
            noise.append(TrendSignal(label="empty context", trend="noise", score=0.2, evidence=[]))
        elif context.event_type_counts.get("PRODUCT_VIEW", 0) <= 1 and not context.cart_signals and not context.paid_signals:
            noise.append(TrendSignal(label="weak browse signal", trend="noise", score=0.3, evidence=evidence[:1]))

        return TrendReport(
            workflowId=workflow_id,
            userId=context.user_id,
            confidence=0.7 if context.all_evidence_event_ids else 0.25,
            evidenceEventIds=context.all_evidence_event_ids,
            rising=rising,
            declining=declining,
            noise=noise,
            stableInterests=stable,
            risingInterests=rising,
            decliningInterests=declining,
            burstIntent=burst,
            noiseEvents=noise,
        )

    def _evidence_refs(self, context: AgentProfileContext) -> list[EvidenceRef]:
        refs = [
            EvidenceRef(eventId=evidence.event_id, eventType=evidence.event_type, reason=evidence.reason or "context")
            for evidence in context.evidence
            if evidence.event_id and evidence.event_type
        ]
        if refs:
            return refs
        return [
            EvidenceRef(eventId=event.event_id, eventType=event.event_type, reason="recent_event")
            for event in context.recent_events
            if event.event_id and event.event_type
        ]
