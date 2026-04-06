import request from '@/utils/request'

// ==================== Types ====================

// Sync Channel Data Params
export interface SyncChannelDataParams {
  channelIds?: string[]
  syncType?: number
}

// Cache Check Request
export interface CacheCheckReq {
  type: 'channel' | 'route' | 'config'
  keys?: string[]
}

// Cache Sync Request
export interface CacheSyncReq {
  type: 'channel' | 'route' | 'config'
  keys?: string[]
  syncAll?: boolean
}

// Cache Item Status
export interface CacheItemStatus {
  key: string
  status: 'MATCH' | 'MISMATCH' | 'MISSING' | 'ORPHAN'
  checksum: string | null
  updateTime?: string
}

// Instance Cache Status
export interface InstanceCacheStatus {
  instanceId: string
  ip: string
  port: number
  items: CacheItemStatus[]
}

// Cache Check Response
export interface CacheCheckRsp {
  type: string
  dbItems: CacheItemStatus[]
  instances: InstanceCacheStatus[]
  checkTime: string
}

// Sync Log Item
export interface SyncLogItem {
  id: number
  syncType: string
  syncMode: number
  syncKeys: string[]
  operator: string
  status: number
  instanceCount: number
  successCount: number
  createTime: string
}

// Sync Log Response
export interface SyncLogRsp {
  total: number
  pageNum: number
  pageSize: number
  rows: SyncLogItem[]
}

// Gateway Instance
export interface GatewayInstance {
  instanceId: string
  host: string
  port: number
  uri: string
}

// ==================== Legacy API (DataSyncController) ====================

/**
 * Sync channel data to gateway
 */
export const syncChannelData = (params: SyncChannelDataParams): Promise<void> => {
  return request.post('/dataSync/syncChannelData', { body: params })
}

/**
 * Sync route data to gateway
 */
export const syncRouteData = (): Promise<void> => {
  return request.post('/dataSync/syncRouteData', {})
}

/**
 * Sync config data to gateway
 */
export const syncConfigData = (): Promise<void> => {
  return request.post('/dataSync/syncConfigData', {})
}

// ==================== New API (CacheStatusController) ====================

/**
 * Get gateway instances list
 */
export const getGatewayInstances = (): Promise<GatewayInstance[]> => {
  return request.get('/cacheStatus/instances')
}

/**
 * Execute consistency check
 */
export const checkConsistency = (params: CacheCheckReq): Promise<CacheCheckRsp> => {
  return request.post('/cacheStatus/check', { body: params })
}

/**
 * Sync data to gateway
 */
export const syncData = (params: CacheSyncReq): Promise<void> => {
  return request.post('/cacheStatus/sync', { body: params })
}

/**
 * Get sync logs list
 */
export const getSyncLogs = (pageNum = 1, pageSize = 10): Promise<SyncLogRsp> => {
  return request.get('/cacheStatus/logs', { params: { pageNum, pageSize } })
}

// API object
export const dataSyncApi = {
  // Legacy
  syncChannelData,
  syncRouteData,
  syncConfigData,
  // New
  getInstances: getGatewayInstances,
  check: checkConsistency,
  sync: syncData,
  getLogs: getSyncLogs
}