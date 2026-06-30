# 02-agent-workflow-bus Agent 工作流消息总线

> 状态：planned  
> 所属版本：V1.2.0  
> 范围：`commerce.agent.exchange`、AgentMessage、routing key、队列、重试与死信设计  
> 更新时间：2026-06-30

## 1. 定位

V1.1 已完成 `commerce.behavior.exchange` 行为事件总线，并在 Python Agent 服务中保留 `commerce.agent.exchange` 的拓扑方向。V1.2 计划正式使用 Agent Bus 支撑异步 Profile Agent workflow。

Agent Bus 的目标不是替代 Java 业务事务，也不是引入生产级分布式事务，而是让画像生成过程具备：

- 异步执行；
- workflow 状态追踪；
- Agent task 分发；
- Artifact 结构化传递；
- Critic challenge / revision；
- 失败消息记录；
- 死信兜底。

## 2. Exchange

```text
commerce.agent.exchange
```

类型：

```text
topic
```

消息要求：

- durable exchange；
- durable queue；
- persistent message；
- consumer 手动 ACK；
- 基础重试；
- dead queue；
- 不传敏感信息。

## 3. Routing keys

```text
agent.task.assigned
agent.artifact.created
agent.challenge.raised
agent.revision.requested
agent.revision.completed
agent.task.completed
agent.task.failed
agent.workflow.completed
```

说明：

| Routing key | 用途 |
|---|---|
| `agent.task.assigned` | Java 或 Profile Manager 分发任务 |
| `agent.artifact.created` | Agent 产出结构化 Artifact |
| `agent.challenge.raised` | Critic 提出问题或质疑 |
| `agent.revision.requested` | 要求某个 Agent 修订 |
| `agent.revision.completed` | 修订完成 |
| `agent.task.completed` | 单个 task 成功 |
| `agent.task.failed` | 单个 task 失败 |
| `agent.workflow.completed` | workflow 完成或失败后汇总 |

## 4. 队列草案

```text
agent.profile-manager.queue
agent.behavior.queue
agent.intent.queue
agent.trend.queue
agent.profile-builder.queue
agent.dead.queue
```

绑定建议：

| Queue | Binding |
|---|---|
| `agent.profile-manager.queue` | `agent.task.assigned`, `agent.workflow.completed` |
| `agent.behavior.queue` | `agent.task.assigned` with receiver BEHAVIOR_AGENT |
| `agent.intent.queue` | `agent.task.assigned` with receiver INTENT_AGENT |
| `agent.trend.queue` | `agent.task.assigned` with receiver TREND_AGENT |
| `agent.profile-builder.queue` | `agent.artifact.created`, `agent.challenge.raised`, `agent.revision.*` |
| `agent.dead.queue` | failed messages after retry |

RabbitMQ topic binding 无法仅靠 receiver 字段过滤，因此实现阶段可以选择：

- routing key 粒度扩展为 `agent.task.assigned.behavior` 等；
- 或消费者读取消息后按 `receiver` 判断；
- 初版优先清晰和可测试，不追求复杂路由优化。

## 5. AgentMessage 模型

字段草案：

```text
messageId
workflowId
taskId
sender
receiver
messageType
artifactType
artifactId
correlationId
timestamp
payload
```

字段说明：

| 字段 | 说明 |
|---|---|
| `messageId` | 消息级唯一 ID |
| `workflowId` | Java 创建的 workflow ID |
| `taskId` | 当前 Agent task ID |
| `sender` | 发送方 Agent role 或 Java |
| `receiver` | 接收方 Agent role 或 Java |
| `messageType` | 任务、Artifact、审核、完成、失败等类型 |
| `artifactType` | Artifact 类型，可空 |
| `artifactId` | Artifact ID，可空 |
| `correlationId` | 关联一次 workflow 链路 |
| `timestamp` | 消息创建时间 |
| `payload` | 结构化 JSON |

## 6. Message type

```text
TASK_ASSIGNED
ARTIFACT_CREATED
CHALLENGE_RAISED
REVISION_REQUESTED
REVISION_COMPLETED
TASK_COMPLETED
TASK_FAILED
WORKFLOW_COMPLETED
```

与 routing key 的关系：

| messageType | routing key |
|---|---|
| `TASK_ASSIGNED` | `agent.task.assigned` |
| `ARTIFACT_CREATED` | `agent.artifact.created` |
| `CHALLENGE_RAISED` | `agent.challenge.raised` |
| `REVISION_REQUESTED` | `agent.revision.requested` |
| `REVISION_COMPLETED` | `agent.revision.completed` |
| `TASK_COMPLETED` | `agent.task.completed` |
| `TASK_FAILED` | `agent.task.failed` |
| `WORKFLOW_COMPLETED` | `agent.workflow.completed` |

## 7. Artifact payload

Artifact 消息建议携带：

```text
artifactId
workflowId
taskId
agentRole
artifactType
artifactJson
evidenceEventIds
createdAt
```

Artifact 类型：

```text
BEHAVIOR_FACT_REPORT
INTENT_REPORT
TREND_REPORT
PROFILE_DRAFT
PROFILE_AUDIT_REPORT
USER_PROFILE_VERSION
```

约束：

- Artifact 不返回 Entity；
- Artifact 不携带密码、JWT、收货手机号、完整地址；
- Artifact 保留 evidence；
- Artifact 可被 Java 保存到 `agent_artifact`。

## 8. 消费流程草案

### 8.1 Java 创建 workflow

```text
POST /api/behavior/me/profile/workflows
-> Java 创建 agent_workflow PENDING
-> Java 创建 PROFILE_MANAGER task
-> Java 发布 TASK_ASSIGNED
-> workflow 进入 RUNNING
```

### 8.2 Python 处理 workflow

```text
Profile Manager 收到 TASK_ASSIGNED
-> 创建 Behavior / Intent / Trend / Critic task
-> 逐个执行规则 Agent
-> 发布 ARTIFACT_CREATED
-> 发布 TASK_COMPLETED
-> 最后发布 WORKFLOW_COMPLETED
```

### 8.3 Java 保存结果

```text
Java 消费 ARTIFACT_CREATED
-> 保存 agent_artifact
-> 更新 agent_task

Java 消费 WORKFLOW_COMPLETED
-> 校验最终画像 Artifact
-> 保存 user_profile_version
-> agent_workflow 标记 SUCCEEDED
```

失败：

```text
TASK_FAILED 或 WORKFLOW_COMPLETED failed
-> 保存错误信息
-> workflow 标记 FAILED
-> 前端轮询展示失败原因
```

## 9. ACK、重试与死信

V1.2 初版建议：

- consumer 手动 ACK；
- 处理成功后 ACK；
- 反序列化、校验或保存失败时记录错误；
- 可重试异常进入基础重试；
- 多次失败后进入 `agent.dead.queue`；
- `messageId` 用于消费日志或幂等；
- `workflowId + taskId + messageType` 可辅助判断重复消息。

不承诺：

- Exactly-once；
- 分布式事务；
- Outbox 完整可靠投递；
- 生产级消息堆积治理。

## 10. Java / Python 边界

Java 不直接查询 Python 内部状态；Java 只通过：

- AgentMessage；
- Artifact；
- workflow / task 状态消息；
- Python HTTP health check。

Python 不直接查询 Java 业务数据库；Python 只处理：

- Java 提供的 `AgentProfileContext`；
- AgentMessage payload；
- 自身规则 Agent 产物。

当前用户隔离仍由 Java Controller 和 `CurrentUserProvider` 保证。Python 不能接收前端传入的任意 `userId` 来绕过 Java 权限。

## 11. PAYMENT_SUCCESS 消息语义

`PAYMENT_SUCCESS` 在 Agent workflow 中必须被视为：

```text
偏好确认
当前需求满足
互补需求触发
```

Critic 在收到 `ProfileDraft` 后必须检查：

- fulfilled SKU / SPU 是否进入 `doNotRecommend`；
- 是否还在继续推荐 fulfilled SKU / SPU；
- complementOpportunities 是否有 evidence；
- 是否存在无证据强推断。

## 12. 与 V1.1 文档关系

V1.1 行为事件与 Agent 通信边界见 [RabbitMQ 行为事件与 Agent 通信说明](01-rabbitmq-agent-bus.md)。V1.1 已完成 behavior bus，V1.2 计划正式启用 Agent workflow bus。

