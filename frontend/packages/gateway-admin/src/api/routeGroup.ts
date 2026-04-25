import request from '@/utils/request'

// ==================== 类型定义 ====================

/**
 * 路由分组
 */
export interface RouteGroup {
  groupId: number
  groupKey: string
  groupName: string
  instanceCount: number
  remark?: string
  createTime?: string
  updateTime?: string
}

/**
 * 分页查询分组请求参数
 */
export interface QueryRouteGroupParams {
  pageNum?: number
  pageSize?: number
  groupKey?: string
  groupName?: string
  status?: number
}

/**
 * 新增分组请求参数
 */
export interface AddRouteGroupParams {
  groupKey: string
  groupName: string
  remark?: string
}

/**
 * 更新分组请求参数
 */
export interface UpdateRouteGroupParams {
  groupId: number
  groupName?: string
  status?: number
  remark?: string
}

/**
 * 分页查询分组列表响应
 */
export interface QueryRouteGroupListResult {
  rows: RouteGroup[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

// ==================== API 函数 ====================

/**
 * 分页查询路由分组列表
 */
export const queryRouteGroupList = (params: QueryRouteGroupParams = {}): Promise<QueryRouteGroupListResult> => {
  return request.post('/routeGroup/queryRouteGroupList', { body: params })
}

/**
 * 获取路由分组详情
 */
export const getRouteGroupDetail = (params: { groupId: number }): Promise<RouteGroup> => {
  return request.post('/routeGroup/getRouteGroupDetail', { body: params })
}

/**
 * 新增路由分组
 */
export const addRouteGroup = (params: AddRouteGroupParams): Promise<void> => {
  return request.post('/routeGroup/addRouteGroup', { body: params })
}

/**
 * 更新路由分组
 */
export const updateRouteGroup = (params: UpdateRouteGroupParams): Promise<void> => {
  return request.post('/routeGroup/updateRouteGroup', { body: params })
}

/**
 * 删除路由分组
 */
export const deleteRouteGroup = (params: { groupId: number }): Promise<void> => {
  return request.post('/routeGroup/deleteRouteGroup', { body: params })
}

/**
 * 获取所有启用的分组列表（用于下拉选择）
 */
export const getEnabledRouteGroups = (): Promise<RouteGroup[]> => {
  return request.post('/routeGroup/getEnabledRouteGroups', { body: {} })
}

// ==================== API 对象导出 ====================

export const routeGroupApi = {
  queryRouteGroupList,
  getRouteGroupDetail,
  addRouteGroup,
  updateRouteGroup,
  deleteRouteGroup,
  getEnabledRouteGroups,
}

// ==================== 兼容性别名（过渡期使用） ====================

/**
 * @deprecated 请使用 RouteGroup
 */
export type InstanceGroup = RouteGroup

/**
 * @deprecated 请使用 getEnabledRouteGroups
 */
export const getEnabledInstanceGroups = getEnabledRouteGroups
