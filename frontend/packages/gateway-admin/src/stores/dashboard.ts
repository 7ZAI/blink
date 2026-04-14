import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { monitorApi, type InstanceInfo as MonitorInstanceInfo, type TrafficHistoryPoint } from '@/api/monitor'

/**
 * 统计信息类型
 */
export interface StatisticsInfo {
  totalInstances: number
  healthyInstances: number
  totalRequests: number
  successRequests: number
  failedRequests: number
  avgResponseTime: number
  successRate?: string
  // 响应时间分布
  p50ResponseTime: number
  p95ResponseTime: number
  p99ResponseTime: number
  maxResponseTime: number
  // 错误分类
  error4xxCount: number
  error5xxCount: number
  errorRate: number
  // 实时 QPS
  currentQps: number
}

/**
 * 实例信息类型（从 API 返回）
 */
export interface InstanceInfo {
  instanceId: string
  serviceId: string
  host: string
  port: number
  uri?: string
  status: number
  statusDesc: string
  healthy: boolean
  onlineTime?: string
  offlineTime?: string
  lastHeartbeat?: string
}

/**
 * 流量数据点类型（SSE 实时推送）
 */
export interface TrafficPoint {
  time: string
  count: number
  timestamp: number
}

/**
 * 仪表盘数据载荷类型（SSE 推送的数据结构）
 */
export interface DashboardDataPayload {
  statistics: StatisticsInfo
  instances: InstanceInfo[]
  latestTraffic: TrafficPoint
  timestamp: number
}

/**
 * Dashboard Store
 * 管理仪表盘数据
 * - 统计信息和流量历史：通过 SSE 推送更新 + API 历史查询
 * - 实例列表：通过 API 主动查询（更准确、实时）
 */
export const useDashboardStore = defineStore('dashboard', () => {
  // 统计信息（由 SSE 推送）
  const statistics = ref<StatisticsInfo>({
    totalInstances: 0,
    healthyInstances: 0,
    totalRequests: 0,
    successRequests: 0,
    failedRequests: 0,
    avgResponseTime: 0,
    successRate: '0%',
    // 响应时间分布
    p50ResponseTime: 0,
    p95ResponseTime: 0,
    p99ResponseTime: 0,
    maxResponseTime: 0,
    // 错误分类
    error4xxCount: 0,
    error5xxCount: 0,
    errorRate: 0,
    // 实时 QPS
    currentQps: 0,
  })

  // 实例列表（由 API 查询）
  const instances = ref<InstanceInfo[]>([])
  const instancesLoading = ref(false)

  // ==================== 流量历史相关状态 ====================

  /**
   * 流量历史数据（支持 API 历史查询 + SSE 实时追加）
   */
  const trafficHistory = ref<TrafficHistoryPoint[]>([])

  /**
   * 流量历史加载状态
   */
  const trafficHistoryLoading = ref(false)

  /**
   * 流量历史加载错误信息
   */
  const trafficHistoryError = ref<string | null>(null)

  /**
   * 当前时间范围（用于历史查询）
   * 为空时表示显示实时 SSE 数据
   */
  const trafficTimeRange = ref<{ startTime?: number; endTime?: number }>({})

  /**
   * 当前数据粒度
   */
  const trafficGranularity = ref<'MINUTE' | 'HOUR'>('MINUTE')

  // SSE 连接状态
  const sseConnected = ref(false)

  // 最后更新时间
  const lastUpdateTime = ref<number>(0)

  /**
   * 从 API 获取实例列表
   * 实例数据来自实例管理服务，更准确可靠
   */
  const fetchInstances = async () => {
    instancesLoading.value = true
    try {
      const result = await monitorApi.getInstanceList()
      // 转换 API 返回的数据格式
      instances.value = (result.instances || []).map((item) => ({
        instanceId: item.instanceId,
        serviceId: item.serviceId,
        host: item.host,
        port: item.port,
        uri: item.uri,
        status: item.status,
        statusDesc: item.statusDesc,
        healthy: item.healthy ?? item.status === 0,
        onlineTime: item.onlineTime,
        offlineTime: item.offlineTime,
        lastHeartbeat: item.lastHeartbeat,
      }))
      console.log('[DashboardStore] 实例列表已更新 | 数量: {}', instances.value.length)
    } catch (error) {
      console.error('[DashboardStore] 获取实例列表失败:', error)
      instances.value = []
    } finally {
      instancesLoading.value = false
    }
  }

  /**
   * 加载流量历史数据（通过 API 查询）
   * @param options 可选参数覆盖当前状态
   */
  const loadTrafficHistory = async (options?: {
    startTime?: number
    endTime?: number
    granularity?: 'MINUTE' | 'HOUR'
  }) => {
    trafficHistoryLoading.value = true
    trafficHistoryError.value = null

    try {
      const params = {
        startTime: options?.startTime ?? trafficTimeRange.value.startTime,
        endTime: options?.endTime ?? trafficTimeRange.value.endTime,
        granularity: options?.granularity ?? trafficGranularity.value,
      }

      const result = await monitorApi.getTrafficHistory(params)
      trafficHistory.value = result.points || []

      // 更新时间范围记录（用于后续 SSE 是否追加判断）
      if (params.startTime !== undefined) {
        trafficTimeRange.value.startTime = params.startTime
      }
      if (params.endTime !== undefined) {
        trafficTimeRange.value.endTime = params.endTime
      }
      if (params.granularity) {
        trafficGranularity.value = params.granularity as 'MINUTE' | 'HOUR'
      }

      console.log('[DashboardStore] 流量历史已加载 | points: {}, granularity: {}', trafficHistory.value.length, params.granularity)
    } catch (error) {
      trafficHistoryError.value = '加载失败'
      console.error('[DashboardStore] 加载流量历史失败:', error)
      trafficHistory.value = []
    } finally {
      trafficHistoryLoading.value = false
    }
  }

  /**
   * 处理 SSE 推送的仪表盘数据
   * 注意：实例列表不再由 SSE 更新，通过 fetchInstances() API 获取
   */
  const handleDashboardData = (data: DashboardDataPayload) => {
    if (!data) {
      return
    }

    // 更新统计信息
    statistics.value = data.statistics

    // 实例列表不再由 SSE 更新，保持 API 查询的数据
    // instances.value = data.instances || []

    // SSE 实时流量数据追加（仅当粒度为 MINUTE 且无固定时间范围时）
    // 当用户选择了特定时间范围时，不再追加实时数据，避免数据混乱
    if (
      data.latestTraffic &&
      trafficGranularity.value === 'MINUTE' &&
      !trafficTimeRange.value.startTime
    ) {
      const newPoint: TrafficHistoryPoint = {
        time: data.latestTraffic.time,
        timestamp: data.latestTraffic.timestamp,
        count: data.latestTraffic.count,
        successCount: 0,
        failedCount: 0,
        peakQps: 0,
      }
      trafficHistory.value.push(newPoint)

      // 保留最近 60 条数据（约 1 小时分钟级数据）
      if (trafficHistory.value.length > 60) {
        trafficHistory.value.shift()
      }
    }

    // 记录更新时间
    lastUpdateTime.value = data.timestamp || Date.now()

    console.log('[DashboardStore] 统计数据已更新 | 总请求: {}', statistics.value.totalRequests)
  }

  /**
   * 设置数据粒度并重新加载
   */
  const setGranularity = (granularity: 'MINUTE' | 'HOUR') => {
    trafficGranularity.value = granularity
    // 切换粒度时重新加载历史数据
    loadTrafficHistory({ granularity })
  }

  /**
   * 设置时间范围并重新加载
   * @param range 时间范围，为空时清除范围（显示实时数据）
   */
  const setTimeRange = (range: { startTime?: number; endTime?: number }) => {
    trafficTimeRange.value = range

    if (range.startTime || range.endTime) {
      // 有时间范围时，加载历史数据
      loadTrafficHistory(range)
    } else {
      // 清除时间范围时，加载默认历史数据（最近 1 小时）
      loadTrafficHistory({ granularity: trafficGranularity.value })
    }
  }

  /**
   * 格式化数字（添加千位分隔符）
   */
  const formatNumber = (num: number): string => {
    if (!num) return '0'
    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  }

  /**
   * 计算成功率（用于显示）
   */
  const successRateDisplay = computed(() => {
    if (!statistics.value.totalRequests || statistics.value.totalRequests === 0) {
      return '0%'
    }
    return statistics.value.successRate ||
      Math.round((statistics.value.successRequests / statistics.value.totalRequests) * 100) + '%'
  })

  /**
   * 计算健康实例比例（用于显示）
   */
  const healthyRatioDisplay = computed(() => {
    if (!statistics.value.totalInstances || statistics.value.totalInstances === 0) {
      return '0/0'
    }
    return `${statistics.value.healthyInstances}/${statistics.value.totalInstances}`
  })

  /**
   * 清空数据
   */
  const clearData = () => {
    statistics.value = {
      totalInstances: 0,
      healthyInstances: 0,
      totalRequests: 0,
      successRequests: 0,
      failedRequests: 0,
      avgResponseTime: 0,
      successRate: '0%',
      // 响应时间分布
      p50ResponseTime: 0,
      p95ResponseTime: 0,
      p99ResponseTime: 0,
      maxResponseTime: 0,
      // 错误分类
      error4xxCount: 0,
      error5xxCount: 0,
      errorRate: 0,
      // 实时 QPS
      currentQps: 0,
    }
    instances.value = []
    trafficHistory.value = []
    trafficTimeRange.value = {}
    trafficGranularity.value = 'MINUTE'
    lastUpdateTime.value = 0
  }

  return {
    statistics,
    instances,
    instancesLoading,
    trafficHistory,
    trafficHistoryLoading,
    trafficHistoryError,
    trafficTimeRange,
    trafficGranularity,
    sseConnected,
    lastUpdateTime,
    fetchInstances,
    loadTrafficHistory,
    handleDashboardData,
    setGranularity,
    setTimeRange,
    formatNumber,
    successRateDisplay,
    healthyRatioDisplay,
    clearData,
  }
})