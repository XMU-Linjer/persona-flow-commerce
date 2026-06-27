# 02-catalog 模块设计

## 1. 模块定位

`catalog` 模块负责 V1.0 的商品目录能力。

它提供：

```text
分类
商品 SPU
商品 SKU
商品列表
商品详情
简单搜索
商品快照查询
```

它的主要目标不是做复杂商品后台，而是让前端可以展示商品，让用户可以浏览、搜索、选择 SKU，并为后续 `shopping`、`trade`、`behavior` 和 V1.1 Agent 提供商品基础数据。

一句话定义：

```text
catalog 是商品数据来源，负责“商品是什么、怎么展示、怎么被选择”。
```

## 2. V1.0 目标

V1.0 catalog 需要完成：

```text
1. 前端可以获取分类列表
2. 前端可以分页查看商品列表
3. 前端可以通过分类和关键词搜索商品
4. 前端可以查看商品详情
5. 前端可以查看商品 SKU 列表并选择 SKU
6. 后续模块可以根据 skuId 获取商品快照
7. 商品数据为后续 behavior 和 V1.1 Agent 分析保留语义基础
```

catalog 不追求完整商品中台，只做演示型电商平台需要的最小商品目录能力。

## 3. 不做什么

V1.0 catalog 不做：

```text
复杂商品后台 CRUD
复杂分类树管理
复杂规格属性组合
品牌管理后台
图片上传
对象存储
商品审核
优惠券
秒杀
复杂搜索引擎
Elasticsearch
复杂排序筛选
库存扣减
订单
购物车
收藏
用户行为落库
RabbitMQ 事件发布
Redis 热词统计
Agent 推荐
支付
```

说明：

```text
库存扣减属于 trade/inventory。
购物车和收藏属于 shopping。
搜索/浏览行为沉淀属于 behavior。
RabbitMQ 行为事件发布属于 behavior API 确认后的后续集成。
Agent 推荐和画像属于 V1.1。
```

catalog 只保留商品展示和商品选择所需的数据。

## 4. 核心概念

### 4.1 Category

`Category` 表示商品分类。

例如：

```text
数码配件
电脑办公
家居生活
运动户外
食品饮料
```

V1.0 允许一到二级分类，但不做复杂分类树管理。

### 4.2 SPU

`SPU` 表示商品主体。

例如：

```text
机械键盘
人体工学椅
无线蓝牙耳机
便携显示器
```

SPU 负责商品名称、描述、主图、详情属性等展示信息。

### 4.3 SKU

`SKU` 表示具体可购买的规格。

例如：

```text
机械键盘 / 青轴 / 白色
机械键盘 / 茶轴 / 黑色
人体工学椅 / 标准款 / 黑色
人体工学椅 / 升级款 / 灰色
```

SKU 负责具体规格名、价格、规格属性、SKU 图片等信息。

关系：

```text
category
  -> product_spu
      -> product_sku
```

## 5. 数据表设计

### 5.1 product_category

商品分类表。

字段建议：

```text
id              BIGINT 主键
name            VARCHAR(100) 分类名称
parent_id       BIGINT 父分类 ID，顶级分类为 NULL
level           INT 分类层级，V1.0 可用 1/2
sort_order      INT 排序值
status          TINYINT 状态，1=启用，0=禁用
icon_url        VARCHAR(500) 分类图标，可为空
created_at      DATETIME 创建时间
updated_at      DATETIME 更新时间
```

约束和索引：

```text
主键：id
索引：parent_id
索引：status
```

说明：

```text
V1.0 不做复杂分类树维护。
分类数据可以先通过 Flyway 初始化。
```

### 5.2 product_spu

商品主体表。

字段建议：

```text
id                  BIGINT 主键
category_id         BIGINT 分类 ID
name                VARCHAR(200) 商品名称
subtitle            VARCHAR(300) 商品副标题，可为空
brand               VARCHAR(100) 品牌，可为空
description         TEXT 商品描述，可为空
main_image_url      VARCHAR(500) 商品主图，可为空
detail_images_json  TEXT 商品详情图 JSON，可为空
attributes_json     TEXT 商品展示属性 JSON，可为空
tags                VARCHAR(300) 商品标签，可为空
status              TINYINT 状态，1=上架，0=下架
sort_order          INT 排序值
created_at          DATETIME 创建时间
updated_at          DATETIME 更新时间
```

约束和索引：

```text
主键：id
索引：category_id
索引：status
索引：name
```

说明：

```text
attributes_json 用来存放商品展示属性，例如材质、重量、适用场景等。
detail_images_json 用来存放详情图列表。
tags 可以存简单标签，例如 “办公,学生,高性价比”。
V1.0 不单独拆商品属性表。
```

attributes_json 示例：

```json
{
  "连接方式": "蓝牙/2.4G/有线",
  "适用场景": "办公、学习、游戏",
  "重量": "约 980g"
}
```

### 5.3 product_sku

商品 SKU 表。

字段建议：

```text
id                  BIGINT 主键
spu_id              BIGINT 商品 SPU ID
sku_name            VARCHAR(200) SKU 名称
specs_json          TEXT SKU 规格 JSON，可为空
price               DECIMAL(10,2) 当前价格
original_price      DECIMAL(10,2) 原价，可为空
image_url           VARCHAR(500) SKU 图片，可为空
status              TINYINT 状态，1=可售，0=不可售
sales_count         INT 销量展示值
created_at          DATETIME 创建时间
updated_at          DATETIME 更新时间
```

约束和索引：

```text
主键：id
索引：spu_id
索引：status
```

说明：

```text
price 使用 DECIMAL，不使用 double。
V1.0 不做复杂规格属性表。
真实库存扣减不放在 catalog 中，由后续 trade/inventory 负责。
sales_count 只是展示字段，不作为真实订单统计来源。
catalog 不保存真实库存，不负责库存流水、库存锁定、下单库存校验或库存扣减。
```

specs_json 示例：

```json
{
  "颜色": "黑色",
  "版本": "标准款",
  "尺寸": "中号"
}
```

### 5.4 不在 catalog V1.0 实现的表

`search_record` 不在 catalog V1.0 实现。

```text
搜索记录
浏览记录
热词统计
用户兴趣统计
```

这些能力属于后续 behavior 模块。catalog 只提供 keyword 查询商品能力，不负责行为落库、RabbitMQ 发布或 Redis 热词统计。

## 6. HTTP 接口设计

catalog 的用户浏览接口允许匿名访问。

原因：

```text
普通电商平台中，未登录用户也可以浏览商品。
收藏、购物车、下单等操作再要求登录。
```

如果后续 behavior 需要记录登录用户的浏览行为，可以在有 token 时读取用户；无 token 时不强制登录。

### 6.1 查询分类列表

```http
GET /api/catalog/categories
```

用途：

```text
前端首页、分类页获取商品分类。
```

响应字段建议：

```text
id
name
parentId
level
sortOrder
status
iconUrl
children
```

说明：

```text
返回树形结构。
数据库仍然通过 parent_id 保存父子关系，由 Service 层组装 children。
```

### 6.2 查询商品列表

```http
GET /api/catalog/products
```

查询参数：

```text
categoryId  可选，按分类过滤
keyword     可选，按商品名称、副标题、品牌进行 LIKE 搜索
page        页码，从 1 开始
size        每页数量
```

用途：

```text
首页商品列表
分类商品列表
搜索结果页
```

响应字段建议：

```text
spuId
categoryId
categoryName
name
subtitle
brand
mainImageUrl
minPrice
maxPrice
salesCount
tags
status
```

说明：

```text
列表只返回商品概要，不返回完整 description 和全部 SKU。
minPrice/maxPrice 从可售 SKU 中计算。
V1.0 搜索用 MySQL LIKE 即可，不接 Elasticsearch。
```

### 6.3 查询商品详情

```http
GET /api/catalog/products/{spuId}
```

用途：

```text
商品详情页展示完整商品信息和 SKU 列表。
```

响应字段建议：

```text
spuId
categoryId
categoryName
name
subtitle
brand
description
mainImageUrl
detailImages
attributes
tags
status
skus
```

SKU 字段：

```text
skuId
skuName
specs
price
originalPrice
imageUrl
status
salesCount
```

说明：

```text
详情接口返回前端展示商品所需的主要字段。
用户选择 SKU 后，后续加入购物车或创建订单使用 skuId。
```

### 6.4 查询 SKU 信息

```http
GET /api/catalog/skus/{skuId}
```

用途：

```text
前端或调试场景根据 skuId 查询 SKU 展示信息。
```

响应字段建议：

```text
skuId
spuId
productName
skuName
categoryId
categoryName
price
originalPrice
mainImageUrl
imageUrl
specs
status
```

说明：

```text
后续模块不建议通过 HTTP 调用此接口。
shopping/trade 应优先通过 ProductQueryApi 获取商品快照。
```

## 7. 跨模块接口

catalog 需要向其他模块提供商品快照查询能力。

### 7.1 ProductQueryApi

```java
public interface ProductQueryApi {

    ProductSnapshot requireSellableSku(Long skuId);

    Map<Long, ProductSnapshot> requireSellableSkus(Collection<Long> skuIds);
}
```

说明：

```text
requireSellableSku 用于 shopping 加购物车、trade 创建订单。
requireSellableSkus 用于购物车列表或订单创建时批量查询，避免循环查库。
不返回 Entity。
不返回库存。
不负责扣库存。
```

### 7.2 ProductSnapshot

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

规则：

```text
只返回后续模块需要的商品快照。
不返回 Entity。
不返回 description、detailImages、attributes 等展示详情。
不返回库存。
不负责扣库存。
unitPrice 是当前商品单价，后续 trade 创建订单时保存为订单价格快照。
imageUrl 是展示图，优先 SKU 图片，没有则使用 SPU 主图。
categoryId/categoryName 保留，方便后续 behavior 和 Agent 分析。
不使用 skuDescription。
不同时暴露 mainImageUrl 和 skuImageUrl 给跨模块快照。
```

### 7.3 requireSellableSku 校验规则

`requireSellableSku` 必须校验：

```text
SKU 存在
SKU status = 1
SPU 存在
SPU status = 1
Category 存在
Category status = 1
```

任一不满足，抛业务异常。

建议错误码：

```text
CATALOG_SKU_NOT_FOUND
CATALOG_PRODUCT_NOT_FOUND
CATALOG_PRODUCT_NOT_SELLABLE
```

HTTP 状态建议：

```text
不存在：404
不可售：400 或 409
```

## 8. 和其他模块的关系

### 8.1 和 shopping 的关系

shopping 模块处理：

```text
收藏
购物车
```

shopping 需要 catalog 提供：

```text
skuId 对应的商品名称、SKU 名称、价格、图片、分类
```

使用方式：

```text
shopping 调用 ProductQueryApi.requireSellableSku(skuId)
```

shopping 不直接读取 catalog 表。

### 8.2 和 trade 的关系

trade 模块处理：

```text
订单
订单项
支付模拟
库存扣减
```

trade 创建订单时需要：

```text
商品快照
价格快照
SKU 是否可售
```

使用方式：

```text
trade 调用 ProductQueryApi.requireSellableSku(skuId)
```

trade 负责库存和订单，不让 catalog 扣库存。

### 8.3 和 behavior 的关系

behavior 模块处理：

```text
搜索行为
浏览行为
收藏行为
加购行为
购买行为
```

catalog 提供行为触发场景：

```text
用户搜索商品
用户浏览商品详情
```

但 catalog 当前阶段不直接发布 behavior 事件。

catalog 阶段不实现行为落库，不实现 RabbitMQ 发布，不实现 Redis 热词统计，也不实现 Agent 推荐。

后续 behavior 模块确认 API 后，catalog 可以在搜索和浏览场景接入 behavior 事件发布。

### 8.4 和 V1.1 Agent 的关系

V1.1 Agent 会使用 catalog 数据理解商品语义。

例如：

```text
商品分类
商品名称
品牌
价格区间
标签
展示属性
```

Agent 可以结合 behavior 数据生成：

```text
用户兴趣分类
价格敏感度
近期购买意图
推荐理由
商品匹配解释
```

所以 catalog 的商品数据要尽量真实，不要全部使用 test/demo 名称。

## 9. 关键流程

### 9.1 商品列表流程

```text
前端请求 GET /api/catalog/products
    -> ProductController 接收 categoryId / keyword / page / size
    -> ProductService 查询上架 SPU
    -> 根据可售 SKU 计算价格区间
    -> 组装 ProductListItemVO
    -> 返回分页结果
```

说明：

```text
V1.0 可以使用 MyBatis-Plus 分页或简单 limit/offset。
keyword 使用 MySQL LIKE。
```

### 9.2 商品详情流程

```text
前端请求 GET /api/catalog/products/{spuId}
    -> ProductService 查询 SPU
    -> 校验商品存在且上架
    -> 查询分类
    -> 查询该 SPU 下可售 SKU
    -> 解析 attributes_json / detail_images_json / specs_json
    -> 组装 ProductDetailVO
    -> 返回前端
```

说明：

```text
后续阶段可以对商品详情加 Redis 缓存。
```

### 9.3 SKU 选择流程

```text
前端在商品详情页选择某个 SKU
    -> 前端得到 skuId
    -> 加入购物车时传 skuId 给 shopping
    -> 创建订单时 trade 根据 skuId 获取商品快照
```

说明：

```text
catalog 只负责告诉前端有哪些 SKU 可以选。
catalog 不负责购物车和订单。
```

### 9.4 ProductQueryApi 流程

```text
shopping/trade 调用 requireSellableSku(skuId)
    -> 查询 SKU
    -> 查询 SPU
    -> 查询 Category
    -> 校验三者状态
    -> 返回 ProductSnapshot
```

说明：

```text
ProductSnapshot 是跨模块快照，不是数据库 Entity。
```

## 10. Redis 缓存设计

V1.0 catalog 可以对商品详情做简单 Redis 缓存。

缓存 key：

```text
catalog:product:detail:{spuId}
```

缓存内容：

```text
ProductDetailVO JSON
```

建议过期时间：

```text
30 分钟到 2 小时均可
```

缓存策略：

```text
查询商品详情时先查 Redis
未命中再查 MySQL
查到后写入 Redis
商品上下架或商品信息变化时删除缓存
```

说明：

```text
Redis 缓存可以放到 catalog 后续阶段实现。
第一阶段不需要实现 Redis。
```

## 11. 安全规则

catalog 浏览接口允许匿名访问：

```text
GET /api/catalog/categories
GET /api/catalog/products
GET /api/catalog/products/{spuId}
GET /api/catalog/skus/{skuId}
```

需要在 SecurityConfig 中放行：

```text
/api/catalog/**
```

说明：

```text
浏览商品不要求登录。
收藏、购物车、下单等用户操作由后续模块要求登录。
```

## 12. 包结构建议

V1.0 catalog 只创建 `com.personaflow.commerce.product` 包。

`search` 包保留为后续复杂搜索扩展方向，V1.0 不创建。简单关键词搜索由 `ProductService` 使用 MySQL LIKE 实现。

```text
com.personaflow.commerce.product
├── controller
├── service
├── dto
├── vo
├── entity
├── mapper
└── api
    └── model
```

建议类：

```text
ProductCategoryEntity
ProductSpuEntity
ProductSkuEntity

ProductCategoryMapper
ProductSpuMapper
ProductSkuMapper

ProductController
ProductService

CategoryVO
ProductListItemVO
ProductDetailVO
SkuVO

ProductQueryApi
ProductSnapshot
```

## 13. Codex 实现阶段

catalog 实现拆成 5 个阶段，每个阶段都必须可以编译或测试。

### 阶段 1：Flyway 表、Entity、Mapper、初始化演示数据

实现：

```text
Flyway V2__create_catalog_tables.sql
product_category
product_spu
product_sku
Entity
Mapper
初始化分类、SPU、SKU 演示数据
```

不实现：

```text
Controller
Service
Redis
admin
shopping
trade
behavior
agent
```

验收：

```text
Flyway 版本号不冲突
至少 5 个分类
至少 20 个 SPU
每个 SPU 至少 1 个 SKU
.\mvnw.cmd -DskipTests compile 通过
```

### 阶段 2：分类、商品列表、商品详情、SKU 查询接口

实现：

```text
GET /api/catalog/categories
GET /api/catalog/products
GET /api/catalog/products/{spuId}
GET /api/catalog/skus/{skuId}
```

同时实现：

```text
ProductController
ProductService
相关 VO
简单分页
keyword LIKE 搜索
/api/catalog/** 匿名放行
```

不实现：

```text
Redis
admin
shopping
trade
behavior
agent
```

验收：

```text
分类能查
商品列表能查
keyword 能搜索
商品详情能返回 SKU
SKU 信息能查
未登录也能访问 catalog 接口
```

### 阶段 3：ProductQueryApi

实现：

```text
ProductQueryApi
ProductSnapshot
requireSellableSku
requireSellableSkus
```

不实现：

```text
shopping
trade
behavior
agent
```

验收：

```text
可售 SKU 返回商品快照
下架 SKU 报错
下架 SPU 报错
不存在 SKU 报错
不返回 Entity
不返回库存
```

### 阶段 4：商品详情 Redis 缓存

实现：

```text
商品详情 Redis 缓存
catalog:product:detail:{spuId}
缓存命中读取
缓存未命中查 MySQL
```

说明：

```text
该阶段可选。
如果时间紧，可以先跳过，后面再补。
```

### 阶段 5：文档同步和边界复查

实现：

```text
同步 catalog HTTP 接口清单
同步 ProductQueryApi 和 ProductSnapshot
检查包结构仍只使用 product
检查没有 search_record、behavior 事件落库、RabbitMQ 发布或 Agent 推荐实现
检查没有 admin、shopping、trade 业务代码越界
```

验收：

```text
文档与当前代码一致
compile/test 可按阶段要求运行
未解决问题明确列出
```

## 14. 验收标准

catalog 模块完成后，应满足：

```text
1. 可以初始化一批真实演示商品
2. 前端可以展示分类
3. 前端可以展示商品列表
4. 前端可以搜索商品
5. 前端可以查看商品详情和 SKU
6. shopping/trade 可以通过 ProductQueryApi 获取商品快照
7. catalog 不负责库存扣减
8. catalog 不负责购物车、订单、行为落库、RabbitMQ 发布和 Agent 推荐
9. compile 通过
10. 单元测试通过
```

## 15. 当前设计结论

catalog 是 V1.0 的商品目录模块。

它不需要复杂，只需要把商品展示所需的数据记录清楚，并提供稳定的查询能力。

V1.0 先使用 MySQL 实现简单查询和搜索，后续通过 Redis 缓存商品详情。

catalog 的核心价值是：

```text
给前端展示商品
给用户选择 SKU
给 shopping/trade 提供商品快照
给 behavior/agent 提供商品语义基础
```
