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

// Route API object (for component using routeApi.xxx pattern)
export const routeApi = {
  getList: getRouteList,
  save: saveRoute,
  delete: deleteRoute,
  refresh: refreshRoutes,
}
