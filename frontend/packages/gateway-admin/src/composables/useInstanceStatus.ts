/**
 * 实例状态实时更新 Composable
 *
 * 通过 SSE 监听实例状态变化，替代轮询获取实例列表
 *
 * @example
 * ```ts
 * const { instances, stats, isConnected, connect, disconnect } = useInstanceStatus()
 *
 * // 在组件挂载时连接
 * onMounted(() => connect())
 *
 * // 在组件卸载时断开
 * onUnmounted(() => disconnect())
 * ```
 */

import { ref, onUnmounted, readonly } from 'vue'
import { useSseConnection } from './useSseConnection'
import { useUserStore } from '@/stores/user'

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
 * SSE 实例状态数据
 */
export interface InstanceStatusData {
  instances: InstanceSummary[]
  stats: InstanceStats
  hasChange: boolean
  changedInstanceIds?: string[]
}

/**
 * 实例状态变化回调
 */
export type InstanceStatusCallback = (data: InstanceStatusData) => void

/**
 * 默认统计值
 */
const DEFAULT_STATS: InstanceStats = {
  total: 0,
  online: 0,
  healthy: 0,
  avgCpuUsage: 0,
}

/**
 * 实例状态实时更新 Composable
 *
 * @param options 配置选项
 * @returns 实例状态和连接控制方法
 */
export function useInstanceStatus(options?: {
  /** 状态变化回调 */
  onStatusChange?: InstanceStatusCallback
  /** 连接成功回调 */
  onConnect?: () => void
  /** 断开连接回调 */
  onDisconnect?: () => void
  /** 连接错误回调 */
  onError?: (error: Error) => void
}) {
  const { onStatusChange, onConnect, onDisconnect, onError } = options || {}

  // 实例列表
  const instances = ref<InstanceSummary[]>([])

  // 统计数据
  const stats = ref<InstanceStats>({ ...DEFAULT_STATS })

  // 连接状态
  const isConnected = ref(false)

  // SSE 连接
  let sseConnection: ReturnType<typeof useSseConnection> | null = null

  /**
   * 处理 SSE 消息
   */
  const handleMessage = (data: unknown) => {
    // 处理不同类型的消息
    if (data && typeof data === 'object') {
      const message = data as Record<string, unknown>

      // 处理 instance_status 类型消息
      if (message.type === 'instance_status' && message.data) {
        const statusData = message.data as InstanceStatusData
        updateStatus(statusData)
        onStatusChange?.(statusData)
      }

      // 处理直接发送的 instance_status 数据（不带 type 包装）
      if ('instances' in data && 'stats' in data) {
        const statusData = data as InstanceStatusData
        updateStatus(statusData)
        onStatusChange?.(statusData)
      }
    }
  }

  /**
   * 更新实例状态
   */
  const updateStatus = (data: InstanceStatusData) => {
    if (data.hasChange || instances.value.length === 0) {
      // 有变化或首次加载，更新全部数据
      instances.value = data.instances || []
      stats.value = data.stats || { ...DEFAULT_STATS }
    } else if (data.changedInstanceIds?.length) {
      // 只更新变化的实例
      const changedIds = new Set(data.changedInstanceIds)
      instances.value = instances.value.map((instance) => {
        if (changedIds.has(instance.instanceId)) {
          const updated = data.instances.find((i) => i.instanceId === instance.instanceId)
          return updated || instance
        }
        return instance
      })
    }
  }

  /**
   * 建立 SSE 连接
   */
  const connect = () => {
    if (sseConnection) {
      sseConnection.disconnect()
    }

    const userStore = useUserStore()
    const token = userStore.token

    sseConnection = useSseConnection({
      url: '/api/gateway-admin/sse/connect',
      token: token || undefined,
      onMessage: handleMessage,
      onConnect: () => {
        isConnected.value = true
        onConnect?.()
      },
      onDisconnect: () => {
        isConnected.value = false
        onDisconnect?.()
      },
      onError: (error) => {
        isConnected.value = false
        onError?.(error)
      },
    })

    sseConnection.connect()
  }

  /**
   * 断开 SSE 连接
   */
  const disconnect = () => {
    if (sseConnection) {
      sseConnection.disconnect()
      sseConnection = null
    }
    isConnected.value = false
  }

  /**
   * 重置状态
   */
  const reset = () => {
    instances.value = []
    stats.value = { ...DEFAULT_STATS }
  }

  // 组件卸载时断开连接
  onUnmounted(() => {
    disconnect()
  })

  return {
    // 状态
    instances: readonly(instances),
    stats: readonly(stats),
    isConnected: readonly(isConnected),

    // 方法
    connect,
    disconnect,
    reset,
  }
}

export default useInstanceStatus
