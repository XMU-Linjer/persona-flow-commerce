from fastapi.testclient import TestClient

from persona_agent_service.main import app


def test_profile_build_endpoint_returns_complete_result():
    client = TestClient(app)

    response = client.post(
        "/agent/profile/build",
        json={
            "userId": 10001,
            "recentEvents": [
                {
                    "eventId": "event-paid-001",
                    "eventType": "PAYMENT_SUCCESS",
                    "skuId": 30001,
                    "spuId": 20001,
                    "categoryId": 201,
                    "orderId": 50001,
                }
            ],
            "eventTypeCounts": {"PAYMENT_SUCCESS": 1},
            "recentKeywords": ["keyboard"],
            "topCategories": [{"targetType": "CATEGORY", "targetId": 201, "count": 3}],
            "viewedProducts": [],
            "cartSignals": [],
            "orderSignals": [],
            "paidSignals": [
                {
                    "eventId": "event-paid-001",
                    "eventType": "PAYMENT_SUCCESS",
                    "skuId": 30001,
                    "spuId": 20001,
                    "categoryId": 201,
                    "preferenceConfirmed": True,
                    "fulfilled": True,
                    "complementTrigger": True,
                    "repeatRecommendationSuppressed": True,
                }
            ],
            "canceledSignals": [],
            "fulfilledNeeds": [],
            "evidenceEventIds": ["event-paid-001"],
            "evidence": [
                {
                    "eventId": "event-paid-001",
                    "eventType": "PAYMENT_SUCCESS",
                    "reason": "fulfilled_need_and_complement_trigger",
                }
            ],
            "generatedAt": "2026-06-29T10:00:00Z",
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["workflowId"].startswith("workflow-")
    assert body["behaviorFactReport"]["artifactType"] == "BehaviorFactReport"
    assert body["intentReport"]["fulfilledNeeds"][0]["repeatRecommendationSuppressed"] is True
    assert body["trendReport"]["artifactType"] == "TrendReport"
    assert body["profile"]["artifactType"] == "UserProfileVersion"
    assert body["profile"]["profile"]["doNotRecommend"][0]["skuId"] == 30001
    assert body["profile"]["profile"]["complementOpportunities"]


def test_profile_build_endpoint_handles_empty_context():
    client = TestClient(app)

    response = client.post("/agent/profile/build", json={"userId": 10001})

    assert response.status_code == 200
    body = response.json()
    assert body["profile"]["profile"]["profileSummary"].startswith("No sufficient behavior evidence")
    assert body["auditReport"]["passed"] is False
