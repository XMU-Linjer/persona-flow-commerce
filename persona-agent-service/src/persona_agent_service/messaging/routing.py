from persona_agent_service.messaging import constants
from persona_agent_service.schemas.enums import AgentMessageType


ROUTING_KEY_BY_MESSAGE_TYPE = {
    AgentMessageType.TASK_ASSIGNED: constants.ROUTING_TASK_ASSIGNED,
    AgentMessageType.TASK_ACCEPTED: constants.ROUTING_TASK_ACCEPTED,
    AgentMessageType.ARTIFACT_CREATED: constants.ROUTING_ARTIFACT_CREATED,
    AgentMessageType.CHALLENGE_RAISED: constants.ROUTING_CHALLENGE_RAISED,
    AgentMessageType.REVISION_REQUESTED: constants.ROUTING_REVISION_REQUESTED,
    AgentMessageType.REVISION_COMPLETED: constants.ROUTING_REVISION_COMPLETED,
    AgentMessageType.TASK_COMPLETED: constants.ROUTING_TASK_COMPLETED,
    AgentMessageType.TASK_FAILED: constants.ROUTING_TASK_FAILED,
    AgentMessageType.WORKFLOW_COMPLETED: constants.ROUTING_WORKFLOW_COMPLETED,
}


def resolve_routing_key(message_type: AgentMessageType | str) -> str:
    parsed_type = AgentMessageType(message_type)
    return ROUTING_KEY_BY_MESSAGE_TYPE[parsed_type]
