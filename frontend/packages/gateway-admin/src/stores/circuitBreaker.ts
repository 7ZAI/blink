import { defineStore } from 'pinia'
import { ref, computed, readonly } from 'vue'
import {
  circuitBreakerApi,
  type CircuitBreakerOverviewNew,
  type CircuitBreakerSummary,
  type InstanceSummary,
  type CircuitBreakerDetail,
  type StateTransitionHistory,
} from '@/api/circuitBreaker'
import {
  registerCircuitBreakerCallback,
  unregisterCircuitBreakerCallback,
} from '@/stores/notification'

/**
 * SSE 熔断器数据推送类型
 */
export interface CircuitBreakerDataPayload {
  circuitBreakers: CircuitBreakerSummary[]
  totalCircuitBreakers: number
  openCount: number
  closedCount: number
  halfOpenCount: number
  totalInstances: number
  healthScore: number
}

/**
 * CircuitBreaker Store - SSE 消息总线模式
 *
 * 职责：
 * 1. 从后端 API 获取初始数据
 * 2. 通过 SSE 实时接收熔断器数据更新
 * 3. 提供实例切换和详情查询能力
 *
 * @author binblink
 * @since 2026-04-16
 */
export const useCircuitBreakerStore = defineStore('circuitBreaker', () => {
  // ==================== 状态 ====================

  // 总览数据（全局共享）
  const overview = ref<CircuitBreakerOverviewNew | null>(null)

  // 实例列表
  const instances = ref<InstanceSummary[]>([])

  // 当前选中的实例ID（null 表示聚合视图）
  const selectedInstanceId = ref<string | null>(null)

  // 详情数据
  const detail = ref<CircuitBreakerDetail | null>(null)

  // 加载状态
  const loading = ref(false)
  const detailLoading = ref(false)

  // ==================== 计算属性 ====================

  // 是否有 OPEN 状态的熔断器
  const hasOpenCircuitBreaker = computed(() =>
    (overview.value?.openCount ?? 0) > 0
  )

  // 健康度等级
  const healthLevel = computed(() => {
    const score = overview.value?.healthScore ?? 100
    if (score >= 80) return 'success'
    if (score >= 60) return 'warning'
    return 'danger'
  })

  // ==================== SSE 数据更新回调 ====================

  /**
   * 处理 SSE 推送的熔断器数据
   */
  const handleCircuitBreakerData = (data: CircuitBreakerDataPayload) => {
    overview.value = {
      circuitBreakers: data.circuitBreakers,
      totalCircuitBreakers: data.totalCircuitBreakers,
      openCount: data.openCount,
      closedCount: data.closedCount,
      halfOpenCount: data.halfOpenCount,
      totalInstances: data.totalInstances,
      healthScore: data.healthScore,
    }
    console.log('[CircuitBreakerStore] SSE 数据更新 | healthScore:', data.healthScore)
  }

  // 注册 SSE 回调
  registerCircuitBreakerCallback(handleCircuitBreakerData)

  // ==================== 方法 ====================

  /**
   * 获取实例列表
   */
  const fetchInstances = async () => {
    try {
      instances.value = await circuitBreakerApi.getInstanceList()
    } catch (error) {
      console.error('[CircuitBreakerStore] 获取实例列表失败:', error)
    }
  }

  /**
   * 获取总览数据
   */
  const fetchOverview = async (instanceId?: string) => {
    loading.value = true
    try {
      overview.value = await circuitBreakerApi.getOverviewNew(instanceId ?? undefined)
    } catch (error) {
      console.error('[CircuitBreakerStore] 获取总览失败:', error)
    } finally {
      loading.value = false
    }
  }

  /**
   * 选择实例
   */
  const selectInstance = async (instanceId: string | null) => {
    selectedInstanceId.value = instanceId
    await fetchOverview(instanceId ?? undefined)
  }

  /**
   * 获取详情
   */
  const fetchDetail = async (name: string, instanceId?: string) => {
    detailLoading.value = true
    try {
      detail.value = await circuitBreakerApi.getDetail(name, instanceId)
    } catch (error) {
      console.error('[CircuitBreakerStore] 获取详情失败:', error)
    } finally {
      detailLoading.value = false
    }
  }

  /**
   * 获取状态转换历史
   */
  const fetchHistory = async (instanceId: string, name: string, limit?: number) => {
    try {
      return await circuitBreakerApi.getHistory(instanceId, name, limit)
    } catch (error) {
      console.error('[CircuitBreakerStore] 获取历史失败:', error)
      return []
    }
  }

  /**
   * 重置状态
   */
  const reset = () => {
    overview.value = null
    instances.value = []
    selectedInstanceId.value = null
    detail.value = null
  }

  /**
   * 清理资源（组件卸载时调用）
   */
  const dispose = () => {
    unregisterCircuitBreakerCallback(handleCircuitBreakerData)
  }

  return {
    // 状态
    overview: readonly(overview),
    instances: readonly(instances),
    selectedInstanceId: readonly(selectedInstanceId),
    detail: readonly(detail),
    loading: readonly(loading),
    detailLoading: readonly(detailLoading),

    // 计算属性
    hasOpenCircuitBreaker,
    healthLevel,

    // 方法
    fetchInstances,
    fetchOverview,
    selectInstance,
    fetchDetail,
    fetchHistory,
    reset,
    dispose,
    handleCircuitBreakerData,
  }
})
