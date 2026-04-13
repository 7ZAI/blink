/**
 * SSE (Server-Sent Events) 连接管理 Composable
 *
 * 提供 SSE 连接的创建、事件监听、自动重连等功能
 *
 * @example
 * ```ts
 * const { connect, disconnect, on, isConnected } = useSse()
 *
 * // 建立连接
 * connect()
 *
 * // 监听事件
 * on('heartbeat', (data) => console.log('心跳', data))
 * on('notification', (data) => console.log('通知', data))
 * on('instance_status', (data) => console.log('实例状态', data))
 *
 * // 断开连接
 * disconnect()
 * ```
 */

import { ref, onUnmounted, readonly } from 'vue'

/**
 * SSE 事件类型
 */
export type SseEventType = 'heartbeat' | 'notification' | 'instance_status' | 'error'

/**
 * SSE 消息基础结构
 */
export interface SseMessage<T = unknown> {
  type: SseEventType
  data: T
  timestamp: number
}

/**
 * 心跳消息数据
 */
export interface HeartbeatData {
  timestamp: number
}

/**
 * 通知消息数据
 */
export interface NotificationData {
  title: string
  content: string
  severity: 'info' | 'warning' | 'error' | 'success'
  createdTime: string
}

/**
 * 实例状态数据
 */
export interface InstanceStatusData {
  instances: InstanceSummary[]
  stats: InstanceStats
  hasChange: boolean
  changedInstanceIds?: string[]
}

/**
 * 实例摘要
 */
export interface InstanceSummary {
  instanceId: string
  status: number
  healthStatus: string
  cpuUsage?: number
  heapUsagePercent?: number
  timestamp?: number
}

/**
 * 实例统计
 */
export interface InstanceStats {
  total: number
  online: number
  healthy: number
  avgCpuUsage: number
}

/**
 * SSE 连接配置
 */
export interface SseOptions {
  /** SSE 端点 URL */
  url?: string
  /** 自动重连 */
  autoReconnect?: boolean
  /** 重连间隔（毫秒） */
  reconnectInterval?: number
  /** 最大重连次数 */
  maxReconnectAttempts?: number
}

/**
 * 默认配置
 */
const DEFAULT_OPTIONS: Required<SseOptions> = {
  url: '/api/gateway-admin/sse/connect',
  autoReconnect: true,
  reconnectInterval: 5000,
  maxReconnectAttempts: 5,
}

/**
 * SSE 连接管理 Composable
 *
 * @param options SSE 配置选项
 * @returns SSE 连接管理方法和状态
 */
export function useSse(options: SseOptions = {}) {
  const config = { ...DEFAULT_OPTIONS, ...options }

  // 连接状态
  const isConnected = ref(false)
  const isConnecting = ref(false)
  const reconnectAttempts = ref(0)

  // EventSource 实例
  let eventSource: EventSource | null = null

  // 事件监听器映射
  const eventListeners = new Map<SseEventType, Set<(data: unknown) => void>>()

  // 重连定时器
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null

  /**
   * 处理连接打开事件
   */
  const handleOpen = () => {
    isConnected.value = true
    isConnecting.value = false
    reconnectAttempts.value = 0
    triggerListeners('heartbeat', { timestamp: Date.now() })
  }

  /**
   * 处理消息事件
   */
  const handleMessage = (event: MessageEvent) => {
    try {
      const message: SseMessage = JSON.parse(event.data)
      triggerListeners(message.type as SseEventType, message.data)
    } catch (error) {
      console.error('[SSE] 解析消息失败:', error)
    }
  }

  /**
   * 处理错误事件
   */
  const handleError = (error: Event) => {
    console.error('[SSE] 连接错误:', error)
    isConnected.value = false
    isConnecting.value = false

    triggerListeners('error', { error })

    // 自动重连
    if (config.autoReconnect && reconnectAttempts.value < config.maxReconnectAttempts) {
      scheduleReconnect()
    }
  }

  /**
   * 触发事件监听器
   */
  const triggerListeners = (type: SseEventType, data: unknown) => {
    const listeners = eventListeners.get(type)
    if (listeners) {
      listeners.forEach((listener) => {
        try {
          listener(data)
        } catch (error) {
          console.error(`[SSE] 事件处理器错误 (${type}):`, error)
        }
      })
    }
  }

  /**
   * 计划重连
   */
  const scheduleReconnect = () => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
    }

    reconnectAttempts.value++
    console.log(`[SSE] 计划重连 (${reconnectAttempts.value}/${config.maxReconnectAttempts})`)

    reconnectTimer = setTimeout(() => {
      connect()
    }, config.reconnectInterval)
  }

  /**
   * 建立 SSE 连接
   */
  const connect = () => {
    if (eventSource) {
      eventSource.close()
    }

    isConnecting.value = true

    try {
      eventSource = new EventSource(config.url)

      eventSource.onopen = handleOpen
      eventSource.onmessage = handleMessage
      eventSource.onerror = handleError

      // 监听特定事件类型
      eventSource.addEventListener('heartbeat', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data)
          triggerListeners('heartbeat', data)
        } catch {
          triggerListeners('heartbeat', { timestamp: Date.now() })
        }
      })

      eventSource.addEventListener('notification', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data)
          triggerListeners('notification', data)
        } catch (error) {
          console.error('[SSE] 解析通知消息失败:', error)
        }
      })

      eventSource.addEventListener('instance_status', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data)
          triggerListeners('instance_status', data)
        } catch (error) {
          console.error('[SSE] 解析实例状态消息失败:', error)
        }
      })
    } catch (error) {
      console.error('[SSE] 创建连接失败:', error)
      isConnecting.value = false
      isConnected.value = false
    }
  }

  /**
   * 断开 SSE 连接
   */
  const disconnect = () => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }

    if (eventSource) {
      eventSource.close()
      eventSource = null
    }

    isConnected.value = false
    isConnecting.value = false
    reconnectAttempts.value = 0
  }

  /**
   * 注册事件监听器
   *
   * @param type 事件类型
   * @param callback 回调函数
   */
  const on = <T = unknown>(type: SseEventType, callback: (data: T) => void) => {
    if (!eventListeners.has(type)) {
      eventListeners.set(type, new Set())
    }
    eventListeners.get(type)!.add(callback as (data: unknown) => void)

    // 返回取消监听函数
    return () => {
      const listeners = eventListeners.get(type)
      if (listeners) {
        listeners.delete(callback as (data: unknown) => void)
      }
    }
  }

  /**
   * 移除事件监听器
   *
   * @param type 事件类型
   * @param callback 回调函数
   */
  const off = <T = unknown>(type: SseEventType, callback: (data: T) => void) => {
    const listeners = eventListeners.get(type)
    if (listeners) {
      listeners.delete(callback as (data: unknown) => void)
    }
  }

  /**
   * 手动触发重连
   */
  const reconnect = () => {
    disconnect()
    reconnectAttempts.value = 0
    connect()
  }

  // 组件卸载时清理
  onUnmounted(() => {
    disconnect()
    eventListeners.clear()
  })

  return {
    // 状态
    isConnected: readonly(isConnected),
    isConnecting: readonly(isConnecting),
    reconnectAttempts: readonly(reconnectAttempts),

    // 方法
    connect,
    disconnect,
    reconnect,
    on,
    off,
  }
}

export default useSse
