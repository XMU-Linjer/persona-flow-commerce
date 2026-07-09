from dataclasses import dataclass
import os


def _env_bool(name: str, default: bool = False) -> bool:
    value = os.getenv(name)
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "on"}


@dataclass(frozen=True)
class AgentSettings:
    rabbitmq_host: str = "127.0.0.1"
    rabbitmq_port: int = 5672
    rabbitmq_username: str = "persona_flow"
    rabbitmq_password: str = "123456"
    deepseek_enabled: bool = False
    deepseek_api_key: str = ""
    deepseek_base_url: str = "https://api.deepseek.com"
    deepseek_model: str = "deepseek-v4-flash"
    deepseek_timeout_seconds: int = 20
    deepseek_max_tokens: int = 1200
    deepseek_temperature: float = 0.2

    @classmethod
    def from_env(cls) -> "AgentSettings":
        return cls(
            rabbitmq_host=os.getenv("RABBITMQ_HOST", "127.0.0.1"),
            rabbitmq_port=int(os.getenv("RABBITMQ_PORT", "5672")),
            rabbitmq_username=os.getenv("RABBITMQ_USERNAME", "persona_flow"),
            rabbitmq_password=os.getenv("RABBITMQ_PASSWORD", "123456"),
            deepseek_enabled=_env_bool("DEEPSEEK_ENABLED", False),
            deepseek_api_key=os.getenv("DEEPSEEK_API_KEY", ""),
            deepseek_base_url=os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com"),
            deepseek_model=os.getenv("DEEPSEEK_MODEL", "deepseek-v4-flash"),
            deepseek_timeout_seconds=int(os.getenv("DEEPSEEK_TIMEOUT_SECONDS", "20")),
            deepseek_max_tokens=int(os.getenv("DEEPSEEK_MAX_TOKENS", "1200")),
            deepseek_temperature=float(os.getenv("DEEPSEEK_TEMPERATURE", "0.2")),
        )

    @property
    def deepseek_configured(self) -> bool:
        return self.deepseek_enabled and bool(self.deepseek_api_key.strip())
