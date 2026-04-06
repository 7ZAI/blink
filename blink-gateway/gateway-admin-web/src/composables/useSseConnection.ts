import { ref, onUnmounted } from 'vue'

interface SseOptions {
  url: string
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
    onMessage,
    onError,
    onConnect,
    onDisconnect,
    maxRetries = 10,
    retryDelay = 1000
  } = options

  const status = ref<SseConnection['status']>('disconnected')
  let eventSource: EventSource | null = null
  let retryCount = 0
  let retryTimer: ReturnType<typeof setTimeout> | null = null

  const connect = () => {
    if (eventSource) {
      eventSource.close()
    }

    status.value = 'connecting'

    // SSE需要使用GET请求，但我们需要传递token
    // 项目通过cookie传递token，所以直接连接即可
    eventSource = new EventSource(url)

    eventSource.onopen = () => {
      status.value = 'connected'
      retryCount = 0
      if (onConnect) {
        onConnect()
      }
    }

    eventSource.onerror = (error) => {
      status.value = 'error'
      if (eventSource) {
        eventSource.close()
        eventSource = null
      }

      if (onError) {
        onError(new Error('SSE connection error'))
      }

      // Exponential backoff retry
      if (retryCount < maxRetries) {
        const delay = Math.min(retryDelay * Math.pow(2, retryCount), 30000)
        retryCount++
        retryTimer = setTimeout(() => {
          connect()
        }, delay)
      } else {
        status.value = 'disconnected'
        if (onDisconnect) {
          onDisconnect()
        }
      }
    }

    eventSource.addEventListener('notification', (event) => {
      try {
        const data = JSON.parse(event.data)
        onMessage(data)
      } catch (e) {
        console.error('Failed to parse SSE message:', e)
      }
    })
  }

  const disconnect = () => {
    if (retryTimer) {
      clearTimeout(retryTimer)
      retryTimer = null
    }
    if (eventSource) {
      eventSource.close()
      eventSource = null
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