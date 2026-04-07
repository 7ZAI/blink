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
  roles?: number[]
}

export interface UpdateUserParams {
  userId: number
  username?: string
  avatar?: string
  sex: number
  phone: string
  email?: string
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