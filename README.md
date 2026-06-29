# PersonaFlow Commerce

## Local Startup

Spring Boot imports the project root `.env` automatically during local startup. Developers do not need to set `$env:MYSQL_USER`, `$env:MYSQL_PASSWORD`, or the matching Redis/RabbitMQ variables manually in PowerShell.

```powershell
cd D:\Workspace\persona-flow-commerce
docker compose up -d

cd persona-commerce-server
.\mvnw.cmd spring-boot:run
```

Health check:

```powershell
curl.exe http://127.0.0.1:8080/actuator/health
```

Frontend dev server:

```powershell
cd D:\Workspace\persona-flow-commerce\persona-commerce-web
npm.cmd run dev -- --host 127.0.0.1
```

If PowerShell blocks `npm.ps1`, use `npm.cmd`.

PersonaFlow Commerce 是一个基于 Spring Boot 的模块化单体电商后端项目。

当前状态：V1.0 后端核心链路已完成 account、catalog、shopping、trade 四个模块；behavior、RabbitMQ 行为事件、admin 管理端和 Agent 推荐仍属于后续阶段。

## V1.0 后端核心链路

当前已完成的主链路：

```text
用户注册 / 登录
-> 浏览分类、商品列表、商品详情和 SKU
-> 收藏 SKU 或加入购物车
-> 通过 skuId + quantity 创建订单
-> 锁定库存
-> 取消待支付订单并释放库存
-> 模拟支付并确认 locked 库存为 sold 库存
-> 查询订单列表和订单详情
```

V1.0 当前不实现：

```text
真实支付、退款、售后、物流、优惠券
购物车结算接口
behavior 事件发布
RabbitMQ 行为消息
admin 业务接口
Agent 推荐
```

## 后端模块

| 模块 | 当前职责 |
| --- | --- |
| account | 注册、登录、当前用户资料、密码修改、地址管理、CurrentUserProvider、AddressQueryApi |
| catalog | 分类、SPU、SKU、商品列表、商品详情、商品详情 Redis 缓存、ProductQueryApi |
| shopping | 收藏 SKU、取消收藏、收藏列表、购物车增删改查 |
| trade | 库存、订单、订单查询、取消订单、模拟支付、payment_record |

## 技术栈

### 后端

- Java 17
- Spring Boot
- Spring MVC
- Spring Security
- JWT
- MyBatis-Plus
- MySQL
- Redis
- Flyway
- Maven
- JUnit 5
- Mockito

### 前端

- Vue 3
- TypeScript
- Vite
- Pinia
- Element Plus

## 当前进度

- [x] Docker Compose 基础设施
- [x] Spring Boot 后端工程
- [x] Vue 前端工程
- [x] account 模块
- [x] catalog 模块
- [x] shopping 模块
- [x] trade 模块
- [x] 后端编译和全量测试通过
- [ ] README / 接口文档展示收口
- [ ] 前后端联调
- [ ] behavior 模块
- [ ] RabbitMQ 行为事件链路
- [ ] admin 管理端
- [ ] Agent 推荐

## 版本规划

### V1.0

收口电商后端核心链路：账户、商品目录、收藏购物车、库存、订单、取消订单和模拟支付。

### V1.1

在 behavior 模块确认后，接入用户行为事件、兴趣统计和 Agent 可用的数据能力。

### V1.2

完善 Agent 通信、审核、推荐决策和执行轨迹。

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
