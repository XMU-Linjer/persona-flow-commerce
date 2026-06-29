# Persona Agent Service

Python service skeleton for PersonaFlow Commerce V1.1 Profile Agent Team.

This service currently provides:

- FastAPI application skeleton
- `GET /health`
- Structured Agent message protocol
- Structured Artifact models
- RabbitMQ `commerce.agent.exchange` topology declarations
- Mock profile workflow message publishing for development verification

It intentionally does not implement real LLM calls, OpenAI/Claude integration, LangChain, RAG, vector databases, or real recommendation algorithms.

## Local Setup

From Windows PowerShell:

```powershell
cd D:\Workspace\persona-flow-commerce\persona-agent-service
python -m pip install -r requirements.txt
$env:PYTHONPATH = "src"
python -m pytest
python -m uvicorn persona_agent_service.main:app --reload --host 127.0.0.1 --port 8001
```

Health check:

```powershell
curl.exe http://127.0.0.1:8001/health
```

Expected response:

```json
{
  "status": "UP",
  "service": "persona-agent-service"
}
```

## RabbitMQ Configuration

The service reads RabbitMQ settings from environment variables:

```text
RABBITMQ_HOST
RABBITMQ_PORT
RABBITMQ_USERNAME
RABBITMQ_PASSWORD
```

Defaults are compatible with the local Docker Compose setup:

```text
host = 127.0.0.1
port = 5672
username = persona_flow
password = 123456
```

The Agent task bus uses:

```text
exchange = commerce.agent.exchange
type = topic
```

This is separate from Java's `commerce.behavior.exchange`.

## Mock Workflow Endpoint

Development-only endpoint:

```http
POST /agent/profile/workflows/mock
```

Example without RabbitMQ publishing:

```powershell
curl.exe -X POST http://127.0.0.1:8001/agent/profile/workflows/mock `
  -H "Content-Type: application/json" `
  -d "{\"userId\":10001,\"publish\":false}"
```

Set `publish` to `true` to publish mock `TASK_ASSIGNED` messages to `commerce.agent.exchange`.
