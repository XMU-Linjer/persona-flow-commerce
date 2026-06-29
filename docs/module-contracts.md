# PersonaFlow Commerce 模块约定

> 状态：V1.0 已实现，V1.1 behavior / RabbitMQ / Agent 合同进入设计收口  
> 作用：定义模块之间允许如何协作，以及哪些内部实现不得跨模块访问  
> 更新时间：2026-06-29

## 1. 基本原则

- 模块之间只通过公开 API 或明确的消息契约协作。
- 不跨模块调用 Mapper。
- 不跨模块导入 Entity。
- 不通过 `common` 放置具体业务模型。
- 跨模块返回模型必须是稳定的快照或专用 record，不能返回数据库 Entity。
- 业务事件发布失败不回滚主业务，失败事件由日志、重试或后续补偿处理。

## 2. V1.0 模块与包

| 业务模块 | Java 包 | 说明 |
|---|---|---|
| account | `auth`, `user` | 账号、认证、当前用户、地址 |
| catalog | `product` | 商品目录、商品快照 |
| shopping | `favorite`, `cart` | 收藏、购物车 |
| trade | `inventory`, `order`, `payment` | 库存、订单、模拟支付 |
| common | `common` | 统一技术基础 |

V1.0 不创建 `search` 包。复杂搜索留给后续版本。

## 3. account 对外合同

### 3.1 当前用户

使用者：shopping、trade。

```java
public interface CurrentUserProvider {

    CurrentUser requireCurrentUser();
}
```

```java
public record CurrentUser(
        Long userId,
        Set<String> roles
) {
}
```

约定：

- `CurrentUser` 只包含 `userId` 和 `roles`；
- 不包含 `username`；
- 不返回 `passwordHash`、地址、头像或资料字段；
- 调用方不解析 JWT，不直接读取 `SecurityContext`；
- `/api/users/me` 可以返回 `username`，但这是 account HTTP 接口自己的查询结果，不属于跨模块模型。

### 3.2 地址快照

使用者：trade。

```java
public interface AddressQueryApi {

    AddressSnapshot requireOwnedAddress(Long userId, Long addressId);
}
```

```java
public record AddressSnapshot(
        Long addressId,
        String recipientName,
        String recipientPhone,
        String province,
        String city,
        String district,
        String detailAddress,
        String postalCode
) {
}
```

约定：

- account 负责校验地址存在且属于指定 `userId`；
- trade 只保存快照，不长期依赖 `user_address` 当前值；
- 不返回 `AddressEntity`；
- `recipientPhone` 是收货联系电话，不是手机号登录能力。

### 3.3 V1.0 不提供

- `UserQueryApi`
- `UserSummary`
- admin 用户管理 API
- 封号、解封、角色管理

## 4. catalog 对外合同

使用者：shopping、trade。

```java
public interface ProductQueryApi {

    ProductSnapshot requireSellableSku(Long skuId);

    Map<Long, ProductSnapshot> requireSellableSkus(Collection<Long> skuIds);
}
```

```java
public record ProductSnapshot(
        Long skuId,
        Long spuId,
        Long categoryId,
        String categoryName,
        String productName,
        String skuName,
        BigDecimal unitPrice,
        String imageUrl
) {
}
```

约定：

- `requireSellableSku` 用于 shopping 加购、收藏校验和 trade 创建订单；
- `requireSellableSkus` 用于购物车列表和订单创建时批量查询；
- SKU、SPU、Category 都必须存在且启用或可售；
- `unitPrice` 是当前商品单价，trade 创建订单时保存为订单价格快照；
- `imageUrl` 优先 SKU 图片，没有则使用 SPU 主图；
- 不返回库存；
- 不负责扣库存、锁库存、库存流水或下单库存校验；
- 不返回 `ProductSkuEntity`、`ProductSpuEntity` 或 `ProductCategoryEntity`。

## 5. shopping 对外合同

V1.0 shopping 只提供 HTTP 能力，不提供跨模块结算 API。

shopping 内部只能通过：

- `CurrentUserProvider` 获取当前 `userId`；
- `ProductQueryApi` 校验 SKU 可售并获取 `ProductSnapshot`。

V1.0 暂不提供：

- `CartCheckoutApi`
- `CartQueryApi`
- `CartItemSnapshot`
- 获取待结算购物车项
- 订单成功后移除已购买购物车项

trade V1.0 可以直接接收 `skuId + quantity` 创建订单。后续如果决定支持“从购物车结算”，再在 trade 设计阶段补充 `CartCheckoutApi` 和 `CartItemSnapshot`。

## 6. trade 对外和依赖合同

trade V1.0 负责库存、订单和模拟支付，不向其他模块提供跨模块 Java 查询 API。

trade 允许依赖：

- `CurrentUserProvider`
- `AddressQueryApi`
- `ProductQueryApi`
- trade 内部的 `InventoryService`

trade 禁止：

- 直接读取 account 表；
- 直接读取 catalog 表；
- 直接读取 shopping 表；
- 导入 account / catalog / shopping Entity；
- 依赖 shopping 创建订单；
- 创建订单后自动清理购物车。

创建订单时：

- 从 account 获取当前用户和地址快照；
- 从 catalog 获取商品快照；
- trade 保存地址快照和商品快照；
- trade 在事务中锁定库存并创建订单。

查询订单、取消订单和模拟支付不调用 `AddressQueryApi` 或 `ProductQueryApi`，只使用 trade 自己保存的快照。

## 7. V1.1 behavior 事件合同

V1.1 新增 behavior 行为事件模块。业务模块只描述已经发生的事实，不把 behavior 作为主业务流程的强事务依赖。

### 7.1 事件发布方

catalog 发布：

- `PRODUCT_VIEW`
- `PRODUCT_SEARCH`

shopping 发布：

- `FAVORITE_ADD`
- `FAVORITE_REMOVE`
- `CART_ADD`
- `CART_REMOVE`
- `CART_CLEAR`

trade 发布：

- `ORDER_CREATED`
- `PAYMENT_SUCCESS`
- `ORDER_CANCELED`

### 7.2 失败处理

- 业务事件发布失败，不回滚 catalog / shopping / trade 主业务。
- 行为事件失败可记录日志、进入重试、进入死信队列或由后续补偿处理。
- behavior 不回调 catalog / shopping / trade 修改业务状态。
- behavior 不控制购物车、库存、订单、支付状态。

### 7.3 PAYMENT_SUCCESS 语义

`PAYMENT_SUCCESS` 不表示继续推荐当前已购买 SKU / SPU。

它表示：

- 偏好确认；
- 当前具体需求满足；
- 互补需求触发。

成交后画像可以增强长期偏好，但短期推荐应避免继续推同一商品，优先考虑互补品、耗材、配件、复购周期或新需求探索。

## 8. V1.1 RabbitMQ 合同

V1.1 使用两条 RabbitMQ 链路：

### 8.1 行为事件链路

```text
catalog / shopping / trade
-> commerce.behavior.exchange
-> behavior.persist.queue
-> behavior_event
```

行为链路用于业务模块发布用户行为事件，behavior 模块消费并持久化。

### 8.2 Agent 任务链路

```text
Profile Manager
-> commerce.agent.exchange
-> Behavior Agent / Intent Agent / Trend Agent / Profile Builder-Critic
-> Artifact
-> user_profile_version
```

Agent 链路用于 Python Agent 服务内部的任务分发、结构化 Artifact 传递、质疑、返工和工作流完成。

## 9. Java 后端与 Python Agent 合同

V1.1 中，Java 后端可以通过内部 HTTP 或 Agent Task Bus 向 Python Agent 提交画像任务。

约定：

- Java 后端负责权限校验和业务数据上下文准备；
- Agent 不直接查询 account、catalog、shopping、trade 的业务表；
- Agent 不绕过 Java 后端权限控制；
- Agent 接收结构化上下文，输出结构化 Artifact；
- Agent 产出的用户画像版本最终由 Java 侧受控保存到 `user_profile_version`。

## 10. 约定状态

| 合同 | 状态 |
|---|---|
| CurrentUserProvider / CurrentUser | V1.0 已实现 |
| AddressQueryApi / AddressSnapshot | V1.0 已实现 |
| ProductQueryApi / ProductSnapshot | V1.0 已实现 |
| CartCheckoutApi / CartQueryApi / CartItemSnapshot | V1.0 不提供 |
| behavior 事件类型和发布边界 | V1.1 设计确定 |
| RabbitMQ 行为事件总线 | V1.1 设计确定 |
| RabbitMQ Agent 任务总线 | V1.1 设计确定 |
| Python Profile Agent Team Artifact 合同 | V1.1 设计确定 |
