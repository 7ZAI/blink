import { ref, onUnmounted } from 'vue'
import { fetchEventSource } from '@microsoft/fetch-event-source'

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

export function useSseConnection(options: SseOptions): SseConnection {
  const {
    url,
    token,
    onMessage,
    onError,
    onConnect,
    onDisconnect,
    maxRetries = 10,
    retryDelay = 1000
  } = options

  const status = ref<SseConnection['status']>('disconnected')
  let abortController: AbortController | null = null
  let retryCount = 0
  let retryTimer: ReturnType<typeof setTimeout> | null = null
  let isManualDisconnect = false

  const connect = () => {
    if (abortController) {
      abortController.abort()
    }

    isManualDisconnect = false
    status.value = 'connecting'
    abortController = new AbortController()

    const headers: Record<string, string> = {
      'Content-Type': 'application/json'
    }

    // 添加认证 header
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
          if (onConnect) {
            onConnect()
          }
        } else if (response.status >= 400 && response.status < 500 && response.status !== 429) {
          // 客户端错误，不重试
          const error = new Error(`HTTP ${response.status}`)
          status.value = 'error'
          if (onError) {
            onError(error)
          }
          if (onDisconnect) {
            onDisconnect()
          }
        } else {
          // 服务器错误，稍后重试
          throw new Error(`HTTP ${response.status}`)
        }
      },

      onmessage: (event) => {
        if (event.event === 'notification' || !event.event) {
          try {
            const data = JSON.parse(event.data)
            onMessage(data)
          } catch (e) {
            console.error('[SSE] Failed to parse message:', e)
          }
        }
      },

      onerror: (error) => {
        if (isManualDisconnect) {
          return
        }

        status.value = 'error'

        if (retryCount < maxRetries) {
          const delay = Math.min(retryDelay * Math.pow(2, retryCount), 30000)
          retryCount++
          console.warn(`[SSE] Connection error, retrying in ${delay}ms (attempt ${retryCount}/${maxRetries})`)
          return delay
        } else {
          status.value = 'disconnected'
          if (onError) {
            onError(error instanceof Error ? error : new Error('SSE connection error'))
          }
          if (onDisconnect) {
            onDisconnect()
          }
        }
      },

      onclose: () => {
        if (!isManualDisconnect) {
          status.value = 'disconnected'
          if (onDisconnect) {
            onDisconnect()
          }
        }
      }
    }).catch((error) => {
      if (!isManualDisconnect && status.value !== 'disconnected') {
        console.error('[SSE] Connection failed:', error)
        status.value = 'error'
        if (onError) {
          onError(error)
        }
      }
    })
  }

  const disconnect = () => {
    isManualDisconnect = true

    if (retryTimer) {
      clearTimeout(retryTimer)
      retryTimer = null
    }

    if (abortController) {
      abortController.abort()
      abortController = null
    }

    status.value = 'disconnected'
    if (onDisconnect) {
      onDisconnect()
    }
  }

  onUnmounted(() => {
    disconnect()
  })

  return {
    status: status.value as SseConnection['status'],
    connect,
    disconnect
  }
}