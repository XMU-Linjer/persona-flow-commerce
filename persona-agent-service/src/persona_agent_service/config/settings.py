from dataclasses import dataclass
import os


@dataclass(frozen=True)
class AgentSettings:
    rabbitmq_host: str = "127.0.0.1"
    rabbitmq_port: int = 5672
    rabbitmq_username: str = "persona_flow"
    rabbitmq_password: str = "123456"

    @classmethod
    def from_env(cls) -> "AgentSettings":
        return cls(
            rabbitmq_host=os.getenv("RABBITMQ_HOST", "127.0.0.1"),
            rabbitmq_port=int(os.getenv("RABBITMQ_PORT", "5672")),
            rabbitmq_username=os.getenv("RABBITMQ_USERNAME", "persona_flow"),
            rabbitmq_password=os.getenv("RABBITMQ_PASSWORD", "123456"),
        )
