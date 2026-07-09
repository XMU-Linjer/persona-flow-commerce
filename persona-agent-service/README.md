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
- Optional DeepSeek insight enhancement with rule-based fallback
- Profile Manager
- Behavior Agent
- Intent Agent
- Trend Agent
- Profile Builder/Critic
- Structured artifacts and profile response
- PAYMENT_SUCCESS demand-state handling
- pytest baseline: 39 pytest passed

Not implemented:

- OpenAI / Claude integration
- LangChain
- RAG
- vector database
- real recommendation algorithm
- LLM-controlled order, payment, inventory, or database writes
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
- optionally enhancing the profile insight with DeepSeek when explicitly configured
- returning structured profile results

Python must not:

- query account / catalog / shopping / trade tables directly
- bypass Java authentication
- modify orders, inventory, cart, address, or payment state
- save profile versions to MySQL by itself

## Optional DeepSeek Enhancement

DeepSeek is an optional profile insight enhancement layer. The rule-based Agent remains the baseline and fallback.

When enabled, the workflow is:

```text
BehaviorAgent
-> IntentAgent
-> TrendAgent
-> ProfileBuilder/Critic rule-based baseline
-> DeepSeekRecommendationAgent insight enhancement
-> local Critic validation
-> DEEPSEEK_ENHANCED or FALLBACK_RULE_BASED profile
```

DeepSeek only receives structured behavior context and rule-based artifacts. It must not control order creation, payment, inventory, cart, address, or any Java business API.

DeepSeek configuration is project-level `.env` based. Put the local real values in the repository root:

```text
D:\Workspace\persona-flow-commerce\.env
```

Example:

```text
DEEPSEEK_ENABLED=true
DEEPSEEK_API_KEY=your_deepseek_api_key
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-v4-flash
DEEPSEEK_TIMEOUT_SECONDS=20
DEEPSEEK_MAX_TOKENS=1200
DEEPSEEK_TEMPERATURE=0.2
```

Rules:

- Do not commit real API keys.
- Do not print API keys in logs.
- Do not set Windows User/System environment variables for DeepSeek.
- Do not manually set PowerShell `$env:DEEPSEEK_API_KEY` before each startup.
- The repository root `.env` is the local real configuration entry.
- `.env` is ignored by Git; `.env.example` is only a template.
- If `DEEPSEEK_ENABLED=false`, the service uses the rule-based profile.
- If `DEEPSEEK_API_KEY` is missing, the service uses the rule-based profile.
- If DeepSeek fails, times out, returns invalid JSON, or fails Critic validation, the service returns `FALLBACK_RULE_BASED`.
- DeepSeek does not participate in order creation, inventory, payment, cart, address, or order status changes.
- `/agent/profile/build` stays compatible with the Java backend; the final `profile.profile` JSON includes `generationMode`.

DeepSeek startup example:

```powershell
cd D:\Workspace\persona-flow-commerce
# Fill .env first:
# DEEPSEEK_ENABLED=true
# DEEPSEEK_API_KEY=your_deepseek_api_key

cd D:\Workspace\persona-flow-commerce\persona-agent-service
$env:PYTHONPATH = "src"
python -m uvicorn persona_agent_service.main:app --host 127.0.0.1 --port 8001
```

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
39 pytest passed
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
