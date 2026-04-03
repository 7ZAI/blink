import request from '@/utils/request'
import type { LoginReq, LoginRsp, CaptchaVO } from '@/types'
import { getDefaultRequestBody } from '@/config/request.config'

export interface LoginConfigRsp {
  captchaEnabled: boolean
  systemTitle: string
  systemLogo: string
  systemFooter: string
  defaultAvatar: string
}

// 登录
export const login = (data: LoginReq): Promise<LoginRsp> => {
  return request.post('/auth/login', {
    body: data,
    ...getDefaultRequestBody(),
  }) as Promise<LoginRsp>
}

// 登出
export const logout = (data: { token: string; userId: string }): Promise<any> => {
  return request.post('/auth/logout', {
    body: data,
    ...getDefaultRequestBody(),
  })
}

// 获取当前登录用户信息
export const getUserInfo = (): Promise<LoginRsp> => {
  return request.post('/auth/getUserInfo', {
    ...getDefaultRequestBody(),
  }) as Promise<LoginRsp>
}

// 获取验证码
export const getCaptcha = (data: {
  captchaType: string
  clientUid?: string
  ts?: number
}): Promise<CaptchaVO> => {
  return request.post('/captcha/get', {
    body: data,
    ...getDefaultRequestBody(),
  }) as Promise<CaptchaVO>
}

// 校验验证码
export const checkCaptcha = (data: {
  captchaId?: string
  captchaType?: string
  pointJson: string
  clientUid?: string
  ts?: number
}): Promise<{
  result: boolean
  msg: string
  captchaId?: string
  captchaVerification?: string
}> => {
  return request.post('/captcha/check', {
    body: data,
    ...getDefaultRequestBody(),
  }) as Promise<{
    result: boolean
    msg: string
    captchaId?: string
    captchaVerification?: string
  }>
}

// 获取登录配置
export const getLoginConfig = (): Promise<LoginConfigRsp> => {
  return request.post('/auth/getLoginConfig', {
    ...getDefaultRequestBody(),
  }) as Promise<LoginConfigRsp>
}

// 首次登录重置密码
export const firstTimeResetPassword = (data: {
  newPassword: string
  confirmPassword: string
}): Promise<void> => {
  return request.post('/auth/firstTimeResetPassword', {
    body: data,
    ...getDefaultRequestBody(),
  }) as Promise<void>
}
