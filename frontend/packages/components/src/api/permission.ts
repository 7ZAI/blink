import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types'

export interface PermissionInfo {
  acId: number
  acName: string
  acEnName: string
  acIdentity: string
  acType: number
  url: string
  dataFilterId: number
  dataFilterName: string
  menuIds: number[]
  createBy: string
  createTime: string
  updateBy: string
  updateTime: string
}

export interface QueryPermissionParams {
  pageNum?: number
  pageSize?: number
  acName?: string
  acIdentity?: string
  acType?: number
}

export interface QueryPermissionRsp<T> {
  pageNum: number
  pageSize: number
  total: number
  pages: number
  rows: T[]
}

export interface AddPermissionParams {
  acName: string
  acEnName?: string
  acIdentity: string
  acType: number
  url?: string
  dataFilterId?: number
  menuIds?: number[]
}

export interface UpdatePermissionParams {
  acId: number
  acName?: string
  acEnName?: string
  acIdentity?: string
  acType?: number
  url?: string
  dataFilterId?: number
  menuIds?: number[]
}

export interface DeletePermissionParams {
  deleteId?: number
  idList?: number[]
  batchDelete: boolean
}

export const getPermissionList = (params?: QueryPermissionParams): Promise<QueryPermissionRsp<PermissionInfo>> => {
  return request.post('/sysPermission/getSysPermissionList', { body: params || {} }) as Promise<QueryPermissionRsp<PermissionInfo>>
}

export const addPermission = (params: AddPermissionParams): Promise<PermissionInfo> => {
  return request.post('/sysPermission/saveSysPermission', { body: params }) as Promise<PermissionInfo>
}

export const updatePermission = (params: UpdatePermissionParams): Promise<void> => {
  return request.post('/sysPermission/modifySysPermission', { body: params }) as Promise<void>
}

export const deletePermission = (params: DeletePermissionParams): Promise<void> => {
  return request.post('/sysPermission/deleteSysPermission', { body: params }) as Promise<void>
}

export interface MenuTreeRsp {
  rows: MenuInfo[]
  total: number
}

/**
 * 获取菜单树（用于权限关联选择）
 */
export const getMenuTreeForPermission = async (): Promise<MenuInfo[]> => {
  const res = await request.post('/sysMenu/getSysMenuList', { body: {} }) as MenuTreeRsp
  return res.rows || []
}

export interface MenuInfo {
  menuId: number
  menuName: string
  type: number
  parentId: number
  children?: MenuInfo[]
}
