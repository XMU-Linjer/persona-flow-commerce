from fastapi.testclient import TestClient

from persona_agent_service.main import app


def test_health_returns_up():
    client = TestClient(app)

    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {
        "status": "UP",
        "service": "persona-agent-service",
    }


def test_mock_workflow_endpoint_can_build_without_publishing():
    client = TestClient(app)

    response = client.post(
        "/agent/profile/workflows/mock",
        json={
            "userId": 10001,
            "evidenceEventIds": ["event-payment-001"],
            "publish": False,
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["published"] is False
    assert body["messageCount"] == 4
    assert len(body["messageIds"]) == 4
    assert body["routingKeys"] == []
