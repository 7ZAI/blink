// 验证码VO
export interface CaptchaVO {
  captchaId?: string
  captchaType?: string
  originalImageBase64?: string
  jigsawImageBase64?: string
  wordList?: string[]
  pointJson?: string
  token?: string
  captchaVerification?: string
}

// 登录请求
export interface LoginReq {
  loginName: string
  password: string
  captchaVO?: CaptchaVO
}

// 登录响应
export interface LoginRsp {
  token: string
  userInfo: UserInfo
  roles: string[]
  menus: Menu[]
  functionMenu: Menu[]
  permissions: string[]
  needResetPassword?: boolean
}

// 用户信息
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

// 菜单
export interface Menu {
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
  permId?: number
  permIdentity?: string  // 关联的权限标识
  permName?: string      // 关联的权限名称
  children?: Menu[]
}

// API 响应包装
export interface ApiResponse<T = any> {
  msgCode: string
  msgInfo: string
  msgType: string
  body: T
}

// 验证码
export interface Captcha {
  captchaKey: string
  captchaImage: string
}

// 登出请求
export interface LogoutReq {
  token: string
  userId: string
}

// 分页结果
export interface PageResult<T> {
  pageNum: number
  pageSize: number
  total: number
  pages: number
  orderBy?: string
  rows: T[]
}
