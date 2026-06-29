from persona_agent_service.schemas.artifacts import BehaviorFact, BehaviorFactReport
from persona_agent_service.schemas.context import AgentProfileContext


class BehaviorAgent:
    def build_report(self, context: AgentProfileContext, workflow_id: str) -> BehaviorFactReport:
        facts = [
            BehaviorFact(
                eventId=event.event_id or f"event-{index}",
                eventType=event.event_type or "UNKNOWN",
                skuId=event.sku_id,
                spuId=event.spu_id,
                categoryId=event.category_id,
                keyword=event.keyword,
            )
            for index, event in enumerate(context.recent_events)
        ]

        counts = dict(context.event_type_counts)
        if not counts:
            for event in context.recent_events:
                if event.event_type:
                    counts[event.event_type] = counts.get(event.event_type, 0) + 1

        evidence_event_ids = context.all_evidence_event_ids
        return BehaviorFactReport(
            workflowId=workflow_id,
            userId=context.user_id,
            confidence=0.75 if evidence_event_ids else 0.25,
            evidenceEventIds=evidence_event_ids,
            facts=facts,
            recentKeywords=_distinct_non_empty(context.recent_keywords),
            eventTypeCounts=counts,
            topCategories=[category.model_dump(by_alias=True, mode="json") for category in context.top_categories],
            paidSignals=[signal.model_dump(by_alias=True, mode="json") for signal in context.paid_signals],
            fulfilledNeeds=[need.model_dump(by_alias=True, mode="json") for need in context.fulfilled_needs],
        )


def _distinct_non_empty(values: list[str]) -> list[str]:
    cleaned = [value.strip() for value in values if value and value.strip()]
    return list(dict.fromkeys(cleaned))
