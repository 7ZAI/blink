/**
 * 请求配置常量
 * 用于统一配置请求参数
 */

// 渠道配置
export const REQUEST_CHANNEL = import.meta.env.VITE_REQUEST_CHANNEL || 'web'

// 客户端IP配置
export const REQUEST_CLIENT_IP = import.meta.env.VITE_REQUEST_CLIENT_IP || '127.0.0.1'

// 版本配置
export const REQUEST_VERSION = import.meta.env.VITE_REQUEST_VERSION || 'v1'

// 请求来源
export const REQUEST_SOURCE = import.meta.env.VITE_REQUEST_SOURCE || 'base-web'

// 语言配置
export const REQUEST_LOCALE = import.meta.env.VITE_REQUEST_LOCALE || 'zh_cn'

/**
 * 获取默认请求头配置
 */
export const getDefaultRequestHeaders = () => ({
  'x-blink-source': REQUEST_SOURCE,
  'x-blink-clientIp': REQUEST_CLIENT_IP,
  'x-blink-locale': REQUEST_LOCALE,
})

/**
 * 获取默认请求体包装配置
 * 包含 token 用于后端认证
 */
export const getDefaultRequestBody = () => {
  const token = localStorage.getItem('token') || ''
  return {
    channel: REQUEST_CHANNEL,
    clientIp: REQUEST_CLIENT_IP,
    version: REQUEST_VERSION,
    token,
  }
}
