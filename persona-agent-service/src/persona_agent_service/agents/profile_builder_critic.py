from persona_agent_service.agents.complement_rules import complement_labels
from persona_agent_service.schemas.artifacts import (
    BehaviorFactReport,
    ComplementOpportunity,
    IntentReport,
    ProfileAuditReport,
    ProfileDraft,
    TrendReport,
    UserProfileVersion,
)
from persona_agent_service.schemas.context import AgentProfileContext
from persona_agent_service.schemas.enums import DemandState


class ProfileBuilderCritic:
    def build(
        self,
        context: AgentProfileContext,
        workflow_id: str,
        behavior_report: BehaviorFactReport,
        intent_report: IntentReport,
        trend_report: TrendReport,
    ) -> tuple[ProfileDraft, ProfileAuditReport, UserProfileVersion]:
        evidence_event_ids = list(dict.fromkeys(
            behavior_report.evidence_event_ids
            + intent_report.evidence_event_ids
            + trend_report.evidence_event_ids
        ))
        preferred_categories = [str(category.get("targetId")) for category in behavior_report.top_categories if category.get("targetId")]
        preference_tags = self._preference_tags(context, preferred_categories)
        complement_opportunities = self._complements(context, intent_report)
        do_not_recommend = self._do_not_recommend(intent_report)
        demand_states = self._demand_states(intent_report, context)
        summary = self._summary(context, demand_states, complement_opportunities)

        draft = ProfileDraft(
            workflowId=workflow_id,
            userId=context.user_id,
            confidence=0.82 if evidence_event_ids else 0.2,
            evidenceEventIds=evidence_event_ids,
            summary=summary,
            preferenceTags=preference_tags,
            preferredCategories=preferred_categories,
            currentIntents=intent_report.current_intents,
            fulfilledNeeds=intent_report.fulfilled_needs,
            complementOpportunities=complement_opportunities,
            demandStates=demand_states,
            doNotRecommend=do_not_recommend,
        )

        audit = self.audit(draft)
        draft.audit_result = audit.self_check_result
        profile_version = UserProfileVersion(
            workflowId=workflow_id,
            userId=context.user_id,
            confidence=draft.confidence,
            evidenceEventIds=evidence_event_ids,
            versionNo=1,
            summary=draft.summary,
            profile={
                "profileSummary": draft.summary,
                "preferenceTags": draft.preference_tags,
                "preferredCategories": draft.preferred_categories,
                "demandStates": [state.value for state in draft.demand_states],
                "fulfilledNeeds": [need.model_dump(by_alias=True, mode="json") for need in draft.fulfilled_needs],
                "complementOpportunities": [
                    opportunity.model_dump(by_alias=True, mode="json")
                    for opportunity in draft.complement_opportunities
                ],
                "doNotRecommend": draft.do_not_recommend,
                "evidenceEventIds": evidence_event_ids,
                "auditResult": audit.self_check_result,
            },
        )
        return draft, audit, profile_version

    def audit(self, draft: ProfileDraft) -> ProfileAuditReport:
        issues: list[str] = []
        if not draft.evidence_event_ids:
            issues.append("missing evidenceEventIds")
        if not draft.summary and (draft.preference_tags or draft.current_intents or draft.fulfilled_needs):
            issues.append("profile has signals but no summary")
        fulfilled_skus = {need.sku_id for need in draft.fulfilled_needs if need.sku_id is not None}
        recommended_fulfilled_skus = {
            opportunity.related_fulfilled_sku_id
            for opportunity in draft.complement_opportunities
            if opportunity.label.lower().startswith("same sku")
        }
        if fulfilled_skus & recommended_fulfilled_skus:
            issues.append("fulfilled SKU is being recommended again")
        for need in draft.fulfilled_needs:
            if not need.repeat_recommendation_suppressed:
                issues.append("PAYMENT_SUCCESS did not suppress repeat recommendation")
        if not draft.evidence_event_ids and draft.confidence > 0.5:
            issues.append("empty profile has overconfident inference")

        result = "passed" if not issues else "; ".join(issues)
        return ProfileAuditReport(
            workflowId=draft.workflow_id,
            userId=draft.user_id,
            confidence=0.9 if not issues else 0.45,
            evidenceEventIds=draft.evidence_event_ids,
            passed=not issues,
            issues=issues,
            selfCheckResult=result,
        )

    def _preference_tags(self, context: AgentProfileContext, preferred_categories: list[str]) -> list[str]:
        tags = list(dict.fromkeys(context.recent_keywords + [f"category:{category}" for category in preferred_categories]))
        return tags or ["insufficient evidence"]

    def _complements(self, context: AgentProfileContext, intent_report: IntentReport) -> list[ComplementOpportunity]:
        if intent_report.complement_opportunities:
            return intent_report.complement_opportunities
        if not intent_report.fulfilled_needs:
            return []
        labels = complement_labels(context.recent_keywords)
        first_need = intent_report.fulfilled_needs[0]
        return [
            ComplementOpportunity(
                label=label,
                relatedFulfilledSkuId=first_need.sku_id,
                complementScore=0.68,
                evidence=first_need.evidence,
            )
            for label in labels
        ]

    def _do_not_recommend(self, intent_report: IntentReport) -> list[dict[str, int | None]]:
        return [
            {
                "skuId": need.sku_id,
                "spuId": need.spu_id,
                "reason": "fulfilled_need_repeat_suppressed",
            }
            for need in intent_report.fulfilled_needs
        ]

    def _demand_states(self, intent_report: IntentReport, context: AgentProfileContext) -> list[DemandState]:
        states = [intent.demand_state for intent in intent_report.current_intents]
        states.extend(need.demand_state for need in intent_report.fulfilled_needs)
        if context.event_type_counts.get("ORDER_CANCELED", 0) > 0:
            states.append(DemandState.COOLING)
        return list(dict.fromkeys(states)) or [DemandState.DISCOVERING]

    def _summary(
        self,
        context: AgentProfileContext,
        demand_states: list[DemandState],
        complement_opportunities: list[ComplementOpportunity],
    ) -> str:
        if not context.all_evidence_event_ids:
            return "No sufficient behavior evidence yet. Keep collecting structured signals."
        state_text = ", ".join(state.value for state in demand_states)
        if complement_opportunities:
            complement_text = ", ".join(opportunity.label for opportunity in complement_opportunities[:3])
            return f"Current demand states: {state_text}. Fulfilled purchase detected; consider complements: {complement_text}."
        if context.recent_keywords:
            return f"Current demand states: {state_text}. Recent interest keywords: {', '.join(context.recent_keywords[:3])}."
        return f"Current demand states: {state_text}. Profile is based on recent structured behavior events."
