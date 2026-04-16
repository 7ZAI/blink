import request from '@/utils/request'

// ==================== 类型定义 ====================

/**
 * 实例状态
 */
export type InstanceStatus = 'online' | 'offline' | 'shutdown'

/**
 * 实例状态码
 */
export const INSTANCE_STATUS = {
  ONLINE: 0,
  OFFLINE: 1,
  SHUTDOWN: 2,
  DRAINING: 3,
} as const

/**
 * 实例基本信息
 */
export interface InstanceInfo {
  id: number
  instanceId: string
  serviceId: string
  host: string
  port: number
  uri: string
  metadata?: string
  status: number
  statusDesc: string
  onlineTime?: string
  offlineTime?: string
  createTime?: string
  updateTime?: string
}

/**
 * JVM 监控指标
 */
export interface JvmMetrics {
  heapUsed?: number
  heapMax?: number
  heapUsagePercent?: number
  nonHeapUsed?: number
  cpuUsage?: number
  youngGcCount?: number
  youngGcTime?: number
  oldGcCount?: number
  oldGcTime?: number
  liveThreads?: number
  peakThreads?: number
  daemonThreads?: number
  timestamp?: number
}

/**
 * 组件健康状态
 */
export interface ComponentHealth {
  name: string
  status: string
  details?: Record<string, unknown>
}

/**
 * 健康状态详情
 */
export interface HealthDetail {
  status: string
  components?: ComponentHealth[]
}

/**
 * HTTP 请求统计
 */
export interface HttpMetrics {
  totalRequests?: number
  successRequests?: number
  failedRequests?: number
  successRate?: number
  avgResponseTime?: number
  // 响应时间分布
  p50ResponseTime?: number
  p95ResponseTime?: number
  p99ResponseTime?: number
  maxResponseTime?: number
  // QPS 指标
  currentQps?: number
  timestamp?: number
}

/**
 * 实例详情响应
 */
export interface InstanceDetail {
  instanceInfo: InstanceInfo
  healthDetail?: HealthDetail
  jvmMetrics?: JvmMetrics
  httpMetrics?: HttpMetrics
}

/**
 * 分页查询实例请求参数
 */
export interface QueryInstanceParams {
  pageNum?: number
  pageSize?: number
  serviceId?: string
  host?: string
  status?: number
}

/**
 * 分页查询实例列表响应
 */
export interface QueryInstanceListResult {
  rows: InstanceInfo[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

/**
 * 删除实例请求参数
 */
export interface DeleteInstanceParams {
  id: number
}

/**
 * 获取实例详情请求参数
 */
export interface GetInstanceDetailParams {
  id: number
}

/**
 * 上线实例请求参数
 */
export interface OnlineInstanceParams {
  instanceId: string
}

/**
 * 下线实例请求参数
 */
export interface OfflineInstanceParams {
  instanceId: string
  reason?: string
}

// ==================== API 函数 ====================

/**
 * 分页查询实例列表
 */
export const queryInstanceList = (params: QueryInstanceParams = {}): Promise<QueryInstanceListResult> => {
  return request.post('/gatewayInstance/queryInstanceList', { body: params })
}

/**
 * 删除实例
 */
export const deleteInstance = (params: DeleteInstanceParams): Promise<void> => {
  return request.post('/gatewayInstance/deleteInstance', { body: params })
}

/**
 * 获取实例详情（含监控指标）
 */
export const getInstanceDetailWithMetrics = (params: GetInstanceDetailParams): Promise<InstanceDetail> => {
  return request.post('/gatewayInstance/getInstanceDetailWithMetrics', { body: params })
}

/**
 * 上线实例
 */
export const onlineInstance = (params: OnlineInstanceParams): Promise<void> => {
  return request.post('/gatewayInstance/onlineInstance', { body: params })
}

/**
 * 下线实例
 */
export const offlineInstance = (params: OfflineInstanceParams): Promise<void> => {
  return request.post('/gatewayInstance/offlineInstance', { body: params })
}

/**
 * 优雅下线实例（流量排空）
 */
export const gracefulOfflineInstance = (params: OfflineInstanceParams): Promise<void> => {
  return request.post('/gatewayInstance/gracefulOfflineInstance', { body: params })
}

/**
 * 手动刷新实例状态（从Nacos实时获取）
 */
export const refreshInstanceStatus = (): Promise<void> => {
  return request.post('/gatewayInstance/refreshInstanceStatus', { body: {} })
}

// ==================== API 对象导出 ====================

export const instanceApi = {
  queryInstanceList,
  deleteInstance,
  getInstanceDetailWithMetrics,
  onlineInstance,
  offlineInstance,
  gracefulOfflineInstance,
  refreshInstanceStatus,
}