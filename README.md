# PersonaFlow Commerce

一个基于 Spring Boot 的电商项目

当前开发版本为 **V1.0**。这一版本先完成纯电商平台和后端技术栈，后续版本再加入用户行为分析、实时兴趣识别和多 Agent 推荐

## V1.0

V1.0 计划完成基础电商链路：

```text
用户登录
→ 浏览和搜索商品
→ 收藏或加入购物车
→ 创建订单
→ 扣减库存
→ 模拟支付
→ 查询订单
```

用户在搜索、浏览、收藏、加购和购买时会产生行为事件。

```text
业务操作
→ RabbitMQ
→ MySQL 保存行为记录
→ Redis 更新搜索热词和近期兴趣数据
```

这些数据会在 V1.1 中交给 Agent 使用。

## 技术结构

```text
Vue 3
   │
   ▼
Spring Boot
   │
   ├── MySQL
   ├── Redis
   └── RabbitMQ
```

V1.0 使用模块化单体，不拆微服务

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
- RabbitMQ
- Flyway

### 前端

- Vue 3
- TypeScript
- Vite
- Pinia
- Element Plus

### 开发与测试

- Maven
- Docker Compose
- Git
- JUnit 5
- Mockito

## 当前进度

- [x] Java、Node.js、Python、Git、Docker 环境
- [x] MySQL 8.4
- [x] Redis 7.4
- [x] RabbitMQ 4 Management
- [x] Docker Compose 基础设施
- [x] V1.0 架构设计
- [x] Spring Boot 工程
- [x] Vue 工程
- [x] Java 连接 MySQL、Redis、RabbitMQ
- [ ] 用户登录
- [ ] 商品和搜索
- [ ] 收藏和购物车
- [ ] 库存和订单
- [ ] 用户行为事件
- [ ] 前后端联调
- [ ] V1.0 完成

## 版本计划

### V1.0

完成纯电商平台、Redis 缓存和 RabbitMQ 用户行为链路。

### V1.1

加入 Behavior Agent、Intent Agent 和 Trend Agent，根据 V1.0 产生的用户行为生成用户画像。

### V1.2

完善 Agent 通信、审核、推荐决策和执行轨迹。

## 文档

- [V1.0 架构设计](docs/v1.0-architecture.md)
- [架构变更记录](docs/architecture-history.md)

## License

暂未添加开源许可证。
