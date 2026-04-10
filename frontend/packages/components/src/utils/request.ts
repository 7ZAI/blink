import axios, {
  type AxiosInstance,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
  type AxiosRequestConfig,
} from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types'
import { getBaseUrl, getCurrentModeName, isGatewayMode } from '@/config/api.config'
import { getDefaultRequestHeaders } from '@/config/request.config'
import { t, getErrorMessage, getCurrentLocale } from '@/config/i18n.config'
import {
  generateTimestamp,
  generateUUID,
  HMACUtil,
  encryptRequest,
  decryptResponse,
} from '@/utils/crypto'
import { channelConfig, HEADER_CONSTANTS } from '@/config/channel'
import { useUserStore } from '@/stores/user'

const SUCCESS_CODE = 'BLINK0000'

const ALLOWED_METHODS = ['post', 'POST']
const ALLOWED_CONTENT_TYPES = ['application/json', 'multipart/form-data']

const baseConfig: AxiosRequestConfig = {
  baseURL: getBaseUrl(),
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
}

const request: AxiosInstance = axios.create(baseConfig)

if (import.meta.hot) {
  import.meta.hot.accept('/src/config/api.config', () => {
    request.defaults.baseURL = getBaseUrl()
    console.log(`[Request] 配置已更新，当前模式: ${getCurrentModeName()}`)
  })
}

/**
 * 将标准Base64转换为URL安全Base64
 * + -> - , / -> _ , 去掉 = 填充
 */
function base64ToUrlSafe(base64: string): string {
  return base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

/**
 * 将URL安全Base64还原为标准Base64
 * - -> + , _ -> / , 补齐 = 填充
 */
function urlSafeToBase64(urlSafe: string): string {
  let base64 = urlSafe.replace(/-/g, '+').replace(/_/g, '/')
  const pad = base64.length % 4
  if (pad) {
    base64 += '='.repeat(4 - pad)
  }
  return base64
}

request.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    const method = config.method?.toLowerCase() || 'get'
    const contentType =
      config.headers['Content-Type'] || config.headers['content-type'] || 'application/json'

    if (!ALLOWED_METHODS.includes(method)) {
      const error = new Error(t('common.methodNotAllowed'))
      ElMessage.error(error.message)
      return Promise.reject(error)
    }

    const isAllowedContentType = ALLOWED_CONTENT_TYPES.some((ct) =>
      contentType.toLowerCase().includes(ct.toLowerCase())
    )

    if (!isAllowedContentType) {
      const error = new Error(t('common.contentTypeNotAllowed'))
      ElMessage.error(error.message)
      return Promise.reject(error)
    }

    const token = localStorage.getItem('token') || ''
    const timestamp = generateTimestamp()
    const nonce = generateUUID()

    config.headers[HEADER_CONSTANTS.X_BLINK_APPKEY] = channelConfig.appKey
    config.headers[HEADER_CONSTANTS.X_BLINK_TIMESTAMP] = timestamp
    config.headers[HEADER_CONSTANTS.X_BLINK_NONCE] = nonce
    config.headers[HEADER_CONSTANTS.X_BLINK_LOCALE] = getCurrentLocale()

    config.headers['Accept'] = 'application/json'

    if (token) {
      config.headers[HEADER_CONSTANTS.X_BLINK_TOKEN] = token
    }

    const defaultHeaders = getDefaultRequestHeaders()
    Object.entries(defaultHeaders).forEach(([key, value]) => {
      if (!config.headers[key]) {
        config.headers[key] = value
      }
    })

    if (!isGatewayMode()) {
      // 直连模式下添加用户信息请求头，让 BlinkRequestContextHolder 能够获取值
      const userStore = useUserStore()
      if (userStore.userInfo) {
        config.headers[HEADER_CONSTANTS.X_BLINK_USER_ID] = String(userStore.userInfo.userId)
        config.headers[HEADER_CONSTANTS.X_BLINK_LOGIN_NAME] = userStore.userInfo.loginName
      }
      return config
    }

    if (!config.data) {
      config.data = {}
    }

    const body = typeof config.data === 'string' ? config.data : JSON.stringify(config.data)

    if (channelConfig.encryptionEnabled && body) {
      const { encryptedBody, encryptedKey, iv } = await encryptRequest(
        body,
        channelConfig.systemPublicKey
      )

      config.data = encryptedBody
      // 使用 URL 安全 Base64 编码，避免 + 和 / 在 HTTP 头中被转义
      config.headers[HEADER_CONSTANTS.X_BLINK_KEY] = base64ToUrlSafe(encryptedKey)
      config.headers[HEADER_CONSTANTS.X_BLINK_IV] = base64ToUrlSafe(iv)
    }

    const sign = await HMACUtil.sign(body, channelConfig.appSecret, {
      timestamp,
      nonce,
      appKey: channelConfig.appKey,
    })
    config.headers[HEADER_CONSTANTS.X_BLINK_SIGN] = sign

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  async (response: AxiosResponse) => {
    if (!isGatewayMode()) {
      const res = response.data as ApiResponse
      if (res.msgCode !== SUCCESS_CODE) {
        const errorMessage = getErrorMessage(res.msgCode, res.msgInfo)
        ElMessage.error(errorMessage)
        return Promise.reject(new Error(errorMessage))
      }
      return res.body
    }

    let encryptedKey = response.headers[HEADER_CONSTANTS.X_BLINK_KEY]
    let iv = response.headers[HEADER_CONSTANTS.X_BLINK_IV]

    let data = response.data

    if (channelConfig.encryptionEnabled && encryptedKey && iv) {
      try {
        // 将 URL 安全 Base64 还原为标准 Base64
        encryptedKey = urlSafeToBase64(encryptedKey)
        iv = urlSafeToBase64(iv)

        if (typeof data === 'string') {
          data = await decryptResponse(data, encryptedKey, iv, channelConfig.channelPrivateKey)
          data = JSON.parse(data)
        }
      } catch (e) {
        console.error('Response decryption failed:', e)
        ElMessage.error(t('common.responseDecryptFailed'))
        return Promise.reject(new Error(t('common.responseDecryptFailed')))
      }
    }

    const res = data as ApiResponse

    if (res.msgCode !== SUCCESS_CODE) {
      const errorMessage = getErrorMessage(res.msgCode, res.msgInfo)
      ElMessage.error(errorMessage)
      return Promise.reject(new Error(errorMessage))
    }

    return res.body
  },
  (error) => {
    let message = t('common.networkError')

    if (error.response) {
      const status = error.response.status
      const data = error.response.data

      switch (status) {
        case 400:
          message = data?.msgInfo || t('message.failed')
          break
        case 401:
          message = t('common.unauthorized')
          localStorage.removeItem('token')
          window.location.href = '/login'
          break
        case 403:
          message = t('common.forbidden')
          break
        case 404:
          message = t('common.notFound')
          break
        case 408:
        case 'ECONNABORTED':
          message = t('common.timeout')
          break
        case 500:
        case 502:
        case 503:
        case 504:
          message = t('common.serverError')
          break
        default:
          message = data?.msgInfo || error.message || t('common.networkError')
      }
    } else if (error.message.includes('timeout')) {
      message = t('common.timeout')
    }

    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request
