from persona_agent_service.messaging import constants
from persona_agent_service.messaging.routing import resolve_routing_key
from persona_agent_service.schemas.enums import AgentMessageType


def test_routing_key_resolution():
    assert resolve_routing_key(AgentMessageType.TASK_ASSIGNED) == "agent.task.assigned"
    assert resolve_routing_key(AgentMessageType.ARTIFACT_CREATED) == "agent.artifact.created"
    assert resolve_routing_key(AgentMessageType.CHALLENGE_RAISED) == "agent.challenge.raised"
    assert resolve_routing_key(AgentMessageType.REVISION_REQUESTED) == "agent.revision.requested"
    assert resolve_routing_key(AgentMessageType.REVISION_COMPLETED) == "agent.revision.completed"
    assert resolve_routing_key(AgentMessageType.TASK_COMPLETED) == "agent.task.completed"
    assert resolve_routing_key(AgentMessageType.TASK_FAILED) == "agent.task.failed"
    assert resolve_routing_key(AgentMessageType.WORKFLOW_COMPLETED) == "agent.workflow.completed"


def test_agent_bus_constants_match_contract():
    assert constants.AGENT_EXCHANGE == "commerce.agent.exchange"
    assert constants.AGENT_EXCHANGE_TYPE == "topic"
    assert constants.AGENT_QUEUE_NAMES == (
        "agent.profile-manager.queue",
        "agent.behavior.queue",
        "agent.intent.queue",
        "agent.trend.queue",
        "agent.profile-builder.queue",
        "agent.dead.queue",
    )
    required_routing_keys = {
        "agent.task.assigned",
        "agent.artifact.created",
        "agent.challenge.raised",
        "agent.revision.requested",
        "agent.revision.completed",
        "agent.task.completed",
        "agent.task.failed",
        "agent.workflow.completed",
    }
    configured = {
        routing_key
        for bindings in constants.AGENT_QUEUE_BINDINGS.values()
        for routing_key in bindings
    }
    assert required_routing_keys.issubset(configured)
