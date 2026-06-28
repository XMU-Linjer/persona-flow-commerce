# 04-trade 模块设计

## 1. 模块定位

`trade` 模块负责 V1.0 的交易能力。

它提供：

```text
库存
订单
订单项
模拟支付
取消订单
```

一句话定义：

```text
trade 是交易闭环模块，负责把“用户想买什么”变成“系统中真实存在的一笔订单”。
```

trade 和前面模块的区别是：

```text
account 主要处理身份和用户资料
catalog 主要处理商品展示和商品快照
shopping 主要处理收藏和购物车意向
trade 负责真正的业务动作：锁库存、创建订单、支付、取消、释放库存
```

V1.0 trade 的核心价值是展示真实后端业务能力：

```text
事务
库存并发控制
订单状态流转
订单项快照
模拟支付
取消订单释放库存
```

------

## 2. V1.0 目标

V1.0 trade 需要完成：

```text
1. 用户可以直接通过 skuId + quantity 创建订单
2. 创建订单时校验当前用户、收货地址、商品可售状态
3. 创建订单时保存收货地址快照
4. 创建订单时保存商品价格快照
5. 创建订单时锁定库存，防止超卖
6. 用户可以查询自己的订单列表
7. 用户可以查询自己的订单详情
8. 用户可以取消待支付订单
9. 取消订单时释放锁定库存
10. 用户可以模拟支付待支付订单
11. 支付成功后将锁定库存转为已售库存
12. 订单状态非法时阻止重复支付、重复取消
```

V1.0 trade 不追求完整电商交易系统，只做演示型电商平台需要的最小交易闭环。

------

## 3. 不做什么

V1.0 trade 不做：

```text
真实支付接入
微信支付
支付宝支付
Stripe
退款
售后
物流
发货
确认收货
评价
发票
优惠券
满减
秒杀
店铺系统
商家系统
多商家拆单
商家结算
购物车结算接口
订单超时自动取消定时任务
库存流水表
分布式锁
分布式事务
behavior 事件发布
RabbitMQ 事件发布
Agent 推荐
admin 管理
```

说明：

```text
真实支付、退款、售后、物流会显著扩大项目规模，V1.0 不做。
优惠券和满减属于营销系统，V1.0 不做。
店铺和商家系统会引入 sellerId、shopId、拆单、结算，V1.0 不做。
购物车结算接口后续如有需要，再设计 CartCheckoutApi。
behavior 和 RabbitMQ 等行为事件能力等 behavior 模块确定后再接入。
admin 管理入口后续由 admin 模块调用已有 trade 能力，trade 当前阶段不新增 admin 业务接口。
```

V1.0 trade 默认是：

```text
单店 / 平台自营电商交易模型
```

因此订单中不设计：

```text
sellerId
shopId
merchantId
```

------

## 4. 核心概念

### 4.1 Order

`Order` 表示一笔订单主记录。

它回答：

```text
谁下的单
订单号是什么
订单总金额是多少
订单当前是什么状态
收货地址快照是什么
什么时候创建、支付或取消
```

订单主表不保存每个商品的详细信息，商品明细放在订单项表中。

------

### 4.2 OrderItem

`OrderItem` 表示订单中的一件商品明细。

它回答：

```text
买的是哪个 skuId
当时商品叫什么
当时 SKU 叫什么
当时单价是多少
买了几件
这一项小计多少钱
```

订单项必须保存商品快照。

原因：

```text
商品后续改名、改价、下架，都不能影响历史订单。
```

所以订单项不能只保存 `skuId`。

------

### 4.3 InventoryStock

`InventoryStock` 表示某个 SKU 的库存状态。

V1.0 使用三段库存模型：

```text
availableQuantity：可售库存
lockedQuantity：已创建订单但未支付的锁定库存
soldQuantity：已支付成功的已售库存
```

库存变化规则：

```text
创建订单：availableQuantity 减少，lockedQuantity 增加
取消订单：lockedQuantity 减少，availableQuantity 增加
支付成功：lockedQuantity 减少，soldQuantity 增加
```

这样比“下单直接扣库存”更接近真实交易系统。

------

### 4.4 PaymentRecord

`PaymentRecord` 表示一次模拟支付记录。

V1.0 只支持模拟支付：

```text
channel = MOCK
```

支付成功后：

```text
订单状态从 PENDING_PAYMENT 变为 PAID
锁定库存转为已售库存
写入 payment_record
```

V1.0 不做真实第三方支付回调。

------

## 5. 数据表设计

### 5.1 inventory_stock

SKU 库存表。

字段建议：

```text
id                  BIGINT 主键
sku_id              BIGINT SKU ID
available_quantity  INT 可售库存
locked_quantity     INT 锁定库存
sold_quantity       INT 已售库存
created_at          DATETIME 创建时间
updated_at          DATETIME 更新时间
```

约束和索引：

```text
主键：id
唯一索引：uk_inventory_stock_sku_id(sku_id)
索引：idx_inventory_stock_sku_id(sku_id)
CHECK：available_quantity >= 0
CHECK：locked_quantity >= 0
CHECK：sold_quantity >= 0
```

说明：

```text
sku_id 对应 catalog 的 product_sku.id。
inventory_stock 是 trade/inventory 拥有的数据。
catalog 不保存真实库存。
catalog 不负责库存扣减、库存锁定或库存释放。
```

初始化策略：

```text
V1.0 可以在 Flyway 中为已有演示 SKU 初始化库存。
每个 SKU 默认初始化一个可售库存，例如 100。
locked_quantity 和 sold_quantity 初始为 0。
```

------

### 5.2 trade_order

订单主表。

字段建议：

```text
id                  BIGINT 主键
order_no            VARCHAR(64) 订单号
user_id             BIGINT 用户 ID
address_id          BIGINT 下单时使用的地址 ID

recipient_name      VARCHAR(100) 收货人姓名快照
recipient_phone     VARCHAR(30) 收货电话快照
province            VARCHAR(100) 省份快照
city                VARCHAR(100) 城市快照
district            VARCHAR(100) 区县快照
detail_address      VARCHAR(300) 详细地址快照
postal_code         VARCHAR(20) 邮编快照，可为空

total_amount        DECIMAL(10,2) 订单总金额
status              TINYINT 订单状态
created_at          DATETIME 创建时间
paid_at             DATETIME 支付时间，可为空
canceled_at         DATETIME 取消时间，可为空
updated_at          DATETIME 更新时间
```

约束和索引：

```text
主键：id
唯一索引：uk_trade_order_order_no(order_no)
索引：idx_trade_order_user_id(user_id)
索引：idx_trade_order_status(status)
索引：idx_trade_order_created_at(created_at)
```

订单状态建议：

```text
10 = PENDING_PAYMENT，待支付
20 = PAID，已支付
30 = CANCELED，已取消
```

说明：

```text
订单表保存地址快照，而不是只保存 address_id。
用户后续修改收货地址，不应影响历史订单。
```

------

### 5.3 trade_order_item

订单项表。

字段建议：

```text
id                  BIGINT 主键
order_id            BIGINT 订单 ID
order_no            VARCHAR(64) 订单号
user_id             BIGINT 用户 ID

sku_id              BIGINT SKU ID
spu_id              BIGINT SPU ID
category_id         BIGINT 分类 ID
category_name       VARCHAR(100) 分类名称快照
product_name        VARCHAR(200) 商品名称快照
sku_name            VARCHAR(200) SKU 名称快照
image_url           VARCHAR(500) 商品展示图快照

unit_price          DECIMAL(10,2) 下单时单价
quantity            INT 购买数量
subtotal            DECIMAL(10,2) 小计金额

created_at          DATETIME 创建时间
```

约束和索引：

```text
主键：id
索引：idx_trade_order_item_order_id(order_id)
索引：idx_trade_order_item_order_no(order_no)
索引：idx_trade_order_item_user_id(user_id)
索引：idx_trade_order_item_sku_id(sku_id)
```

说明：

```text
订单项保存商品快照。
unit_price 来自 ProductSnapshot.unitPrice。
subtotal = unit_price * quantity。
trade 创建订单后，不再依赖商品当前名称或当前价格展示历史订单。
```

------

### 5.4 payment_record

模拟支付记录表。

字段建议：

```text
id                  BIGINT 主键
payment_no          VARCHAR(64) 支付单号
order_id            BIGINT 订单 ID
order_no            VARCHAR(64) 订单号
user_id             BIGINT 用户 ID
amount              DECIMAL(10,2) 支付金额
channel             VARCHAR(30) 支付渠道
status              TINYINT 支付状态
paid_at             DATETIME 支付成功时间
created_at          DATETIME 创建时间
updated_at          DATETIME 更新时间
```

约束和索引：

```text
主键：id
唯一索引：uk_payment_record_payment_no(payment_no)
唯一索引：uk_payment_record_order_id(order_id)
索引：idx_payment_record_order_no(order_no)
索引：idx_payment_record_user_id(user_id)
```

支付渠道：

```text
MOCK
```

支付状态建议：

```text
20 = SUCCESS
```

说明：

```text
V1.0 模拟支付只做成功支付。
不实现支付失败、退款、回调、对账。
一个订单只允许有一条成功支付记录。
```

------

## 6. HTTP 接口设计

trade 的所有用户接口都需要登录。

原因：

```text
订单、支付、取消订单都是用户个人交易数据。
```

Controller 不接收 userId，当前用户只能通过 `CurrentUserProvider` 获取。

------

### 6.1 创建订单

```http
POST /api/trade/orders
```

请求字段：

```json
{
  "addressId": 1,
  "items": [
    {
      "skuId": 1001,
      "quantity": 2
    },
    {
      "skuId": 1002,
      "quantity": 1
    }
  ]
}
```

用途：

```text
当前用户直接通过 skuId + quantity 创建订单。
```

流程：

```text
OrderService 获取当前 userId
    -> 校验 items 非空
    -> 校验每个 quantity > 0
    -> 调用 AddressQueryApi.requireOwnedAddress(userId, addressId)
    -> 收集 skuId
    -> 调用 ProductQueryApi.requireSellableSkus(skuIds)
    -> 计算订单项 subtotal
    -> 计算 totalAmount
    -> 开启事务
    -> 锁定每个 SKU 的库存
    -> 生成 orderNo
    -> 写入 trade_order
    -> 写入 trade_order_item
    -> 返回订单创建结果
```

规则：

```text
必须登录
addressId 必须属于当前用户
items 不能为空
quantity 必须大于 0
SKU 必须存在且可售
库存必须足够
订单创建成功后状态为 PENDING_PAYMENT
创建订单时锁定库存
创建订单不触发支付
创建订单不发布 behavior 事件
创建订单不清理购物车
```

响应字段建议：

```text
orderId
orderNo
totalAmount
status
createdAt
items
```

------

### 6.2 查询订单列表

```http
GET /api/trade/orders
```

查询参数：

```text
status  可选，按订单状态过滤
page    页码，默认 1
size    每页数量，默认 10
```

用途：

```text
当前用户查看自己的订单列表。
```

流程：

```text
OrderService 获取当前 userId
    -> 按 userId 查询订单列表
    -> 可选按 status 过滤
    -> 分页返回订单概要
```

响应字段建议：

```text
orderId
orderNo
totalAmount
status
createdAt
paidAt
canceledAt
```

说明：

```text
订单列表只返回概要信息。
如果前端需要商品图片和商品名，可以在列表中返回每个订单的前 1 到 3 个订单项摘要。
V1.0 可以先只返回订单主信息，详情页再查订单项。
```

------

### 6.3 查询订单详情

```http
GET /api/trade/orders/{orderId}
```

用途：

```text
当前用户查看自己的订单详情。
```

流程：

```text
OrderService 获取当前 userId
    -> 根据 orderId + userId 查询订单
    -> 不存在则返回 TRADE_ORDER_NOT_FOUND
    -> 查询订单项
    -> 查询支付记录，可为空
    -> 组装订单详情
```

响应字段建议：

```text
orderId
orderNo
userId
addressSnapshot
totalAmount
status
createdAt
paidAt
canceledAt
items
payment
```

说明：

```text
只能查询自己的订单。
不能通过 orderId 查看别人的订单。
```

------

### 6.4 取消订单

```http
POST /api/trade/orders/{orderId}/cancel
```

用途：

```text
当前用户取消自己的待支付订单。
```

流程：

```text
OrderService 获取当前 userId
    -> 根据 orderId + userId 查询订单
    -> 不存在则返回 TRADE_ORDER_NOT_FOUND
    -> 校验订单状态必须是 PENDING_PAYMENT
    -> 查询订单项
    -> 开启事务
    -> 修改订单状态为 CANCELED
    -> 释放每个订单项的锁定库存
    -> 返回取消结果
```

规则：

```text
只能取消自己的订单
只能取消 PENDING_PAYMENT 订单
PAID 订单不能取消
CANCELED 订单不能重复取消
取消订单时释放锁定库存
取消订单不做退款
取消订单不发布 behavior 事件
```

库存变化：

```text
locked_quantity -= quantity
available_quantity += quantity
```

------

### 6.5 模拟支付

```http
POST /api/trade/orders/{orderId}/pay
```

用途：

```text
当前用户模拟支付自己的待支付订单。
```

请求字段：

```json
{
  "channel": "MOCK"
}
```

说明：

```text
V1.0 channel 可以不传，默认 MOCK。
不让前端传支付金额，支付金额直接使用订单 totalAmount。
```

流程：

```text
PaymentService 获取当前 userId
    -> 根据 orderId + userId 查询订单
    -> 不存在则返回 TRADE_ORDER_NOT_FOUND
    -> 校验订单状态必须是 PENDING_PAYMENT
    -> 查询订单项
    -> 开启事务
    -> 创建 payment_record
    -> 修改订单状态为 PAID
    -> 将锁定库存转为已售库存
    -> 返回支付结果
```

规则：

```text
只能支付自己的订单
只能支付 PENDING_PAYMENT 订单
PAID 订单不能重复支付
CANCELED 订单不能支付
支付成功后订单状态为 PAID
支付成功后锁定库存转为已售库存
模拟支付不调用第三方支付平台
模拟支付不发布 behavior 事件
```

库存变化：

```text
locked_quantity -= quantity
sold_quantity += quantity
```

------

## 7. 跨模块依赖

### 7.1 依赖 account

trade 使用：

```java
CurrentUserProvider.requireCurrentUser()
```

用途：

```text
获取当前登录用户 userId。
```

固定规则：

```text
trade 不直接读取 SecurityContext。
trade 不直接读取 sys_user、user_login_identity、sys_role。
Controller 不接收 userId。
userId 只来自 CurrentUserProvider。
```

------

### 7.2 依赖 account 地址能力

trade 使用：

```java
AddressQueryApi.requireOwnedAddress(Long userId, Long addressId)
```

用途：

```text
创建订单时校验地址属于当前用户。
创建订单时获取收货地址快照。
```

固定规则：

```text
trade 不直接读取 user_address 表。
trade 不导入 AddressEntity。
trade 把 AddressSnapshot 复制到 trade_order 中。
用户后续修改地址，不影响历史订单。
```

------

### 7.3 依赖 catalog

trade 使用：

```java
ProductQueryApi.requireSellableSkus(Collection<Long> skuIds)
```

用途：

```text
创建订单时校验 SKU 可售。
创建订单时获取商品名称、SKU 名称、分类、单价、图片快照。
```

固定规则：

```text
trade 不直接读取 product_category、product_spu、product_sku。
trade 不导入 catalog Entity。
trade 不让 catalog 判断库存。
trade 不让 catalog 扣库存。
trade 将 ProductSnapshot 复制到 trade_order_item 中。
```

------

### 7.4 不依赖 shopping

V1.0 trade 暂不依赖 shopping。

原因：

```text
V1.0 创建订单直接接收 skuId + quantity。
shopping 只是用户购买前意向，不强制成为下单入口。
暂不提供 CartCheckoutApi、CartQueryApi、CartItemSnapshot。
订单创建成功后不自动清理购物车。
```

后续如果要支持“从购物车结算”，再在 trade 或 shopping 后续版本中设计：

```text
CartCheckoutApi
CartItemSnapshot
订单创建成功后清理购物车项
```

------

### 7.5 暂不依赖 behavior

V1.0 trade 当前阶段不发布 behavior 事件。

后续 behavior 模块确认 API 后，可以在支付成功或订单创建成功后发布：

```text
PURCHASE
```

当前阶段不实现：

```text
BehaviorEventPublisher
RabbitMQ
消息重试
死信队列
消费幂等
```

------

## 8. 库存设计

### 8.1 创建订单时锁定库存

创建订单时不能只先查库存再扣库存。

错误方式：

```text
先 select available_quantity
判断够不够
再 update
```

这种方式在并发下可能超卖。

V1.0 使用条件更新锁定库存：

```sql
UPDATE inventory_stock
SET available_quantity = available_quantity - #{quantity},
    locked_quantity = locked_quantity + #{quantity},
    updated_at = NOW()
WHERE sku_id = #{skuId}
  AND available_quantity >= #{quantity}
```

判断方式：

```text
影响行数 = 1，说明锁定成功
影响行数 = 0，说明库存不存在或库存不足
```

如果需要区分库存不存在和库存不足，可以先按 skuId 查询库存记录：

```text
没有库存记录：TRADE_STOCK_NOT_FOUND
有库存记录但条件更新失败：TRADE_STOCK_NOT_ENOUGH
```

------

### 8.2 取消订单时释放库存

取消待支付订单时释放锁定库存：

```sql
UPDATE inventory_stock
SET available_quantity = available_quantity + #{quantity},
    locked_quantity = locked_quantity - #{quantity},
    updated_at = NOW()
WHERE sku_id = #{skuId}
  AND locked_quantity >= #{quantity}
```

说明：

```text
释放库存必须要求 locked_quantity 足够。
如果 locked_quantity 不足，说明库存数据异常，应返回库存状态错误或抛业务异常。
```

------

### 8.3 支付成功时确认库存

支付成功时把锁定库存转为已售库存：

```sql
UPDATE inventory_stock
SET locked_quantity = locked_quantity - #{quantity},
    sold_quantity = sold_quantity + #{quantity},
    updated_at = NOW()
WHERE sku_id = #{skuId}
  AND locked_quantity >= #{quantity}
```

说明：

```text
支付成功不再减少 available_quantity。
available_quantity 在创建订单时已经减少。
支付成功只是把 locked_quantity 转成 sold_quantity。
```

------

## 9. 状态流转

### 9.1 订单状态

订单状态：

```text
10 = PENDING_PAYMENT，待支付
20 = PAID，已支付
30 = CANCELED，已取消
```

合法流转：

```text
PENDING_PAYMENT -> PAID
PENDING_PAYMENT -> CANCELED
```

非法流转：

```text
PAID -> CANCELED
PAID -> PAID
CANCELED -> PAID
CANCELED -> CANCELED
```

非法状态操作统一返回：

```text
TRADE_ORDER_STATUS_NOT_ALLOWED
```

------

### 9.2 支付状态

V1.0 支付记录只保存成功支付：

```text
20 = SUCCESS
```

不实现：

```text
支付失败
支付处理中
退款
回调确认
```

------

## 10. 错误码设计

建议新增错误码：

```text
TRADE_ORDER_NOT_FOUND
TRADE_ORDER_STATUS_NOT_ALLOWED
TRADE_ORDER_EMPTY_ITEMS
TRADE_INVALID_QUANTITY
TRADE_DUPLICATE_SKU
TRADE_STOCK_NOT_FOUND
TRADE_STOCK_NOT_ENOUGH
TRADE_STOCK_STATE_INVALID
TRADE_PAYMENT_RECORD_EXISTS
```

含义：

| 错误码                         | HTTP 状态 | 场景                         |
| ------------------------------ | --------- | ---------------------------- |
| TRADE_ORDER_NOT_FOUND          | 404       | 订单不存在或不属于当前用户   |
| TRADE_ORDER_STATUS_NOT_ALLOWED | 409       | 当前订单状态不允许支付或取消 |
| TRADE_ORDER_EMPTY_ITEMS        | 400       | 创建订单时 items 为空        |
| TRADE_INVALID_QUANTITY         | 400       | quantity <= 0                |
| TRADE_DUPLICATE_SKU            | 400       | 创建订单请求中出现重复 skuId |
| TRADE_STOCK_NOT_FOUND          | 404       | 某个 SKU 没有库存记录        |
| TRADE_STOCK_NOT_ENOUGH         | 409       | 可售库存不足                 |
| TRADE_STOCK_STATE_INVALID      | 409       | 锁定库存不足等库存状态异常   |
| TRADE_PAYMENT_RECORD_EXISTS    | 409       | 支付记录已存在，防止重复支付 |

可复用其他模块错误：

```text
ACCOUNT_ADDRESS_NOT_FOUND
CATALOG_SKU_NOT_FOUND
CATALOG_PRODUCT_NOT_FOUND
CATALOG_PRODUCT_NOT_SELLABLE
```

------

## 11. 关键流程

### 11.1 创建订单流程

```text
前端请求 POST /api/trade/orders
    -> OrderController 接收 addressId 和 items
    -> OrderService 获取当前 userId
    -> 校验 items 非空
    -> 校验 quantity > 0
    -> 校验 skuId 不重复
    -> AddressQueryApi.requireOwnedAddress(userId, addressId)
    -> ProductQueryApi.requireSellableSkus(skuIds)
    -> 计算 totalAmount
    -> 开启事务
    -> 逐个 SKU 条件更新锁定库存
    -> 生成 orderNo
    -> 写入 trade_order
    -> 写入 trade_order_item
    -> 返回订单创建结果
```

事务说明：

```text
锁定库存、写订单、写订单项必须在同一个事务中。
任一步失败，整个事务回滚。
```

------

### 11.2 查询订单详情流程

```text
前端请求 GET /api/trade/orders/{orderId}
    -> OrderService 获取当前 userId
    -> 查询 orderId + userId 的订单
    -> 不存在则返回 TRADE_ORDER_NOT_FOUND
    -> 查询订单项
    -> 查询支付记录
    -> 组装 OrderDetailVO
    -> 返回
```

------

### 11.3 取消订单流程

```text
前端请求 POST /api/trade/orders/{orderId}/cancel
    -> OrderService 获取当前 userId
    -> 查询 orderId + userId 的订单
    -> 不存在则返回 TRADE_ORDER_NOT_FOUND
    -> 校验订单状态为 PENDING_PAYMENT
    -> 查询订单项
    -> 开启事务
    -> 更新订单状态为 CANCELED
    -> 释放锁定库存
    -> 返回取消结果
```

事务说明：

```text
修改订单状态和释放库存必须在同一个事务中。
如果释放库存失败，订单取消也应回滚。
```

------

### 11.4 模拟支付流程

```text
前端请求 POST /api/trade/orders/{orderId}/pay
    -> PaymentController 接收请求
    -> PaymentService 获取当前 userId
    -> 查询 orderId + userId 的订单
    -> 不存在则返回 TRADE_ORDER_NOT_FOUND
    -> 校验订单状态为 PENDING_PAYMENT
    -> 查询订单项
    -> 开启事务
    -> 写入 payment_record
    -> 更新订单状态为 PAID
    -> 确认库存，将 locked 转为 sold
    -> 返回支付结果
```

事务说明：

```text
写支付记录、更新订单状态、确认库存必须在同一个事务中。
任一步失败，整个事务回滚。
```

------

## 12. 安全规则

trade 所有用户接口都需要登录：

```text
POST /api/trade/orders
GET /api/trade/orders
GET /api/trade/orders/{orderId}
POST /api/trade/orders/{orderId}/cancel
POST /api/trade/orders/{orderId}/pay
```

SecurityConfig 不需要匿名放行 `/api/trade/**`。

因为当前规则是：

```text
/api/auth/register 匿名
/api/auth/login 匿名
/api/catalog/** 匿名
/actuator/health 匿名
/api/admin/** 需要 ROLE_ADMIN
/api/shopping/** 默认需要认证
/api/trade/** 默认需要认证
其他接口默认需要认证
```

trade 属于“其他接口”，因此默认需要认证。

------

## 13. 包结构建议

trade 模块由两个主要 Java 包组成：

```text
inventory
order
```

模拟支付属于 `order` 包中的订单状态流转能力，V1.0 不单独创建 `payment` 包。

建议结构：

```text
com.personaflow.commerce.inventory
├── service
├── entity
└── mapper

com.personaflow.commerce.order
├── controller
├── service
├── dto
├── vo
├── entity
└── mapper
```

建议类：

```text
InventoryStockEntity
InventoryStockMapper
InventoryService

TradeOrderEntity
TradeOrderItemEntity
TradeOrderMapper
TradeOrderItemMapper
PaymentRecordMapper
OrderController
OrderService
PaymentService

CreateOrderRequest
CreateOrderItemRequest
PayOrderRequest

OrderCreateVO
OrderListItemVO
OrderDetailVO
OrderItemVO
PaymentVO

PaymentRecordEntity
```

说明：

```text
模块名是 trade。
内部包使用 inventory、order。
inventory 只处理库存数据和库存状态变化。
order 处理订单创建、查询、取消和模拟支付。
```

------

## 14. Codex 实现阶段

trade 实现拆成 8 个阶段，每个阶段都必须可以编译或测试。

### 阶段 0：文档设计与冲突检查

实现：

```text
创建或同步 docs/modules/04-trade.md
检查 docs/v1.0-architecture.md
检查 docs/module-contracts.md
确认 trade 与 account/catalog/shopping/behavior 边界
```

不实现：

```text
Java 代码
SQL
Controller
Service
测试
```

验收：

```text
文档边界清楚
接口清单确定
不与 account/catalog/shopping 已确定约定冲突
明确 V1.0 trade 不依赖 shopping
明确 V1.0 trade 不发布 behavior 事件
```

------

### 阶段 1：表、Entity、Mapper、库存初始化

实现：

```text
Flyway V4__create_trade_tables.sql
inventory_stock
trade_order
trade_order_item
payment_record

InventoryStockEntity
TradeOrderEntity
TradeOrderItemEntity
PaymentRecordEntity

InventoryStockMapper
TradeOrderMapper
TradeOrderItemMapper
PaymentRecordMapper

为已有演示 SKU 初始化库存
```

不实现：

```text
Controller
Service
HTTP 接口
库存锁定逻辑
订单创建逻辑
支付逻辑
取消订单逻辑
behavior
RabbitMQ
admin
agent
```

验收：

```text
Flyway 版本号不冲突
表字段和索引正确
库存表有初始化数据
.\mvnw.cmd -DskipTests compile 通过
```

------

### 阶段 2：库存服务

实现：

```text
InventoryService
锁定库存
释放库存
确认扣减库存
```

核心方法建议：

```java
void lockStock(Long skuId, Integer quantity);

void releaseLockedStock(Long skuId, Integer quantity);

void confirmLockedStock(Long skuId, Integer quantity);
```

不实现：

```text
订单创建
支付
取消订单
HTTP 接口
behavior
RabbitMQ
admin
agent
```

验收：

```text
库存充足时锁定成功
库存不足时返回 TRADE_STOCK_NOT_ENOUGH
库存不存在时返回 TRADE_STOCK_NOT_FOUND
释放库存能把 locked 转回 available
确认库存能把 locked 转为 sold
库存状态异常时返回 TRADE_STOCK_STATE_INVALID
测试不连接真实 MySQL，使用 Mockito
```

------

### 阶段 3：创建订单接口

实现：

```text
POST /api/trade/orders
OrderController
OrderService.createOrder
CreateOrderRequest
CreateOrderItemRequest
OrderCreateVO
OrderItemVO
```

依赖：

```text
CurrentUserProvider
AddressQueryApi
ProductQueryApi
InventoryService
```

不实现：

```text
订单查询
取消订单
支付
behavior
RabbitMQ
admin
agent
购物车结算
```

验收：

```text
创建订单成功
items 为空返回 TRADE_ORDER_EMPTY_ITEMS
quantity <= 0 返回 TRADE_INVALID_QUANTITY
重复 skuId 返回 TRADE_DUPLICATE_SKU
地址不属于当前用户时终止创建订单
SKU 不可售时终止创建订单
库存不足时终止创建订单
订单项保存商品快照
订单保存地址快照
创建订单后状态为 PENDING_PAYMENT
未登录访问返回 401
```

------

### 阶段 4：订单查询接口

实现：

```text
GET /api/trade/orders
GET /api/trade/orders/{orderId}
OrderListItemVO
OrderDetailVO
PaymentVO
```

不实现：

```text
取消订单
支付
behavior
RabbitMQ
admin
agent
```

验收：

```text
只能查询当前用户订单
订单不存在或不属于当前用户返回 TRADE_ORDER_NOT_FOUND
订单详情返回订单项快照
订单详情可返回支付信息
订单列表支持分页
未登录访问返回 401
```

------

### 阶段 5：取消订单接口

实现：

```text
POST /api/trade/orders/{orderId}/cancel
OrderService.cancelOrder
```

依赖：

```text
InventoryService.releaseLockedStock
```

不实现：

```text
退款
支付
behavior
RabbitMQ
admin
agent
```

验收：

```text
待支付订单可以取消
取消后订单状态为 CANCELED
取消后释放锁定库存
已支付订单不能取消
已取消订单不能重复取消
订单不存在或不属于当前用户返回 TRADE_ORDER_NOT_FOUND
未登录访问返回 401
```

------

### 阶段 6：模拟支付接口

实现：

```text
POST /api/trade/orders/{orderId}/pay
PaymentService.payOrder
PayOrderRequest
PaymentVO
```

依赖：

```text
InventoryService.confirmLockedStock
```

不实现：

```text
真实支付
支付失败
支付回调
退款
behavior
RabbitMQ
admin
agent
```

验收：

```text
待支付订单可以模拟支付
支付后订单状态为 PAID
支付后创建 payment_record
支付后 locked 库存转为 sold 库存
已支付订单不能重复支付
已取消订单不能支付
订单不存在或不属于当前用户返回 TRADE_ORDER_NOT_FOUND
未登录访问返回 401
```

------

### 阶段 7：trade 收尾

实现：

```text
同步 trade HTTP 接口清单
同步错误码
同步库存状态流转
检查模块边界
运行 compile 和目标测试
```

验收：

```text
没有真实支付
没有退款、售后、物流
没有优惠券
没有店铺或商家系统
没有购物车结算接口
没有 behavior/RabbitMQ 代码
没有 admin/agent 代码
没有直接读取 account/catalog 表
没有直接导入 account/catalog Entity
compile 通过
目标测试通过
完整 mvnw test 通过
```

------

## 15. 验收标准

trade 模块完成后，应满足：

```text
1. 用户可以直接通过 skuId + quantity 创建订单
2. 创建订单会校验当前用户
3. 创建订单会校验地址归属
4. 创建订单会校验 SKU 可售
5. 创建订单会校验库存充足
6. 创建订单会保存地址快照
7. 创建订单会保存商品快照和价格快照
8. 创建订单会锁定库存，避免超卖
9. 用户可以查询自己的订单列表
10. 用户可以查询自己的订单详情
11. 用户可以取消待支付订单
12. 取消订单会释放锁定库存
13. 用户可以模拟支付待支付订单
14. 支付成功会将锁定库存转为已售库存
15. 已支付订单不能重复支付
16. 已取消订单不能支付
17. 已支付订单不能取消
18. trade 不直接读取 account/catalog 表
19. trade 不依赖 shopping
20. trade 不实现真实支付、退款、售后、物流、优惠券、behavior、RabbitMQ、Agent
21. compile 通过
22. 单元测试通过
23. 完整 mvnw test 通过
```

------

## 16. 当前设计结论

trade 是 V1.0 的交易闭环模块。

它不需要做完整电商交易中台，但必须把核心交易动作做真实：

```text
创建订单
锁定库存
保存订单项快照
取消订单释放库存
模拟支付确认库存
订单状态流转
```

V1.0 trade 的核心设计判断是：

```text
创建订单时锁定库存
取消订单时释放库存
支付成功时确认库存
```

trade 的核心技术价值是：

```text
用事务保证订单、订单项、库存变更一致
用条件更新避免并发超卖
用订单状态限制非法操作
用订单项快照保证历史订单稳定
```

这部分是 V1.0 后端最能体现业务深度的模块。
