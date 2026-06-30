# 05-behavior 行为事件模块

> 状态：completed  
> 所属版本：V1.1.0  
> 范围：Java 后端 behavior 模块、RabbitMQ behavior 事件消费、画像上下文、画像版本保存  
> 更新时间：2026-06-30

## 1. 模块定位

behavior 是 V1.1 新增的 Java 后端模块，负责把 V1.0 电商主链路产生的用户行为事实沉淀为结构化数据，并为 Python Profile Agent Team 准备画像上下文。

behavior 不负责真实推荐算法，不直接修改商品、购物车、订单、库存或支付状态，也不替代 Java 后端的权限、事务和业务校验。

## 2. 已完成能力

behavior 模块已完成：

- 行为事件模型；
- 行为事件持久化；
- RabbitMQ 消费日志；
- `eventId` 幂等落库；
- 当前用户行为查询；
- 行为摘要统计；
- AgentProfileContext 生成；
- `PAYMENT_SUCCESS` 需求状态标记；
- Python Agent 调用；
- 用户画像版本保存；
- 前端 AI 购物洞察所需接口。

## 3. 不做什么

behavior 不实现：

- 真实推荐算法；
- 真实 LLM；
- RAG；
- Outbox；
- 分布式事务；
- Agent 异步任务总线调度主链路；
- admin 行为查询后台；
- 营销自动化；
- 购物车清理；
- 库存扣减；
- 订单状态变更；
- 真实支付、退款、物流或优惠券。

## 4. 核心概念

| 概念 | 说明 |
|---|---|
| BehaviorEvent | 用户行为事实 |
| BehaviorEventPublisher | catalog / shopping / trade 的事件发布接口 |
| BehaviorEventConsumer | 从 RabbitMQ 消费行为事件并落库 |
| BehaviorConsumeLog | 消息消费幂等、状态和错误记录 |
| BehaviorSummary | 当前用户行为摘要 |
| AgentProfileContext | 给 Python Agent 的结构化画像上下文 |
| UserProfileVersion | Java 保存的用户画像版本 |

## 5. 行为类型

当前固定行为类型：

- `PRODUCT_VIEW`
- `PRODUCT_SEARCH`
- `FAVORITE_ADD`
- `FAVORITE_REMOVE`
- `CART_ADD`
- `CART_REMOVE`
- `CART_CLEAR`
- `ORDER_CREATED`
- `PAYMENT_SUCCESS`
- `ORDER_CANCELED`

## 6. PAYMENT_SUCCESS 语义

`PAYMENT_SUCCESS` 不意味着继续推荐当前已购买商品。

它同时表示：

- 偏好确认；
- 当前具体需求满足；
- 互补需求触发。

在 AgentProfileContext 中，支付成功信号会进入 `fulfilledNeeds`，并带有：

- `preferenceConfirmed = true`
- `fulfilled = true`
- `complementTrigger = true`

短期策略应避免重复推荐同一 SKU / SPU，转向配件、耗材、复购周期、相邻品类或下一阶段需求。

## 7. 数据表

### 7.1 behavior_event

保存用户行为事实。

关键字段：

| 字段 | 说明 |
|---|---|
| id | 主键 |
| event_id | 全局唯一行为事件 ID |
| user_id | 用户 ID |
| event_type | 行为类型 |
| source_module | 来源模块 |
| object_type | 行为对象类型 |
| object_id | 行为对象 ID |
| keyword | 搜索关键词，可空 |
| sku_id | SKU ID，可空 |
| spu_id | SPU ID，可空 |
| category_id | 分类 ID，可空 |
| order_id | 订单 ID，可空 |
| amount | 金额，可空 |
| payload_json | 扩展上下文 JSON |
| occurred_at | 业务发生时间 |
| created_at | 落库时间 |

约束和索引：

- `event_id` 唯一；
- `user_id + occurred_at` 索引；
- `event_type + occurred_at` 索引；
- `sku_id` / `spu_id` / `category_id` 普通索引。

敏感信息要求：

- 不保存密码；
- 不保存 JWT；
- 不保存收货人手机号；
- 不保存完整收货地址。

### 7.2 behavior_consume_log

保存 RabbitMQ 消费记录。

关键字段：

| 字段 | 说明 |
|---|---|
| id | 主键 |
| message_id | 消息 ID |
| event_id | 行为事件 ID |
| status | 数字状态 |
| retry_count | 重试次数 |
| error_message | 错误信息 |
| created_at | 创建时间 |
| updated_at | 更新时间 |

用途：

- 识别重复 message；
- 记录 PROCESSING / SUCCESS / FAILED；
- 辅助排查消费失败；
- 配合死信队列处理持续失败消息。

### 7.3 user_profile_version

保存画像版本，不覆盖历史。

关键字段：

| 字段 | 说明 |
|---|---|
| id | 主键 |
| user_id | 用户 ID |
| version_no | 版本号 |
| profile_json | 结构化画像结果 JSON |
| summary | 面向前端的摘要 |
| source_workflow_id | Python Agent 工作流 ID |
| created_at | 创建时间 |

索引：

- `user_id + created_at`

## 8. RabbitMQ 行为事件消费

```text
commerce.behavior.exchange
-> behavior.persist.queue
-> BehaviorEventConsumer
-> behavior_event
```

已实现：

- topic exchange；
- 持久化消息；
- 手动 ACK；
- 基础重试；
- 死信队列 `behavior.dead.queue`；
- consume log；
- `eventId` 幂等；
- 消费失败记录 FAILED；
- 持续失败消息进入死信。

## 9. 行为发布规则

catalog：

- 商品详情成功返回后发布 `PRODUCT_VIEW`；
- 商品关键词搜索成功后发布 `PRODUCT_SEARCH`。

shopping：

- 收藏成功后发布 `FAVORITE_ADD`；
- 取消收藏成功后发布 `FAVORITE_REMOVE`；
- 加购成功后发布 `CART_ADD`；
- 删除购物车项成功后发布 `CART_REMOVE`；
- 清空购物车成功后发布 `CART_CLEAR`。

trade：

- 订单创建成功后发布 `ORDER_CREATED`；
- 模拟支付成功后发布 `PAYMENT_SUCCESS`；
- 订单取消成功后发布 `ORDER_CANCELED`。

发布失败不回滚主业务。

## 10. AgentProfileContext

behavior 为 Python Agent 准备结构化上下文。

字段包括：

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

behavior 不拼 prompt，不调用真实 LLM，不决定最终推荐排序。

## 11. HTTP 接口

当前接口：

```http
GET  /api/behavior/me/events
GET  /api/behavior/me/summary
GET  /api/behavior/me/agent-context
GET  /api/behavior/me/profile/latest
POST /api/behavior/me/profile/refresh
```

参数：

- `/events` 支持 `limit` 和 `eventType`；
- `/summary` 支持 `days`；
- `/agent-context` 支持 `days`；
- `/profile/refresh` 支持 `days`。

安全规则：

- 全部需要登录；
- 只查询当前用户；
- Controller 不接收 `userId`；
- 不新增 admin 行为查询；
- 不暴露敏感信息。

## 12. Java 调 Python Agent

```text
BehaviorProfileRefreshService
-> BehaviorContextService
-> HttpAgentProfileClient
-> POST /agent/profile/build
-> UserProfileVersionService
-> user_profile_version
```

失败策略：

- Python Agent 不可用时返回 `AGENT_SERVICE_UNAVAILABLE`；
- Python 返回非法结果时返回 `AGENT_PROFILE_BUILD_FAILED`；
- 不影响普通商品、购物车、订单等 V1.0 主链路。

## 13. 错误码

| 错误码 | HTTP | 说明 |
|---|---:|---|
| `BEHAVIOR_EVENT_INVALID` | 400 | 行为事件格式非法 |
| `BEHAVIOR_EVENT_DUPLICATE` | 409 | 行为事件重复 |
| `BEHAVIOR_PROFILE_NOT_FOUND` | 404 | 用户画像不存在 |
| `BEHAVIOR_PROFILE_TASK_FAILED` | 409 | 画像任务失败 |
| `AGENT_SERVICE_UNAVAILABLE` | 503 | Python Agent 不可用 |
| `AGENT_PROFILE_BUILD_FAILED` | 409 | Agent 画像生成失败 |

错误响应沿用统一结构：`ApiResponse.code` 为 int，业务错误码在 `errorCode` 中返回。

## 14. 验证结果

当前已验证：

- Java 后端：219 tests passed；
- behavior 事件可经 RabbitMQ 消费并落库；
- 重复 `eventId` 不重复落库；
- consume log 可记录成功和失败；
- 业务事件发布失败不影响主业务；
- 当前用户可查询行为、摘要、Agent 上下文和画像版本；
- `/api/behavior/me/profile/refresh` 能真实调用 Python Agent；
- Python Agent 不可用时返回 `AGENT_SERVICE_UNAVAILABLE`。

## 15. 结论

behavior 是 V1.1 的行为数据沉淀层和画像上下文准备层。它把 V1.0 的业务事实转化为可查询、可追溯、可版本化的画像输入，但不承担真实推荐算法、真实 LLM、RAG 或业务状态修改职责。
