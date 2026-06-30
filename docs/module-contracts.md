# PersonaFlow Commerce 模块合同

> 状态：V1.0 主链路已完成，V1.1 behavior / Agent 画像链路已完成，V1.2 异步 Agent workflow 为 planned  
> 更新时间：2026-06-30

## 1. 总体原则

模块之间通过明确接口协作，不跨边界直接读取其他模块的表或 Entity。

核心原则：

- account 提供当前用户身份和地址快照；
- catalog 提供商品快照；
- shopping 只负责收藏和购物车；
- trade 只通过跨模块 API 获取地址和商品快照；
- behavior 只接收行为事实，不反向修改主业务；
- Python Agent 只接收 Java 准备好的结构化上下文，不直接访问业务数据库。

## 2. account 合同

### 2.1 CurrentUserProvider

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

说明：

- `CurrentUser` 不包含 username；
- `requireCurrentUser()` 从认证上下文获取当前用户；
- 不查询数据库；
- 未登录时返回统一未认证错误；
- 跨模块只能使用 `userId` 和 `roles`。

### 2.2 AddressQueryApi

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

说明：

- 给 trade 创建订单时使用；
- 只允许查询指定 userId 名下地址；
- 返回快照，不返回 Entity；
- `recipientPhone` 是收货联系电话，不是手机号登录身份。

## 3. catalog 合同

### 3.1 ProductQueryApi

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

说明：

- shopping 加购、收藏列表、购物车列表使用；
- trade 创建订单使用；
- 返回当前商品展示快照；
- 不返回 Entity；
- 不返回库存；
- 不负责扣库存。

## 4. shopping 合同

V1.0 shopping 不向 trade 提供跨模块结算接口。

已完成范围：

- 收藏 SKU；
- 取消收藏；
- 收藏列表；
- 加入购物车；
- 修改购物车数量；
- 删除购物车项；
- 清空购物车；
- 购物车列表。

不提供：

- `CartCheckoutApi`；
- `CartQueryApi`；
- `CartItemSnapshot`；
- 从购物车创建订单；
- 创建订单后自动清理购物车。

trade 后续如要支持“从购物车结算”，需要在 trade 阶段另行设计合同。当前前端从购物车结算时直接传 `skuId + quantity` 给 trade 创建订单接口。

## 5. trade 合同

trade 允许依赖：

- `CurrentUserProvider`
- `AddressQueryApi`
- `ProductQueryApi`
- `InventoryService`

trade 不允许：

- 直接读取 account 表；
- 直接读取 catalog 表；
- 直接读取 shopping 表；
- 导入 account / catalog / shopping Entity；
- 调用 shopping 清理购物车。

创建订单时：

- 保存地址快照；
- 保存商品快照；
- 调用库存锁定；
- 状态为 `PENDING_PAYMENT = 10`。

取消订单时：

- 只允许 `PENDING_PAYMENT -> CANCELED`；
- 调用库存释放。

模拟支付时：

- 只允许 `PENDING_PAYMENT -> PAID`；
- 写入 `payment_record`；
- 调用库存确认扣减。

## 6. behavior 事件合同

### 6.1 行为事件类型

```text
PRODUCT_VIEW
PRODUCT_SEARCH
FAVORITE_ADD
FAVORITE_REMOVE
CART_ADD
CART_REMOVE
CART_CLEAR
ORDER_CREATED
PAYMENT_SUCCESS
ORDER_CANCELED
```

### 6.2 行为事件消息

```text
messageId
eventId
eventType
userId
sourceModule
objectType
objectId
keyword
skuId
spuId
categoryId
orderId
amount
payloadJson
occurredAt
traceId
version
```

约定：

- `eventId` 是行为事实幂等键；
- `messageId` 是消息跟踪和消费日志键；
- 行为事件只描述已经发生的事实；
- 发布失败不回滚主业务；
- behavior 不回调 catalog / shopping / trade 修改业务状态；
- behavior 不控制购物车、库存、订单、支付状态；
- payload 不保存密码、JWT、收货人手机号、完整收货地址。

### 6.3 PAYMENT_SUCCESS 语义

`PAYMENT_SUCCESS` 不表示继续推荐当前已购买 SKU / SPU。

它表示：

- 偏好确认；
- 当前具体需求满足；
- 互补需求触发。

成交后画像可以增强长期偏好，但短期推荐应避免继续推同一商品，优先考虑互补品、耗材、配件、复购周期或新需求探索。

## 7. RabbitMQ 合同

### 7.1 behavior 行为链路

```text
catalog / shopping / trade
-> commerce.behavior.exchange
-> behavior.persist.queue
-> behavior_event
```

已完成：

- topic exchange；
- durable queue；
- 手动 ACK；
- 基础重试；
- 死信队列；
- consume log；
- `eventId` 幂等。

### 7.2 Agent 通信骨架

Python 服务保留：

```text
commerce.agent.exchange
```

用途：

- 后续异步 Profile Agent Team 任务分发；
- 结构化 Artifact 消息；
- challenge / revision / completed 等工作流消息。

当前真实画像刷新链路使用 Java HTTP 调 Python `POST /agent/profile/build`，不是异步 Agent 总线。

### 7.3 V1.2 planned：Agent workflow 合同

V1.2 计划正式使用 `commerce.agent.exchange` 作为异步 Profile Agent workflow 总线。

计划合同：

- Java 创建 `agent_workflow`，并通过 `AgentMessage` 投递任务；
- Python Agent 通过 `AgentMessage` 接收任务、产出 Artifact、发布 task / workflow 状态；
- Java 保存 `agent_task`、`agent_artifact` 和最终 `user_profile_version`；
- Java 不直接查询 Python 内部状态；
- Python Agent 不直接查询 Java 业务数据库；
- Agent 之间只通过 AgentMessage / Artifact 交换结构化结果；
- 当前用户隔离仍由 Java Controller 和 `CurrentUserProvider` 保证；
- 前端只查询 Java 后端，不直接访问 Python Agent 或 RabbitMQ。

V1.2 planned Artifact：

```text
BehaviorFactReport
IntentReport
TrendReport
ProfileDraft
ProfileAuditReport
UserProfileVersion
```

V1.2 planned workflow / task 状态：

```text
agent_workflow: PENDING / RUNNING / SUCCEEDED / FAILED / CANCELED
agent_task: PENDING / RUNNING / SUCCEEDED / FAILED / SKIPPED
```

## 8. Java 后端与 Python Agent 合同

Java 请求 Python：

```http
POST /agent/profile/build
```

Python 健康检查：

```http
GET /health
```

Java 提供给 Python 的输入是 `AgentProfileContext`。

核心字段：

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

Python 返回：

- `workflowId`
- `summary`
- `profile`
- `fulfilledNeeds`
- `complementOpportunities`
- `doNotRecommend`
- `evidence`

约束：

- Agent 不直接查询 account、catalog、shopping、trade 表；
- Agent 不绕过 Java 权限控制；
- Agent 不修改订单、库存、购物车或支付状态；
- Agent 产出的用户画像版本由 Java 保存到 `user_profile_version`。

## 9. V1.1 当前用户行为接口

```http
GET  /api/behavior/me/events
GET  /api/behavior/me/summary
GET  /api/behavior/me/agent-context
GET  /api/behavior/me/profile/latest
POST /api/behavior/me/profile/refresh
```

约定：

- 全部需要登录；
- 只查询当前用户；
- Controller 不接收 `userId`；
- 不提供 admin 查询；
- 不暴露敏感信息。

## 10. 未实现合同

当前没有提供：

- 真实支付合同；
- 退款合同；
- 物流合同；
- 优惠券合同；
- admin 管理合同；
- 真实推荐算法合同；
- 真实 LLM / RAG 合同；
- Outbox 合同；
- 分布式事务合同；
- CartCheckoutApi / CartQueryApi / CartItemSnapshot。

## 11. 合同状态

| 合同 | 状态 |
|---|---|
| `CurrentUserProvider` | 已实现 |
| `AddressQueryApi` | 已实现 |
| `ProductQueryApi` | 已实现 |
| shopping checkout API | V1.0 不提供 |
| trade order API | 已实现 |
| behavior event message | 已实现 |
| RabbitMQ behavior bus | 已实现 |
| AgentProfileContext | 已实现 |
| Java -> Python profile build | 已实现 |
| Agent workflow bus | V1.2 planned |
| Agent Artifact tracking | V1.2 planned |
| Python real LLM | 不实现 |
| RAG | 不实现 |
| Outbox | 不实现 |
