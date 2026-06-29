import pytest
from pydantic import ValidationError

from persona_agent_service.agents.mock_workflow import build_mock_profile_workflow
from persona_agent_service.schemas.enums import AgentMessageType, AgentRole
from persona_agent_service.schemas.messages import AgentMessage, AgentTaskPayload


def test_agent_message_serializes_with_camel_case_fields():
    payload = AgentTaskPayload(
        userId=10001,
        contextRef="java-behavior-context:user:10001",
        evidenceEventIds=["event-001"],
        instructions=["no real llm"],
    )
    message = AgentMessage(
        workflowId="workflow-001",
        taskId="task-001",
        sender=AgentRole.PROFILE_MANAGER,
        receiver=AgentRole.BEHAVIOR_AGENT,
        messageType=AgentMessageType.TASK_ASSIGNED,
        payload=payload.model_dump(by_alias=True),
    )

    serialized = message.model_dump(by_alias=True, mode="json")

    assert serialized["messageId"]
    assert serialized["workflowId"] == "workflow-001"
    assert serialized["taskId"] == "task-001"
    assert serialized["sender"] == "PROFILE_MANAGER"
    assert serialized["receiver"] == "BEHAVIOR_AGENT"
    assert serialized["messageType"] == "TASK_ASSIGNED"
    assert serialized["payload"]["userId"] == 10001
    assert serialized["payload"]["evidenceEventIds"] == ["event-001"]


def test_invalid_message_type_fails_validation():
    with pytest.raises(ValidationError):
        AgentMessage.model_validate(
            {
                "workflowId": "workflow-001",
                "taskId": "task-001",
                "sender": "PROFILE_MANAGER",
                "receiver": "BEHAVIOR_AGENT",
                "messageType": "NOT_A_REAL_TYPE",
                "payload": {},
            }
        )


def test_mock_profile_workflow_builds_task_assigned_messages():
    messages = build_mock_profile_workflow(10001, ["event-payment-001"])

    assert len(messages) == 4
    assert {message.receiver for message in messages} == {
        AgentRole.BEHAVIOR_AGENT,
        AgentRole.INTENT_AGENT,
        AgentRole.TREND_AGENT,
        AgentRole.PROFILE_BUILDER_CRITIC,
    }
    assert all(message.message_type == AgentMessageType.TASK_ASSIGNED for message in messages)
    assert all(message.payload["evidenceEventIds"] == ["event-payment-001"] for message in messages)
    assert all("PAYMENT_SUCCESS" in message.payload["instructions"][2] for message in messages)
