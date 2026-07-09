from dataclasses import dataclass
import os
from pathlib import Path


DEEPSEEK_DEFAULTS = {
    "DEEPSEEK_ENABLED": "false",
    "DEEPSEEK_API_KEY": "",
    "DEEPSEEK_BASE_URL": "https://api.deepseek.com",
    "DEEPSEEK_MODEL": "deepseek-v4-flash",
    "DEEPSEEK_TIMEOUT_SECONDS": "20",
    "DEEPSEEK_MAX_TOKENS": "1200",
    "DEEPSEEK_TEMPERATURE": "0.2",
}


def find_project_env_file() -> Path | None:
    testing = _env_bool("PERSONA_AGENT_TESTING", False)
    explicit_path = os.getenv("PERSONA_AGENT_DOTENV_PATH")
    if testing and explicit_path:
        path = Path(explicit_path).expanduser()
        return path if path.is_file() else None

    if testing:
        return None

    for root in _candidate_project_roots():
        env_file = root / ".env"
        if env_file.is_file():
            return env_file
    return None


def _candidate_project_roots() -> list[Path]:
    current_dir = Path.cwd()
    settings_file = Path(__file__).resolve()
    candidates: list[Path] = []
    for start in (current_dir, settings_file.parent):
        candidates.extend(_walk_up_project_roots(start))
    return list(dict.fromkeys(candidates))


def _walk_up_project_roots(start: Path) -> list[Path]:
    roots = []
    current = start.resolve()
    if current.is_file():
        current = current.parent
    for path in (current, *current.parents):
        if _looks_like_project_root(path):
            roots.append(path)
    return roots


def _looks_like_project_root(path: Path) -> bool:
    return (
        (path / "docker-compose.yml").is_file()
        or (
            (path / "persona-commerce-server").is_dir()
            and (path / "persona-agent-service").is_dir()
        )
    )


def load_project_env_values(env_file: Path | None = None) -> dict[str, str]:
    path = env_file or find_project_env_file()
    if path is None:
        return {}
    return _parse_env_file(path)


def _parse_env_file(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip().strip('"').strip("'")
        if key:
            values[key] = value
    return values


def get_deepseek_config_from_project_env() -> dict[str, str]:
    env_values = load_project_env_values()
    return {
        key: env_values.get(key, default)
        for key, default in DEEPSEEK_DEFAULTS.items()
    }


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
        deepseek_config = get_deepseek_config_from_project_env()
        return cls(
            rabbitmq_host=os.getenv("RABBITMQ_HOST", "127.0.0.1"),
            rabbitmq_port=int(os.getenv("RABBITMQ_PORT", "5672")),
            rabbitmq_username=os.getenv("RABBITMQ_USERNAME", "persona_flow"),
            rabbitmq_password=os.getenv("RABBITMQ_PASSWORD", "123456"),
            deepseek_enabled=_str_bool(deepseek_config["DEEPSEEK_ENABLED"], False),
            deepseek_api_key=deepseek_config["DEEPSEEK_API_KEY"],
            deepseek_base_url=deepseek_config["DEEPSEEK_BASE_URL"],
            deepseek_model=deepseek_config["DEEPSEEK_MODEL"],
            deepseek_timeout_seconds=int(deepseek_config["DEEPSEEK_TIMEOUT_SECONDS"]),
            deepseek_max_tokens=int(deepseek_config["DEEPSEEK_MAX_TOKENS"]),
            deepseek_temperature=float(deepseek_config["DEEPSEEK_TEMPERATURE"]),
        )

    @property
    def deepseek_configured(self) -> bool:
        return self.deepseek_enabled and bool(self.deepseek_api_key.strip())


def _str_bool(value: str, default: bool = False) -> bool:
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "on"}
