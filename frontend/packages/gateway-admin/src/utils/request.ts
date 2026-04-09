import { ref } from 'vue'
import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

const SUCCESS_CODE = 'BLINK0000'

// Offline state - exported for components to use
export const isOffline = ref(false)

const request: AxiosInstance = axios.create({
  baseURL: '/gateway-admin',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Request interceptor
request.interceptors.request.use(
  (config) => {
    // Add token if exists
    const token = localStorage.getItem('token') || sessionStorage.getItem('token')
    if (token) {
      config.headers['x-blink-token'] = token
    }

    // Add user info from localStorage (set by user store after login)
    const userInfoStr = localStorage.getItem('userInfo') || sessionStorage.getItem('userInfo')
    if (userInfoStr) {
      try {
        const userInfo = JSON.parse(userInfoStr)
        if (userInfo.userId) {
          config.headers['x-blink-usrId'] = String(userInfo.userId)
        }
        if (userInfo.loginName) {
          config.headers['x-blink-loginName'] = userInfo.loginName
        }
      } catch (e) {
        // ignore parse error
      }
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor
request.interceptors.response.use(
  (response: AxiosResponse) => {
    // Reset offline state on successful response
    isOffline.value = false
    const res = response.data
    if (res.msgCode === SUCCESS_CODE) {
      return res.body
    } else {
      ElMessage.error(res.msgInfo || 'Request failed')
      return Promise.reject(new Error(res.msgInfo || 'Request failed'))
    }
  },
  (error) => {
    // Network error detection (no response from server)
    if (!error.response) {
      isOffline.value = true
      ElMessage.error('网络连接失败，请检查网络')
      return Promise.reject(error)
    }

    // Reset offline state if we got a response
    isOffline.value = false

    let message = error.message || 'Network error'
    const status = error.response.status
    switch (status) {
      case 401:
        // Clear token from both localStorage and sessionStorage
        localStorage.removeItem('token')
        sessionStorage.removeItem('token')
        window.location.href = '/login'
        message = '未授权，请重新登录'
        break
      case 403:
        message = '禁止访问'
        break
      case 404:
        message = '资源不存在'
        break
      case 500:
      case 502:
      case 503:
      case 504:
        message = '服务器错误'
        break
      default:
        message = error.response.data?.msgInfo || error.message
    }
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request

/**
 * Create a request function with pre-configured path
 * @param url API path
 * @param data Request body
 * @param config Axios config
 */
export const createRequest = <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
  return request.post(url, { body: data }, config)
}
