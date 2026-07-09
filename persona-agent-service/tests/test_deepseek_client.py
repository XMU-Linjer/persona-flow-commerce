import json

import httpx
import pytest

from persona_agent_service.config.settings import AgentSettings
from persona_agent_service.llm.deepseek_client import DeepSeekClient
from persona_agent_service.llm.exceptions import (
    DeepSeekAuthenticationError,
    DeepSeekInvalidResponseError,
    DeepSeekRateLimitError,
    DeepSeekRequestError,
    DeepSeekTimeoutError,
)


def settings() -> AgentSettings:
    return AgentSettings(
        deepseek_enabled=True,
        deepseek_api_key="test-api-key",
        deepseek_base_url="https://deepseek.example",
        deepseek_model="deepseek-test",
        deepseek_timeout_seconds=3,
        deepseek_max_tokens=512,
        deepseek_temperature=0.3,
    )


def completion_response(content: dict) -> httpx.Response:
    return httpx.Response(
        200,
        json={
            "choices": [
                {
                    "message": {
                        "content": json.dumps(content),
                    }
                }
            ]
        },
    )


def test_deepseek_client_builds_chat_completion_request():
    captured = {}

    def handler(request: httpx.Request) -> httpx.Response:
        captured["url"] = str(request.url)
        captured["authorization"] = request.headers.get("authorization")
        captured["body"] = json.loads(request.content.decode("utf-8"))
        return completion_response({"summary": "ok"})

    client = DeepSeekClient(settings(), httpx.Client(transport=httpx.MockTransport(handler)))

    result = client.complete_json("system", "user")

    assert result == {"summary": "ok"}
    assert captured["url"] == "https://deepseek.example/chat/completions"
    assert captured["authorization"] == "Bearer test-api-key"
    assert captured["body"]["model"] == "deepseek-test"
    assert captured["body"]["response_format"] == {"type": "json_object"}
    assert captured["body"]["temperature"] == 0.3
    assert captured["body"]["max_tokens"] == 512
    assert captured["body"]["stream"] is False
    assert captured["body"]["messages"] == [
        {"role": "system", "content": "system"},
        {"role": "user", "content": "user"},
    ]


def test_deepseek_client_raises_on_non_2xx_status():
    client = DeepSeekClient(
        settings(),
        httpx.Client(transport=httpx.MockTransport(lambda request: httpx.Response(500, json={"error": "bad"}))),
    )

    with pytest.raises(DeepSeekRequestError):
        client.complete_json("system", "user")


def test_deepseek_client_raises_on_auth_status():
    client = DeepSeekClient(
        settings(),
        httpx.Client(transport=httpx.MockTransport(lambda request: httpx.Response(401, json={"error": "auth"}))),
    )

    with pytest.raises(DeepSeekAuthenticationError):
        client.complete_json("system", "user")


def test_deepseek_client_raises_on_rate_limit():
    client = DeepSeekClient(
        settings(),
        httpx.Client(transport=httpx.MockTransport(lambda request: httpx.Response(429, json={"error": "limit"}))),
    )

    with pytest.raises(DeepSeekRateLimitError):
        client.complete_json("system", "user")


def test_deepseek_client_raises_on_timeout():
    def handler(request: httpx.Request) -> httpx.Response:
        raise httpx.TimeoutException("slow")

    client = DeepSeekClient(settings(), httpx.Client(transport=httpx.MockTransport(handler)))

    with pytest.raises(DeepSeekTimeoutError):
        client.complete_json("system", "user")


def test_deepseek_client_raises_on_invalid_response_json():
    client = DeepSeekClient(
        settings(),
        httpx.Client(transport=httpx.MockTransport(lambda request: httpx.Response(200, content=b"not-json"))),
    )

    with pytest.raises(DeepSeekInvalidResponseError):
        client.complete_json("system", "user")


def test_deepseek_client_raises_on_invalid_message_content_json():
    client = DeepSeekClient(
        settings(),
        httpx.Client(
            transport=httpx.MockTransport(
                lambda request: httpx.Response(
                    200,
                    json={"choices": [{"message": {"content": "not-json"}}]},
                )
            )
        ),
    )

    with pytest.raises(DeepSeekInvalidResponseError):
        client.complete_json("system", "user")

