import { ref, onUnmounted } from 'vue'
import { fetchEventSource } from '@microsoft/fetch-event-source'
import { ElMessage } from 'element-plus'

interface SseOptions {
  url: string
  token?: string
  onMessage: (data: any) => void
  onError?: (error: Error) => void
  onConnect?: () => void
  onDisconnect?: () => void
  maxRetries?: number
  retryDelay?: number
}

interface SseConnection {
  status: 'connecting' | 'connected' | 'disconnected' | 'error'
  connect: () => void
  disconnect: () => void
}

/**
 * SSE 连接管理
 *
 * 连接标识策略（后端实现）：
 * - connectionKey = userId:token
 * - 相同 token 的重连请求会替换旧连接
 * - 多设备登录：不同 token = 不同连接
 *
 * 前端职责：
 * - 携带 token 请求头
 * - 断开时 abort 连接
 * - 后端会自动处理连接替换，前端无需主动通知
 */
export function useSseConnection(options: SseOptions): SseConnection {
  const {
    url,
    token,
    onMessage,
    onError,
    onConnect,
    onDisconnect,
    maxRetries = 10,
    retryDelay = 1000,
  } = options

  const status = ref<SseConnection['status']>('disconnected')
  let abortController: AbortController | null = null
  let retryCount = 0
  let isManualDisconnect = false
  let hasShownError = false

  const connect = () => {
    if (abortController) {
      abortController.abort()
    }

    isManualDisconnect = false
    hasShownError = false
    retryCount = 0
    status.value = 'connecting'
    abortController = new AbortController()

    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    }

    // 携带 token，后端用 userId:token 作为连接标识
    if (token) {
      headers['x-blink-token'] = token
    }

    fetchEventSource(url, {
      method: 'POST',
      headers,
      body: JSON.stringify({}),
      signal: abortController.signal,

      onopen: async (response) => {
        if (response.ok) {
          status.value = 'connected'
          retryCount = 0
          console.log('[SSE] 连接成功')
          onConnect?.()
        } else if (response.status >= 400 && response.status < 500 && response.status !== 429) {
          status.value = 'error'
          onError?.(new Error(`HTTP ${response.status}`))
          onDisconnect?.()
        } else {
          throw new Error(`HTTP ${response.status}`)
        }
      },

      onmessage: (event) => {
        try {
          const data = JSON.parse(event.data)
          onMessage(data)
        } catch (e) {
          console.error('[SSE] 解析消息失败:', e)
        }
      },

      onerror: (error) => {
        if (isManualDisconnect) return

        status.value = 'error'

        if (retryCount < maxRetries) {
          const delay = Math.min(retryDelay * Math.pow(2, retryCount), 30000)
          retryCount++
          console.warn(`[SSE] 连接错误，${delay}ms后重试 (${retryCount}/${maxRetries})`)
          return delay
        } else {
          status.value = 'disconnected'
          console.error('[SSE] 重试次数已达上限')
          if (!hasShownError) {
            hasShownError = true
            ElMessage.warning('实时推送连接失败，请刷新页面')
          }
          onError?.(error instanceof Error ? error : new Error('SSE error'))
          onDisconnect?.()
        }
      },

      onclose: () => {
        if (!isManualDisconnect) {
          status.value = 'disconnected'
          onDisconnect?.()
        }
      },
    }).catch((error) => {
      if (!isManualDisconnect && status.value !== 'disconnected') {
        console.error('[SSE] 连接失败:', error)
        status.value = 'error'
        onError?.(error)
      }
    })
  }

  const disconnect = () => {
    isManualDisconnect = true

    if (abortController) {
      abortController.abort()
      abortController = null
    }

    status.value = 'disconnected'
    onDisconnect?.()
  }

  onUnmounted(() => {
    disconnect()
  })

  return {
    status: status.value as SseConnection['status'],
    connect,
    disconnect,
  }
}