/**
 * 实例状态实时更新 Composable
 *
 * 通过复用 notification store 的 SSE 连接监听实例状态变化
 * 遵循消息总线模式：SSE 连接由 MainLayout 统一管理，组件只监听数据
 *
 * @example
 * ```ts
 * const { instances, stats, isConnected } = useInstanceStatus()
 *
 * // SSE 连接由 MainLayout 统一管理，无需在组件中手动 connect/disconnect
 * ```
 */

import { ref, readonly, watch, onUnmounted } from 'vue'
import { useNotificationStore, registerInstanceStatusCallback, unregisterInstanceStatusCallback } from '@/stores/notification'

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

// 全局实例状态缓存（所有组件共享）
const globalInstances = ref<InstanceSummary[]>([])
const globalStats = ref<InstanceStats>({ ...DEFAULT_STATS })

/**
 * 实例状态实时更新 Composable
 *
 * 复用 notification store 的 SSE 连接，不再独立创建连接
 *
 * @param options 配置选项
 * @returns 实例状态
 */
export function useInstanceStatus(options?: {
  /** 状态变化回调 */
  onStatusChange?: InstanceStatusCallback
}) {
  const { onStatusChange } = options || {}

  // 使用全局缓存，避免每个组件重复创建状态
  const instances = globalInstances
  const stats = globalStats

  // 监听 notification store 的 SSE 状态
  const notificationStore = useNotificationStore()

  // 返回 SSE 连接状态（来自 notification store）
  const isConnected = readonly(ref(notificationStore.sseStatus === 'connected'))

  /**
   * 更新实例状态（由 SSE 消息触发）
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

    // 触发回调
    onStatusChange?.(data)
  }

  // 注册到 notification store 的回调列表
  registerInstanceStatusCallback(updateStatus)

  /**
   * 重置状态
   */
  const reset = () => {
    instances.value = []
    stats.value = { ...DEFAULT_STATS }
  }

  // 不再需要 connect/disconnect，由 notification store 统一管理
  // 提供空方法以保持向后兼容（如果有组件仍在调用）
  // 遵循消息总线模式：SSE 连接由 MainLayout 统一管理
  const connect = () => {
    // SSE 连接由 MainLayout 管理，组件不应直接操作
    // 如果 SSE 未连接，组件应该等待而不是主动连接
    console.warn('[useInstanceStatus] SSE 连接由 MainLayout 统一管理，组件不应调用 connect()')
  }

  const disconnect = () => {
    // 不主动断开连接，由 MainLayout 在退出时统一断开
    console.warn('[useInstanceStatus] SSE 连接由 MainLayout 统一管理，组件不应调用 disconnect()')
  }

  // 组件卸载时移除回调注册
  onUnmounted(() => {
    unregisterInstanceStatusCallback(updateStatus)
  })

  return {
    // 状态
    instances: readonly(instances),
    stats: readonly(stats),
    isConnected,

    // 方法（向后兼容，但不再独立管理连接）
    connect,
    disconnect,
    reset,
    updateStatus,
  }
}

export default useInstanceStatus
