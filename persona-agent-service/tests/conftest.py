import pytest


@pytest.fixture(autouse=True)
def disable_deepseek_environment(monkeypatch):
    monkeypatch.delenv("DEEPSEEK_ENABLED", raising=False)
    monkeypatch.delenv("DEEPSEEK_API_KEY", raising=False)

