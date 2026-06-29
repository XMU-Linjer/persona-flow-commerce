# RabbitMQ 行为事件与 Agent 任务总线设计

> 状态：V1.1 设计中  
> 范围：RabbitMQ exchange、queue、routing key、消息协议和可靠性策略  
> 更新时间：2026-06-29

## 1. 设计目标

V1.1 使用 RabbitMQ 承担两类异步通信：

- 行为事件总线：业务模块发布用户行为，behavior 模块消费并落库；
- Agent 任务总线：Profile Manager 和各 Agent 之间传递任务、Artifact、挑战、返工和完成状态。

两条链路使用不同 exchange，避免业务事件和 Agent 内部工作流耦合。

## 2. 行为事件总线

### 2.1 Exchange

```text
commerce.behavior.exchange
```

类型：`topic`

用途：catalog、shopping、trade 发布用户行为事件，behavior 模块消费并持久化。

### 2.2 Routing Keys

```text
behavior.product.view
behavior.product.search
behavior.favorite.add
behavior.favorite.remove
behavior.cart.add
behavior.cart.remove
behavior.cart.clear
behavior.order.created
behavior.payment.success
behavior.order.canceled
```

### 2.3 Queues

```text
behavior.persist.queue
behavior.dead.queue
```

### 2.4 链路

```text
catalog / shopping / trade
-> commerce.behavior.exchange
-> behavior.persist.queue
-> behavior_event
```

## 3. Agent 任务总线

### 3.1 Exchange

```text
commerce.agent.exchange
```

类型：`topic`

用途：Profile Manager 分发 Agent 任务，Agents 之间交换结构化 Artifact、challenge、revision 和 completion status。

### 3.2 Routing Keys

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

### 3.3 Queue 建议

```text
agent.profile-manager.queue
agent.behavior.queue
agent.intent.queue
agent.trend.queue
agent.profile-builder.queue
agent.dead.queue
```

### 3.4 链路

```text
Profile Manager
-> commerce.agent.exchange
-> Behavior Agent / Intent Agent / Trend Agent / Profile Builder-Critic
-> Artifact
-> user_profile_version
```

## 4. Agent 统一消息协议

Agent 消息统一使用 JSON，并通过 `workflowId`、`taskId`、`messageId` 和 `correlationId` 串联工作流。

```json
{
  "messageId": "msg-001",
  "workflowId": "wf-001",
  "taskId": "task-001",
  "sender": "ProfileManager",
  "receiver": "TrendAgent",
  "messageType": "TASK_ASSIGNED",
  "artifactType": null,
  "artifactId": null,
  "correlationId": "msg-parent",
  "timestamp": "2026-06-29T10:00:00Z",
  "payload": {}
}
```

字段说明：

| 字段 | 说明 |
|---|---|
| messageId | 消息唯一 ID |
| workflowId | 一次画像工作流 ID |
| taskId | 当前任务 ID |
| sender | 发送者 |
| receiver | 接收者 |
| messageType | 消息类型 |
| artifactType | Artifact 类型，可空 |
| artifactId | Artifact ID，可空 |
| correlationId | 父消息或关联消息 ID |
| timestamp | 发送时间 |
| payload | 结构化载荷 |

## 5. Agent 消息类型

| messageType | 说明 |
|---|---|
| `TASK_ASSIGNED` | Profile Manager 分配任务 |
| `ARTIFACT_CREATED` | Agent 产出结构化 Artifact |
| `CHALLENGE_RAISED` | Agent 或 Builder 对结果提出质疑 |
| `REVISION_REQUESTED` | 请求返工 |
| `REVISION_COMPLETED` | 返工完成 |
| `TASK_COMPLETED` | 单个任务完成 |
| `TASK_FAILED` | 单个任务失败 |
| `WORKFLOW_COMPLETED` | 整个画像工作流完成 |

## 6. Artifact 传递规则

Agent 之间不传递任意自然语言结论，而是传递结构化 Artifact。

允许的 Artifact：

- `BehaviorFactReport`
- `IntentReport`
- `TrendReport`
- `ProfileDraft`
- `ProfileAuditReport`
- `UserProfileVersion`

Artifact 可以先保存在 Java 后端或 Python Agent 服务自己的存储中，RabbitMQ 消息只传递 `artifactId`、`artifactType` 和必要摘要。

## 7. V1.1 首轮必须设计并说明的能力

V1.1 第一轮需要设计并尽量实现：

- 消息持久化；
- 手动 ACK；
- 死信队列；
- 基础重试；
- 消费幂等。

行为事件的幂等键建议使用 `eventId`。Agent 消息的幂等键建议使用 `messageId`。

## 8. V1.1 首轮可设计但不强制实现的增强

以下能力进入 V1.1 后续增强，不要求第一轮实现：

- Publisher Confirm；
- 消息积压处理；
- Outbox 可靠投递；
- Agent 返工消息闭环的完整可视化；
- Agent 工作流超时恢复；
- 多 Agent 并发调度优化。

这些能力必须在文档中留出扩展方向，但不能在第一轮为了追求完整而扩大实现范围。

## 9. 行为事件失败策略

- 事件发布失败不回滚主业务；
- 消费失败先重试；
- 多次失败进入 `behavior.dead.queue`；
- 重复事件不重复写入 `behavior_event`；
- 失败消息需要保留错误原因，便于后续排查。

## 10. Agent 任务失败策略

- Agent 单任务失败发布 `agent.task.failed`；
- Profile Manager 负责判断是否重试、降级或终止工作流；
- Profile Builder/Critic 可以请求一次返工；
- 工作流最终失败不影响 V1.0 电商主链路。

## 11. 当前设计结论

V1.1 RabbitMQ 同时服务两类链路：

- `commerce.behavior.exchange` 用于业务行为事件；
- `commerce.agent.exchange` 用于 Agent 任务通信。

两者职责分离、消息协议分离、队列分离。behavior 事件服务业务事实沉淀，Agent 任务总线服务画像团队协作。
