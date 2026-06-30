# 02-async-profile-agent-workflow 异步 Profile Agent 工作流

> 状态：planned  
> 所属版本：V1.2.0  
> 范围：Python Profile Agent Team 的异步 workflow、Agent 角色、Artifact 与 Critic 审核设计  
> 更新时间：2026-06-30

## 1. 定位

V1.2 计划把 V1.1 的同步规则版 Profile Agent Team 升级为异步工作流。升级目标不是接入真实 LLM，也不是实现复杂推荐算法，而是让画像生成过程具备 workflow 状态、Agent task 留痕、Artifact 证据和 Critic 审核记录。

当前 V1.1 链路：

```text
Java HTTP 调 Python /agent/profile/build
-> Python 同步执行 Profile Agent Team
-> 返回画像结果
-> Java 保存 user_profile_version
```

V1.2 计划链路：

```text
Java 创建 workflow
-> Agent Bus 投递 task
-> Python Profile Manager 编排多个规则 Agent
-> Agent 逐步产出 Artifact
-> Critic 审核画像草案
-> Java 保存 task / artifact / profile version
```

## 2. Agent 角色

### 2.1 PROFILE_MANAGER

职责：

- 接收 `agent.task.assigned`；
- 初始化 workflow；
- 调度 Behavior / Intent / Trend / Critic；
- 汇总 task 状态；
- 发布 workflow completed / failed。

不负责：

- 直接查询 Java 业务数据库；
- 写入 Java MySQL；
- 修改订单、库存、购物车或支付状态。

### 2.2 BEHAVIOR_AGENT

职责：

- 整理 `AgentProfileContext.recentEvents`；
- 统计行为类型；
- 提取关键 eventId；
- 输出 `BehaviorFactReport`。

要求：

- 只描述可由行为事件证明的事实；
- 不做无证据推断；
- 不生成自然语言推荐结论。

### 2.3 INTENT_AGENT

职责：

- 识别探索、比较、已满足、冷却、互补就绪等需求状态；
- 从 `PAYMENT_SUCCESS` 提取已满足需求；
- 输出 `IntentReport`。

固定语义：

```text
PAYMENT_SUCCESS = 偏好确认 + 当前需求满足 + 互补需求触发
```

### 2.4 TREND_AGENT

职责：

- 汇总品类、SKU、SPU 趋势；
- 区分短期意图和长期偏好；
- 避免把一次成交误判为继续推荐同一商品；
- 输出 `TrendReport`。

### 2.5 PROFILE_BUILDER_CRITIC

职责：

- 合成 `ProfileDraft`；
- 审核画像草案；
- 生成 `ProfileAuditReport`；
- 检查 `PAYMENT_SUCCESS` 是否被正确处理；
- 审核通过后输出可保存的 `UserProfileVersion`。

Critic 不负责：

- 接入真实 LLM；
- 改写业务数据；
- 触发订单、购物车或支付动作。

## 3. Artifact 类型

| Artifact | 来源 | 说明 |
|---|---|---|
| `BehaviorFactReport` | BEHAVIOR_AGENT | 行为事实、事件数量、关键 evidence |
| `IntentReport` | INTENT_AGENT | 需求阶段、fulfilledNeeds、complementTrigger |
| `TrendReport` | TREND_AGENT | 品类、SKU、SPU 趋势 |
| `ProfileDraft` | PROFILE_BUILDER_CRITIC | 待审核画像草稿 |
| `ProfileAuditReport` | PROFILE_BUILDER_CRITIC | 审核结论、问题、修订建议 |
| `UserProfileVersion` | PROFILE_BUILDER_CRITIC | 审核通过后可保存画像版本 |

Artifact 原则：

- 结构化 JSON；
- 保存 evidence eventId；
- 只追加，不覆盖；
- 不保存密码、JWT、完整地址、收货人手机号；
- 不保存无关自然语言 prompt。

## 4. Workflow 状态

```text
PENDING
RUNNING
SUCCEEDED
FAILED
CANCELED
```

状态流转建议：

```text
PENDING -> RUNNING -> SUCCEEDED
PENDING -> RUNNING -> FAILED
PENDING -> CANCELED
RUNNING -> CANCELED
```

V1.2 可以先不实现用户主动取消，`CANCELED` 作为预留状态。

## 5. Task 状态

```text
PENDING
RUNNING
SUCCEEDED
FAILED
SKIPPED
```

每个 Agent task 至少记录：

- `taskId`；
- `workflowId`；
- `agentRole`；
- `status`；
- `inputArtifactIds`；
- `outputArtifactIds`；
- `errorMessage`；
- `startedAt`；
- `completedAt`。

## 6. 工作流草案

```text
1. Java 创建 agent_workflow，状态 PENDING。
2. Java 构建 AgentProfileContext。
3. Java 发布 agent.task.assigned 给 PROFILE_MANAGER。
4. PROFILE_MANAGER 创建 / 推进 Agent task。
5. BEHAVIOR_AGENT 输出 BehaviorFactReport。
6. INTENT_AGENT 输出 IntentReport。
7. TREND_AGENT 输出 TrendReport。
8. PROFILE_BUILDER_CRITIC 生成 ProfileDraft。
9. PROFILE_BUILDER_CRITIC 生成 ProfileAuditReport。
10. 审核通过后生成 UserProfileVersion artifact。
11. Python 发布 agent.workflow.completed。
12. Java 保存 user_profile_version，workflow 标记 SUCCEEDED。
```

失败路径：

```text
Agent task 失败
-> 发布 agent.task.failed
-> Java 记录失败 task
-> workflow 标记 FAILED
-> 前端展示失败原因
```

## 7. Critic 审核规则

Critic 必须检查：

- `PAYMENT_SUCCESS` 是否被标记为 fulfilled；
- 已成交 SKU / SPU 是否进入 `doNotRecommend`；
- `complementOpportunities` 是否存在 evidence；
- 是否错误地继续推荐已满足需求；
- 是否存在无 eventId 支撑的强推断；
- 画像 summary 是否夸大用户意图。

审核失败时可以产生：

```text
agent.challenge.raised
agent.revision.requested
agent.revision.completed
```

V1.2 初版可以先实现规则版审核，不接真实 LLM。

## 8. PAYMENT_SUCCESS 处理

当出现 `PAYMENT_SUCCESS`：

- 进入 `fulfilledNeeds`；
- 进入短期 `doNotRecommend`；
- 作为 `complementOpportunities` 的证据；
- `preferenceConfirmed = true`；
- `fulfilled = true`；
- `complementTrigger = true`。

禁止把 `PAYMENT_SUCCESS` 简化为继续推荐当前 SKU / SPU 的正反馈。

## 9. Java / Python 边界

Java 负责：

- 当前用户身份；
- workflow 权限隔离；
- 构建 `AgentProfileContext`；
- 创建 workflow；
- 保存 task / artifact；
- 保存 `user_profile_version`；
- 给前端提供查询接口。

Python 负责：

- 执行规则版 Agent workflow；
- 产出 Artifact；
- 发布 workflow / task 状态消息。

Python 不允许：

- 直接查 Java 业务数据库；
- 绕过 Java 权限；
- 修改订单、库存、购物车、支付状态；
- 自行保存画像版本到 MySQL。

## 10. 前端展示方向

前端 `/ai-insights` 计划展示：

- workflow 列表；
- workflow 当前状态；
- task 步骤；
- Artifact 列表；
- Critic 审核结果；
- 失败原因；
- 最新 `user_profile_version`。

页面重点是解释画像如何生成，而不是直接做“猜你喜欢”。

## 11. 非目标

V1.2 不实现：

- 真实 LLM；
- OpenAI / Claude；
- LangChain；
- RAG；
- 向量数据库；
- 真实推荐算法；
- Agent 自动下单；
- Agent 修改业务状态；
- 生产级异步任务治理；
- Outbox 完整可靠消息。

## 12. 与 V1.1 文档关系

V1.1 文档见 [Profile Agent Team 画像团队](01-profile-agent-team.md)。V1.1 是规则版同步工作流，V1.2 计划升级为异步 workflow 与 Artifact 留痕。

