import request from '@/utils/request'

// Monitor API Types - 与后端一致

// 查询网关实例请求参数
export interface MonitorQuery {
  instanceId?: string
}

// 查询统计数据请求参数
export interface StatisticsQuery {
  timeRange?: string // 时间范围（可选）
}

// 实例信息
export interface InstanceInfo {
  instanceId: string
  serviceId: string
  host: string
  port: number
  uri: string
  status: number // 0-在线，1-离线，2-下线
  statusDesc: string
  healthy: boolean // 健康状态
  onlineTime?: string
  offlineTime?: string
  lastHeartbeat?: string
}

// 实例详情信息（完整监控指标）
export interface InstanceDetailInfo {
  // 实例基本信息
  instanceId: string
  serviceId: string
  host: string
  port: number
  uri: string
  healthStatus: string
  statusDesc: string
  timestamp: number

  // JVM 内存指标
  heapUsed: number
  heapMax: number
  heapUsagePercent: number
  nonHeapUsed: number
  cpuUsage: number
  memoryUsage: number

  // GC 统计指标
  youngGcCount: number
  youngGcTime: number
  oldGcCount: number
  oldGcTime: number
  totalGcCount: number
  totalGcTime: number

  // 线程指标
  liveThreads: number
  peakThreads: number
  daemonThreads: number

  // HTTP 统计指标
  totalRequests: number
  successRequests: number
  failedRequests: number
  successRate: number
  avgResponseTime: number
  // 响应时间分布指标
  p50ResponseTime: number
  p95ResponseTime: number
  p99ResponseTime: number
  maxResponseTime: number
  // QPS 指标
  currentQps: number
  activeConnections: number
}

// 统计信息
export interface StatisticsInfo {
  totalInstances: number
  healthyInstances: number
  totalRequests: number
  successRequests: number
  failedRequests: number
  avgResponseTime: number
}

// 实例列表结果
export interface InstanceListResult {
  total: number
  instances: InstanceInfo[]
}

// 下线实例参数
export interface OfflineInstanceParams {
  instanceId: string
  reason?: string
}

// 上线实例参数
export interface OnlineInstanceParams {
  instanceId: string
}

// ==================== 流量历史查询相关类型 ====================

/**
 * 流量历史查询参数
 */
export interface TrafficHistoryQuery {
  startTime?: number // 开始时间（毫秒时间戳）
  endTime?: number // 结束时间（毫秒时间戳）
  granularity?: 'MINUTE' | 'HOUR' // 数据粒度
}

/**
 * 流量数据点（历史查询返回）
 */
export interface TrafficHistoryPoint {
  time: string // 时间（格式化字符串）
  timestamp: number // 时间戳（毫秒）
  count: number // 请求数量（增量）
  successCount: number // 成功请求数
  failedCount: number // 失败请求数
  peakQps: number // 峰值 QPS
  // 响应时间分布
  avgResponseTime?: number // 平均响应时间（ms）
  p50ResponseTime?: number // P50 响应时间（ms）
  p95ResponseTime?: number // P95 响应时间（ms）
  p99ResponseTime?: number // P99 响应时间（ms）
  maxResponseTime?: number // 最大响应时间（ms）
  // 错误分类
  error4xxCount?: number // 4xx 错误数
  error5xxCount?: number // 5xx 错误数
  errorRate?: number // 错误率（%）
  // 实时指标
  currentQps?: number // 实时 QPS
}

/**
 * 流量历史查询结果
 */
export interface TrafficHistoryResult {
  points: TrafficHistoryPoint[] // 流量数据点列表
  totalRequests: number // 总请求数（时间范围内）
  peakQps: number // 峰值 QPS
  // 响应时间分布汇总
  avgP50ResponseTime?: number // 平均 P50 响应时间
  avgP95ResponseTime?: number // 平均 P95 响应时间
  avgP99ResponseTime?: number // 平均 P99 响应时间
  maxResponseTime?: number // 最大响应时间
  // 错误分类汇总
  totalError4xx?: number // 总 4xx 错误数
  totalError5xx?: number // 总 5xx 错误数
  avgErrorRate?: number // 平均错误率
}

// Get gateway instances
export const getGatewayInstances = (params: MonitorQuery = {}): Promise<InstanceListResult> => {
  return request.post('/monitor/getGatewayInstances', { body: params })
}

// Get statistics
export const getStatistics = (params: StatisticsQuery = {}): Promise<StatisticsInfo> => {
  return request.post('/monitor/getStatistics', { body: params })
}

// Get health status
export const getHealthStatus = (params: { instanceId?: string } = {}): Promise<any> => {
  return request.post('/monitor/getHealthStatus', { body: params })
}

// ==================== 实例管理接口 ====================

/**
 * 获取网关实例列表（管理用）
 */
export const getInstanceList = (): Promise<InstanceListResult> => {
  return request.post('/gatewayInstance/getGatewayInstances', { body: {} })
}

/**
 * 获取实例详情（基础信息）
 * @param instanceId 实例ID
 */
export const getInstanceDetail = (instanceId: string): Promise<InstanceInfo> => {
  return request.post('/gatewayInstance/getGatewayInstanceDetail', { body: { instanceId } })
}

/**
 * 获取实例监控详情（完整 JVM/GC/HTTP 指标）
 * @param instanceId 实例ID
 */
export const getMonitorInstanceDetail = (instanceId: string): Promise<InstanceDetailInfo> => {
  return request.post('/monitor/getInstanceDetail', { body: { instanceId } })
}

/**
 * 下线网关实例
 * @param params 下线参数
 */
export const offlineInstance = (params: OfflineInstanceParams): Promise<void> => {
  return request.post('/gatewayInstance/offlineInstance', { body: params })
}

/**
 * 上线网关实例
 * @param params 上线参数
 */
export const onlineInstance = (params: OnlineInstanceParams): Promise<void> => {
  return request.post('/gatewayInstance/onlineInstance', { body: params })
}

/**
 * 查询流量历史数据
 * @param params 查询参数（时间范围、粒度）
 */
export const getTrafficHistory = (params: TrafficHistoryQuery = {}): Promise<TrafficHistoryResult> => {
  return request.post('/monitor/getTrafficHistory', { body: params })
}

// Monitor API object (for component using monitorApi.xxx pattern)
export const monitorApi = {
  getGatewayInstances,
  getStatistics,
  getHealthStatus,
  getInstanceList,
  getInstanceDetail,
  getMonitorInstanceDetail,
  offlineInstance,
  onlineInstance,
  getTrafficHistory,
}
