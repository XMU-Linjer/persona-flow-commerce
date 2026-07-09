from pydantic import ValidationError

from persona_agent_service.agents.profile_builder_critic import ProfileBuilderCritic
from persona_agent_service.config.settings import AgentSettings
from persona_agent_service.llm.deepseek_client import DeepSeekClient
from persona_agent_service.llm.exceptions import DeepSeekError
from persona_agent_service.llm.prompts import DEEPSEEK_SYSTEM_PROMPT, build_deepseek_user_prompt
from persona_agent_service.schemas.artifacts import BehaviorFactReport, IntentReport, TrendReport, UserProfileVersion
from persona_agent_service.schemas.context import AgentProfileContext
from persona_agent_service.schemas.llm import DeepSeekRecommendationInsight


GENERATION_RULE_BASED = "RULE_BASED"
GENERATION_DEEPSEEK_ENHANCED = "DEEPSEEK_ENHANCED"
GENERATION_FALLBACK_RULE_BASED = "FALLBACK_RULE_BASED"


class DeepSeekRecommendationAgent:
    def __init__(
        self,
        settings: AgentSettings | None = None,
        client: DeepSeekClient | None = None,
        critic: ProfileBuilderCritic | None = None,
    ):
        self.settings = settings or AgentSettings.from_env()
        self.client = client or DeepSeekClient(self.settings)
        self.critic = critic or ProfileBuilderCritic()

    def enhance_profile(
        self,
        context: AgentProfileContext,
        behavior_report: BehaviorFactReport,
        intent_report: IntentReport,
        trend_report: TrendReport,
        rule_based_profile: UserProfileVersion,
    ) -> UserProfileVersion:
        if not self.settings.deepseek_configured:
            return self._with_generation_metadata(
                rule_based_profile,
                GENERATION_RULE_BASED,
                llm_enabled=False,
            )

        try:
            user_prompt = build_deepseek_user_prompt(
                context=context,
                behavior_report=behavior_report,
                intent_report=intent_report,
                trend_report=trend_report,
                rule_based_profile=rule_based_profile,
            )
            response = self.client.complete_json(DEEPSEEK_SYSTEM_PROMPT, user_prompt)
            insight = DeepSeekRecommendationInsight.model_validate(response)
            issues = self.critic.audit_deepseek_insight(context, insight)
            if issues:
                return self._fallback(rule_based_profile, "; ".join(issues))
            return self._merge_insight(rule_based_profile, insight)
        except (DeepSeekError, ValidationError, ValueError) as exception:
            return self._fallback(rule_based_profile, str(exception))

    def _merge_insight(
        self,
        rule_based_profile: UserProfileVersion,
        insight: DeepSeekRecommendationInsight,
    ) -> UserProfileVersion:
        enhanced = rule_based_profile.model_copy(deep=True)
        profile = dict(enhanced.profile)
        profile.update(
            {
                "profileSummary": insight.summary,
                "preferenceTags": insight.preference_tags,
                "demandStage": insight.demand_stage.value,
                "demandStates": [
                    state.model_dump(by_alias=True, mode="json")
                    for state in insight.demand_states
                ],
                "fulfilledNeeds": [
                    need.model_dump(by_alias=True, mode="json")
                    for need in insight.fulfilled_needs
                ],
                "doNotRecommend": [
                    item.model_dump(by_alias=True, mode="json")
                    for item in insight.do_not_recommend
                ],
                "complementOpportunities": [
                    opportunity.model_dump(by_alias=True, mode="json")
                    for opportunity in insight.complement_opportunities
                ],
                "recommendationReasons": insight.recommendation_reasons,
                "riskChecks": insight.risk_checks.model_dump(by_alias=True, mode="json"),
                "generationMode": GENERATION_DEEPSEEK_ENHANCED,
                "llmProvider": "deepseek",
                "llmModel": self.settings.deepseek_model,
                "llmEnabled": True,
                "llmError": None,
                "deepseekAuditResult": "passed",
            }
        )
        enhanced.profile = profile
        enhanced.summary = insight.summary
        enhanced.confidence = insight.confidence
        enhanced.evidence_event_ids = list(dict.fromkeys(
            enhanced.evidence_event_ids + sorted(self._insight_evidence_ids(insight))
        ))
        return enhanced

    def _fallback(self, rule_based_profile: UserProfileVersion, reason: str) -> UserProfileVersion:
        return self._with_generation_metadata(
            rule_based_profile,
            GENERATION_FALLBACK_RULE_BASED,
            llm_enabled=True,
            llm_error=reason,
        )

    def _with_generation_metadata(
        self,
        profile_version: UserProfileVersion,
        generation_mode: str,
        llm_enabled: bool,
        llm_error: str | None = None,
    ) -> UserProfileVersion:
        result = profile_version.model_copy(deep=True)
        profile = dict(result.profile)
        profile.update(
            {
                "generationMode": generation_mode,
                "llmProvider": "deepseek",
                "llmModel": self.settings.deepseek_model,
                "llmEnabled": llm_enabled,
                "llmError": llm_error,
            }
        )
        result.profile = profile
        return result

    def _insight_evidence_ids(self, insight: DeepSeekRecommendationInsight) -> set[str]:
        event_ids: list[str] = []
        for state in insight.demand_states:
            event_ids.extend(state.evidence_event_ids)
        for need in insight.fulfilled_needs:
            event_ids.extend(need.evidence_event_ids)
        for opportunity in insight.complement_opportunities:
            event_ids.extend(opportunity.evidence_event_ids)
        return {event_id for event_id in event_ids if event_id}

