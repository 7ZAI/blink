import request from '@/utils/request'
import type { ApiResponse } from '@/types'

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
  createBy: string
  createTime: string
  updateBy: string
  updateTime: string
  permId?: number
  permIdentity?: string
  permName?: string
  children?: MenuInfo[]
}

export interface QueryMenuParams {
  menuName?: string
  status?: number
}

export interface QueryMenuRsp {
  rows: MenuInfo[]
  total?: number
  menus?: MenuInfo[]
}

export interface AddMenuParams {
  menuName: string
  menuEnName?: string
  type: number
  icon?: string
  url?: string
  orderNumber?: number
  status?: number
  parentId?: number
  componentPath?: string
  permId?: number
}

export interface UpdateMenuParams {
  menuId: number
  menuName?: string
  menuEnName?: string
  type?: number
  icon?: string
  url?: string
  orderNumber?: number
  status?: number
  parentId?: number
  componentPath?: string
  permId?: number
}

export interface DeleteMenuParams {
  deleteId?: number
  idList?: number[]
  batchDelete: boolean
}

export const getMenuList = (params?: QueryMenuParams): Promise<QueryMenuRsp> => {
  return request.post('/sysMenu/getSysMenuList', { body: params || {} }) as Promise<QueryMenuRsp>
}

export const addMenu = (params: AddMenuParams): Promise<MenuInfo> => {
  return request.post('/sysMenu/saveSysMenu', { body: params }) as Promise<MenuInfo>
}

export const updateMenu = (params: UpdateMenuParams): Promise<MenuInfo> => {
  return request.post('/sysMenu/modifySysMenu', { body: params }) as Promise<MenuInfo>
}

export const deleteMenu = (params: DeleteMenuParams): Promise<void> => {
  return request.post('/sysMenu/deleteSysMenu', { body: params }) as Promise<void>
}

/**
 * 检查菜单角色分配
 */
export interface CheckMenuRoleParams {
  menuId: number
  newPermId?: number
}

export interface CheckMenuRoleRsp {
  assigned: boolean
  roles: RoleInfo[]
  currentPermId: number | null
  permChanged: boolean
}

export interface RoleInfo {
  roleId: number
  roleName: string
  roleEnName: string
  roleCode: string
  status: number
}

export const checkMenuRoleAssignment = (params: CheckMenuRoleParams): Promise<CheckMenuRoleRsp> => {
  return request.post('/sysMenu/checkMenuRoleAssignment', {
    body: params,
  }) as Promise<CheckMenuRoleRsp>
}

/**
 * 获取接口权限列表（用于菜单关联选择）
 * 使用分页接口，只查询接口权限(acType=1)
 */
export const getApiPermissions = (
  params?: QueryPermissionParams
): Promise<QueryPermissionRsp<PermissionInfo>> => {
  return request.post('/sysPermission/getSysPermissionList', {
    body: { acType: 1, ...params },
  }) as Promise<QueryPermissionRsp<PermissionInfo>>
}

export interface QueryPermissionParams {
  pageNum?: number
  pageSize?: number
  acName?: string
  acIdentity?: string
}

export interface QueryPermissionRsp<T> {
  rows: T[]
  total: number
  pageNum: number
  pageSize: number
}

export interface PermissionInfo {
  acId: number
  acName: string
  acIdentity: string
  acType: number
  url: string
}
