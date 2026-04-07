import request from '@/utils/request'

/**
 * 登录请求参数
 */
export interface LoginReq {
  loginName: string
  password: string
  rememberMe?: boolean
}

/**
 * 菜单信息
 */
export interface MenuVO {
  menuId: number
  menuName: string
  menuEnName: string
  parentId: number
  menuLevel: number
  type: number
  icon: string
  url: string
  componentPath: string
  orderNumber: number
  status: number
  permId?: number
  permIdentity?: string
  permName?: string
  hasChildren: boolean
  children?: MenuVO[]
  createTime?: string
  updateTime?: string
}

/**
 * 用户信息
 */
export interface UserInfoVO {
  userId: number
  loginName: string
  username: string
  avatar?: string
  avatarStyle?: string
  sex?: number
  phone?: string
  email?: string
  groupName?: string
  lastLoginTime?: string
  locked?: number
  superFlag?: number
  pswRetry?: number
}

/**
 * 登录响应（包含菜单和权限）
 */
export interface LoginRsp {
  token: string
  userInfo: UserInfoVO
  roles: string[]
  roleIds: number[]
  menus: MenuVO[]
  functionMenu: MenuVO[]
  permissions: string[]
  needResetPassword?: boolean
}

/**
 * 用户登录
 */
export const login = (params: LoginReq): Promise<LoginRsp> => {
  return request.post('/auth/login', { body: params })
}

/**
 * 用户登出
 */
export const logout = (): Promise<void> => {
  return request.post('/auth/logout', { body: {} })
}

/**
 * 获取当前用户信息（包含菜单）
 */
export const getUserInfo = (): Promise<LoginRsp> => {
  return request.post('/auth/getUserInfo', { body: {} })
}

/**
 * 修改密码
 */
export const modifyPassword = (params: { oldPassword: string; newPassword: string }): Promise<void> => {
  return request.post('/auth/modifyPassword', { body: params })
}