# PersonaFlow Commerce

PersonaFlow Commerce 是一个用于作品集和面试展示的 V1.0 全栈电商 Demo。项目重点展示模块化后端边界、完整电商主链路、统一错误响应、库存状态流转，以及可本地演示的 Vue 前端。

当前 V1.0 聚焦“核心电商闭环”。behavior 行为事件、RabbitMQ 异步事件流、Agent 推荐、admin 管理后台和 RAG 属于 V1.1+ 规划，不是当前已完成能力。

## 项目定位

这个项目适合展示：

- 清晰的 account / catalog / shopping / trade 模块边界
- Spring Boot 模块化单体后端设计
- JWT 登录认证和权限规则
- MySQL + Flyway 数据库迁移
- Redis 商品详情缓存
- 订单快照、库存锁定、取消释放、模拟支付确认扣减
- Vue 3 + Element Plus 的可演示前端页面

## 技术栈

后端：

- Java 17
- Spring Boot 3
- Spring MVC
- Spring Security
- JWT
- MyBatis-Plus
- MySQL 8.4
- Redis
- Flyway
- Maven
- JUnit 5
- Mockito

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
- RabbitMQ 容器，当前主要为 V1.1 事件流规划预留

## V1.0 已实现模块

account：

- 用户注册 / 登录
- 当前用户资料
- 修改密码
- 收货地址管理
- `CurrentUserProvider`
- `AddressQueryApi`

catalog：

- 商品分类树
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

- 库存模型
- 创建订单
- 订单列表
- 订单详情
- 取消订单
- 模拟支付
- 支付记录

前端页面：

- 登录
- 注册
- 商品列表
- 商品详情
- 收藏
- 购物车
- 地址管理
- 结算页
- 订单列表
- 订单详情

## 核心业务链路

```text
注册 / 登录
-> 浏览商品列表并搜索商品
-> 进入商品详情并选择 SKU
-> 收藏 SKU 或加入购物车
-> 新增收货地址
-> 立即购买或从购物车结算
-> 创建订单
-> 查看订单详情
-> 模拟支付待支付订单
-> 再创建一笔订单并取消
-> 在订单列表查看不同状态
```

## 后端亮点

- 模块边界清晰，按 account / catalog / shopping / trade 分层。
- trade 不直接读取 account / catalog / shopping 表。
- 创建订单只通过 `CurrentUserProvider`、`AddressQueryApi`、`ProductQueryApi` 协作。
- 订单保存地址快照和商品快照，避免历史订单受后续资料或商品变更影响。
- 库存使用 `available_quantity` / `locked_quantity` / `sold_quantity` 三段模型。
- 创建订单锁库存：`available -> locked`。
- 取消订单释放库存：`locked -> available`。
- 模拟支付确认扣减：`locked -> sold`。
- 库存更新使用条件更新，降低并发超卖风险。
- 订单状态机限制非法流转：
  - `10 = PENDING_PAYMENT`
  - `20 = PAID`
  - `30 = CANCELED`
- 非法状态流转返回统一业务错误。
- 统一 API 响应结构，`ApiResponse.code` 保持整数，业务错误使用字符串 `errorCode`。
- 后端全量测试基线：`171` 个测试通过。

## 本地启动

如需本地环境变量，请参考 `.env.example` 创建项目根目录 `.env`。`.env` 会被 Docker Compose 使用，也会被 Spring Boot 本地启动自动读取。`.env` 不提交。

启动后端依赖：

```powershell
cd D:\Workspace\persona-flow-commerce
docker compose up -d
```

启动后端：

```powershell
cd persona-commerce-server
.\mvnw.cmd spring-boot:run
```

后端健康检查：

```powershell
curl.exe http://127.0.0.1:8080/actuator/health
```

启动前端：

```powershell
cd D:\Workspace\persona-flow-commerce\persona-commerce-web
npm.cmd install
npm.cmd run dev -- --host 127.0.0.1
```

访问地址：

```text
http://127.0.0.1:5173/
```

说明：

- Spring Boot 会自动读取项目根目录 `.env`。
- 不需要手动设置 `$env:MYSQL_USER`、`$env:MYSQL_PASSWORD` 等 PowerShell 环境变量。
- `.env.example` 是提交到仓库的模板。
- `.env` 是本地配置文件，不应提交。
- PowerShell 下如果 `npm.ps1` 被拦截，建议使用 `npm.cmd`。

## V1.0 演示流程

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
15. 在订单列表查看全部、待支付、已支付、已取消状态。

## 验证命令

后端测试：

```powershell
cd D:\Workspace\persona-flow-commerce\persona-commerce-server
.\mvnw.cmd test
```

当前后端测试基线：

```text
Tests run: 171, Failures: 0, Errors: 0, Skipped: 0
```

前端构建：

```powershell
cd D:\Workspace\persona-flow-commerce\persona-commerce-web
npm.cmd run build
```

## V1.0 未实现内容

- 真实支付
- 微信支付 / 支付宝 / Stripe
- 退款
- 售后
- 物流
- 优惠券
- 店铺 / 商家系统
- admin 管理接口或后台
- behavior 行为事件发布
- RabbitMQ 行为事件生产者 / 消费者
- Agent 推荐
- RAG

## V1.1 规划

V1.1 计划围绕“行为数据 + 异步事件 + Agent 推荐解释”继续扩展：

- behavior 行为事件
- RabbitMQ 异步事件流
- Agent Team 用户画像
- 推荐解释
- 面向后续 Agent 的数据分析能力

以上内容是后续规划，不属于当前 V1.0 已完成能力。

## 文档

- [V1.0 架构设计](docs/v1.0-architecture.md)
- [模块约定](docs/module-contracts.md)
- [account 模块](docs/modules/01-account.md)
- [catalog 模块](docs/modules/02-catalog.md)
- [shopping 模块](docs/modules/03-shopping.md)
- [trade 模块](docs/modules/04-trade.md)
- [架构变更记录](docs/architecture-history.md)

## License

暂未添加开源许可证。
