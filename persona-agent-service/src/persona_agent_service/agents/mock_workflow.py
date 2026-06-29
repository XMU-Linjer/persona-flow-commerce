from uuid import uuid4

from persona_agent_service.schemas.enums import AgentMessageType, AgentRole
from persona_agent_service.schemas.messages import AgentMessage, AgentTaskPayload


def build_mock_profile_workflow(
    user_id: int,
    evidence_event_ids: list[str] | None = None,
) -> list[AgentMessage]:
    workflow_id = f"workflow-{uuid4()}"
    evidence = evidence_event_ids or []
    receivers = (
        AgentRole.BEHAVIOR_AGENT,
        AgentRole.INTENT_AGENT,
        AgentRole.TREND_AGENT,
        AgentRole.PROFILE_BUILDER_CRITIC,
    )

    messages: list[AgentMessage] = []
    for receiver in receivers:
        task_id = f"task-{receiver.value.lower().replace('_', '-')}-{uuid4()}"
        payload = AgentTaskPayload(
            userId=user_id,
            contextRef=f"java-behavior-context:user:{user_id}",
            evidenceEventIds=evidence,
            instructions=[
                "Use structured context only.",
                "Do not call real LLMs in this skeleton stage.",
                "Treat PAYMENT_SUCCESS as fulfilled need and complement trigger.",
            ],
        )
        messages.append(
            AgentMessage(
                workflowId=workflow_id,
                taskId=task_id,
                sender=AgentRole.PROFILE_MANAGER,
                receiver=receiver,
                messageType=AgentMessageType.TASK_ASSIGNED,
                payload=payload.model_dump(by_alias=True),
            )
        )
    return messages
