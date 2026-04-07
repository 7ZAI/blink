import request from '@/utils/request'

// Monitor API Types - 与后端一致

// 查询网关实例请求参数
export interface MonitorQuery {
  instanceId?: string
}

// 查询统计数据请求参数
export interface StatisticsQuery {
  timeRange?: string  // 时间范围（可选）
}

// 实例信息
export interface InstanceInfo {
  instanceId: string
  serviceId: string
  host: string
  port: number
  uri: string
  status: number  // 0-在线，1-离线，2-下线
  statusDesc: string
  healthy: boolean  // 健康状态
  onlineTime?: string
  offlineTime?: string
  lastHeartbeat?: string
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
 * 获取实例详情
 * @param instanceId 实例ID
 */
export const getInstanceDetail = (instanceId: string): Promise<InstanceInfo> => {
  return request.post('/gatewayInstance/getGatewayInstanceDetail', { body: { instanceId } })
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

// Monitor API object (for component using monitorApi.xxx pattern)
export const monitorApi = {
  getGatewayInstances,
  getStatistics,
  getHealthStatus,
  getInstanceList,
  getInstanceDetail,
  offlineInstance,
  onlineInstance
}