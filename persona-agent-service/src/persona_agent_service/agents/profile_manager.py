from uuid import uuid4

from persona_agent_service.agents.behavior_agent import BehaviorAgent
from persona_agent_service.agents.deepseek_recommendation_agent import DeepSeekRecommendationAgent
from persona_agent_service.agents.intent_agent import IntentAgent
from persona_agent_service.agents.profile_builder_critic import ProfileBuilderCritic
from persona_agent_service.agents.trend_agent import TrendAgent
from persona_agent_service.schemas.context import AgentProfileContext
from persona_agent_service.schemas.profile import ProfileBuildResult


class ProfileManager:
    def __init__(
        self,
        behavior_agent: BehaviorAgent | None = None,
        intent_agent: IntentAgent | None = None,
        trend_agent: TrendAgent | None = None,
        profile_builder_critic: ProfileBuilderCritic | None = None,
        deepseek_recommendation_agent: DeepSeekRecommendationAgent | None = None,
    ):
        self.behavior_agent = behavior_agent or BehaviorAgent()
        self.intent_agent = intent_agent or IntentAgent()
        self.trend_agent = trend_agent or TrendAgent()
        self.profile_builder_critic = profile_builder_critic or ProfileBuilderCritic()
        self.deepseek_recommendation_agent = deepseek_recommendation_agent or DeepSeekRecommendationAgent(
            critic=self.profile_builder_critic
        )

    def build_profile(self, context: AgentProfileContext) -> ProfileBuildResult:
        workflow_id = f"workflow-{uuid4()}"
        behavior_report = self.behavior_agent.build_report(context, workflow_id)
        intent_report = self.intent_agent.build_report(context, workflow_id)
        trend_report = self.trend_agent.build_report(context, workflow_id)
        profile_draft, audit_report, profile = self.profile_builder_critic.build(
            context=context,
            workflow_id=workflow_id,
            behavior_report=behavior_report,
            intent_report=intent_report,
            trend_report=trend_report,
        )
        profile = self.deepseek_recommendation_agent.enhance_profile(
            context=context,
            behavior_report=behavior_report,
            intent_report=intent_report,
            trend_report=trend_report,
            rule_based_profile=profile,
        )
        return ProfileBuildResult(
            workflowId=workflow_id,
            behaviorFactReport=behavior_report,
            intentReport=intent_report,
            trendReport=trend_report,
            profileDraft=profile_draft,
            auditReport=audit_report,
            profile=profile,
        )
