# PersonaFlow Commerce

PersonaFlow Commerce 是一个面向简历、面试和工程展示的全栈项目。

项目定位：

> 基于 Spring Boot + RabbitMQ + FastAPI 的电商行为事件驱动用户画像系统。

它不是“AI 推荐商城”，也不是只做页面演示的 CRUD 项目。V1.0 先完成可跑通的电商主链路，V1.1 在主链路之上加入行为事件采集、RabbitMQ 可靠消费、结构化画像上下文、规则版 Profile Agent Team 和前端 AI 购物洞察展示。

## 技术栈

后端：

- Java 17
- Spring Boot 3
- Spring MVC
- Spring Security + JWT
- MyBatis-Plus
- MySQL 8.4
- Redis
- RabbitMQ
- Flyway
- Maven
- JUnit 5 / Mockito

Python Agent：

- Python
- FastAPI
- Pydantic
- pytest
- 规则版 Profile Agent Team

前端：

- Vue 3
- TypeScript
- Vite
- Vue Router
- Pinia
- Axios
- Element Plus

基础设施：

- Docker Compose
- MySQL
- Redis
- RabbitMQ

## 当前完成状态

### V1.0 电商主链路

account：

- 用户注册 / 登录
- 当前用户资料
- 修改密码
- 收货地址管理
- `CurrentUserProvider`
- `AddressQueryApi`

catalog：

- 商品分类
- 商品列表
- 商品详情
- SKU 查询
- Redis 商品详情缓存
- `ProductQueryApi`
- `ProductSnapshot`

shopping：

- 收藏 SKU
- 取消收藏
- 收藏列表
- 加入购物车
- 修改购物车数量
- 删除购物车项
- 清空购物车
- 购物车列表

trade：

- 库存三段模型
- 创建订单
- 订单列表 / 订单详情
- 取消订单
- 模拟支付
- 支付记录

前端：

- 登录 / 注册
- 商品列表 / 商品详情
- 收藏
- 购物车
- 地址管理
- 结算页
- 订单列表 / 订单详情
- 取消订单
- 模拟支付

### V1.1 行为与画像链路

行为数据：

- `behavior_event`
- `behavior_consume_log`
- `user_profile_version`

RabbitMQ 行为事件总线：

- `commerce.behavior.exchange`
- `behavior.persist.queue`
- `behavior.dead.queue`
- 手动 ACK
- 基础重试
- 死信队列
- consume log
- `eventId` 幂等落库

行为发布接入：

- catalog 发布 `PRODUCT_VIEW` / `PRODUCT_SEARCH`
- shopping 发布 `FAVORITE_ADD` / `FAVORITE_REMOVE` / `CART_ADD` / `CART_REMOVE` / `CART_CLEAR`
- trade 发布 `ORDER_CREATED` / `PAYMENT_SUCCESS` / `ORDER_CANCELED`

行为查询与画像上下文：

- 当前用户最近行为查询
- 行为摘要统计
- `AgentProfileContext`
- 需求状态建模
- `PAYMENT_SUCCESS` 被解释为“偏好确认 + 当前需求满足 + 互补需求触发”

Python Agent：

- `persona-agent-service`
- `GET /health`
- `POST /agent/profile/build`
- 规则版 Profile Agent Team
- 不接真实 LLM
- 不接 RAG

Java 与 Agent 链路：

- Java 后端准备 `AgentProfileContext`
- Java 通过 HTTP 调用 Python Agent
- Agent 返回结构化画像结果
- Java 保存 `user_profile_version`
- 前端查询最新画像

前端 AI 购物洞察：

- `/ai-insights`
- latest profile 展示
- fulfilledNeeds 展示
- complementOpportunities 展示
- doNotRecommend 展示
- 行为摘要和最近行为展示
- Agent 不可用时显示错误，不白屏

## 核心业务链路

```text
注册 / 登录
-> 浏览商品列表并搜索商品
-> 进入商品详情并选择 SKU
-> 收藏 SKU 或加入购物车
-> 新增收货地址
-> 立即购买或从购物车结算
-> 创建订单
-> 锁定库存 available -> locked
-> 查看订单详情
-> 模拟支付
-> 确认库存 locked -> sold
-> 打开 AI 购物洞察
-> 刷新画像
-> 查看已满足需求、配套机会和不再推荐项
```

## 后端工程亮点

- 模块边界清晰，按 account / catalog / shopping / trade / behavior 分层。
- trade 不直接读取 account / catalog / shopping 表。
- 创建订单只通过 `CurrentUserProvider`、`AddressQueryApi`、`ProductQueryApi` 协作。
- 订单保存地址快照和商品快照，避免历史订单被后续资料或商品变更影响。
- 库存使用 `available_quantity` / `locked_quantity` / `sold_quantity` 三段模型。
- 创建订单锁库存：`available -> locked`。
- 取消订单释放库存：`locked -> available`。
- 模拟支付确认扣减：`locked -> sold`。
- 库存更新使用条件更新，避免并发超卖。
- 订单状态机限制非法流转：
  - `10 = PENDING_PAYMENT`
  - `20 = PAID`
  - `30 = CANCELED`
- 统一 API 响应结构，`ApiResponse.code` 保持整数，业务错误使用字符串 `errorCode`。
- RabbitMQ 行为事件消费支持手动 ACK、重试、死信和 consume log。
- 行为事件使用 `eventId` 保证幂等落库。
- Java 负责权限、事务、行为上下文和画像版本保存；Python Agent 只处理结构化画像生成。

## PAYMENT_SUCCESS 语义

`PAYMENT_SUCCESS` 不是“继续推荐当前商品”的简单正反馈。

在 V1.1 中，它表示：

- 偏好确认；
- 当前具体需求已经满足；
- 可以触发配套需求、耗材、配件、复购周期或相邻场景探索；
- 短期内应避免重复推荐同一 SKU / SPU。

## 本地启动

准备项目根目录 `.env`。可以参考 `.env.example`。`.env` 会被 Docker Compose 使用，也会被 Spring Boot 本地启动自动读取；`.env` 不提交。

启动依赖：

```powershell
cd D:\Workspace\persona-flow-commerce
docker compose up -d
```

启动 Java 后端：

```powershell
cd D:\Workspace\persona-flow-commerce\persona-commerce-server
.\mvnw.cmd spring-boot:run
```

后端健康检查：

```powershell
curl.exe http://127.0.0.1:8080/actuator/health
```

启动 Python Agent：

```powershell
cd D:\Workspace\persona-flow-commerce\persona-agent-service
$env:PYTHONPATH = "src"
python -m uvicorn persona_agent_service.main:app --host 127.0.0.1 --port 8001
```

Agent 健康检查：

```powershell
curl.exe http://127.0.0.1:8001/health
```

启动 Vue 前端：

```powershell
cd D:\Workspace\persona-flow-commerce\persona-commerce-web
npm.cmd install
npm.cmd run dev -- --host 127.0.0.1
```

访问地址：

```text
http://127.0.0.1:5173/
```

PowerShell 下如果 `npm.ps1` 被拦截，建议使用 `npm.cmd`。

## V1.1 演示流程

1. 注册新用户。
2. 登录。
3. 浏览商品列表。
4. 搜索商品。
5. 进入商品详情。
6. 选择 SKU。
7. 收藏商品。
8. 加入购物车。
9. 新增收货地址。
10. 立即购买或从购物车结算。
11. 创建订单。
12. 查看订单详情。
13. 模拟支付。
14. 再创建订单并取消。
15. 打开 `/ai-insights`。
16. 点击“刷新画像”。
17. 查看 `fulfilledNeeds`、`complementOpportunities`、`doNotRecommend`、行为摘要和最近行为。
18. 停止 Python Agent 后再次刷新画像，可看到 `AGENT_SERVICE_UNAVAILABLE`，页面不白屏。

## 验证结果

当前已验证：

- Java 后端：219 tests passed
- Python Agent：21 pytest passed
- Vue 前端：`npm.cmd run build` passed
- 真实联调：
  - Python `/health` UP
  - Java `/actuator/health` UP
  - db / rabbit / redis UP
  - `/api/behavior/me/profile/refresh` 能真实调用 Python
  - `/api/behavior/me/profile/latest` 能查到画像
  - `/ai-insights` 能展示 latest profile、fulfilledNeeds、complementOpportunities、doNotRecommend
  - Python Agent 停止时刷新画像返回 `AGENT_SERVICE_UNAVAILABLE`，页面不白屏

验证命令：

```powershell
cd D:\Workspace\persona-flow-commerce\persona-commerce-server
.\mvnw.cmd test
```

```powershell
cd D:\Workspace\persona-flow-commerce\persona-agent-service
$env:PYTHONPATH = "src"
python -m pytest
```

```powershell
cd D:\Workspace\persona-flow-commerce\persona-commerce-web
npm.cmd run build
```

## 当前未实现内容

当前项目没有实现：

- 真实支付
- 微信支付 / 支付宝支付 / Stripe
- 支付回调
- 支付失败处理
- 退款
- 售后
- 物流
- 优惠券 / 满减 / 秒杀
- 店铺 / 商家系统
- admin 管理后台
- 真实推荐算法
- 真实 LLM
- RAG
- Outbox
- 分布式事务
- 生产级高并发压测

## 文档

- [V1.0 架构设计](docs/v1.0-architecture.md)
- [V1.1 架构设计](docs/v1.1-architecture.md)
- [模块约定](docs/module-contracts.md)
- [account 模块](docs/modules/01-account.md)
- [catalog 模块](docs/modules/02-catalog.md)
- [shopping 模块](docs/modules/03-shopping.md)
- [trade 模块](docs/modules/04-trade.md)
- [behavior 模块](docs/modules/05-behavior.md)
- [RabbitMQ 行为事件与 Agent 总线](docs/messaging/01-rabbitmq-agent-bus.md)
- [Profile Agent Team](docs/agents/01-profile-agent-team.md)
- [架构历史](docs/architecture-history.md)

## License

暂未添加开源许可证。
