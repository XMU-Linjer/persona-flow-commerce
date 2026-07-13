from persona_agent_service.agents.complement_rules import complement_labels
from persona_agent_service.schemas.artifacts import (
    ComplementOpportunity,
    CurrentIntent,
    EvidenceRef,
    IntentReport,
    ScoreSet,
    payment_success_fulfilled_need,
)
from persona_agent_service.schemas.context import AgentProfileContext, ContextDemandSignal
from persona_agent_service.schemas.enums import DemandState


class IntentAgent:
    def build_report(self, context: AgentProfileContext, workflow_id: str) -> IntentReport:
        evidence_event_ids = context.all_evidence_event_ids
        demand_state = self._determine_state(context)
        evidence = self._evidence_refs(context)
        fulfilled_needs = self._fulfilled_needs(context)
        current_intents = self._current_intents(context, demand_state, evidence)
        complements = self._complement_opportunities(context, fulfilled_needs)

        return IntentReport(
            workflowId=workflow_id,
            userId=context.user_id,
            confidence=0.78 if evidence_event_ids else 0.2,
            evidenceEventIds=evidence_event_ids,
            currentIntents=current_intents,
            fulfilledNeeds=fulfilled_needs,
            complementOpportunities=complements,
        )

    def _determine_state(self, context: AgentProfileContext) -> DemandState:
        counts = context.event_type_counts
        if context.has_payment_success:
            return DemandState.FULFILLED
        if counts.get("CART_ADD", 0) > 0 or counts.get("ORDER_CREATED", 0) > 0 or context.cart_signals or context.order_signals:
            return DemandState.INTENT
        if counts.get("ORDER_CANCELED", 0) > 0 or counts.get("CART_REMOVE", 0) > 0 or context.canceled_signals:
            return DemandState.COOLING
        if counts.get("PRODUCT_VIEW", 0) + counts.get("PRODUCT_SEARCH", 0) > 2:
            return DemandState.INTERESTED
        if context.recent_events or context.recent_keywords:
            return DemandState.DISCOVERING
        return DemandState.DISCOVERING

    def _current_intents(
        self,
        context: AgentProfileContext,
        demand_state: DemandState,
        evidence: list[EvidenceRef],
    ) -> list[CurrentIntent]:
        if demand_state == DemandState.FULFILLED:
            return []
        label = context.recent_keywords[0] if context.recent_keywords else "explore adjacent categories"
        return [
            CurrentIntent(
                label=label,
                demandState=demand_state,
                scores=ScoreSet(
                    preferenceScore=0.4,
                    intentScore=0.75 if demand_state == DemandState.INTENT else 0.35,
                    fulfilledScore=0.0,
                    complementScore=0.3,
                ),
                evidence=evidence[:3],
            )
        ]

    def _fulfilled_needs(self, context: AgentProfileContext):
        signals = context.paid_signals or context.fulfilled_needs
        return [
            payment_success_fulfilled_need(
                event_id=signal.event_id or "payment-success",
                sku_id=signal.sku_id,
                spu_id=signal.spu_id,
                category_id=signal.category_id,
            )
            for signal in signals
        ]

    def _complement_opportunities(self, context: AgentProfileContext, fulfilled_needs):
        """Generate complement opportunities from all user behavior signals.

        Previous design required payment (fulfilled_needs != []) to trigger
        complements.  The revised approach derives recommendation keywords
        from any signal the user has demonstrated — search terms, viewed
        categories, cart adds, favorites — so that profiles begin building
        before the first purchase and are continuously enriched afterwards.
        """
        keywords = list(dict.fromkeys(context.recent_keywords))
        if fulfilled_needs:
            keywords.extend(
                str(need.category_id or "")
                for need in fulfilled_needs
                if need.category_id is not None
            )
        labels = complement_labels(keywords)
        if not labels:
            return []

        primary_evidence = context.evidence[:3] if context.evidence else []
        primary_evidence_refs = [
            EvidenceRef(
                eventId=ev.event_id or "context",
                eventType=ev.event_type or "INTEREST_SIGNAL",
                reason=ev.reason or "interest_signal",
            )
            for ev in primary_evidence
        ]
        if not primary_evidence_refs and context.recent_events:
            for event in context.recent_events[:3]:
                primary_evidence_refs.append(
                    EvidenceRef(
                        eventId=event.event_id or "context",
                        eventType=event.event_type or "INTEREST_SIGNAL",
                        reason="recent_behavior",
                    )
                )

        first_related_sku = (
            fulfilled_needs[0].sku_id
            if fulfilled_needs and fulfilled_needs[0].sku_id is not None
            else next(
                (
                    event.sku_id
                    for event in context.recent_events
                    if event.sku_id is not None
                ),
                None,
            )
        )

        return [
            ComplementOpportunity(
                label=label,
                relatedFulfilledSkuId=first_related_sku,
                complementScore=0.68,
                evidence=primary_evidence_refs,
            )
            for label in labels
        ]

    def _evidence_refs(self, context: AgentProfileContext) -> list[EvidenceRef]:
        refs: list[EvidenceRef] = []
        for evidence in context.evidence:
            if evidence.event_id and evidence.event_type:
                refs.append(EvidenceRef(eventId=evidence.event_id, eventType=evidence.event_type, reason=evidence.reason or "context"))
        if refs:
            return refs
        for event in context.recent_events[:5]:
            if event.event_id and event.event_type:
                refs.append(EvidenceRef(eventId=event.event_id, eventType=event.event_type, reason="recent_event"))
        for signal in context.paid_signals:
            refs.append(self._signal_ref(signal))
        return refs

    def _signal_ref(self, signal: ContextDemandSignal) -> EvidenceRef:
        return EvidenceRef(
            eventId=signal.event_id or "signal",
            eventType=signal.event_type or "UNKNOWN",
            reason="demand_signal",
        )
