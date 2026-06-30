# Profile Agent Team 画像团队

> 状态：completed as rule-based workflow  
> 范围：Python `persona-agent-service` 的规则版画像工作流、结构化 Artifact 和画像语义  
> 更新时间：2026-06-30

## 1. 定位

Profile Agent Team 是 V1.1 的 Python 画像服务。它接收 Java behavior 模块准备好的 `AgentProfileContext`，生成可解释、可版本化的用户画像结果。

当前实现是规则版工作流，不接真实 LLM，不接 OpenAI / Claude，不使用 LangChain，不做 RAG，也不实现真实推荐算法。

## 2. 已完成接口

```http
GET  /health
POST /agent/profile/build
```

`GET /health` 返回：

```json
{
  "status": "UP",
  "service": "persona-agent-service"
}
```

`POST /agent/profile/build` 接收 Java 提供的结构化上下文，并返回画像结果、workflowId、summary、fulfilledNeeds、complementOpportunities、doNotRecommend 和 evidence。

## 3. 工作流

```text
Java BehaviorContextService
-> AgentProfileContext
-> POST /agent/profile/build
-> Profile Manager
-> Behavior Agent
-> Intent Agent
-> Trend Agent
-> Profile Builder/Critic
-> AgentProfileBuildResponse
-> Java user_profile_version
```

Python Agent 不直接查询 Java 业务数据库，也不直接访问 account、catalog、shopping、trade 表。

## 4. Agent 角色

### 4.1 Profile Manager

职责：

- 接收画像构建请求；
- 调度规则版 Agent；
- 汇总结构化 Artifact；
- 生成 workflowId；
- 返回最终画像结果。

不负责：

- 写入数据库；
- 修改业务状态；
- 执行支付、下单或购物车操作。

### 4.2 Behavior Agent

职责：

- 整理最近行为事实；
- 统计行为类型；
- 保留关键 eventId 作为 evidence；
- 输出 `BehaviorFactReport`。

不做主观推荐，也不推断无法由事件支持的结论。

### 4.3 Intent Agent

职责：

- 识别短期需求状态；
- 识别已满足需求；
- 从 `PAYMENT_SUCCESS` 中提取偏好确认和互补触发信号；
- 输出 `IntentReport`。

`PAYMENT_SUCCESS` 的含义固定为：

```text
preferenceConfirmed = true
fulfilled = true
complementTrigger = true
```

### 4.4 Trend Agent

职责：

- 汇总较稳定的品类、SKU、SPU 行为趋势；
- 区分长期偏好和短期意图；
- 避免把一次性购买误判为持续推荐需求；
- 输出 `TrendReport`。

### 4.5 Profile Builder/Critic

职责：

- 合成画像摘要；
- 生成 fulfilledNeeds；
- 生成 complementOpportunities；
- 生成 doNotRecommend；
- 保留 evidence；
- 输出最终画像版本。

## 5. 结构化 Artifact

当前规则版工作流使用结构化数据，不传递随意自然语言结论。

| Artifact | 来源 | 说明 |
|---|---|---|
| `BehaviorFactReport` | Behavior Agent | 确定性行为事实 |
| `IntentReport` | Intent Agent | 需求状态、已满足需求、互补触发 |
| `TrendReport` | Trend Agent | 长期偏好和趋势 |
| `ProfileDraft` | Profile Builder/Critic | 画像草稿 |
| `ProfileAuditReport` | Profile Builder/Critic | 自检结果 |
| `UserProfileVersion` | Profile Builder/Critic | 可保存画像版本 |

## 6. 需求状态

当前规则版画像使用以下需求状态：

- `EXPLORING`：搜索、浏览为主，仍在探索；
- `CONSIDERING`：收藏、加购增强，进入比较；
- `FULFILLED`：支付成功，当前具体需求满足；
- `COOLING`：成交后短期冷却，不重复推同一 SKU / SPU；
- `COMPLEMENT_READY`：存在配套需求机会；
- `CHURN_RISK`：出现取消或长时间无进一步动作。

## 7. PAYMENT_SUCCESS 处理

成交事件不是“继续推荐当前商品”的单向正反馈。

当出现 `PAYMENT_SUCCESS`：

- 该 SKU / SPU 进入 `fulfilledNeeds`；
- `doNotRecommend` 加入该 SKU / SPU 的短期重复推荐抑制；
- `complementOpportunities` 生成配套机会；
- evidence 中保留对应 eventId；
- summary 中体现“当前需求已满足，建议探索互补需求”。

示例语义：

```json
{
  "fulfilledNeeds": [
    {
      "skuId": 30009,
      "spuId": 20005,
      "preferenceConfirmed": true,
      "fulfilled": true,
      "complementTrigger": true
    }
  ],
  "doNotRecommend": [
    {
      "skuId": 30009,
      "reason": "fulfilled_need_repeat_suppressed"
    }
  ],
  "complementOpportunities": [
    {
      "opportunity": "explore adjacent categories",
      "evidence": ["PAYMENT_SUCCESS"]
    }
  ]
}
```

## 8. Java / Python 边界

Java 负责：

- 用户认证；
- 当前用户身份；
- 行为数据查询；
- AgentProfileContext 生成；
- 调用 Python；
- 校验返回结果；
- 保存 `user_profile_version`；
- 给 Vue 前端提供查询接口。

Python 负责：

- 接收结构化上下文；
- 执行规则版画像工作流；
- 返回结构化画像结果。

Python 禁止：

- 直接查询业务数据库；
- 直接修改订单、库存、购物车；
- 绕过 Java 权限；
- 保存画像版本到 MySQL。

## 9. Agent 消息总线边界

Python 服务保留 Agent 消息协议和 `commerce.agent.exchange` 拓扑命名，用于后续异步工作流扩展。

当前完成态：

- 真实画像刷新链路使用 HTTP；
- 不把 `commerce.agent.exchange` 作为页面刷新主链路；
- 不实现生产级异步 Agent 调度；
- 不实现 Outbox。

## 10. 错误处理

Python Agent 不可用时：

- Java 返回 `AGENT_SERVICE_UNAVAILABLE`；
- Vue AI 购物洞察页面显示错误提示；
- 页面不白屏；
- 电商主链路不受影响。

Agent 返回非法画像时：

- Java 返回 `AGENT_PROFILE_BUILD_FAILED`；
- 不保存空画像或错误画像。

## 11. 测试结果

当前已验证：

- Python Agent：21 pytest passed；
- Java 调 Python 真实链路通过；
- `/agent/profile/build` 能返回 workflowId、summary、fulfilledNeeds、complementOpportunities、doNotRecommend；
- Java 能保存 `user_profile_version`；
- `/api/behavior/me/profile/latest` 能查到最新画像。

## 12. 未实现

当前没有实现：

- 真实 LLM；
- OpenAI / Claude 接入；
- LangChain；
- RAG；
- 向量数据库；
- 真实推荐算法；
- 生产级多 Agent 异步调度；
- Agent 自动下单；
- Agent 修改业务状态。

## 13. 结论

Profile Agent Team 当前是规则版、可解释、可测试的画像工作流。它的价值在于把电商行为事件转化为结构化需求状态和画像版本，而不是冒充已经完成真实大模型推荐系统。
