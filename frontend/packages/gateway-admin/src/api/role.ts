import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types'

export interface RoleInfo {
  roleId: number
  roleName: string
  roleEnName: string
  roleCode: string
  status: number
  roleType: number
  createBy: string
  updateBy: string
  createTime: string
  updateTime: string
}

export interface QueryRoleParams {
  pageNum?: number
  pageSize?: number
  roleName?: string
  roleCode?: string
  status?: number
}

export interface AddRoleParams {
  roleName: string
  roleEnName?: string
  roleCode: string
  status: number
  roleType: number
}

export interface UpdateRoleParams {
  roleId: number
  roleName?: string
  roleEnName?: string
  roleCode?: string
  status?: number
  roleType?: number
}

export interface DeleteRoleParams {
  deleteId?: number
  idList?: number[]
  batchDelete: boolean
}

export interface QueryRoleRsp {
  pageNum: number
  pageSize: number
  total: number
  pages: number
  rows: RoleInfo[]
}

export interface AssignPermissionParams {
  roleId: number
  permissionIds: number[]
}

export interface AssignMenuParams {
  roleId: number
  menuIds: number[]
}

export interface PermissionInfo {
  acId: number
  acName: string
  acEnName: string
  acIdentity: string
  acType: number
  icon: string
  url: string
  dataFilterId?: number
  dataFilterName?: string
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

export interface UserInfo {
  userId: number
  loginName: string
  username: string
  avatar: string
  phone: string
  email: string
  locked: number
  createTime: string
}

export interface RoleDetailRsp {
  roleInfo: RoleInfo
  permissions: PermissionInfo[]
  menus: MenuInfo[]
  users: UserInfo[]
}

export const getRoleList = (params?: QueryRoleParams): Promise<QueryRoleRsp> => {
  return request.post('/sysRole/getSysRoleList', { body: params || {} }) as Promise<QueryRoleRsp>
}

export const addRole = (params: AddRoleParams): Promise<RoleInfo> => {
  return request.post('/sysRole/saveSysRole', { body: params }) as Promise<RoleInfo>
}

export const updateRole = (params: UpdateRoleParams): Promise<RoleInfo> => {
  return request.post('/sysRole/modifySysRole', { body: params }) as Promise<RoleInfo>
}

export const deleteRole = (params: DeleteRoleParams): Promise<void> => {
  return request.post('/sysRole/deleteSysRole', { body: params }) as Promise<void>
}

export const getAllRoles = async (): Promise<RoleInfo[]> => {
  const res = await getRoleList({ pageNum: 1, pageSize: 1000 })
  return res.rows || []
}

export const assignPermissions = (params: AssignPermissionParams): Promise<void> => {
  return request.post('/sysRole/assignPermissions', { body: params }) as Promise<void>
}

export const assignMenus = (params: AssignMenuParams): Promise<void> => {
  return request.post('/sysRole/assignMenus', { body: params }) as Promise<void>
}

export const getRoleDetail = (roleId: number): Promise<RoleDetailRsp> => {
  return request.post('/sysRole/getRoleDetail', { body: { roleId } }) as Promise<RoleDetailRsp>
}

export interface AssignRoleToUsersParams {
  roleId: number
  userIds: number[]
}

export const assignRoleToUsers = (params: AssignRoleToUsersParams): Promise<void> => {
  return request.post('/sysRole/assignRoleToUsers', { body: params }) as Promise<void>
}
