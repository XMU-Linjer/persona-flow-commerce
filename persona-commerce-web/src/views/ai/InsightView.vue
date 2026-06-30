<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Connection,
  DataAnalysis,
  DocumentChecked,
  Refresh,
  Warning,
} from '@element-plus/icons-vue'
import {
  getAgentContext,
  getBehaviorSummary,
  getLatestProfile,
  listBehaviorEvents,
  refreshProfile,
  type AgentDemandSignal,
  type AgentEvidence,
  type AgentProfileContext,
  type BehaviorEvent,
  type BehaviorSummary,
  type UserProfileLatest,
} from '@/api/behavior'

interface ProfileDocument {
  artifactId?: string
  workflowId?: string
  userId?: number
  artifactType?: string
  createdAt?: string
  confidence?: number
  evidenceEventIds?: string[]
  versionNo?: number
  summary?: string
  profile?: ProfilePayload
}

interface ProfilePayload {
  profileSummary?: string
  preferenceTags?: string[]
  preferredCategories?: string[]
  demandStates?: string[]
  fulfilledNeeds?: AgentDemandSignal[]
  complementOpportunities?: ComplementOpportunity[]
  doNotRecommend?: DoNotRecommend[]
  evidenceEventIds?: string[]
  auditResult?: string
}

interface ComplementOpportunity {
  label?: string
  relatedFulfilledSkuId?: number
  complementScore?: number
  evidence?: AgentEvidence[]
}

interface DoNotRecommend {
  skuId?: number
  spuId?: number
  reason?: string
}

interface ApiErrorLike {
  response?: {
    data?: {
      errorCode?: string
      message?: string
    }
  }
  message?: string
}

const loading = ref(false)
const refreshing = ref(false)
const errorText = ref('')
const latestProfile = ref<UserProfileLatest | null>(null)
const agentContext = ref<AgentProfileContext | null>(null)
const behaviorSummary = ref<BehaviorSummary | null>(null)
const recentEvents = ref<BehaviorEvent[]>([])

const profileDocument = computed<ProfileDocument | null>(() => {
  const raw = latestProfile.value?.profileJson
  if (!raw) return null

  try {
    return JSON.parse(raw) as ProfileDocument
  } catch {
    return null
  }
})

const profilePayload = computed(() => profileDocument.value?.profile ?? null)

const profileSummary = computed(() => {
  return (
    latestProfile.value?.summary ||
    profilePayload.value?.profileSummary ||
    profileDocument.value?.summary ||
    ''
  )
})

const demandStates = computed(() => {
  const states = profilePayload.value?.demandStates || []
  if (states.length > 0) return states

  if ((agentContext.value?.fulfilledNeeds || []).length > 0) {
    return ['FULFILLED']
  }
  if ((agentContext.value?.recentEvents || []).length > 0) {
    return ['DISCOVERING']
  }
  return []
})

const fulfilledNeeds = computed<AgentDemandSignal[]>(() => {
  return profilePayload.value?.fulfilledNeeds || agentContext.value?.fulfilledNeeds || []
})

const complementOpportunities = computed<ComplementOpportunity[]>(() => {
  return profilePayload.value?.complementOpportunities || []
})

const doNotRecommend = computed<DoNotRecommend[]>(() => {
  return profilePayload.value?.doNotRecommend || []
})

const evidenceIds = computed(() => {
  return (
    profilePayload.value?.evidenceEventIds ||
    profileDocument.value?.evidenceEventIds ||
    agentContext.value?.evidenceEventIds ||
    []
  )
})

const evidenceRows = computed<AgentEvidence[]>(() => {
  if ((agentContext.value?.evidence || []).length > 0) {
    return agentContext.value?.evidence || []
  }
  return evidenceIds.value.map((eventId) => ({
    eventId,
    eventType: undefined,
    reason: 'profile evidence',
  }))
})

const eventTypeCounts = computed(() => {
  return agentContext.value?.eventTypeCounts || behaviorSummary.value?.eventTypeCounts || {}
})

const eventCountRows = computed(() => {
  return Object.entries(eventTypeCounts.value)
    .map(([eventType, count]) => ({ eventType, count }))
    .sort((left, right) => right.count - left.count)
})

const recentKeywords = computed(() => {
  return agentContext.value?.recentKeywords || behaviorSummary.value?.recentKeywords || []
})

async function loadInsights() {
  loading.value = true
  errorText.value = ''
  try {
    const [latest, context, events, summary] = await Promise.all([
      getLatestProfile(),
      getAgentContext({ days: 30 }),
      listBehaviorEvents({ limit: 20 }),
      getBehaviorSummary(),
    ])
    latestProfile.value = latest
    agentContext.value = context
    recentEvents.value = events
    behaviorSummary.value = summary
  } catch (error) {
    errorText.value = resolveErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function refreshCurrentProfile() {
  refreshing.value = true
  errorText.value = ''
  try {
    latestProfile.value = await refreshProfile({ days: 30 })
    ElMessage.success('画像刷新成功')
    await loadInsights()
  } catch (error) {
    errorText.value = resolveErrorMessage(error)
  } finally {
    refreshing.value = false
  }
}

function resolveErrorMessage(error: unknown) {
  const apiError = error as ApiErrorLike
  return (
    apiError.response?.data?.errorCode ||
    apiError.response?.data?.message ||
    apiError.message ||
    '请求失败'
  )
}

function formatTime(value?: string) {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}

function formatMoney(value?: number) {
  if (value == null) return '-'
  return `¥${Number(value).toFixed(2)}`
}

function formatScore(value?: number) {
  if (value == null) return '-'
  return `${Math.round(Number(value) * 100)}%`
}

function demandStateType(state: string) {
  if (state === 'FULFILLED') return 'success'
  if (state === 'COOLING') return 'warning'
  if (state === 'DISCOVERING') return 'info'
  return 'primary'
}

function eventTypeType(eventType?: string) {
  if (eventType === 'PAYMENT_SUCCESS') return 'success'
  if (eventType === 'ORDER_CANCELED') return 'warning'
  if (eventType?.startsWith('CART')) return 'primary'
  if (eventType?.startsWith('FAVORITE')) return 'danger'
  return 'info'
}

onMounted(loadInsights)
</script>

<template>
  <section>
    <div class="page-title">
      <div>
        <h1>AI 购物洞察</h1>
        <p>从真实行为事件生成用户画像，重点呈现已满足需求、配套机会和行为证据。</p>
      </div>
      <div class="title-actions">
        <el-button :icon="Refresh" plain :loading="loading" @click="loadInsights">刷新数据</el-button>
        <el-button
          :icon="DataAnalysis"
          type="primary"
          :loading="refreshing"
          @click="refreshCurrentProfile"
        >
          刷新画像
        </el-button>
      </div>
    </div>

    <el-alert
      v-if="errorText"
      class="error-alert"
      :title="errorText"
      type="error"
      show-icon
      :closable="false"
    />

    <el-skeleton :loading="loading" animated>
      <template #template>
        <div class="insight-grid">
          <el-skeleton-item class="panel-skeleton" variant="rect" />
          <el-skeleton-item class="panel-skeleton" variant="rect" />
        </div>
      </template>

      <div class="insight-layout">
        <el-card class="summary-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>画像摘要</span>
              <el-tag v-if="latestProfile?.exists" type="success">已生成</el-tag>
              <el-tag v-else type="info">暂无画像</el-tag>
            </div>
          </template>

          <el-empty
            v-if="!latestProfile?.exists"
            description="暂无画像，请先浏览、加购或支付商品后刷新画像"
          >
            <el-button type="primary" :loading="refreshing" @click="refreshCurrentProfile">
              立即刷新画像
            </el-button>
          </el-empty>

          <template v-else>
            <p class="summary-text">{{ profileSummary || '暂无画像摘要' }}</p>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="画像版本">
                {{ latestProfile.versionNo || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="置信度">
                {{ formatScore(profileDocument?.confidence) }}
              </el-descriptions-item>
              <el-descriptions-item label="Workflow">
                {{ latestProfile.sourceWorkflowId || profileDocument?.workflowId || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="生成时间">
                {{ formatTime(latestProfile.createdAt || profileDocument?.createdAt) }}
              </el-descriptions-item>
            </el-descriptions>
          </template>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>需求状态</span>
              <el-icon><Connection /></el-icon>
            </div>
          </template>
          <div v-if="demandStates.length" class="tag-list">
            <el-tag
              v-for="state in demandStates"
              :key="state"
              size="large"
              :type="demandStateType(state)"
            >
              {{ state }}
            </el-tag>
          </div>
          <el-empty v-else description="暂无需求状态" />

          <div v-if="recentKeywords.length" class="keyword-block">
            <h3>最近搜索关键词</h3>
            <div class="tag-list">
              <el-tag v-for="keyword in recentKeywords" :key="keyword" effect="plain">
                {{ keyword }}
              </el-tag>
            </div>
          </div>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>已满足需求</span>
              <el-icon><DocumentChecked /></el-icon>
            </div>
          </template>

          <el-empty v-if="fulfilledNeeds.length === 0" description="暂无已满足需求" />
          <el-table v-else :data="fulfilledNeeds" size="small">
            <el-table-column label="SKU" prop="skuId" width="100" />
            <el-table-column label="SPU" prop="spuId" width="100" />
            <el-table-column label="类目" prop="categoryId" width="100" />
            <el-table-column label="金额" width="110">
              <template #default="{ row }: { row: AgentDemandSignal }">
                {{ formatMoney(row.amount) }}
              </template>
            </el-table-column>
            <el-table-column label="确认偏好" width="110">
              <template #default="{ row }: { row: AgentDemandSignal }">
                <el-tag :type="row.preferenceConfirmed ? 'success' : 'info'">
                  {{ row.preferenceConfirmed ? '是' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="配套触发" width="110">
              <template #default="{ row }: { row: AgentDemandSignal }">
                <el-tag :type="row.complementTrigger ? 'success' : 'info'">
                  {{ row.complementTrigger ? '是' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="重复推荐" min-width="120">
              <template #default="{ row }: { row: AgentDemandSignal }">
                <el-tag :type="row.repeatRecommendationSuppressed ? 'warning' : 'info'">
                  {{ row.repeatRecommendationSuppressed ? '已抑制' : '未抑制' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>配套机会</span>
              <el-icon><DataAnalysis /></el-icon>
            </div>
          </template>

          <el-empty v-if="complementOpportunities.length === 0" description="暂无配套机会" />
          <el-table v-else :data="complementOpportunities" size="small">
            <el-table-column label="机会" prop="label" min-width="160" />
            <el-table-column label="关联已满足 SKU" prop="relatedFulfilledSkuId" width="150" />
            <el-table-column label="配套分" width="100">
              <template #default="{ row }: { row: ComplementOpportunity }">
                {{ formatScore(row.complementScore) }}
              </template>
            </el-table-column>
            <el-table-column label="证据" min-width="180">
              <template #default="{ row }: { row: ComplementOpportunity }">
                <el-tag
                  v-for="item in row.evidence || []"
                  :key="item.eventId"
                  class="inline-tag"
                  effect="plain"
                  size="small"
                >
                  {{ item.eventType || item.eventId }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>不再推荐</span>
              <el-icon><Warning /></el-icon>
            </div>
          </template>

          <el-empty v-if="doNotRecommend.length === 0" description="暂无抑制项" />
          <el-table v-else :data="doNotRecommend" size="small">
            <el-table-column label="SKU" prop="skuId" width="120" />
            <el-table-column label="SPU" prop="spuId" width="120" />
            <el-table-column label="原因" prop="reason" min-width="220" />
          </el-table>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>行为摘要</span>
              <el-tag type="info">{{ recentEvents.length }} 条最近行为</el-tag>
            </div>
          </template>

          <el-empty v-if="eventCountRows.length === 0" description="暂无行为摘要" />
          <div v-else class="count-grid">
            <div v-for="item in eventCountRows" :key="item.eventType" class="count-item">
              <el-tag :type="eventTypeType(item.eventType)" effect="plain">
                {{ item.eventType }}
              </el-tag>
              <strong>{{ item.count }}</strong>
            </div>
          </div>
        </el-card>

        <el-card class="full-width" shadow="never">
          <template #header>
            <div class="card-header">
              <span>行为证据</span>
              <el-tag type="info">{{ evidenceIds.length }} 个 eventId</el-tag>
            </div>
          </template>

          <el-empty v-if="evidenceRows.length === 0" description="暂无行为证据" />
          <div v-else class="evidence-grid">
            <div v-for="item in evidenceRows" :key="item.eventId" class="evidence-item">
              <strong>{{ item.eventType || 'EVIDENCE' }}</strong>
              <span>{{ item.eventId }}</span>
              <small>{{ item.reason || 'profile evidence' }}</small>
            </div>
          </div>
        </el-card>

        <el-card class="full-width" shadow="never">
          <template #header>
            <div class="card-header">
              <span>最近行为</span>
              <el-tag type="info">最近 20 条</el-tag>
            </div>
          </template>

          <el-empty v-if="recentEvents.length === 0" description="暂无行为事件" />
          <el-table v-else :data="recentEvents" size="small">
            <el-table-column label="类型" width="170">
              <template #default="{ row }: { row: BehaviorEvent }">
                <el-tag :type="eventTypeType(row.eventType)" effect="plain">
                  {{ row.eventType }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="来源" prop="sourceModule" width="110" />
            <el-table-column label="关键词" prop="keyword" min-width="120" />
            <el-table-column label="SKU" prop="skuId" width="100" />
            <el-table-column label="SPU" prop="spuId" width="100" />
            <el-table-column label="订单" prop="orderId" width="100" />
            <el-table-column label="金额" width="110">
              <template #default="{ row }: { row: BehaviorEvent }">
                {{ formatMoney(row.amount) }}
              </template>
            </el-table-column>
            <el-table-column label="发生时间" min-width="170">
              <template #default="{ row }: { row: BehaviorEvent }">
                {{ formatTime(row.occurredAt) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>
    </el-skeleton>
  </section>
</template>

<style scoped>
.title-actions,
.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.card-header {
  justify-content: space-between;
  color: #1f2a37;
  font-weight: 700;
}

.error-alert {
  margin-bottom: 16px;
}

.insight-layout,
.insight-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.8fr);
  gap: 16px;
}

.summary-card :deep(.el-card__body) {
  min-height: 220px;
}

.summary-text {
  margin: 0 0 16px;
  color: #1f2a37;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.7;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.keyword-block {
  margin-top: 18px;
}

.keyword-block h3 {
  margin: 0 0 10px;
  color: #53606f;
  font-size: 14px;
}

.inline-tag {
  margin: 0 6px 6px 0;
}

.count-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px;
}

.count-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid #edf1f5;
  border-radius: 8px;
  background: #fbfcfd;
}

.count-item strong {
  color: #1f2a37;
  font-size: 18px;
}

.full-width {
  grid-column: 1 / -1;
}

.evidence-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 10px;
}

.evidence-item {
  display: grid;
  gap: 4px;
  padding: 12px;
  border: 1px solid #edf1f5;
  border-radius: 8px;
  background: #fbfcfd;
}

.evidence-item strong {
  color: #1f8f75;
  font-size: 13px;
}

.evidence-item span,
.evidence-item small {
  overflow-wrap: anywhere;
}

.evidence-item span {
  color: #1f2a37;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}

.evidence-item small {
  color: #6b7280;
}

.panel-skeleton {
  height: 320px;
  border-radius: 8px;
}

@media (max-width: 980px) {
  .insight-layout,
  .insight-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .title-actions {
    justify-content: flex-start;
  }
}
</style>
