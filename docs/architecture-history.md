# Architecture History

本文记录 PersonaFlow Commerce 每个版本的架构变化。README 展示当前项目状态，具体设计放在对应架构文档中。

## V0.1.0 - 本地开发环境搭建

### 日期

2026-06-24

### 状态

已完成

### 上一版本

无，项目刚开始建立。

### 本版本

完成本地开发环境和基础设施准备：

- Java 17；
- Node.js 与 npm；
- Git；
- Docker Desktop；
- Docker Compose；
- MySQL 8.4；
- Redis；
- RabbitMQ Management。

### 主要变化

- 项目从空目录进入可开发状态；
- 本地中间件统一由 Docker Compose 管理；
- 环境变量通过 `.env` 管理，仓库只提交 `.env.example`。

### 影响

后续 Java 服务统一连接 Docker Compose 中的 MySQL、Redis 和 RabbitMQ。

### 原因

统一开发环境，减少本机安装和版本差异带来的问题。

### 迁移

无。

---

## V1.0.0 - 纯电商平台

### 日期

2026-06-29

### 状态

已完成

### 上一版本

V0.1.0 只完成了开发环境和基础设施，没有业务代码。

### 本版本

完成 V1.0 后端和前端主链路：

- account：注册、登录、当前用户、资料、密码、地址；
- catalog：分类、SPU、SKU、商品列表、商品详情、SKU 查询、Redis 商品详情缓存；
- shopping：收藏、购物车；
- trade：库存、创建订单、订单查询、取消订单、模拟支付；
- Vue 前端演示页面；
- MySQL / Redis / RabbitMQ Docker Compose 基础设施。

### 主要变化

从基础设施阶段进入完整电商主链路：

- 新增 `persona-commerce-server`；
- 新增 `persona-commerce-web`；
- 新增 Flyway 迁移；
- 新增 account、catalog、shopping、trade 核心表；
- 建立统一响应、错误码、JWT 鉴权、库存三段模型和订单状态机；
- 完成前后端本地联调。

### 影响

V1.0 可以作为可演示的纯电商平台。RabbitMQ 容器已准备，但业务事件流、Agent 任务总线和用户画像不属于 V1.0 已完成范围。

### 原因

先建立稳定的电商业务底座，再在 V1.1 引入行为事件、异步消息和多 Agent 画像能力。

### 迁移

通过 Flyway 从空库初始化 V1.0 表结构和演示数据。V0.1.0 没有业务数据需要迁移。

---

## V1.1.0 - 行为事件、RabbitMQ Agent 总线与画像团队

### 日期

待完成

### 状态

设计中

### 上一版本

V1.0.0 已完成纯电商平台，但没有行为事件流、Agent 画像和 RabbitMQ Agent 任务总线。

### 本版本

计划加入 behavior 行为事件模块、RabbitMQ 行为事件总线、RabbitMQ Agent 任务总线、Python Profile Agent Team、用户画像版本和前端 AI 购物洞察展示。

### 主要变化

从纯电商平台扩展为具备行为事件流、异步消息通信和多 Agent 画像能力的 AI 电商项目。

### 影响

新增 `docs/v1.1-architecture.md`、`docs/modules/05-behavior.md`、`docs/messaging/01-rabbitmq-agent-bus.md`、`docs/agents/01-profile-agent-team.md`。

后续会新增 behavior/messaging Java 包、`behavior_event` 等表、RabbitMQ exchange/queue、Python Agent 服务和前端 AI 购物洞察页面。

### 原因

让项目从普通 Java 电商项目升级为具备异步消息、行为分析和多 Agent 协作的 AI 后端项目。

### 迁移

V1.0 业务数据无需迁移。V1.1 通过新增表、新增消息队列和新增服务完成扩展。
