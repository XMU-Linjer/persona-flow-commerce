# RabbitMQ 行为事件与 Agent 通信说明

> 状态：completed for behavior bus；Agent task bus 为 Python 服务骨架能力  
> 范围：RabbitMQ exchange、queue、routing key、消息可靠性和当前使用边界  
> 更新时间：2026-06-30

## 1. 总体边界

V1.1 当前真正接入业务主链路的是 behavior 行为事件总线。

已完成：

- Java 后端声明并使用 `commerce.behavior.exchange`；
- catalog / shopping / trade 发布行为事件；
- behavior 消费事件并落库；
- 手动 ACK、基础重试、死信队列、consume log；
- `eventId` 幂等落库。

Python `persona-agent-service` 保留 Agent 消息协议和 `commerce.agent.exchange` 拓扑能力，用于服务骨架和后续异步 Agent 工作流扩展。当前 `/api/behavior/me/profile/refresh` 的真实链路是 Java HTTP 调用 Python `POST /agent/profile/build`，不是异步 Agent 总线。

V1.2 planned 将正式使用 `commerce.agent.exchange` 承载异步 Profile Agent workflow。详细设计见 [Agent 工作流消息总线](02-agent-workflow-bus.md)。

## 2. behavior 行为事件总线

Exchange：

```text
commerce.behavior.exchange
```

类型：

```text
topic
```

队列：

```text
behavior.persist.queue
behavior.dead.queue
```

用途：

- catalog、shopping、trade 发布用户行为事实；
- behavior 模块消费并持久化到 `behavior_event`；
- `behavior_consume_log` 记录消费状态；
- 死信队列接收持续失败消息。

## 3. behavior routing keys

| 行为类型 | Routing key |
|---|---|
| `PRODUCT_VIEW` | `behavior.product.view` |
| `PRODUCT_SEARCH` | `behavior.product.search` |
| `FAVORITE_ADD` | `behavior.favorite.add` |
| `FAVORITE_REMOVE` | `behavior.favorite.remove` |
| `CART_ADD` | `behavior.cart.add` |
| `CART_REMOVE` | `behavior.cart.remove` |
| `CART_CLEAR` | `behavior.cart.clear` |
| `ORDER_CREATED` | `behavior.order.created` |
| `PAYMENT_SUCCESS` | `behavior.payment.success` |
| `ORDER_CANCELED` | `behavior.order.canceled` |

## 4. behavior 消息模型

行为消息字段：

```text
messageId
eventId
eventType
userId
sourceModule
objectType
objectId
keyword
skuId
spuId
categoryId
orderId
amount
payloadJson
occurredAt
traceId
version
```

说明：

- `messageId` 用于消息级追踪；
- `eventId` 用于行为事实幂等；
- `eventType` 必须能映射到 `BehaviorEventType`；
- `payloadJson` 保存扩展上下文；
- 不保存密码、JWT、收货人手机号、完整收货地址。

## 5. behavior 消费流程

```text
收到消息
-> 校验 eventType 和必要字段
-> 写入 / 更新 behavior_consume_log PROCESSING
-> 调用 BehaviorEventService.saveEvent(command)
-> 重复 eventId 返回已有事件，不重复插入
-> 更新 behavior_consume_log SUCCESS
-> 手动 ACK
```

失败流程：

```text
校验失败或保存失败
-> 记录 behavior_consume_log FAILED
-> 抛出消费异常
-> RabbitMQ 基础重试
-> 持续失败进入 behavior.dead.queue
```

## 6. 可靠性策略

已完成：

- durable exchange；
- durable queue；
- 持久化消息；
- 手动 ACK；
- 基础重试；
- 死信队列；
- `messageId` 消费日志；
- `eventId` 行为事件幂等。

未实现：

- Outbox 可靠投递；
- 分布式事务；
- Exactly-once 语义；
- 消息堆积治理；
- 死信可视化运营后台；
- 生产级压测。

当前策略适合本地演示和面试展示：主业务不被行为事件链路阻塞，同时行为消费具备可追踪和可恢复的基础能力。

## 7. 业务模块发布边界

catalog：

- `PRODUCT_VIEW`
- `PRODUCT_SEARCH`

shopping：

- `FAVORITE_ADD`
- `FAVORITE_REMOVE`
- `CART_ADD`
- `CART_REMOVE`
- `CART_CLEAR`

trade：

- `ORDER_CREATED`
- `PAYMENT_SUCCESS`
- `ORDER_CANCELED`

发布原则：

- 只发布已经发生的事实；
- 发布失败不回滚主业务；
- 不在 behavior 消费端修改主业务状态；
- 不在消息中传敏感信息。

## 8. Agent 通信骨架

Python 服务中保留 Agent 通信协议和拓扑命名：

```text
commerce.agent.exchange
```

设计用途：

- Profile Manager 分发任务；
- Agent 间传递结构化 Artifact；
- challenge / revision / completed 等工作流消息；
- 后续扩展异步画像任务。

当前边界：

- V1.1 页面画像刷新链路不依赖 `commerce.agent.exchange`；
- Java 通过 HTTP 调 Python `POST /agent/profile/build`；
- Agent Team 使用规则版同步工作流生成画像；
- 不实现复杂 Agent 异步调度；
- 不实现 Outbox。

## 9. Agent 消息协议方向

保留的 Agent 消息字段：

```text
messageId
workflowId
taskId
sender
receiver
messageType
artifactType
artifactId
payload
createdAt
correlationId
```

保留的消息类型：

- `TASK_ASSIGNED`
- `ARTIFACT_CREATED`
- `CHALLENGE_RAISED`
- `REVISION_REQUESTED`
- `REVISION_COMPLETED`
- `TASK_COMPLETED`
- `TASK_FAILED`
- `WORKFLOW_COMPLETED`

这些用于后续异步 Agent 工作流，不作为当前主演示路径。

V1.2 planned 中，这组协议会升级为正式的 AgentMessage 合同，并配合 `agent_workflow`、`agent_task`、`agent_artifact` 记录 workflow 状态与 Artifact 留痕。

## 10. PAYMENT_SUCCESS 消息语义

`behavior.payment.success` 是画像强信号，但不是“继续推荐当前 SKU”的指令。

它代表：

- 偏好确认；
- 当前需求满足；
- 互补需求触发。

Python Agent 和前端 AI 洞察页会把它展示为已满足需求和配套机会证据。

## 11. 验证结果

当前已验证：

- behavior exchange / queue / binding 配置可用；
- publisher 能发送行为消息；
- consumer 能消费并落库；
- consume log 能记录 SUCCESS / FAILED；
- 重复 `eventId` 不重复落库；
- 非法事件可进入失败路径；
- Java 后端：219 tests passed。

## 12. 结论

V1.1 的 RabbitMQ 重点是 behavior 行为事件可靠采集。Agent 任务总线保留为后续架构扩展方向，当前真实画像刷新链路采用 Java HTTP 调 Python Agent，避免把项目夸大成已经具备生产级异步多 Agent 调度系统。

V1.2 planned 将在不接真实 LLM、不实现完整 Outbox 的前提下，把 Agent 任务总线用于异步 Profile Agent workflow，重点展示状态追踪、Artifact 证据链和 Critic 审核。
