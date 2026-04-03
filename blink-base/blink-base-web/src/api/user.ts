import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types'

export interface QueryUserParams {
  pageNum: number
  pageSize: number
  loginName?: string
  username?: string
  sex?: number
  startDate?: string
  endDate?: string
  groupId?: number
}

export interface UserInfo {
  userId: number
  loginName: string
  username: string
  avatar: string
  avatarStyle: string
  sex: number
  phone: string
  email: string
  groupName: string
  lastLoginTime: string
  locked: number
  superFlag: number
  pswRetry: number
  createBy: string
  updateBy: string
  createTime: string
  updateTime: string
  lockTime: string
  remark: string
  group?: {
    groupId: number
    groupName: string
  }
  roles?: {
    roleId: number
    roleName: string
  }[]
}

export interface UserDetail {
  userId: number
  loginName: string
  password: string
  username: string
  avatar: string
  avatarStyle: string
  sex: number
  phone: string
  email: string
  lastLoginTime: string
  locked: number
  salt: string
  pswRetry: number
  superFlag: number
  remark: string
  createBy: string
  updateBy: string
  createTime: string
  updateTime: string
  lockTime: string
}

export interface AddUserParams {
  loginName: string
  username?: string
  avatar?: string
  sex: number
  phone: string
  email?: string
  groupId?: number
  roles?: number[]
}

export interface UpdateUserParams {
  userId: number
  username?: string
  avatar?: string
  sex: number
  phone: string
  email?: string
  groupIdList?: number[]
  roleIdList?: number[]
}

export interface DeleteUserParams {
  userId?: number
  userIdList?: number[]
  batchDelete: boolean
}

export const getUserList = (params: QueryUserParams): Promise<PageResult<UserInfo>> => {
  return request.post('/sysUser/getSysUserList', { body: params }) as Promise<PageResult<UserInfo>>
}

export const getUserDetail = (loginName: string): Promise<UserDetail> => {
  return request.post('/sysUser/getSysUserDetail', { body: { loginName } }) as Promise<UserDetail>
}

export const addUser = (params: AddUserParams): Promise<ApiResponse> => {
  return request.post('/sysUser/saveSysUser', { body: params })
}

export const updateUser = (params: UpdateUserParams): Promise<void> => {
  return request.post('/sysUser/modifySysUser', { body: params }) as Promise<void>
}

export interface LockUserParams {
  userId: number
  locked: number
}

export const lockUser = (params: LockUserParams): Promise<void> => {
  return request.post('/sysUser/lockUser', { body: params }) as Promise<void>
}

export const deleteUser = (params: DeleteUserParams): Promise<void> => {
  return request.post('/sysUser/deleteSysUser', { body: params }) as Promise<void>
}

export interface AssignUserRoleParams {
  userIdList: number[]
  roleIdList: number[]
}

export const assignUserRoles = (params: AssignUserRoleParams): Promise<void> => {
  return request.post('/sysUser/assignUserRoles', { body: params }) as Promise<void>
}

export interface ModifyPasswordParams {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

export const modifyPassword = (params: ModifyPasswordParams): Promise<void> => {
  return request.post('/sysUser/modifyPassword', { body: params }) as Promise<void>
}

export interface ResetPasswordParams {
  userId: number
  newPassword: string
}

export const resetPassword = (params: ResetPasswordParams): Promise<void> => {
  return request.post('/sysUser/resetPassword', { body: params }) as Promise<void>
}

// 用户偏好设置
export interface UserPreference {
  preferenceId?: number
  userId?: number
  theme: string
  language: string
  sidebarCollapsed: boolean
  fontSize: number
  createTime?: string
  updateTime?: string
}

export interface SavePreferenceParams {
  theme: string
  language: string
  sidebarCollapsed: boolean
  fontSize: number
}

export const saveUserPreference = (params: SavePreferenceParams): Promise<void> => {
  return request.post('/sysUser/saveUserPreference', { body: params }) as Promise<void>
}

export const getUserPreference = (): Promise<UserPreference> => {
  return request.post('/sysUser/getUserPreference', { body: {} }) as Promise<UserPreference>
}

// 用户权限信息
export interface UserPermissionRsp {
  roles: RoleInfo[]
  menus: MenuInfo[]
  permissions: PermissionInfo[]
}

export interface RoleInfo {
  roleId: number
  roleName: string
  roleEnName: string
  roleCode: string
  status: number
}

export interface MenuInfo {
  menuId: number
  menuName: string
  menuEnName: string
  type: number
  icon: string
  url: string
  orderNumber: number
  status: number
  parentId: number
  menuLevel: number
  componentPath: string
  hasChildren: boolean
  children?: MenuInfo[]
}

export interface PermissionInfo {
  acId: number
  acName: string
  acEnName: string
  acIdentity: string
  acType: number
  icon: string
  url: string
}

export const getUserPermissions = (userId: number): Promise<UserPermissionRsp> => {
  return request.post('/sysUser/getUserPermissions', { body: { userId } }) as Promise<UserPermissionRsp>
}
