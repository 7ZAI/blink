/**
 * 网关请求工具
 * 支持加密模式和非加密模式
 * 整合签名、加密、解密功能
 */

import axios, {
  type AxiosInstance,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import { ElMessage } from 'element-plus'
import {
  generateTimestamp,
  generateUUID,
  HMACUtil,
  encryptRequest,
  decryptResponse,
} from '@/utils/crypto'
import { channelConfig, HEADER_CONSTANTS } from '@/config/channel'
import { t, getErrorMessage, getCurrentLocale } from '@/config/i18n.config'
import { getDefaultRequestHeaders } from '@/config/request.config'
import type { ApiResponse } from '@/types'

const SUCCESS_CODE = 'BLINK0000'

/**
 * 网关请求配置
 */
interface GatewayRequestOptions {
  /** 是否启用加密 */
  enableEncryption?: boolean
  /** 是否启用签名 */
  enableSignature?: boolean
  /** 自定义基础URL */
  baseURL?: string
  /** 超时时间 */
  timeout?: number
}

/**
 * 默认网关请求配置
 */
const defaultOptions: GatewayRequestOptions = {
  enableEncryption: channelConfig.encryptionEnabled,
  enableSignature: true,
  baseURL: '/base',
  timeout: 30000,
}

/**
 * 创建网关请求实例
 * @param options 请求配置选项
 * @returns Axios实例
 */
export function createGatewayRequest(options: GatewayRequestOptions = {}): AxiosInstance {
  const config = { ...defaultOptions, ...options }

  const instance = axios.create({
    baseURL: config.baseURL,
    timeout: config.timeout,
    headers: {
      'Content-Type': 'application/json',
    },
  })

  // 请求拦截器
  instance.interceptors.request.use(
    async (requestConfig: InternalAxiosRequestConfig) => {
      // 生成基础参数
      const timestamp = generateTimestamp()
      const nonce = generateUUID()
      const token = localStorage.getItem('token') || ''

      // 设置公共请求头
      requestConfig.headers[HEADER_CONSTANTS.X_BLINK_APPKEY] = channelConfig.appKey
      requestConfig.headers[HEADER_CONSTANTS.X_BLINK_TIMESTAMP] = timestamp
      requestConfig.headers[HEADER_CONSTANTS.X_BLINK_NONCE] = nonce
      requestConfig.headers[HEADER_CONSTANTS.X_BLINK_LOCALE] = getCurrentLocale()

      // 设置token
      if (token) {
        requestConfig.headers[HEADER_CONSTANTS.X_BLINK_TOKEN] = token
      }

      // 合并默认请求头
      const defaultHeaders = getDefaultRequestHeaders()
      Object.entries(defaultHeaders).forEach(([key, value]) => {
        if (!requestConfig.headers[key]) {
          requestConfig.headers[key] = value
        }
      })

      // 获取请求体
      const body = requestConfig.data ? JSON.stringify(requestConfig.data) : ''

      // 加密模式
      if (config.enableEncryption && body) {
        const { encryptedBody, encryptedKey, iv } = await encryptRequest(
          body,
          channelConfig.systemPublicKey
        )

        requestConfig.data = encryptedBody
        requestConfig.headers[HEADER_CONSTANTS.X_BLINK_KEY] = encryptedKey
        requestConfig.headers[HEADER_CONSTANTS.X_BLINK_IV] = iv

        // 加密模式下签名数据为空
        const sign = await HMACUtil.sign('', channelConfig.appSecret, {
          timestamp,
          nonce,
          appKey: channelConfig.appKey,
        })
        requestConfig.headers[HEADER_CONSTANTS.X_BLINK_SIGN] = sign
      }
      // 非加密模式（仅签名）
      else if (config.enableSignature) {
        const sign = await HMACUtil.sign(body, channelConfig.appSecret, {
          timestamp,
          nonce,
          appKey: channelConfig.appKey,
        })
        requestConfig.headers[HEADER_CONSTANTS.X_BLINK_SIGN] = sign
      }

      return requestConfig
    },
    (error) => {
      return Promise.reject(error)
    }
  )

  // 响应拦截器
  instance.interceptors.response.use(
    async (response: AxiosResponse) => {
      const encryptedKey = response.headers[HEADER_CONSTANTS.X_BLINK_KEY]
      const iv = response.headers[HEADER_CONSTANTS.X_BLINK_IV]

      let data = response.data

      // 解密响应
      if (config.enableEncryption && encryptedKey && iv) {
        try {
          if (typeof data === 'string') {
            data = await decryptResponse(data, encryptedKey, iv, channelConfig.channelPrivateKey)
            data = JSON.parse(data)
          }
        } catch (e) {
          console.error('Response decryption failed:', e)
          ElMessage.error('响应解密失败')
          return Promise.reject(new Error('响应解密失败'))
        }
      }

      const res = data as ApiResponse

      // 业务错误处理
      if (res.msgCode !== SUCCESS_CODE) {
        const errorMessage = getErrorMessage(res.msgCode, res.msgInfo)
        ElMessage.error(errorMessage)
        return Promise.reject(new Error(errorMessage))
      }

      return res.body
    },
    (error) => {
      let message = t('common.networkError') || '网络错误'

      if (error.response) {
        const status = error.response.status
        const data = error.response.data

        switch (status) {
          case 400:
            message = data?.msgInfo || t('message.failed') || '请求失败'
            break
          case 401:
            message = t('common.unauthorized') || '未授权'
            localStorage.removeItem('token')
            window.location.href = '/login'
            break
          case 403:
            message = t('common.forbidden') || '禁止访问'
            break
          case 404:
            message = t('common.notFound') || '资源不存在'
            break
          case 408:
          case 'ECONNABORTED':
            message = t('common.timeout') || '请求超时'
            break
          case 500:
          case 502:
          case 503:
          case 504:
            message = t('common.serverError') || '服务器错误'
            break
          default:
            message = data?.msgInfo || error.message || t('common.networkError') || '网络错误'
        }
      } else if (error.message.includes('timeout')) {
        message = t('common.timeout') || '请求超时'
      }

      ElMessage.error(message)
      return Promise.reject(error)
    }
  )

  return instance
}

// 默认网关请求实例（加密模式）
const gatewayRequest = createGatewayRequest()

// 非加密模式的网关请求实例
const gatewayRequestNoEncryption = createGatewayRequest({
  enableEncryption: false,
  enableSignature: true,
})

export { gatewayRequest, gatewayRequestNoEncryption }
export default gatewayRequest
