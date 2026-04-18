import request from '@/utils/request'

// ==================== 类型定义 ====================

/**
 * 实例分组
 */
export interface InstanceGroup {
  groupId: number
  groupKey: string
  groupName: string
  status: number
  remark?: string
  createTime?: string
  updateTime?: string
}

/**
 * 分页查询分组请求参数
 */
export interface QueryInstanceGroupParams {
  pageNum?: number
  pageSize?: number
  groupKey?: string
  groupName?: string
  status?: number
}

/**
 * 新增分组请求参数
 */
export interface AddInstanceGroupParams {
  groupKey: string
  groupName: string
  remark?: string
}

/**
 * 更新分组请求参数
 */
export interface UpdateInstanceGroupParams {
  groupId: number
  groupName?: string
  status?: number
  remark?: string
}

/**
 * 分页查询分组列表响应
 */
export interface QueryInstanceGroupListResult {
  rows: InstanceGroup[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

// ==================== API 函数 ====================

/**
 * 分页查询实例分组列表
 */
export const queryInstanceGroupList = (params: QueryInstanceGroupParams = {}): Promise<QueryInstanceGroupListResult> => {
  return request.post('/instanceGroup/queryInstanceGroupList', { body: params })
}

/**
 * 获取实例分组详情
 */
export const getInstanceGroupDetail = (params: { groupId: number }): Promise<InstanceGroup> => {
  return request.post('/instanceGroup/getInstanceGroupDetail', { body: params })
}

/**
 * 新增实例分组
 */
export const addInstanceGroup = (params: AddInstanceGroupParams): Promise<void> => {
  return request.post('/instanceGroup/addInstanceGroup', { body: params })
}

/**
 * 更新实例分组
 */
export const updateInstanceGroup = (params: UpdateInstanceGroupParams): Promise<void> => {
  return request.post('/instanceGroup/updateInstanceGroup', { body: params })
}

/**
 * 删除实例分组
 */
export const deleteInstanceGroup = (params: { groupId: number }): Promise<void> => {
  return request.post('/instanceGroup/deleteInstanceGroup', { body: params })
}

/**
 * 获取所有启用的分组列表（用于下拉选择）
 */
export const getEnabledInstanceGroups = (): Promise<InstanceGroup[]> => {
  return request.post('/instanceGroup/getEnabledInstanceGroups', { body: {} })
}

// ==================== API 对象导出 ====================

export const instanceGroupApi = {
  queryInstanceGroupList,
  getInstanceGroupDetail,
  addInstanceGroup,
  updateInstanceGroup,
  deleteInstanceGroup,
  getEnabledInstanceGroups,
}
