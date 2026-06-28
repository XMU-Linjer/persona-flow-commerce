# 03-shopping 模块设计

## 1. 模块定位

`shopping` 模块负责 V1.0 的用户购买前操作。

它提供：

```text
收藏
取消收藏
收藏列表
加入购物车
修改购物车数量
删除购物车项
清空购物车
购物车列表
```

一句话定义：

```text
shopping 是用户下单前的购买意向模块，负责记录用户想买什么，但不负责真正成交。
```

shopping 的核心职责是维护当前登录用户的收藏和购物车数据，并通过 catalog 模块提供的商品快照组装前端展示结果。

## 2. V1.0 目标

V1.0 shopping 需要完成：

```text
1. 用户可以收藏某个 SKU
2. 用户可以取消收藏某个 SKU
3. 用户可以查看收藏列表
4. 用户可以把某个 SKU 加入购物车
5. 用户可以修改购物车商品数量
6. 用户可以删除购物车项
7. 用户可以清空购物车
8. 用户可以查看购物车列表
9. shopping 可以通过 ProductQueryApi 获取商品快照
10. shopping 不直接读取 catalog 表
```

shopping 不追求复杂电商营销能力，只做演示型电商平台需要的最小收藏和购物车能力。

## 3. 不做什么

V1.0 shopping 不做：

```text
订单创建
支付
库存扣减
库存校验
库存锁定
优惠券
满减
价格最终结算
购物车选中状态
购物车跨设备同步策略
购物车失效商品自动清理任务
行为事件落库
RabbitMQ 事件发布
Agent 推荐
admin 管理
```

说明：

```text
订单创建属于 trade。
支付属于 trade。
库存扣减和库存校验属于 trade/inventory。
行为事件落库、RabbitMQ、Redis 行为统计属于 behavior。
Agent 推荐和画像属于 V1.1。
```

shopping 只记录用户收藏了什么、购物车里有什么。

## 4. 核心概念

### 4.1 Favorite

`Favorite` 表示用户收藏了某个 SKU。

V1.0 收藏对象确定为：

```text
skuId
```

原因：

```text
catalog 已经提供 ProductQueryApi.requireSellableSku(skuId)
shopping 可以直接通过 skuId 获取商品快照
不需要额外设计 SPU 级收藏快照接口
```

规则：

```text
必须登录
同一个用户不能重复收藏同一个 skuId
重复收藏直接返回成功，保持幂等
取消不存在的收藏也返回成功，保持简单
```

### 4.2 CartItem

`CartItem` 表示用户购物车中的一个 SKU 和数量。

购物车项只保存：

```text
userId
skuId
quantity
```

商品名、SKU 名、价格、图片、分类等展示信息不长期复制到购物车表中，而是在查询购物车列表时通过 `ProductQueryApi` 获取商品快照。

规则：

```text
必须登录
加入购物车时校验 SKU 可售
同一个用户同一个 skuId 只保留一条购物车项
重复加入时增加数量
quantity 必须大于 0
```

## 5. 数据表设计

### 5.1 user_favorite

用户收藏表。

字段建议：

```text
id              BIGINT 主键
user_id         BIGINT 用户 ID
sku_id          BIGINT SKU ID
created_at      DATETIME 创建时间
```

约束和索引：

```text
主键：id
唯一索引：uk_user_favorite_user_sku(user_id, sku_id)
索引：idx_user_favorite_user_id(user_id)
索引：idx_user_favorite_sku_id(sku_id)
```

说明：

```text
user_id 来自 account 的 CurrentUserProvider。
sku_id 来自前端请求，由 catalog 的 ProductQueryApi 校验是否可售。
表中不保存商品名称、价格、图片。
查询收藏列表时再通过 ProductQueryApi 查询商品快照。
```

### 5.2 shopping_cart_item

购物车项表。

字段建议：

```text
id              BIGINT 主键
user_id         BIGINT 用户 ID
sku_id          BIGINT SKU ID
quantity        INT 商品数量
created_at      DATETIME 创建时间
updated_at      DATETIME 更新时间
```

约束和索引：

```text
主键：id
唯一索引：uk_cart_item_user_sku(user_id, sku_id)
索引：idx_cart_item_user_id(user_id)
索引：idx_cart_item_sku_id(sku_id)
```

说明：

```text
同一用户同一 SKU 只能有一条购物车项。
重复加购时增加 quantity。
quantity 必须大于 0。
购物车不保存真实库存。
购物车不负责库存校验。
购物车不保存最终订单价格。
```

## 6. HTTP 接口设计

shopping 的所有接口都需要登录。

原因：

```text
收藏和购物车都是用户个人数据，必须知道当前 userId。
```

当前 userId 只能通过 `CurrentUserProvider` 获取，Controller 不接收 userId 参数。

------

### 6.1 添加收藏

```http
POST /api/shopping/favorites/{skuId}
```

用途：

```text
当前用户收藏某个 SKU。
```

流程：

```text
Controller 接收 skuId
    -> FavoriteService 获取当前 userId
    -> 调用 ProductQueryApi.requireSellableSku(skuId)
    -> 如果已收藏，直接返回成功
    -> 如果未收藏，写入 user_favorite
```

响应字段建议：

```text
skuId
favorited
```

规则：

```text
必须登录
SKU 必须存在且可售
重复收藏返回成功
不发布 behavior 事件
```

------

### 6.2 取消收藏

```http
DELETE /api/shopping/favorites/{skuId}
```

用途：

```text
当前用户取消收藏某个 SKU。
```

流程：

```text
Controller 接收 skuId
    -> FavoriteService 获取当前 userId
    -> 删除 user_id + sku_id 对应记录
    -> 如果记录不存在，也返回成功
```

响应字段建议：

```text
skuId
favorited
```

规则：

```text
必须登录
取消不存在的收藏返回成功
不发布 behavior 事件
```

------

### 6.3 查询收藏列表

```http
GET /api/shopping/favorites
```

查询参数：

```text
page    页码，默认 1
size    每页数量，默认 10
```

用途：

```text
当前用户查看收藏列表。
```

流程：

```text
FavoriteService 获取当前 userId
    -> 分页查询 user_favorite
    -> 收集 skuId
    -> 调用 ProductQueryApi.requireSellableSkus(skuIds)
    -> 组装 FavoriteItemVO
    -> 返回分页结果
```

响应字段建议：

```text
favoriteId
skuId
spuId
categoryId
categoryName
productName
skuName
unitPrice
imageUrl
createdAt
```

说明：

```text
收藏表只存 skuId。
前端展示所需商品信息来自 ProductSnapshot。
如果某个收藏的 SKU 已经不可售，V1.0 可以让 ProductQueryApi 抛异常并终止查询；后续如需更好体验，再设计“失效收藏项”。
```

------

### 6.4 加入购物车

```http
POST /api/shopping/cart/items
```

请求字段：

```json
{
  "skuId": 1001,
  "quantity": 2
}
```

用途：

```text
当前用户把某个 SKU 加入购物车。
```

流程：

```text
CartService 获取当前 userId
    -> 校验 quantity > 0
    -> 调用 ProductQueryApi.requireSellableSku(skuId)
    -> 查询当前用户是否已有该 skuId 的购物车项
    -> 如果没有，新增购物车项
    -> 如果已有，quantity 累加
    -> 返回购物车项结果
```

响应字段建议：

```text
cartItemId
skuId
quantity
```

规则：

```text
必须登录
SKU 必须存在且可售
quantity 必须大于 0
重复加入同一 SKU 时增加数量
不做库存校验
不发布 behavior 事件
```

------

### 6.5 修改购物车数量

```http
PATCH /api/shopping/cart/items/{cartItemId}
```

请求字段：

```json
{
  "quantity": 3
}
```

用途：

```text
当前用户修改某个购物车项数量。
```

流程：

```text
CartService 获取当前 userId
    -> 根据 cartItemId + userId 查询购物车项
    -> 不存在则返回购物车项不存在错误
    -> 校验 quantity > 0
    -> 更新 quantity
```

响应字段建议：

```text
cartItemId
skuId
quantity
```

规则：

```text
只能修改自己的购物车项
不能通过 cartItemId 修改别人购物车
quantity 必须大于 0
不做库存校验
```

------

### 6.6 删除购物车项

```http
DELETE /api/shopping/cart/items/{cartItemId}
```

用途：

```text
当前用户删除自己的某个购物车项。
```

流程：

```text
CartService 获取当前 userId
    -> 根据 cartItemId + userId 删除
    -> 不存在则返回 SHOPPING_CART_ITEM_NOT_FOUND
```

V1.0 规则：

```text
删除不存在或不属于当前用户的 cartItemId，返回 SHOPPING_CART_ITEM_NOT_FOUND。
```

原因：

```text
cartItemId 是购物车项内部 ID。
如果前端传错，明确报错更好排查。
```

------

### 6.7 清空购物车

```http
DELETE /api/shopping/cart/items
```

用途：

```text
当前用户清空自己的购物车。
```

流程：

```text
CartService 获取当前 userId
    -> 删除该用户所有购物车项
    -> 返回成功
```

规则：

```text
只能清空自己的购物车
购物车本来为空也返回成功
```

------

### 6.8 查询购物车列表

```http
GET /api/shopping/cart/items
```

用途：

```text
当前用户查看购物车列表。
```

流程：

```text
CartService 获取当前 userId
    -> 查询当前用户所有购物车项
    -> 收集 skuId
    -> 调用 ProductQueryApi.requireSellableSkus(skuIds)
    -> 组装 CartItemVO
    -> 计算 subtotal
    -> 返回购物车列表
```

响应字段建议：

```text
cartItemId
skuId
spuId
categoryId
categoryName
productName
skuName
unitPrice
imageUrl
quantity
subtotal
createdAt
updatedAt
```

说明：

```text
subtotal = unitPrice * quantity
subtotal 只是购物车展示金额，不是订单最终金额。
创建订单时 trade 会重新获取商品快照并执行自己的下单校验。
```

## 7. 跨模块依赖

### 7.1 依赖 account

shopping 使用：

```java
CurrentUserProvider.requireCurrentUser()
```

用途：

```text
获取当前登录用户 userId。
```

固定规则：

```text
shopping 不直接读取 SecurityContext。
shopping 不直接读取 sys_user、user_login_identity、sys_role。
Controller 不接收 userId。
userId 只来自 CurrentUserProvider。
```

### 7.2 依赖 catalog

shopping 使用：

```java
ProductQueryApi.requireSellableSku(Long skuId)
ProductQueryApi.requireSellableSkus(Collection<Long> skuIds)
```

用途：

```text
添加收藏时校验 SKU 可售
加入购物车时校验 SKU 可售
查询收藏列表时获取商品展示快照
查询购物车列表时获取商品展示快照
```

固定规则：

```text
shopping 不直接读取 product_category、product_spu、product_sku。
shopping 不直接导入 catalog Entity。
shopping 只接收 ProductSnapshot。
shopping 不负责判断商品上下架细节，由 catalog 负责。
```

## 8. 是否需要跨模块接口给 trade

V1.0 shopping 暂不提供 `CartQueryApi`。

原因：

```text
为了速通，trade 可以先直接接收 skuId + quantity 创建订单。
购物车只是用户购买前的临时意向，不必强制成为下单入口。
```

后续如果 trade 需要从购物车创建订单，再补充：

```text
CartCheckoutApi
CartItemSnapshot
```

暂不实现：

```text
CartQueryApi
CartCheckoutApi
CartItemSnapshot
订单创建后自动清理购物车项
```

这些留到 trade 模块设计时再确认。

## 9. 错误码

当前使用：

```text
SHOPPING_CART_ITEM_NOT_FOUND
SHOPPING_INVALID_QUANTITY
```

可复用 catalog 错误：

```text
CATALOG_SKU_NOT_FOUND
CATALOG_PRODUCT_NOT_SELLABLE
```

收藏重复不作为错误：

```text
重复收藏返回成功
取消不存在的收藏返回成功
```

购物车错误规则：

```text
quantity <= 0 返回 SHOPPING_INVALID_QUANTITY，HTTP 400
cartItemId 不存在或不属于当前用户返回 SHOPPING_CART_ITEM_NOT_FOUND，HTTP 404
```

## 10. 关键流程

### 10.1 添加收藏流程

```text
前端请求 POST /api/shopping/favorites/{skuId}
    -> FavoriteController 接收 skuId
    -> FavoriteService 获取当前 userId
    -> ProductQueryApi.requireSellableSku(skuId)
    -> 查询是否已收藏
    -> 已收藏则直接返回成功
    -> 未收藏则写入 user_favorite
    -> 返回收藏成功
```

### 10.2 取消收藏流程

```text
前端请求 DELETE /api/shopping/favorites/{skuId}
    -> FavoriteController 接收 skuId
    -> FavoriteService 获取当前 userId
    -> 删除 user_id + sku_id 记录
    -> 不存在也返回成功
```

### 10.3 查询收藏列表流程

```text
前端请求 GET /api/shopping/favorites
    -> FavoriteService 获取当前 userId
    -> 分页查询 user_favorite
    -> 收集 skuId
    -> ProductQueryApi.requireSellableSkus(skuIds)
    -> 组装 FavoriteItemVO
    -> 返回 PageResult
```

### 10.4 加入购物车流程

```text
前端请求 POST /api/shopping/cart/items
    -> CartController 接收 skuId 和 quantity
    -> CartService 获取当前 userId
    -> 校验 quantity > 0
    -> ProductQueryApi.requireSellableSku(skuId)
    -> 查询 user_id + sku_id 是否已存在
    -> 不存在则新增
    -> 已存在则累加 quantity
    -> 返回购物车项
```

### 10.5 修改购物车数量流程

```text
前端请求 PATCH /api/shopping/cart/items/{cartItemId}
    -> CartService 获取当前 userId
    -> 查询 cartItemId + userId
    -> 不存在则返回 SHOPPING_CART_ITEM_NOT_FOUND
    -> 校验 quantity > 0
    -> 更新 quantity
```

### 10.6 查询购物车列表流程

```text
前端请求 GET /api/shopping/cart/items
    -> CartService 获取当前 userId
    -> 查询当前用户购物车项
    -> 收集 skuId
    -> ProductQueryApi.requireSellableSkus(skuIds)
    -> 组装 CartItemVO
    -> 计算 subtotal
    -> 返回列表
```

## 11. 安全规则

shopping 所有接口都需要登录：

```text
POST /api/shopping/favorites/{skuId}
DELETE /api/shopping/favorites/{skuId}
GET /api/shopping/favorites
POST /api/shopping/cart/items
PATCH /api/shopping/cart/items/{cartItemId}
DELETE /api/shopping/cart/items/{cartItemId}
DELETE /api/shopping/cart/items
GET /api/shopping/cart/items
```

SecurityConfig 不需要专门放行 `/api/shopping/**`。

因为当前规则是：

```text
/api/auth/register 匿名
/api/auth/login 匿名
/api/catalog/** 匿名
/actuator/health 匿名
/api/admin/** 需要 ROLE_ADMIN
/api/shopping/** 默认需要认证
其他接口默认需要认证
```

shopping 属于“其他接口”，因此默认需要认证。

## 12. 包结构建议

shopping 模块由两个 Java 包组成：

```text
favorite
cart
```

建议结构：

```text
com.personaflow.commerce.favorite
├── controller
├── service
├── dto
├── vo
├── entity
└── mapper

com.personaflow.commerce.cart
├── controller
├── service
├── dto
├── vo
├── entity
└── mapper
```

建议类：

```text
FavoriteEntity
FavoriteMapper
FavoriteController
FavoriteService
FavoriteItemVO
FavoriteStatusVO

CartItemEntity
CartItemMapper
CartController
CartService
AddCartItemRequest
UpdateCartItemRequest
CartItemVO
CartItemSimpleVO
```

说明：

```text
模块名是 shopping。
内部包沿用总体架构中的 favorite 和 cart。
```

## 13. Codex 实现阶段

shopping 实现拆成 5 个阶段，每个阶段都必须可以编译或测试。

### 阶段 0：文档冲突收口

实现：

```text
同步 docs/modules/03-shopping.md
检查 docs/v1.0-architecture.md
检查 docs/module-contracts.md
确认 shopping 与 account/catalog/trade/behavior 边界
确认 V1.0 不提供 CartQueryApi、CartCheckoutApi 或 CartItemSnapshot
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
不与 account/catalog 已确定约定冲突
```

### 阶段 1：表、Entity、Mapper

实现：

```text
Flyway V3__create_shopping_tables.sql
user_favorite
shopping_cart_item
FavoriteEntity
FavoriteMapper
CartItemEntity
CartItemMapper
```

不实现：

```text
Controller
Service
ProductQueryApi 调用
HTTP 接口
behavior
trade
admin
```

验收：

```text
Flyway 版本号不冲突
表字段和唯一索引正确
.\mvnw.cmd -DskipTests compile 通过
```

### 阶段 2：收藏接口

实现：

```text
POST /api/shopping/favorites/{skuId}
DELETE /api/shopping/favorites/{skuId}
GET /api/shopping/favorites
FavoriteController
FavoriteService
Favorite 相关 VO
```

依赖：

```text
CurrentUserProvider
ProductQueryApi
```

不实现：

```text
购物车
trade
behavior
RabbitMQ
Agent
```

验收：

```text
添加收藏成功
重复收藏幂等成功
取消收藏成功
取消不存在收藏幂等成功
收藏列表能返回商品快照
未登录访问返回 401
```

### 阶段 3：购物车接口

实现：

```text
POST /api/shopping/cart/items
PATCH /api/shopping/cart/items/{cartItemId}
DELETE /api/shopping/cart/items/{cartItemId}
DELETE /api/shopping/cart/items
GET /api/shopping/cart/items
CartController
CartService
Cart 相关 DTO/VO
```

依赖：

```text
CurrentUserProvider
ProductQueryApi
```

不实现：

```text
订单
支付
库存
behavior
RabbitMQ
Agent
```

验收：

```text
加入购物车成功
重复加入同一 skuId 时累加 quantity
修改数量成功
quantity <= 0 返回错误
删除购物车项成功
清空购物车成功
购物车列表能返回商品快照和 subtotal
未登录访问返回 401
```

### 阶段 4：shopping 收尾

实现：

```text
同步文档
检查接口清单
检查模块边界
运行 compile 和目标测试
```

验收：

```text
没有 trade/order/payment/inventory 代码
没有 behavior/RabbitMQ 代码
没有 admin 代码
没有直接读取 catalog 表
没有直接读取 account 表
```

## 14. 验收标准

shopping 模块完成后，应满足：

```text
1. 用户可以收藏 SKU
2. 用户可以取消收藏 SKU
3. 收藏操作具备幂等性
4. 用户可以查询收藏列表
5. 用户可以加入购物车
6. 重复加入同一 SKU 会增加数量
7. 用户可以修改购物车数量
8. 用户可以删除购物车项
9. 用户可以清空购物车
10. 用户可以查询购物车列表
11. 收藏列表和购物车列表能展示商品快照
12. shopping 只通过 CurrentUserProvider 获取当前用户
13. shopping 只通过 ProductQueryApi 获取商品快照
14. shopping 不直接读取 account/catalog 表
15. shopping 不实现订单、支付、库存、behavior、RabbitMQ 或 Agent
16. compile 通过
17. 单元测试通过
```

## 15. 当前设计结论

shopping 是 V1.0 的收藏和购物车模块。

它不需要复杂，只需要记录当前用户的购买意向，并在展示时通过 catalog 获取商品快照。

V1.0 shopping 的核心价值是：

```text
连接用户身份和商品快照
支持收藏和购物车
为后续 trade 创建订单提供用户购买意向来源
为后续 behavior 和 Agent 提供可观察的业务动作
```
