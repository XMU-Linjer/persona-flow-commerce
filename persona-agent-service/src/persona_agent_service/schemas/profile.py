from pydantic import BaseModel, ConfigDict, Field

from persona_agent_service.schemas.artifacts import (
    BehaviorFactReport,
    IntentReport,
    ProfileAuditReport,
    ProfileDraft,
    TrendReport,
    UserProfileVersion,
)


class ProfileBuildResult(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    workflow_id: str = Field(alias="workflowId")
    behavior_fact_report: BehaviorFactReport = Field(alias="behaviorFactReport")
    intent_report: IntentReport = Field(alias="intentReport")
    trend_report: TrendReport = Field(alias="trendReport")
    profile_draft: ProfileDraft = Field(alias="profileDraft")
    audit_report: ProfileAuditReport = Field(alias="auditReport")
    profile: UserProfileVersion
