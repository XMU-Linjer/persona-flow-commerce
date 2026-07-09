import json
from typing import Any

import httpx

from persona_agent_service.config.settings import AgentSettings
from persona_agent_service.llm.exceptions import (
    DeepSeekAuthenticationError,
    DeepSeekConfigurationError,
    DeepSeekInvalidResponseError,
    DeepSeekRateLimitError,
    DeepSeekRequestError,
    DeepSeekTimeoutError,
)


class DeepSeekClient:
    def __init__(
        self,
        settings: AgentSettings | None = None,
        http_client: httpx.Client | None = None,
    ):
        self.settings = settings or AgentSettings.from_env()
        self._http_client = http_client

    def complete_json(self, system_prompt: str, user_prompt: str) -> dict[str, Any]:
        if not self.settings.deepseek_api_key.strip():
            raise DeepSeekConfigurationError("DEEPSEEK_API_KEY is not configured")

        payload = {
            "model": self.settings.deepseek_model,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
            "response_format": {"type": "json_object"},
            "temperature": self.settings.deepseek_temperature,
            "max_tokens": self.settings.deepseek_max_tokens,
            "stream": False,
        }
        headers = {
            "Authorization": f"Bearer {self.settings.deepseek_api_key}",
            "Content-Type": "application/json",
        }
        response = self._post(payload, headers)
        response_json = self._response_json(response)
        content = self._message_content(response_json)
        return self._content_json(content)

    def _post(self, payload: dict[str, Any], headers: dict[str, str]) -> httpx.Response:
        url = f"{self.settings.deepseek_base_url.rstrip('/')}/chat/completions"
        try:
            if self._http_client is not None:
                response = self._http_client.post(
                    url,
                    json=payload,
                    headers=headers,
                    timeout=self.settings.deepseek_timeout_seconds,
                )
            else:
                with httpx.Client(timeout=self.settings.deepseek_timeout_seconds) as client:
                    response = client.post(url, json=payload, headers=headers)
        except httpx.TimeoutException as exception:
            raise DeepSeekTimeoutError("DeepSeek request timed out") from exception
        except httpx.RequestError as exception:
            raise DeepSeekRequestError("DeepSeek request failed") from exception

        self._raise_for_status(response)
        return response

    def _raise_for_status(self, response: httpx.Response) -> None:
        if 200 <= response.status_code < 300:
            return
        if response.status_code in {401, 403}:
            raise DeepSeekAuthenticationError("DeepSeek authentication failed")
        if response.status_code == 429:
            raise DeepSeekRateLimitError("DeepSeek rate limit exceeded")
        raise DeepSeekRequestError(f"DeepSeek request returned HTTP {response.status_code}")

    def _response_json(self, response: httpx.Response) -> dict[str, Any]:
        try:
            value = response.json()
        except ValueError as exception:
            raise DeepSeekInvalidResponseError("DeepSeek response is not valid JSON") from exception
        if not isinstance(value, dict):
            raise DeepSeekInvalidResponseError("DeepSeek response JSON is not an object")
        return value

    def _message_content(self, response_json: dict[str, Any]) -> str:
        try:
            content = response_json["choices"][0]["message"]["content"]
        except (KeyError, IndexError, TypeError) as exception:
            raise DeepSeekInvalidResponseError("DeepSeek response does not contain message content") from exception
        if not isinstance(content, str) or not content.strip():
            raise DeepSeekInvalidResponseError("DeepSeek message content is empty")
        return content

    def _content_json(self, content: str) -> dict[str, Any]:
        try:
            value = json.loads(content)
        except json.JSONDecodeError as exception:
            raise DeepSeekInvalidResponseError("DeepSeek message content is not valid JSON") from exception
        if not isinstance(value, dict):
            raise DeepSeekInvalidResponseError("DeepSeek message content JSON is not an object")
        return value

