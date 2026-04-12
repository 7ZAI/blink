import request from '@/utils/request'

// Route API Types

// 路由查询参数
export interface RouteQuery {
  routeId?: string
  routeName?: string
  routesGroup?: string
  storageMode?: string
  status?: number
  uri?: string
  pageNum?: number
  pageSize?: number
}

// 断言配置（支持自定义类型扩展）
export interface PredicateConfig {
  name: string
  args: Record<string, any>
  customName?: string
  customArgsJson?: string
}

// 过滤器配置（支持自定义类型扩展）
export interface FilterConfig {
  name: string
  args: Record<string, any>
  customName?: string
  customArgsJson?: string
}

// 路由定义（数据库存储）
export interface RouteDefinition {
  routeId: string
  routeName?: string
  uri: string
  predicates?: PredicateConfig[]
  filters?: FilterConfig[]
  orderNum?: number
  metadata?: Record<string, any>
  routesGroup?: string
  storageMode?: string
  nacosDataId?: string
  nacosGroup?: string
  status?: number
  remark?: string
  createBy?: string
  createTime?: string
  updateBy?: string
  updateTime?: string
  // 新增字段 - 乐观锁和推送状态
  version?: number
  pushStatus?: number // 0-未推送 1-已推送 2-推送失败
  lastPushTime?: string
}

// 保存路由请求
export interface SaveRouteReq {
  routeId: string
  routeName?: string
  uri: string
  predicates?: PredicateConfig[]
  filters?: FilterConfig[]
  orderNum?: number
  metadata?: Record<string, any>
  routesGroup?: string
  storageMode?: string
  nacosDataId?: string
  nacosGroup?: string
  remark?: string
  status?: number
}

// 更新路由请求
export interface UpdateRouteReq {
  routeId: string
  routeName?: string
  uri?: string
  predicates?: PredicateConfig[]
  filters?: FilterConfig[]
  orderNum?: number
  metadata?: Record<string, any>
  routesGroup?: string
  storageMode?: string
  nacosDataId?: string
  nacosGroup?: string
  status?: number
  remark?: string
  // 新增字段 - 乐观锁版本号（必传）
  version?: number
}

// 删除路由请求
export interface DeleteRouteReq {
  routesGroup?: string
  routeIds: string[]
}

// 查询路由历史请求
export interface QueryRouteHistoryReq {
  routeId: string
  operationType?: string
  operatorName?: string
  pageNum?: number
  pageSize?: number
}

// 回滚路由请求
export interface RollbackRouteReq {
  routeId: string
  historyId: number
  syncToStorage?: boolean
}

// 路由历史记录
export interface RouteHistory {
  historyId: number
  routeId: string
  routeName?: string
  operationType: string
  beforeData?: RouteDefinition
  afterData?: RouteDefinition
  operatorId?: number
  operatorName?: string
  operateTime: string
  remark?: string
}

// 分页结果
export interface PageResult<T> {
  pageNum: number
  pageSize: number
  total: number
  pages: number
  orderBy: string
  rows: T[]
}

// ========== Redis/数据库 路由管理 ==========

/**
 * 获取路由列表（从数据库）
 */
export const getRouteList = (params: RouteQuery): Promise<PageResult<RouteDefinition>> => {
  return request.post('/route/getRouteList', { body: params })
}

/**
 * 获取路由详情
 */
export const getRouteDetail = (routeId: string): Promise<RouteDefinition> => {
  return request.post('/route/getRouteDetail', { body: routeId })
}

/**
 * 保存路由（新增）
 */
export const saveRoute = (data: SaveRouteReq): Promise<void> => {
  return request.post('/route/saveRoute', { body: data })
}

/**
 * 更新路由
 */
export const updateRoute = (data: UpdateRouteReq): Promise<void> => {
  return request.post('/route/updateRoute', { body: data })
}

/**
 * 删除路由
 */
export const deleteRoute = (params: DeleteRouteReq): Promise<void> => {
  return request.post('/route/deleteRoute', { body: params })
}

/**
 * 查询路由变更历史
 */
export const getRouteHistory = (params: QueryRouteHistoryReq): Promise<PageResult<RouteHistory>> => {
  return request.post('/route/getRouteHistory', { body: params })
}

/**
 * 回滚路由
 */
export const rollbackRoute = (data: RollbackRouteReq): Promise<void> => {
  return request.post('/route/rollbackRoute', { body: data })
}

/**
 * 刷新路由缓存
 */
export const refreshRoutes = (): Promise<void> => {
  return request.post('/route/refreshRoutes', { body: {} })
}

// ========== 存储方式和实例同步 ==========

/** 存储方式 VO */
export interface StorageModeVO {
  mode: string
  name: string
  description: string
}

/** 网关实例 VO */
export interface GatewayInstanceVO {
  instanceId: string
  serviceId: string
  host: string
  port: number
  uri: string
  status: number
  statusDesc: string
}

/** 同步路由请求 */
export interface SyncRoutesReq {
  storageMode?: string
  routesGroup?: string
  dataId?: string
  group?: string
  pushMode: string
  targetInstanceIds?: string[]
  routeIds?: string[]
}

/**
 * 获取支持的存储方式列表
 */
export const getStorageModes = (): Promise<StorageModeVO[]> => {
  return request.post('/route/getStorageModes', { body: {} })
}

/**
 * 获取在线网关实例列表
 */
export const getOnlineGatewayInstances = (): Promise<GatewayInstanceVO[]> => {
  return request.post('/route/getOnlineGatewayInstances', { body: {} })
}

/**
 * 同步路由到指定实例
 */
export const syncRoutesToInstances = (data: SyncRoutesReq): Promise<void> => {
  return request.post('/route/syncRoutesToInstances', { body: data })
}

// ========== Nacos 路由管理 ==========

/** Nacos 路由查询请求 */
export interface QueryNacosRouteReq {
  dataId: string
  group?: string
  pageNum?: number
  pageSize?: number
}

/** Nacos 路由保存请求 */
export interface SaveNacosRouteReq {
  dataId: string
  group?: string
  routes: RouteDefinition[]
}

/** Nacos 路由删除请求 */
export interface DeleteNacosRouteReq {
  dataId: string
  group?: string
  routeIds: string[]
}

/**
 * 查询 Nacos 路由列表
 */
export const getNacosRouteList = (params: QueryNacosRouteReq): Promise<PageResult<RouteDefinition>> => {
  return request.post('/route/getNacosRouteList', { body: params })
}

/**
 * 保存 Nacos 路由
 */
export const saveNacosRoute = (data: SaveNacosRouteReq): Promise<void> => {
  return request.post('/route/saveNacosRoute', { body: data })
}

/**
 * 删除 Nacos 路由
 */
export const deleteNacosRoute = (params: DeleteNacosRouteReq): Promise<void> => {
  return request.post('/route/deleteNacosRoute', { body: params })
}

// ========== 路由推送管理 ==========

/** 推送路由请求 */
export interface PushRoutesReq {
  routeIds: string[]
  storageMode?: string
  routesGroup?: string
  nacosDataId?: string
  nacosGroup?: string
  pushMode?: string
  targetInstanceIds?: string[]
  remark?: string
}

/** 查询推送历史请求 */
export interface QueryPushLogReq {
  storageMode?: string
  routesGroup?: string
  pushResult?: number
  operatorName?: string
  pageNum?: number
  pageSize?: number
}

/** 查询实例路由请求 */
export interface QueryInstanceRoutesReq {
  instanceId: string
}

/** 查询实例推送历史请求 */
export interface QueryInstancePushHistoryReq {
  instanceId: string
  pageSize?: number
}

/** 校验实例路由请求 */
export interface ValidateInstanceRoutesReq {
  instanceId: string
}

/** 比对实例路由请求 */
export interface CompareInstanceRoutesReq {
  instanceId: string
}

/** 比对结果 */
export interface CompareResult {
  matchedCount: number
  addedCount: number
  modifiedCount: number
  deletedCount: number
  differences?: Array<{
    routeId: string
    diffType: 'added' | 'modified' | 'deleted'
    detail?: string
  }>
}

/** 回滚推送请求 */
export interface RollbackPushReq {
  pushId: number
  pushMode?: string
  targetInstanceIds?: string[]
}

/** 推送记录实体 */
export interface GaRoutePushLogDO {
  pushId: number
  storageMode: string
  routesGroup?: string
  nacosDataId?: string
  nacosGroup?: string
  routeIds: string
  routeSnapshot?: RouteDefinition[]
  pushMode: string
  targetInstanceIds?: string
  instanceCount?: number
  successCount?: number
  pushResult: number
  pushDetail?: Record<string, any>
  operatorId?: number
  operatorName?: string
  pushTime: string
  remark?: string
  // 新增字段 - 失败详情和确认状态
  failedInstanceIds?: string // 失败实例ID列表(JSON数组)
  instanceErrors?: Record<string, string> // 各实例错误信息(JSON对象)
  confirmStatus?: number // 0-待确认 1-已确认 2-超时
}

/**
 * 推送路由到实例
 */
export const pushRoutes = (data: PushRoutesReq): Promise<void> => {
  return request.post('/route/pushRoutes', { body: data })
}

/**
 * 查询推送历史
 */
export const getPushHistory = (params: QueryPushLogReq): Promise<PageResult<GaRoutePushLogDO>> => {
  return request.post('/route/getPushHistory', { body: params })
}

/**
 * 查询实例当前路由
 */
export const getInstanceRoutes = (params: QueryInstanceRoutesReq): Promise<RouteDefinition[]> => {
  return request.post('/route/getInstanceRoutes', { body: params })
}

/**
 * 查询实例推送历史
 */
export const getInstancePushHistory = (params: QueryInstancePushHistoryReq): Promise<PageResult<GaRoutePushLogDO>> => {
  return request.post('/route/getInstancePushHistory', { body: params })
}

/**
 * 校验实例路由
 */
export const validateInstanceRoutes = (params: ValidateInstanceRoutesReq): Promise<{
  consistent: boolean
  differences?: Array<{
    routeId: string
    diffType: string
    expected: string
    actual: string
  }>
}> => {
  return request.post('/route/validateInstanceRoutes', { body: params })
}

/**
 * 比对实例路由（与仓库对比）
 */
export const compareInstanceRoutes = (params: CompareInstanceRoutesReq): Promise<CompareResult> => {
  return request.post('/route/compareInstanceRoutes', { body: params })
}

/**
 * 回滚推送
 */
export const rollbackPush = (data: RollbackPushReq): Promise<void> => {
  return request.post('/route/rollbackPush', { body: data })
}

// ========== 新增接口 - 路由管理增强功能 ==========

/** 全量推送请求 */
export interface FullPushRoutesReq {
  storageMode: string
  routesGroup?: string
  nacosDataId?: string
  nacosGroup?: string
}

/** 批量状态更新请求 */
export interface BatchUpdateStatusReq {
  routeIds: string[]
  status: number // 0-禁用 1-启用
  routesGroup?: string
}

/** 路由分组统计响应 */
export interface RoutesGroupStatsRsp {
  groupTotal: number
  enabledCount: number
  disabledCount: number
  notPushedCount: number
  pushedCount: number
  pushFailedCount: number
}

/** 导出路由请求 */
export interface ExportRoutesReq {
  routeIds?: string[]
  routesGroup?: string
}

/** 导入路由请求 */
export interface ImportRoutesReq {
  routesData: string // JSON字符串
  routesGroup?: string
  overwrite?: boolean // 是否覆盖已存在的路由
}

/** 导入路由响应 */
export interface ImportRoutesRsp {
  successCount: number
  failedCount: number
  failedRoutes: Array<{ routeId: string; reason: string }>
}

/** 克隆路由请求 */
export interface CloneRouteReq {
  sourceRouteId: string
  newRouteId?: string // 新路由ID，可选，不填则自动生成
  newRouteName?: string
}

/**
 * 全量推送 - 推送指定分组下所有启用状态路由
 */
export const fullPushRoutes = (data: FullPushRoutesReq): Promise<void> => {
  return request.post('/route/fullPushRoutes', { body: data })
}

/**
 * 批量更新路由状态（启用/禁用）
 */
export const batchUpdateStatus = (data: BatchUpdateStatusReq): Promise<void> => {
  return request.post('/route/batchUpdateStatus', { body: data })
}

/**
 * 查询路由分组统计信息
 */
export const getRoutesGroupStats = (routesGroup: string): Promise<RoutesGroupStatsRsp> => {
  return request.post('/route/getRoutesGroupStats', { body: routesGroup })
}

/**
 * 导出路由为JSON
 */
export const exportRoutes = (data: ExportRoutesReq): Promise<string> => {
  return request.post('/route/exportRoutes', { body: data })
}

/**
 * 导入路由
 */
export const importRoutes = (data: ImportRoutesReq): Promise<ImportRoutesRsp> => {
  return request.post('/route/importRoutes', { body: data })
}

/**
 * 克隆路由
 */
export const cloneRoute = (data: CloneRouteReq): Promise<RouteDefinition> => {
  return request.post('/route/cloneRoute', { body: data })
}

// Route API object (for component using routeApi.xxx pattern)
export const routeApi = {
  getList: getRouteList,
  getDetail: getRouteDetail,
  save: saveRoute,
  update: updateRoute,
  delete: deleteRoute,
  getHistory: getRouteHistory,
  rollback: rollbackRoute,
  refresh: refreshRoutes,
  getStorageModes,
  getOnlineGatewayInstances,
  syncRoutesToInstances,
  getNacosRouteList,
  saveNacosRoute,
  deleteNacosRoute,
  pushRoutes,
  getPushHistory,
  getInstanceRoutes,
  getInstancePushHistory,
  validateInstanceRoutes,
  compareInstanceRoutes,
  rollbackPush,
  // 新增接口方法
  fullPushRoutes,
  batchUpdateStatus,
  getRoutesGroupStats,
  exportRoutes,
  importRoutes,
  cloneRoute,
}