import request from '@/utils/request'

// Route API Types

// 路由查询参数
export interface RouteQuery {
  routesGroup?: string
  pageNum?: number
  pageSize?: number
}

// 断言定义
export interface PredicateDefinition {
  name: string
  args: Record<string, any>
}

// 过滤器定义
export interface FilterDefinition {
  name: string
  args: Record<string, any>
}

// 路由定义
export interface RouteDefinition {
  id: string
  uri: string
  predicates: PredicateDefinition[]
  filters: FilterDefinition[]
  order?: number
  metadata?: Record<string, any>
}

// 路由表单
export interface RouteForm {
  routesGroup: string
  routes: RouteDefinition[]
}

// 分页结果
export interface PageResult<T> {
  pageNum: number
  pageSize: number
  total: number
  pages: number
  orderBy: string
  rows: T[]
  routes?: T[]
}

/**
 * 获取路由列表
 * @param params 查询参数
 */
export const getRouteList = (params: RouteQuery): Promise<PageResult<RouteDefinition>> => {
  return request.post('/route/getRouteList', { body: params })
}

/**
 * 保存路由
 * @param data 路由表单数据
 */
export const saveRoute = (data: RouteForm): Promise<void> => {
  return request.post('/route/saveRoute', { body: data })
}

/**
 * 删除路由
 * @param params 删除参数
 */
export const deleteRoute = (params: { routesGroup: string; routeIds: string[] }): Promise<void> => {
  return request.post('/route/deleteRoute', { body: params })
}

/**
 * 刷新路由缓存
 */
export const refreshRoutes = (): Promise<void> => {
  return request.post('/route/refreshRoutes', { body: {} })
}

// ========== 新增类型定义 ==========

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
  storageMode: string
  routesGroup?: string
  dataId?: string
  group?: string
  pushMode: string
  targetInstanceIds?: string[]
  routeIds?: string[]
}

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

// ========== 新增 API 接口 ==========

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

// Route API object (for component using routeApi.xxx pattern)
export const routeApi = {
  getList: getRouteList,
  save: saveRoute,
  delete: deleteRoute,
  refresh: refreshRoutes,
  getStorageModes,
  getOnlineGatewayInstances,
  syncRoutesToInstances,
  getNacosRouteList,
  saveNacosRoute,
  deleteNacosRoute,
}
