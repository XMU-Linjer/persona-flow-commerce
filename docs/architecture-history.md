# Architecture History

记录 PersonaFlow Commerce 每个版本的架构变化。

README 只展示当前版本，具体版本设计放在对应架构文档中，这里只记录版本之间发生了什么变化。

---

## V0.1.0 — 本地开发环境搭建

### 日期

2026-06-24

### 状态

已完成

### 上一版本

无，项目刚开始建立。

### 本版本

完成本地开发环境和基础设施准备：

- Java 17；
- Python 3.11；
- Node.js 与 npm；
- Git；
- Docker Desktop；
- Docker Compose；
- MySQL 8.4；
- Redis 7.4；
- RabbitMQ 4 Management。

MySQL、Redis 和 RabbitMQ 均由 Docker Compose 管理，并通过健康检查。

### 主要变化

- 项目从空目录进入可开发状态；
- 本地中间件统一改为 Docker 容器运行；
- 环境变量通过 `.env` 管理；
- 仓库只提交 `.env.example`。

### 影响

- 后续 Java 服务统一连接当前 MySQL、Redis 和 RabbitMQ；
- 本地端口和镜像版本暂时固定；
- 不再单独在 Windows 中安装 MySQL、Redis 和 RabbitMQ；
- 后续工程骨架可以直接使用现有 Compose 配置。

### 原因

统一开发环境，减少本机安装和版本差异带来的问题。

### 迁移

无。

---

## V1.0.0 — 纯电商平台

### 日期

待完成

### 状态

设计中

### 上一版本

V0.1.0 只完成了开发环境和基础设施，没有业务代码。

### 本版本

计划加入：

- Spring Boot 模块化单体；
- Vue 3 前端；
- 用户登录和权限；
- 商品、分类和搜索；
- 收藏和购物车；
- 库存和订单；
- 模拟支付；
- RabbitMQ 用户行为事件；
- Redis 商品缓存、搜索热词和兴趣统计；
- MySQL 电商业务表。

设计文档：

```text
docs/v1.0-architecture.md
```

### 主要变化

- 仓库从基础设施阶段进入业务开发阶段；
- 新增 Java 后端和 Vue 前端；
- MySQL 开始保存用户、商品、库存、订单和行为数据；
- Redis 开始承担缓存和实时统计；
- RabbitMQ 开始处理用户行为消息。

### 影响

- 新增 `persona-commerce-server`；
- 新增 `persona-commerce-web`；
- 新增 Flyway 数据库迁移；
- 新增商品、订单、库存和用户行为相关表；
- 新增 Redis Key；
- 新增 RabbitMQ Exchange、Queue 和 Routing Key；
- README 在 V1.0 完成后更新为当前版本说明。

### 原因

先完成一个纯电商平台，建立 Java 后端、电商业务和中间件使用经验，再为后续 Agent 版本准备真实用户行为数据。

### 迁移

- 保留 V0.1.0 的 Docker Compose 配置；
- 在现有基础设施上创建 Java 和 Vue 工程；
- 通过 Flyway 初始化业务表；
- 不直接修改已经提交的 V0.1.0 版本记录。

---

## 版本记录格式

后续每次发生较大的架构调整时，按下面的格式记录：

```text
## 版本号 — 版本名称

### 日期
版本完成或开始使用的日期。

### 状态
设计中、开发中、已完成或已废弃。

### 上一版本
上一版的主要结构和状态。

### 本版本
这一版实际采用的结构。

### 主要变化
和上一版相比改了什么。

### 影响
影响到哪些模块、接口、数据库表、Redis Key、MQ 消息或部署配置。

### 原因
为什么做这次调整。

### 迁移
已有代码或数据需要如何调整；没有迁移内容时写“无”。
```
