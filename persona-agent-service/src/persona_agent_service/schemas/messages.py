from datetime import datetime, timezone
from typing import Any
from uuid import uuid4

from pydantic import BaseModel, ConfigDict, Field

from persona_agent_service.schemas.enums import AgentMessageType, AgentRole


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


class AgentTaskPayload(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    user_id: int = Field(alias="userId")
    context_ref: str | None = Field(default=None, alias="contextRef")
    evidence_event_ids: list[str] = Field(default_factory=list, alias="evidenceEventIds")
    instructions: list[str] = Field(default_factory=list)


class AgentArtifactPayload(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    artifact_id: str = Field(alias="artifactId")
    artifact_type: str = Field(alias="artifactType")
    summary: str | None = None
    evidence_event_ids: list[str] = Field(default_factory=list, alias="evidenceEventIds")
    data: dict[str, Any] = Field(default_factory=dict)


class AgentMessage(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    message_id: str = Field(default_factory=lambda: str(uuid4()), alias="messageId")
    workflow_id: str = Field(alias="workflowId")
    task_id: str | None = Field(default=None, alias="taskId")
    sender: AgentRole
    receiver: AgentRole
    message_type: AgentMessageType = Field(alias="messageType")
    artifact_type: str | None = Field(default=None, alias="artifactType")
    artifact_id: str | None = Field(default=None, alias="artifactId")
    correlation_id: str | None = Field(default=None, alias="correlationId")
    timestamp: datetime = Field(default_factory=utc_now)
    payload: dict[str, Any] = Field(default_factory=dict)

    def to_json_bytes(self) -> bytes:
        return self.model_dump_json(by_alias=True).encode("utf-8")
