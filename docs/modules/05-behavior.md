# 05-behavior 行为事件模块设计

> 状态：V1.1 设计中  
> 所属版本：V1.1.0  
> 范围：Java 后端 behavior 模块，不包含完整 Agent Team 实现  
> 更新时间：2026-06-29

## 1. 模块定位

behavior 是 V1.1 新增的 Java 后端模块，负责把 V1.0 主链路产生的用户行为事实沉淀为结构化数据，并为后续 Profile Agent Team 准备画像上下文。

behavior 不负责解释、推理和推荐决策本身。Agent 推理由 Python Profile Agent Team 完成。

## 2. V1.1 目标

behavior 模块负责：

- 行为事件模型；
- 行为事件落库；
- 用户行为查询；
- 用户画像版本保存；
- 给 Agent 准备结构化上下文；
- 暴露前端 AI 购物洞察所需的画像查询能力。

## 3. 不做什么

behavior 模块不负责：

- Agent task scheduling；
- Agent 内部消息协议；
- prompt 细节；
- 推荐决策实验；
- 营销自动化；
- 直接修改 catalog / shopping / trade 业务状态；
- 购物车清理；
- 库存扣减；
- 订单状态变更；
- admin 管理后台。

## 4. 核心概念

| 概念 | 说明 |
|---|---|
| BehaviorEvent | 用户行为事件事实 |
| Event Publisher | catalog / shopping / trade 中的事件发布点 |
| Behavior Consumer | 从 RabbitMQ 消费行为事件并落库 |
| User Behavior Context | 给 Agent 的结构化行为上下文 |
| UserProfileVersion | Agent 生成并保存的用户画像版本 |

## 5. 行为类型设计

V1.1 行为类型固定为：

- `PRODUCT_VIEW`
- `PRODUCT_SEARCH`
- `FAVORITE_ADD`
- `FAVORITE_REMOVE`
- `CART_ADD`
- `CART_REMOVE`
- `CART_CLEAR`
- `ORDER_CREATED`
- `PAYMENT_SUCCESS`
- `ORDER_CANCELED`

### PAYMENT_SUCCESS 语义

`PAYMENT_SUCCESS` 不意味着继续推荐当前产品。

它表示：

- 偏好确认；
- 当前具体需求满足；
- 互补需求触发。

成交后不应继续推同一 SKU / SPU，而应识别互补商品、耗材、配件、复购周期或下一阶段需求。

## 6. 数据表设计

### 6.1 behavior_event

建议字段：

| 字段 | 说明 |
|---|---|
| id | 主键 |
| event_id | 全局唯一事件 ID |
| user_id | 用户 ID |
| event_type | 行为类型 |
| source_module | 来源模块 |
| object_type | 行为对象类型，如 SKU、SPU、ORDER、KEYWORD |
| object_id | 行为对象 ID |
| keyword | 搜索关键词，可空 |
| sku_id | SKU ID，可空 |
| spu_id | SPU ID，可空 |
| category_id | 分类 ID，可空 |
| order_id | 订单 ID，可空 |
| amount | 金额，可空 |
| payload_json | 扩展字段 JSON |
| occurred_at | 业务发生时间 |
| created_at | 落库时间 |

约束：

- `event_id` 唯一；
- 按 `user_id + occurred_at` 建索引；
- 按 `event_type + occurred_at` 建索引。

### 6.2 behavior_consume_log

用于 RabbitMQ 消费幂等。

建议字段：

| 字段 | 说明 |
|---|---|
| id | 主键 |
| message_id | 消息 ID |
| event_id | 行为事件 ID |
| status | 消费状态 |
| retry_count | 重试次数 |
| error_message | 错误信息 |
| created_at | 创建时间 |
| updated_at | 更新时间 |

### 6.3 user_profile_version

用于保存 Agent 生成的画像版本。

建议字段：

| 字段 | 说明 |
|---|---|
| id | 主键 |
| user_id | 用户 ID |
| version_no | 版本号 |
| profile_json | 结构化画像 JSON |
| summary | 面向前端的摘要 |
| source_workflow_id | Agent 工作流 ID |
| created_at | 创建时间 |

## 7. HTTP 接口设计

V1.1 初步规划：

```http
GET  /api/behavior/me/events
GET  /api/behavior/me/profile/latest
POST /api/behavior/me/profile/refresh
GET  /api/behavior/me/insights
```

说明：

- 所有 `/api/behavior/**` 默认需要登录；
- 只查询当前用户自己的行为和画像；
- admin 行为查询不在 V1.1 当前阶段实现；
- 精确 DTO / VO 在 behavior 实现阶段确认。

## 8. 跨模块依赖

behavior 作为消费方接收事件，不反向控制业务模块。

允许：

- 接收 catalog / shopping / trade 发布的行为事件；
- 为 Agent 准备行为上下文；
- 保存 Agent 生成的用户画像版本。

禁止：

- 直接修改商品、购物车、订单、库存、支付状态；
- 调用 catalog / shopping / trade 的 Mapper；
- 导入其他业务模块 Entity；
- 失败时回滚主业务。

## 9. 行为事件发布规则

catalog 发布：

- 商品详情浏览成功后发布 `PRODUCT_VIEW`；
- 商品关键词搜索成功后发布 `PRODUCT_SEARCH`。

shopping 发布：

- 收藏成功后发布 `FAVORITE_ADD`；
- 取消收藏成功后发布 `FAVORITE_REMOVE`；
- 加购成功后发布 `CART_ADD`；
- 删除购物车项成功后发布 `CART_REMOVE`；
- 清空购物车成功后发布 `CART_CLEAR`。

trade 发布：

- 订单创建成功后发布 `ORDER_CREATED`；
- 模拟支付成功后发布 `PAYMENT_SUCCESS`；
- 订单取消成功后发布 `ORDER_CANCELED`。

事件发布失败不影响主业务成功结果。

## 10. 用户画像数据准备

behavior 给 Agent 的上下文应是结构化数据，建议包括：

- 最近搜索关键词；
- 最近浏览 SKU / SPU；
- 收藏和取消收藏；
- 购物车新增、删除、清空；
- 订单创建、取消、支付成功；
- 商品分类、商品名称、SKU 名、价格；
- 行为发生时间；
- 当前需求是否可能已经满足。

behavior 不拼 prompt，不直接调用大模型，不决定最终推荐。

## 11. 错误码设计

建议新增：

| 错误码 | HTTP | 说明 |
|---|---:|---|
| `BEHAVIOR_EVENT_INVALID` | 400 | 行为事件格式非法 |
| `BEHAVIOR_EVENT_DUPLICATE` | 409 | 行为事件重复 |
| `BEHAVIOR_PROFILE_NOT_FOUND` | 404 | 用户画像不存在 |
| `BEHAVIOR_PROFILE_TASK_FAILED` | 409 | 画像任务失败 |

错误响应仍沿用 `ApiResponse.code` 为 int、`errorCode` 为字符串业务错误码的结构。

## 12. 测试要求

测试应覆盖：

- 行为事件落库成功；
- 重复 `event_id` 幂等；
- 消费失败进入重试或死信；
- 查询当前用户行为；
- 查询用户画像版本；
- 事件发布失败不影响主业务；
- behavior 不调用 catalog / shopping / trade Mapper；
- `PAYMENT_SUCCESS` 上下文标记为偏好确认、需求满足、互补触发。

## 13. Codex 实现阶段

### 阶段 1：表、Entity、Mapper

新增行为事件、消费幂等、画像版本相关表和基础 Mapper。

### 阶段 2：RabbitMQ 行为事件消费落库

新增 `commerce.behavior.exchange`、队列、消费者、ACK、重试、死信和幂等。

### 阶段 3：业务模块事件发布接入

catalog / shopping / trade 在业务成功后发布行为事件。发布失败不回滚主业务。

### 阶段 4：行为查询与画像上下文

提供当前用户行为查询和 Agent 上下文准备能力。

### 阶段 5：画像版本保存和前端查询

保存 Agent 结果，提供前端 AI 洞察查询接口。

## 14. 验收标准

- 行为事件能从 RabbitMQ 消费并落库；
- 重复消息不会重复落库；
- 死信队列可接收持续失败消息；
- 业务事件发布失败不影响主业务；
- 用户可以查询自己的行为摘要和画像结果；
- behavior 不实现 Agent 调度和推理；
- behavior 不修改商品、购物车、订单或库存。

## 15. 当前设计结论

V1.1 behavior 模块是 Java 后端的行为数据沉淀层。它连接 V1.0 业务事实和 Python Profile Agent Team，但不承担 Agent 内部协作、prompt、推荐实验或业务状态修改职责。
