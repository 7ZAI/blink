/**
 * 验证码VO
 */
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

/**
 * 登录配置
 */
export interface LoginConfigRsp {
  captchaEnabled: boolean
  systemTitle: string
  systemLogo: string
  systemFooter: string
  defaultAvatar: string
}

/**
 * API 响应类型
 */
export interface ApiResponse<T = unknown> {
  code: string
  msg: string
  body: T
}

/**
 * 分页参数
 */
export interface PageParams {
  pageNum: number
  pageSize: number
}

/**
 * 分页结果
 */
export interface PageResult<T> {
  pageNum: number
  pageSize: number
  total: number
  pages: number
  orderBy: string
  rows: T[]
}

/**
 * 通用选项类型
 */
export interface OptionItem {
  label: string
  value: string | number
}
