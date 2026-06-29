# Profile Agent Team 画像团队设计

> 状态：V1.1 设计中  
> 范围：Python Agent 服务的角色、Artifact、工作流和画像语义  
> 更新时间：2026-06-29

## 1. 设计目标

Profile Agent Team 基于 V1.0 和 V1.1 behavior 数据，为用户生成可解释、可版本化的购物画像。

目标不是“直接替用户下单”，而是生成结构化用户画像、需求状态和 AI 购物洞察，供前端展示和后续推荐能力使用。

## 2. 总体工作流

```text
Java behavior 模块准备结构化上下文
-> Profile Manager 创建 workflowId 和任务 DAG
-> Behavior Agent 生成 BehaviorFactReport
-> Intent Agent 生成 IntentReport
-> Trend Agent 生成 TrendReport
-> Profile Builder/Critic 生成、审查并必要时返工 ProfileDraft
-> 保存 UserProfileVersion
```

Agent 之间交换结构化 Artifact，不交换任意自然语言闲聊式结论。

## 3. Agent 角色

### 3.1 Profile Manager

职责：

- 接收画像更新任务；
- 创建 `workflowId`；
- 创建任务 DAG；
- 调度 Behavior Agent、Intent Agent、Trend Agent 和 Profile Builder/Critic；
- 处理任务超时、失败和返工；
- 汇总工作流状态；
- 发布 `WORKFLOW_COMPLETED` 或失败状态。

Profile Manager 不直接生成最终画像，不直接修改业务数据。

### 3.2 Behavior Agent

职责：

- 读取 Java 后端提供的结构化行为上下文；
- 抽取确定性的行为事实；
- 识别最近浏览、搜索、收藏、加购、下单、支付和取消行为；
- 输出 `BehaviorFactReport`。

Behavior Agent 不做主观推荐，不推断长期偏好。

### 3.3 Intent Agent

职责：

- 判断短期购买意图；
- 判断需求状态；
- 识别购买阶段；
- 识别成交后的互补需求；
- 输出 `IntentReport`。

`PAYMENT_SUCCESS` 对 Intent Agent 的含义是：偏好确认、当前具体需求满足、互补需求触发。

### 3.4 Trend Agent

职责：

- 分析长期稳定兴趣；
- 发现上升兴趣；
- 识别突发短期意图；
- 识别正在减弱的兴趣；
- 过滤噪声；
- 输出 `TrendReport`。

Trend Agent 需要区分长期偏好和短期意图，避免把一次性购买误判为持续推荐需求。

### 3.5 Profile Builder/Critic

职责：

- 基于 BehaviorFactReport、IntentReport、TrendReport 生成 `ProfileDraft`；
- 自检画像是否互相矛盾；
- 对明显错误提出 `ProfileAuditReport`；
- 如需返工，最多触发一次 revision；
- 生成最终 `UserProfileVersion`；
- 请求 Java 后端保存画像版本。

Profile Builder/Critic 不直接访问业务数据库。

## 4. 结构化 Artifact

Agent 之间必须交换结构化 Artifact。

| Artifact | 产生者 | 说明 |
|---|---|---|
| `BehaviorFactReport` | Behavior Agent | 确定性行为事实 |
| `IntentReport` | Intent Agent | 短期意图、需求状态、互补机会 |
| `TrendReport` | Trend Agent | 长期偏好、趋势、噪声 |
| `ProfileDraft` | Profile Builder/Critic | 待审查画像草稿 |
| `ProfileAuditReport` | Profile Builder/Critic | 自检和质疑报告 |
| `UserProfileVersion` | Profile Builder/Critic | 最终可保存画像版本 |

Artifact 应包含：

- `artifactId`
- `workflowId`
- `userId`
- `artifactType`
- `createdAt`
- `confidence`
- `evidence`
- `payload`

## 5. 需求状态

V1.1 画像使用以下需求状态：

- `DISCOVERING`：正在探索，还没有明确目标；
- `INTERESTED`：表现出兴趣，但购买意图较弱；
- `INTENT`：存在较明确购买意图；
- `FULFILLED`：当前具体需求已经被购买满足；
- `COOLING`：兴趣正在降温；
- `RENEWAL`：可能进入复购、补货或升级周期。

## 6. 四类评分

| score | 含义 |
|---|---|
| `preferenceScore` | 长期偏好强度 |
| `intentScore` | 当前购买意图强度 |
| `fulfilledScore` | 需求满足程度 |
| `complementScore` | 互补推荐机会 |

评分必须结合证据来源，不应只因为一次浏览或一次购买就给出极端结论。

## 7. 成交后的推荐语义

成交后不继续推荐当前 SKU / SPU。

`PAYMENT_SUCCESS` 同时意味着：

- 该类商品偏好被确认；
- 当前具体需求大概率已经满足；
- 可以触发互补推荐机会。

示例：

- 用户购买机械键盘后，不应继续推荐同一键盘；
- 可以识别键帽、腕托、鼠标、清洁工具等互补机会；
- 长期偏好可记录为“电脑外设”或“办公效率设备”。

## 8. UserProfileVersion 建议结构

```json
{
  "userId": 1,
  "versionNo": 3,
  "summary": "用户近期关注电脑外设，已购买键盘，短期更适合推荐配件和桌面整理用品。",
  "preferenceTags": [
    {
      "tag": "电脑外设",
      "preferenceScore": 0.82,
      "evidence": ["PAYMENT_SUCCESS", "PRODUCT_VIEW", "CART_ADD"]
    }
  ],
  "currentIntents": [
    {
      "category": "键盘配件",
      "intentScore": 0.64,
      "demandState": "INTENT",
      "complementScore": 0.78
    }
  ],
  "fulfilledNeeds": [
    {
      "skuId": 1001,
      "demandState": "FULFILLED",
      "fulfilledScore": 0.91
    }
  ]
}
```

## 9. RabbitMQ 协作

Profile Agent Team 使用 `commerce.agent.exchange`。

主要消息：

- `agent.task.assigned`
- `agent.artifact.created`
- `agent.challenge.raised`
- `agent.revision.requested`
- `agent.revision.completed`
- `agent.task.completed`
- `agent.task.failed`
- `agent.workflow.completed`

Profile Manager 是工作流协调者。其他 Agent 不直接互相调用服务端内部方法，而是通过消息和 Artifact 协作。

## 10. Java 后端边界

Java 后端负责：

- 用户身份和权限；
- 准备结构化行为上下文；
- 提交画像任务；
- 保存画像版本；
- 给前端提供画像查询接口。

Agent 服务禁止：

- 直接访问 account / catalog / shopping / trade 数据库表；
- 绕过 Java 后端权限控制；
- 直接创建订单、取消订单或支付；
- 修改购物车、库存、商品状态或用户资料。

## 11. 测试要求

后续实现时需要覆盖：

- Profile Manager 创建 workflowId 和任务 DAG；
- Behavior Agent 输出确定性行为事实；
- Intent Agent 正确处理 `PAYMENT_SUCCESS`；
- Trend Agent 区分长期偏好和短期意图；
- Profile Builder/Critic 触发一次返工；
- Artifact 使用结构化 JSON；
- 最终生成 `UserProfileVersion`；
- Agent 失败不会影响 V1.0 电商主链路。

## 12. 当前设计结论

Profile Agent Team 是 V1.1 的 Python 画像团队。它基于 Java 后端提供的结构化上下文工作，通过 RabbitMQ Agent 任务总线协作，输出结构化 Artifact 和可版本化用户画像。
