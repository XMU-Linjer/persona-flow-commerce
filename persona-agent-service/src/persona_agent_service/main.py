import logging
from time import perf_counter
from uuid import uuid4

from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel, ConfigDict, Field

from persona_agent_service.agents.mock_workflow import build_mock_profile_workflow
from persona_agent_service.agents.profile_manager import ProfileManager
from persona_agent_service.messaging.publisher import AgentBusPublisher
from persona_agent_service.schemas.context import AgentProfileContext
from persona_agent_service.schemas.profile import ProfileBuildResult


class MockWorkflowRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    user_id: int = Field(default=10001, alias="userId")
    evidence_event_ids: list[str] = Field(default_factory=list, alias="evidenceEventIds")
    publish: bool = True


app = FastAPI(title="Persona Agent Service", version="0.1.0")
logger = logging.getLogger(__name__)


@app.get("/health")
def health() -> dict[str, str]:
    return {
        "status": "UP",
        "service": "persona-agent-service",
    }


@app.post("/agent/profile/workflows/mock")
def create_mock_profile_workflow(request: MockWorkflowRequest) -> dict[str, object]:
    messages = build_mock_profile_workflow(
        user_id=request.user_id,
        evidence_event_ids=request.evidence_event_ids,
    )
    routing_keys: list[str] = []
    if request.publish:
        publisher = AgentBusPublisher()
        try:
            routing_keys = [publisher.publish(message) for message in messages]
        except Exception as exception:
            raise HTTPException(
                status_code=503,
                detail=f"Failed to publish mock workflow: {exception}",
            ) from exception

    return {
        "workflowId": messages[0].workflow_id if messages else None,
        "published": request.publish,
        "messageCount": len(messages),
        "messageIds": [message.message_id for message in messages],
        "routingKeys": routing_keys,
    }


@app.post("/agent/profile/build", response_model=ProfileBuildResult)
def build_profile(context: AgentProfileContext, request: Request) -> ProfileBuildResult:
    request_id = request.headers.get("x-profile-request-id") or f"python-{uuid4()}"
    started_at = perf_counter()
    logger.info(
        "Profile build started requestId=%s userId=%s eventCount=%s evidenceCount=%s",
        request_id,
        context.user_id,
        len(context.recent_events),
        len(context.evidence_event_ids),
    )
    try:
        result = ProfileManager().build_profile(context)
        logger.info(
            "Profile build succeeded requestId=%s userId=%s workflowId=%s generationMode=%s elapsedMs=%s",
            request_id,
            context.user_id,
            result.workflow_id,
            result.profile.profile.get("generationMode", "unknown"),
            round((perf_counter() - started_at) * 1000),
        )
        return result
    except Exception:
        logger.exception(
            "Profile build failed requestId=%s userId=%s elapsedMs=%s",
            request_id,
            context.user_id,
            round((perf_counter() - started_at) * 1000),
        )
        raise
