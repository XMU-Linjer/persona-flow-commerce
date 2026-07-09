from contextlib import contextmanager
from pathlib import Path
from uuid import uuid4

from persona_agent_service.agents.deepseek_recommendation_agent import DeepSeekRecommendationAgent
from persona_agent_service.config.settings import AgentSettings
from persona_agent_service.schemas.artifacts import UserProfileVersion
from persona_agent_service.schemas.context import AgentProfileContext


class FailingDeepSeekClient:
    def complete_json(self, system_prompt: str, user_prompt: str):
        raise AssertionError("DeepSeek client should not be called")


def write_env(path, content: str) -> None:
    path.write_text(content.strip() + "\n", encoding="utf-8")


@contextmanager
def temporary_env_file(content: str):
    directory = Path.cwd() / ".pytest-env-tests"
    directory.mkdir(exist_ok=True)
    path = directory / f"{uuid4()}.env"
    write_env(path, content)
    try:
        yield path
    finally:
        path.unlink(missing_ok=True)
        try:
            directory.rmdir()
        except OSError:
            pass


def test_deepseek_settings_read_project_env_file(monkeypatch):
    with temporary_env_file(
        """
        DEEPSEEK_ENABLED=true
        DEEPSEEK_API_KEY=file_key
        DEEPSEEK_BASE_URL=https://api.deepseek.com
        DEEPSEEK_MODEL=deepseek-v4-flash
        DEEPSEEK_TIMEOUT_SECONDS=21
        DEEPSEEK_MAX_TOKENS=1300
        DEEPSEEK_TEMPERATURE=0.3
        """
    ) as env_file:
        monkeypatch.setenv("PERSONA_AGENT_DOTENV_PATH", str(env_file))

        settings = AgentSettings.from_env()

        assert settings.deepseek_enabled is True
        assert settings.deepseek_api_key == "file_key"
        assert settings.deepseek_timeout_seconds == 21
        assert settings.deepseek_max_tokens == 1300
        assert settings.deepseek_temperature == 0.3


def test_project_env_deepseek_api_key_overrides_system_environment(monkeypatch):
    with temporary_env_file(
        """
        DEEPSEEK_ENABLED=true
        DEEPSEEK_API_KEY=file_key
        """
    ) as env_file:
        monkeypatch.setenv("PERSONA_AGENT_DOTENV_PATH", str(env_file))
        monkeypatch.setenv("DEEPSEEK_ENABLED", "false")
        monkeypatch.setenv("DEEPSEEK_API_KEY", "system_key")

        settings = AgentSettings.from_env()

        assert settings.deepseek_enabled is True
        assert settings.deepseek_api_key == "file_key"


def test_missing_project_env_uses_deepseek_defaults(monkeypatch):
    monkeypatch.delenv("PERSONA_AGENT_DOTENV_PATH", raising=False)
    monkeypatch.setenv("DEEPSEEK_ENABLED", "true")
    monkeypatch.setenv("DEEPSEEK_API_KEY", "system_key")

    settings = AgentSettings.from_env()

    assert settings.deepseek_enabled is False
    assert settings.deepseek_api_key == ""
    assert settings.deepseek_base_url == "https://api.deepseek.com"
    assert settings.deepseek_model == "deepseek-v4-flash"


def test_deepseek_disabled_in_project_env_does_not_call_client(monkeypatch):
    with temporary_env_file(
        """
        DEEPSEEK_ENABLED=false
        DEEPSEEK_API_KEY=file_key
        """
    ) as env_file:
        monkeypatch.setenv("PERSONA_AGENT_DOTENV_PATH", str(env_file))
        agent = DeepSeekRecommendationAgent(
            settings=AgentSettings.from_env(),
            client=FailingDeepSeekClient(),
        )

        result = agent.enhance_profile(
            context=empty_context(),
            behavior_report=empty_report(),
            intent_report=empty_report(),
            trend_report=empty_report(),
            rule_based_profile=rule_based_profile(),
        )

        assert result.profile["generationMode"] == "RULE_BASED"


def test_empty_deepseek_key_in_project_env_does_not_call_client(monkeypatch):
    with temporary_env_file(
        """
        DEEPSEEK_ENABLED=true
        DEEPSEEK_API_KEY=
        """
    ) as env_file:
        monkeypatch.setenv("PERSONA_AGENT_DOTENV_PATH", str(env_file))
        agent = DeepSeekRecommendationAgent(
            settings=AgentSettings.from_env(),
            client=FailingDeepSeekClient(),
        )

        result = agent.enhance_profile(
            context=empty_context(),
            behavior_report=empty_report(),
            intent_report=empty_report(),
            trend_report=empty_report(),
            rule_based_profile=rule_based_profile(),
        )

        assert result.profile["generationMode"] == "RULE_BASED"


def empty_context() -> AgentProfileContext:
    return AgentProfileContext.model_validate({"userId": 10001})


def empty_report():
    return None


def rule_based_profile() -> UserProfileVersion:
    return UserProfileVersion(
        workflowId="workflow-test",
        userId=10001,
        confidence=0.2,
        evidenceEventIds=[],
        versionNo=1,
        summary="rule profile",
        profile={"profileSummary": "rule profile"},
    )
