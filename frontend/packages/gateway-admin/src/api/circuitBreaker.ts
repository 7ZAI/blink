import request from '@/utils/request'

// ==================== 类型定义 ====================

/**
 * 熔断器状态
 */
export interface CircuitBreakerStatus {
  name: string
  state: string // CLOSED/OPEN/HALF_OPEN
  failureRate: number
  slowCallRate: number
  numberOfCalls: number
  numberOfFailedCalls: number
  numberOfSlowCalls: number
  numberOfSuccessfulCalls: number
  stateTransitionTime?: number
  instanceId: string
  timestamp: number
}

/**
 * 熔断器配置
 */
export interface CircuitBreakerConfig {
  name: string
  baseConfig?: string
  slidingWindowType: string
  slidingWindowSize: number
  minimumNumberOfCalls: number
  failureRateThreshold: number
  slowCallRateThreshold: number
  slowCallDurationThreshold: number
  waitDurationInOpenState: number
  permittedNumberOfCallsInHalfOpenState: number
  automaticTransitionFromOpenToHalfOpenEnabled: boolean
  instanceStatuses?: CircuitBreakerStatus[]
}

/**
 * 熔断器总览
 */
export interface CircuitBreakerOverview {
  circuitBreakers: CircuitBreakerConfig[]
  totalCircuitBreakers: number
  openCount: number
  closedCount: number
  halfOpenCount: number
  totalInstances: number
}

// ==================== 新增类型定义 ====================

/**
 * 实例摘要
 */
export interface InstanceSummary {
  instanceId: string
  host: string
  port: number
  status: string
  healthStatus: string
  summary: {
    total: number
    open: number
    closed: number
    halfOpen: number
  }
}

/**
 * 熔断器汇总（重构后）
 */
export interface CircuitBreakerSummary {
  name: string
  baseConfig?: string
  failureRateThreshold: number
  slidingWindowSize: number
  minimumNumberOfCalls: number
  waitDurationInOpenState: number
  closedCount: number
  openCount: number
  halfOpenCount: number
  instances?: CircuitBreakerInstanceStatus[]
}

/**
 * 熔断器实例状态
 */
export interface CircuitBreakerInstanceStatus {
  instanceId: string
  state: string
  failureRate: number
  slowCallRate: number
  numberOfCalls: number
  numberOfFailedCalls: number
  numberOfSuccessfulCalls: number
  stateTransitionTime?: number
  timestamp: number
}

/**
 * 状态转换历史
 */
export interface StateTransitionHistory {
  fromState: string
  toState: string
  timestamp: number
  reason: string
  failureRate?: number
  numberOfCalls?: number
}

/**
 * 熔断器详情
 */
export interface CircuitBreakerDetail {
  config: CircuitBreakerConfig
  instances: CircuitBreakerInstanceStatus[]
  history: StateTransitionHistory[]
  trend: TrendData[]
}

/**
 * 趋势数据
 */
export interface TrendData {
  timestamp: number
  failureRate: number
  slowCallRate: number
  numberOfCalls: number
}

/**
 * 熔断器总览（重构后）
 */
export interface CircuitBreakerOverviewNew {
  circuitBreakers: CircuitBreakerSummary[]
  totalCircuitBreakers: number
  openCount: number
  closedCount: number
  halfOpenCount: number
  totalInstances: number
  healthScore: number
}

// ==================== API 函数 ====================

/**
 * 获取熔断器监控总览
 */
export const getOverview = (): Promise<CircuitBreakerOverview> => {
  return request.post('/circuitBreaker/getOverview', { body: {} })
}

/**
 * 获取指定熔断器配置详情
 * @param name 熔断器名称
 */
export const getConfig = (name: string): Promise<CircuitBreakerConfig> => {
  return request.post('/circuitBreaker/getConfig', { body: { name } })
}

// ==================== 新增 API 函数 ====================

/**
 * 获取实例列表
 */
export const getInstanceList = (): Promise<InstanceSummary[]> => {
  return request.post('/circuitBreaker/getInstanceList', { body: {} })
}

/**
 * 获取熔断器总览（支持按实例筛选）
 * @param instanceId 实例ID（可选）
 */
export const getOverviewNew = (instanceId?: string): Promise<CircuitBreakerOverviewNew> => {
  return request.post('/circuitBreaker/getOverview', { body: { instanceId } })
}

/**
 * 获取熔断器详情
 * @param name 熔断器名称
 * @param instanceId 实例ID（可选）
 */
export const getDetail = (name: string, instanceId?: string): Promise<CircuitBreakerDetail> => {
  return request.post('/circuitBreaker/getDetail', { body: { name, instanceId } })
}

/**
 * 获取状态转换历史
 * @param instanceId 实例ID
 * @param name 熔断器名称
 * @param limit 返回数量限制
 */
export const getHistory = (instanceId: string, name: string, limit?: number): Promise<StateTransitionHistory[]> => {
  return request.post('/circuitBreaker/getHistory', { body: { instanceId, name, limit } })
}

// ==================== API 对象导出 ====================

export const circuitBreakerApi = {
  getOverview,
  getConfig,
  getInstanceList,
  getOverviewNew,
  getDetail,
  getHistory,
}

// ==================== 状态常量 ====================

/**
 * 熔断器状态类型
 */
export const STATE_OPTIONS = [
  { value: 'CLOSED', label: '关闭（正常）', color: 'success' },
  { value: 'OPEN', label: '开启（熔断）', color: 'danger' },
  { value: 'HALF_OPEN', label: '半开（探测）', color: 'warning' },
]

/**
 * 滑动窗口类型
 */
export const WINDOW_TYPE_OPTIONS = [
  { value: 'COUNT_BASED', label: '基于计数' },
  { value: 'TIME_BASED', label: '基于时间' },
]
