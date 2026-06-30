# PersonaFlow Commerce 架构历史

> 更新时间：2026-06-30

## V1.0.0 - 电商主链路

状态：completed

### 目标

先建立稳定、可演示、可测试的电商业务底座。

### 完成内容

基础设施：

- Docker Compose；
- MySQL；
- Redis；
- RabbitMQ 容器；
- Spring Boot 读取项目根目录 `.env`。

account：

- 注册 / 登录；
- JWT；
- 当前用户；
- 用户资料；
- 修改密码；
- 地址管理；
- `CurrentUserProvider`；
- `AddressQueryApi`。

catalog：

- 分类；
- SPU；
- SKU；
- 商品列表；
- 商品详情；
- Redis 商品详情缓存；
- `ProductQueryApi`；
- `ProductSnapshot`。

shopping：

- 收藏；
- 购物车；
- 不负责订单、支付、库存。

trade：

- `inventory_stock`；
- `trade_order`；
- `trade_order_item`；
- `payment_record`；
- 创建订单；
- 查询订单；
- 取消订单；
- 模拟支付；
- 库存锁定、释放、确认扣减。

frontend：

- Vue 3 + Element Plus；
- 登录 / 注册；
- 商品浏览；
- 收藏；
- 购物车；
- 地址；
- 结算；
- 订单；
- 取消订单；
- 模拟支付。

### 关键设计

- trade 不直接读取 account / catalog / shopping 表；
- 订单保存地址快照和商品快照；
- 库存使用 `available_quantity` / `locked_quantity` / `sold_quantity`；
- 创建订单：`available -> locked`；
- 取消订单：`locked -> available`；
- 模拟支付：`locked -> sold`；
- 订单状态只允许：
  - `PENDING_PAYMENT -> PAID`
  - `PENDING_PAYMENT -> CANCELED`

### V1.0 边界

V1.0 不实现 behavior、Agent、RAG、admin、真实支付、退款、物流和优惠券。

## V1.1.0 - 行为事件与规则版画像链路

状态：completed

### 目标

在 V1.0 电商主链路之上，增加行为事件采集、RabbitMQ 可靠消费、结构化画像上下文、Python 规则版 Profile Agent Team 和前端 AI 购物洞察展示。

项目定位更新为：

> 基于 Spring Boot + RabbitMQ + FastAPI 的电商行为事件驱动用户画像系统。

### 完成内容

behavior 数据层：

- `behavior_event`；
- `behavior_consume_log`；
- `user_profile_version`；
- Flyway `V5__create_behavior_tables.sql`。

RabbitMQ behavior 总线：

- `commerce.behavior.exchange`；
- `behavior.persist.queue`；
- `behavior.dead.queue`；
- 手动 ACK；
- 基础重试；
- 死信队列；
- consume log；
- `eventId` 幂等落库。

业务事件发布：

- catalog 发布 `PRODUCT_VIEW` / `PRODUCT_SEARCH`；
- shopping 发布 `FAVORITE_ADD` / `FAVORITE_REMOVE` / `CART_ADD` / `CART_REMOVE` / `CART_CLEAR`；
- trade 发布 `ORDER_CREATED` / `PAYMENT_SUCCESS` / `ORDER_CANCELED`。

behavior 查询与上下文：

- `/api/behavior/me/events`；
- `/api/behavior/me/summary`；
- `/api/behavior/me/agent-context`；
- `/api/behavior/me/profile/latest`；
- `/api/behavior/me/profile/refresh`；
- `AgentProfileContext`；
- fulfilledNeeds；
- evidenceEventIds；
- `PAYMENT_SUCCESS` 需求状态标记。

Python Agent：

- `persona-agent-service`；
- FastAPI `GET /health`；
- FastAPI `POST /agent/profile/build`；
- 规则版 Profile Agent Team；
- Behavior Agent；
- Intent Agent；
- Trend Agent；
- Profile Builder/Critic。

Java / Python 链路：

- Java 准备 `AgentProfileContext`；
- Java HTTP 调 Python Agent；
- Python 返回结构化画像结果；
- Java 保存 `user_profile_version`；
- Agent 不可用时返回 `AGENT_SERVICE_UNAVAILABLE`。

前端：

- `/ai-insights`；
- latest profile；
- fulfilledNeeds；
- complementOpportunities；
- doNotRecommend；
- 行为摘要；
- 最近行为；
- Agent 不可用错误展示，不白屏。

### PAYMENT_SUCCESS 决策

V1.1 明确：

```text
PAYMENT_SUCCESS = 偏好确认 + 当前需求满足 + 互补需求触发
```

因此成交 SKU / SPU：

- 进入 `fulfilledNeeds`；
- 进入短期 `doNotRecommend`；
- 作为 `complementOpportunities` 的证据；
- 不被简单视为继续推荐当前商品的正反馈。

### 当前未实现

V1.1 没有实现：

- 真实支付；
- 真实推荐算法；
- 真实 LLM；
- RAG；
- Outbox；
- 分布式事务；
- 生产级高并发压测；
- 真实退款 / 物流 / 优惠券；
- admin 管理后台。

### 验证结果

当前已验证：

- Java 后端：219 tests passed；
- Python Agent：21 pytest passed；
- Vue 前端：`npm.cmd run build` passed；
- Python `/health` UP；
- Java `/actuator/health` UP；
- db / rabbit / redis UP；
- `/api/behavior/me/profile/refresh` 能真实调用 Python；
- `/api/behavior/me/profile/latest` 能查到画像；
- `/ai-insights` 能展示 latest profile、fulfilledNeeds、complementOpportunities、doNotRecommend；
- Python Agent 停止时刷新画像返回 `AGENT_SERVICE_UNAVAILABLE`，页面不白屏。

## V1.2.0 - 异步 Profile Agent 工作流与 Artifact 追踪

状态：planned

### 目标

把 V1.1 的同步画像刷新链路升级为异步 workflow，让 Profile Agent Team 的执行过程具备状态追踪、Artifact 留痕、Critic 审核和前端可解释展示。

### 计划内容

Java 后端：

- 创建 `agent_workflow`；
- 维护 `agent_task` 状态；
- 保存 `agent_artifact`；
- 通过 `commerce.agent.exchange` 投递 `AgentMessage`；
- 接收 Agent workflow 结果；
- workflow 成功后保存 `user_profile_version`；
- 提供当前用户 workflow 查询接口。

RabbitMQ Agent Bus：

- 正式使用 `commerce.agent.exchange`；
- 使用 `agent.task.assigned`、`agent.artifact.created`、`agent.task.completed`、`agent.task.failed`、`agent.workflow.completed` 等 routing key；
- 保留基础重试、死信和消息追踪；
- 不承诺完整 Outbox 或分布式事务。

Python Agent：

- Profile Manager 编排 Behavior / Intent / Trend / Profile Builder/Critic；
- 每个 Agent 输出结构化 Artifact；
- Critic 审核 `PAYMENT_SUCCESS` 语义；
- 不接真实 LLM；
- 不直接查询 Java 业务数据库。

前端：

- 在 `/ai-insights` 展示 workflow 列表；
- 展示 task 状态；
- 展示 Artifact；
- 展示 Critic 审核结果与失败原因。

### V1.2 边界

V1.2 planned 不实现：

- 真实 LLM；
- OpenAI / Claude；
- LangChain；
- RAG；
- 向量数据库；
- 复杂推荐算法；
- 真实支付；
- 物流系统；
- 生产级高并发压测；
- 分布式事务；
- Outbox 完整可靠消息。

Admin Lite 可以放在后期展示增强，不是 V1.2 主线。

## 后续可能方向

这些是未来规划，不属于当前已完成状态：

- V1.2 异步 Agent workflow 落地；
- 真实 LLM 画像生成；
- RAG 商品知识增强；
- Outbox 可靠投递；
- 推荐实验与指标；
- admin 运营后台；
- 生产级压测与容量治理。
