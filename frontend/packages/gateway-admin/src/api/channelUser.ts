import request from '@/utils/request'

// Page Result (re-exported for convenience)
export interface PageResult<T> {
  pageNum: number
  pageSize: number
  total: number
  pages: number
  orderBy: string
  rows: T[]
}

// Query Simple User Params
export interface QuerySimpleUserParams {
  pageNum: number
  pageSize: number
  keyword?: string
}

// Simple User Info
export interface SimpleUserInfo {
  userId: number
  loginName: string
  username: string
}

// Role Info
export interface RoleInfo {
  roleId: number
  roleName: string
  roleEnName: string
  status: number
  remark: string
}

// Permission Info
export interface PermissionInfo {
  acId: number
  acName: string
  acEnName: string
  acType: number
  acUrl: string
  status: number
}

// Data Filter Info
export interface DataFilterInfo {
  dataFilterId: number
  dataFilterName: string
  dataFilterEnName: string
  entityClass: string
  tableName: string
  ruleType: string
  status: number
}

// User Permission Detail
export interface UserPermissionDetail {
  roles: RoleInfo[]
  permissions: PermissionInfo[]
  dataFilters: DataFilterInfo[]
}

// Get simple user list
export const getSimpleUserList = (
  params: QuerySimpleUserParams
): Promise<PageResult<SimpleUserInfo>> => {
  return request.post('/channelUser/getSimpleUserList', { body: params })
}

// Get user permission detail
export const getUserPermissionDetail = (userId: number): Promise<UserPermissionDetail> => {
  return request.post('/channelUser/getUserPermissionDetail', { body: { userId } })
}
