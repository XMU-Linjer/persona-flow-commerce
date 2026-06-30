# Persona Agent Service

`persona-agent-service` is the Python Agent service for PersonaFlow Commerce V1.1.

It is part of:

> 基于 Spring Boot + RabbitMQ + FastAPI 的电商行为事件驱动用户画像系统。

The service receives a structured `AgentProfileContext` from the Java backend and returns a rule-based user profile result for the Java side to save into `user_profile_version`.

## Current Status

Completed:

- FastAPI service
- `GET /health`
- `POST /agent/profile/build`
- Rule-based Profile Agent Team
- Profile Manager
- Behavior Agent
- Intent Agent
- Trend Agent
- Profile Builder/Critic
- Structured artifacts and profile response
- PAYMENT_SUCCESS demand-state handling
- pytest baseline: 21 pytest passed

Not implemented:

- real LLM calls
- OpenAI / Claude integration
- LangChain
- RAG
- vector database
- real recommendation algorithm
- production-grade async multi-agent scheduler
- direct database writes
- direct order/cart/inventory modification

## Role In V1.1

Java is responsible for:

- authentication
- current user resolution
- behavior event persistence
- behavior summary
- `AgentProfileContext` construction
- calling this Python service
- validating the response
- saving `user_profile_version`
- serving Vue frontend APIs

Python is responsible for:

- receiving structured context
- running the rule-based Profile Agent Team workflow
- returning structured profile results

Python must not:

- query account / catalog / shopping / trade tables directly
- bypass Java authentication
- modify orders, inventory, cart, address, or payment state
- save profile versions to MySQL by itself

## API

### Health

```http
GET /health
```

Response:

```json
{
  "status": "UP",
  "service": "persona-agent-service"
}
```

### Build Profile

```http
POST /agent/profile/build
```

Input:

- `userId`
- `recentEvents`
- `eventTypeCounts`
- `recentKeywords`
- `topCategories`
- `viewedProducts`
- `cartSignals`
- `orderSignals`
- `paidSignals`
- `canceledSignals`
- `fulfilledNeeds`
- `evidenceEventIds`
- `generatedAt`

Output includes:

- `workflowId`
- `summary`
- `profile`
- `fulfilledNeeds`
- `complementOpportunities`
- `doNotRecommend`
- `evidence`

## PAYMENT_SUCCESS Semantics

`PAYMENT_SUCCESS` is not treated as "recommend the same SKU again".

It means:

- preference confirmed
- current concrete demand fulfilled
- complement opportunity triggered

The rule-based workflow therefore:

- places the purchased SKU/SPU into `fulfilledNeeds`
- suppresses short-term repeated recommendation through `doNotRecommend`
- creates complement opportunities
- keeps the payment event as evidence

## Local Setup

From the repository root, start shared dependencies first:

```powershell
cd D:\Workspace\persona-flow-commerce
docker compose up -d
```

Start the Python Agent service:

```powershell
cd D:\Workspace\persona-flow-commerce\persona-agent-service
$env:PYTHONPATH = "src"
python -m uvicorn persona_agent_service.main:app --host 127.0.0.1 --port 8001
```

Health check:

```powershell
curl.exe http://127.0.0.1:8001/health
```

## Tests

```powershell
cd D:\Workspace\persona-flow-commerce\persona-agent-service
$env:PYTHONPATH = "src"
python -m pytest
```

Current verified result:

```text
21 pytest passed
```

## Integration With Java

Java backend calls:

```text
http://127.0.0.1:8001/agent/profile/build
```

The Java endpoint:

```http
POST /api/behavior/me/profile/refresh?days=30
```

builds `AgentProfileContext`, calls this Python service, then saves the returned profile into `user_profile_version`.

If this Python service is down, Java returns:

```text
AGENT_SERVICE_UNAVAILABLE
```

The Vue `/ai-insights` page shows the error and does not white-screen.

## Agent Task Bus Boundary

The codebase keeps Agent message schemas and RabbitMQ naming for later async workflow evolution, including `commerce.agent.exchange`.

Current V1.1 profile refresh path is synchronous HTTP from Java to Python. It does not rely on a production-grade RabbitMQ Agent task bus, Outbox, or distributed transaction.
