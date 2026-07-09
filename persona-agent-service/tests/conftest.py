import pytest


@pytest.fixture(autouse=True)
def disable_deepseek_environment(monkeypatch):
    monkeypatch.setenv("PERSONA_AGENT_TESTING", "true")
    monkeypatch.delenv("PERSONA_AGENT_DOTENV_PATH", raising=False)
    monkeypatch.delenv("DEEPSEEK_ENABLED", raising=False)
    monkeypatch.delenv("DEEPSEEK_API_KEY", raising=False)

