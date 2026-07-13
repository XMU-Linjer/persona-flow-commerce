# PersonaFlow Commerce

PersonaFlow Commerce 是一个面向简历、面试和工程展示的全栈项目。

项目定位：

> 基于 Spring Boot + RabbitMQ + FastAPI 的电商行为事件驱动用户画像系统，从浏览、搜索、收藏、加购到支付的全链路行为采集，通过规则 Agent Team + DeepSeek 语义增强自动生成用户画像，并基于画像实现首页个性化推荐与 AI 购物洞察展示。

项目从 V1.0 电商主链路起步，V1.1 加入行为事件采集与画像闭环，当前已进一步扩展了推荐能力（首页个性化推荐、DeepSeek 语义互补发现）和画像自动刷新机制（事件驱动的混合触发器）。

## 技术栈

后端（Java 17 + Spring Boot 3）：

- Spring MVC · Spring Security + JWT · MyBatis-Plus
- MySQL 8.4 · Redis · RabbitMQ · Flyway
- JUnit 5 · Mockito · AssertJ

Python Agent：

- Python 3 · FastAPI · Pydantic · pytest
- 规则版 Profile Agent Team（Behavior / Intent / Trend / Critic）
- DeepSeek API 语义增强（可选接入）

前端：

- Vue 3 · TypeScript · Vite · Vue Router · Pinia
- Axios · Element Plus

基础设施：

- Docker Compose（MySQL + Redis + RabbitMQ）

## 当前完成状态

### V1.0 电商主链路

| 模块 | 能力 |
|------|------|
| account | 注册 / 登录 · 用户资料 · 修改密码 · 收货地址管理 · `CurrentUserProvider` · `AddressQueryApi` |
| catalog | 商品分类 · 商品列表 · 商品详情 · SKU 查询 · Redis 缓存 · `ProductQueryApi` · `ProductSnapshot` |
| shopping | 收藏 · 购物车 · 数量修改 · 清空 |
| trade | 库存三段模型 · 创建订单 · 订单列表/详情 · 取消订单 · 模拟支付 · 支付记录 |
| 前端 | 登录/注册 · 商品列表/详情 · 收藏 · 购物车 · 地址管理 · 结算页 · 订单管理 |

### V1.1 行为采集与画像闭环

行为数据存储：

- `behavior_event` 表（eventId 幂等落库）
- `behavior_consume_log` 表（消息级别消费追踪）
- `user_profile_version` 表（画像版本快照）

RabbitMQ 行为事件总线：

- `commerce.behavior.exchange` 交换器
- `behavior.persist.queue` / `behavior.dead.queue`
- 手动 ACK · 基础重试 · 死信队列 · consume log · eventId 幂等

行为发布接入：

- catalog → `PRODUCT_VIEW` / `PRODUCT_SEARCH`
- shopping → `FAVORITE_ADD` / `FAVORITE_REMOVE` / `CART_ADD` / `CART_REMOVE` / `CART_CLEAR`
- trade → `ORDER_CREATED` / `PAYMENT_SUCCESS` / `ORDER_CANCELED`

Python Agent（`persona-agent-service`）：

- `GET /health` — 健康检查
- `POST /agent/profile/build` — 同步画像构建
- Profile Manager 编排 BehaviorAgent / IntentAgent / TrendAgent / ProfileBuilderCritic
- DeepSeekRecommendationAgent（可选）：通过 DeepSeek API 语义增强画像，生成自然语言摘要、偏好标签、互补机会、风险检查
- DeepSeek 不可用时自动回退到规则画像（`FALLBACK_RULE_BASED`）

Java ↔ Agent 链路：

- Java 构建 `AgentProfileContext`（聚合最近事件、关键词、需求信号、证据链）
- Java 通过 HTTP 调用 Python Agent 的 `/agent/profile/build`
- Agent 返回结构化画像结果（含 `profile` JSON、`complementOpportunities`、`doNotRecommend` 等）
- Java 保存 `user_profile_version`，前端查询最新画像

### 推荐与画像增强（当前已实现）

首页个性化推荐：

- 商品列表页顶部新增"为你推荐"区域，最多展示 6 个推荐商品
- 优先读取画像的 `complementOpportunities` 标签作为搜索关键词
- 回退使用用户行为摘要中的 `recentKeywords`
- 自动排除 `doNotRecommend` 列表中的 SPU

AI 购物洞察页（`/ai-insights`）：

- 画像摘要 · 需求状态 · 已满足需求 · 配套机会 · 不再推荐项
- 基于画像的推荐商品（配套机会关键词 → 商品搜索）
- 行为摘要 · 行为证据 · 最近行为事件列表
- Python Agent 不可用时显示错误，不白屏

互补推荐机制（profile-builder-critic 双路径）：

1. **规则版**（始终可用）：从用户浏览、搜索、收藏、加购等行为中提取关键词，直接作为推荐种子词
2. **DeepSeek 语义增强版**（可选配置 `DEEPSEEK_API_KEY`）：对规则画像进行语义级别的补充，生成更丰富的 `complementOpportunities`、`preferenceTags`、`riskChecks`，通过 Critic 审核后方可合并

画像自动刷新调度器（`ProfileRefreshScheduler`）：

- **混合触发器**：计数阈值（累积 N 条新事件触发）+ 最大延迟（T 秒兜底）+ 空闲抑制（无事件不启动定时器）
- 在 `BehaviorEventConsumer` 成功落库后自动调用 `onNewEvent()`
- 防止高并发下 Python Agent 被打穿，同时确保低频用户的最后一小批事件也能最终刷新
- 配置项：`behavior.profile.refresh.batch-threshold=5` / `max-delay-seconds=30` / `enabled=true`
- 定期清理僵尸条目（`Scheduled` 任务每 5 分钟）

## 核心业务链路

```text
注册 / 登录
→ 首页看到「为你推荐」个性化商品
→ 浏览商品列表并搜索商品
→ 进入商品详情并选择 SKU
→ 收藏 SKU 或加入购物车
→ 新增收货地址
→ 立即购买或从购物车结算
→ 创建订单 → 锁定库存 available → locked
→ 查看订单详情
→ 模拟支付 → 确认库存 locked → sold
→ 行为事件通过 RabbitMQ 异步落库
→ ProfileRefreshScheduler 自动触发画像刷新
→ 打开 /ai-insights 查看画像摘要、配套机会、推荐商品
→ 画像伴随新行为自动持续更新
```

## 后端工程亮点

- 模块边界清晰，按 account / catalog / shopping / trade / behavior 分层
- trade 不直接读取 account / catalog / shopping 表
- 订单保存地址快照和商品快照，避免历史订单被后续变更影响
- 库存 `available_quantity` / `locked_quantity` / `sold_quantity` 三段模型，条件更新防止并发超卖
- 订单状态机：`PENDING_PAYMENT(10)` → `PAID(20)` · `CANCELED(30)`
- 统一 API 响应结构，`ApiResponse.code` 整数，业务错误使用 `errorCode` 字符串
- RabbitMQ 手动 ACK + 重试 + 死信 + consume log + eventId 幂等
- 行为事件总线解耦：各业务模块只发布事件，behavior 模块独立消费、持久化和画像刷新
- Java 负责权限、事务、行为上下文和画像版本保存；Python Agent 只处理结构化画像生成
- 画像刷新从 HTTP 请求中解耦：新增 `refreshByUserId()` 方法，无需 SecurityContext，支持 RabbitMQ 消费线程异步调用

## PAYMENT_SUCCESS 语义

`PAYMENT_SUCCESS` 不是"继续推荐当前商品"的简单正反馈。它表示：

- 偏好确认 — 用户用真实支付确认了偏好
- 当前需求满足 — 该 SKU/SPU 的需求已经关闭
- 配套需求触发 — 可以推荐耗材、配件、复购品类或相邻场景商品
- 短期内避免重复推荐同一 SKU/SPU（列入 `doNotRecommend`）

非支付行为（浏览、搜索、收藏、加购）同样参与画像构建和推荐生成——用户不需要完成支付就能看到个性化推荐。

## 本地启动

准备 `.env` 文件（参考 `.env.example`），不提交。

### 方式一：一键启动

```powershell
.\scripts\start-dev.ps1
```

### 方式二：手动分终端

```powershell
# 1. 启动依赖（MySQL / Redis / RabbitMQ）
docker compose up -d

# 2. Java 后端
cd persona-commerce-server
.\mvnw.cmd spring-boot:run

# 3. Python Agent
cd persona-agent-service
$env:PYTHONPATH = "src"
python -m uvicorn persona_agent_service.main:app --host 127.0.0.1 --port 8001

# 4. Vue 前端
cd persona-commerce-web
npm.cmd install
npm.cmd run dev -- --host 127.0.0.1
```

常用地址：

```text
Vue:           http://127.0.0.1:5173/
Java health:   http://127.0.0.1:8080/actuator/health
Python health: http://127.0.0.1:8001/health
RabbitMQ UI:   http://127.0.0.1:15672
```

## 验证命令

```powershell
# Java 后端
cd persona-commerce-server
.\mvnw.cmd test

# Python Agent
cd persona-agent-service
$env:PYTHONPATH = "src"
python -m pytest

# Vue 前端
cd persona-commerce-web
npm.cmd run build
```

## 当前未实现内容

- 真实支付 / 微信支付 / 支付宝 / Stripe / 支付回调 / 退款 / 售后
- 物流系统
- 优惠券 / 满减 / 秒杀 / 店铺 / 商家系统
- admin 管理后台
- 真实推荐算法（协同过滤、向量召回等，当前为规则 + 语义增强）
- Outbox 模式 / 分布式事务
- 生产级高并发压测

DeepSeek API 为可选接入——不配置 `DEEPSEEK_API_KEY` 时系统自动降级为纯规则画像，不影响核心功能。

## 文档

- [V1.0 架构设计](docs/v1.0-architecture.md)
- [V1.1 架构设计](docs/v1.1-architecture.md)
- [V1.2 架构设计（planned）](docs/v1.2-architecture.md)
- [模块约定](docs/module-contracts.md)
- [account](docs/modules/01-account.md) · [catalog](docs/modules/02-catalog.md) · [shopping](docs/modules/03-shopping.md) · [trade](docs/modules/04-trade.md) · [behavior](docs/modules/05-behavior.md)
- [RabbitMQ 行为事件与 Agent 总线](docs/messaging/01-rabbitmq-agent-bus.md)
- [Agent 工作流消息总线（planned）](docs/messaging/02-agent-workflow-bus.md)
- [Profile Agent Team](docs/agents/01-profile-agent-team.md)
- [异步 Profile Agent 工作流（planned）](docs/agents/02-async-profile-agent-workflow.md)
- [架构历史](docs/architecture-history.md)

## License

暂未添加开源许可证。