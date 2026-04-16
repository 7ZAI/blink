/**
 * 熔断器数据管理 Composable
 *
 * 封装熔断器 Store 的常用操作，提供便捷的数据访问
 *
 * @example
 * ```ts
 * const { overview, instances, selectInstance, loading } = useCircuitBreaker()
 * ```
 */

import { onMounted, onUnmounted } from 'vue'
import { useCircuitBreakerStore } from '@/stores/circuitBreaker'

/**
 * 熔断器数据管理 Composable
 */
export function useCircuitBreaker() {
  const store = useCircuitBreakerStore()

  /**
   * 初始化数据
   */
  const init = async () => {
    await Promise.all([
      store.fetchInstances(),
      store.fetchOverview(),
    ])
  }

  /**
   * 刷新数据
   */
  const refresh = async () => {
    await store.fetchOverview(store.selectedInstanceId ?? undefined)
  }

  /**
   * 选择实例
   */
  const selectInstance = async (instanceId: string | null) => {
    await store.selectInstance(instanceId)
  }

  /**
   * 获取详情
   */
  const fetchDetail = async (name: string) => {
    await store.fetchDetail(name, store.selectedInstanceId ?? undefined)
  }

  /**
   * 获取历史
   */
  const fetchHistory = async (instanceId: string, name: string, limit?: number) => {
    return store.fetchHistory(instanceId, name, limit)
  }

  // 组件挂载时初始化
  onMounted(() => {
    if (!store.overview) {
      init()
    }
  })

  // 组件卸载时清理
  onUnmounted(() => {
    store.dispose()
  })

  return {
    // 状态
    overview: store.overview,
    instances: store.instances,
    selectedInstanceId: store.selectedInstanceId,
    detail: store.detail,
    loading: store.loading,
    detailLoading: store.detailLoading,

    // 计算属性
    hasOpenCircuitBreaker: store.hasOpenCircuitBreaker,
    healthLevel: store.healthLevel,

    // 方法
    init,
    refresh,
    selectInstance,
    fetchDetail,
    fetchHistory,
    reset: store.reset,
  }
}

export default useCircuitBreaker
