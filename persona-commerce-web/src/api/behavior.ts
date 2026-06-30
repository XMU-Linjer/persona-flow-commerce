import http from './http'

export interface BehaviorEvent {
  id?: number
  eventId: string
  userId?: number
  eventType: string
  sourceModule?: string
  objectType?: string
  objectId?: number
  keyword?: string
  skuId?: number
  spuId?: number
  categoryId?: number
  orderId?: number
  amount?: number
  payloadJson?: string
  occurredAt?: string
  createdAt?: string
}

export interface BehaviorTargetSummary {
  targetType?: string
  targetId?: number
  count: number
  lastOccurredAt?: string
}

export interface AgentDemandSignal {
  eventId?: string
  eventType?: string
  skuId?: number
  spuId?: number
  categoryId?: number
  orderId?: number
  amount?: number
  preferenceConfirmed?: boolean
  fulfilled?: boolean
  complementTrigger?: boolean
  repeatRecommendationSuppressed?: boolean
  occurredAt?: string
  demandState?: string
  scores?: Record<string, number>
  evidence?: AgentEvidence[]
}

export interface AgentEvidence {
  eventId?: string
  eventType?: string
  reason?: string
  occurredAt?: string
}

export interface AgentBehaviorEvent extends BehaviorEvent {}

export interface AgentProfileContext {
  userId: number
  recentEvents: AgentBehaviorEvent[]
  eventTypeCounts: Record<string, number>
  recentKeywords: string[]
  topCategories: BehaviorTargetSummary[]
  viewedProducts: BehaviorTargetSummary[]
  cartSignals: AgentDemandSignal[]
  orderSignals: AgentDemandSignal[]
  paidSignals: AgentDemandSignal[]
  canceledSignals: AgentDemandSignal[]
  fulfilledNeeds: AgentDemandSignal[]
  evidenceEventIds: string[]
  evidence: AgentEvidence[]
  generatedAt?: string
}

export interface BehaviorSummary {
  userId: number
  eventTypeCounts: Record<string, number>
  recentKeywords: string[]
  topCategories: BehaviorTargetSummary[]
  viewedProducts: BehaviorTargetSummary[]
  cartSignals: AgentDemandSignal[]
  demandSignals: AgentDemandSignal[]
  generatedAt?: string
}

export interface UserProfileLatest {
  userId: number
  exists: boolean
  profileVersionId?: number
  versionNo?: number
  profileJson?: string
  summary?: string
  sourceWorkflowId?: string
  createdAt?: string
}

export function listBehaviorEvents(params: { limit?: number; eventType?: string } = {}) {
  return http.get<BehaviorEvent[], BehaviorEvent[]>('/api/behavior/me/events', { params })
}

export function getBehaviorSummary() {
  return http.get<BehaviorSummary, BehaviorSummary>('/api/behavior/me/summary')
}

export function getAgentContext(params: { days?: number } = {}) {
  return http.get<AgentProfileContext, AgentProfileContext>('/api/behavior/me/agent-context', {
    params,
  })
}

export function getLatestProfile() {
  return http.get<UserProfileLatest, UserProfileLatest>('/api/behavior/me/profile/latest')
}

export function refreshProfile(params: { days?: number } = {}) {
  return http.post<UserProfileLatest, UserProfileLatest>('/api/behavior/me/profile/refresh', null, {
    params,
  })
}
