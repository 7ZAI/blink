/**
 * Dashboard Store 单元测试
 *
 * 测试流量历史功能：
 * 1. loadTrafficHistory - 加载历史数据
 * 2. handleDashboardData - SSE 推送处理
 * 3. setGranularity - 切换粒度
 * 4. setTimeRange - 设置时间范围
 *
 * @author binblink
 * @since 2026-04-14
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useDashboardStore } from '../dashboard'
import { monitorApi, type TrafficHistoryResult } from '@/api/monitor'
import type { DashboardDataPayload, StatisticsInfo } from '../dashboard'

// Mock monitorApi
vi.mock('@/api/monitor', () => ({
  monitorApi: {
    getInstanceList: vi.fn().mockResolvedValue({ instances: [] }),
    getTrafficHistory: vi.fn(),
  },
  TrafficHistoryResult: {},
}))

// Helper: 创建完整的 StatisticsInfo 对象
function createStatisticsInfo(partial: Partial<StatisticsInfo> = {}): StatisticsInfo {
  return {
    totalInstances: partial.totalInstances ?? 1,
    healthyInstances: partial.healthyInstances ?? 1,
    totalRequests: partial.totalRequests ?? 100,
    successRequests: partial.successRequests ?? 95,
    failedRequests: partial.failedRequests ?? 5,
    avgResponseTime: partial.avgResponseTime ?? 50,
    p50ResponseTime: partial.p50ResponseTime ?? 30,
    p95ResponseTime: partial.p95ResponseTime ?? 45,
    p99ResponseTime: partial.p99ResponseTime ?? 60,
    maxResponseTime: partial.maxResponseTime ?? 100,
    error4xxCount: partial.error4xxCount ?? 3,
    error5xxCount: partial.error5xxCount ?? 2,
    errorRate: partial.errorRate ?? 5,
    currentQps: partial.currentQps ?? 10,
  }
}

describe('Dashboard Store - 流量历史功能', () => {
  let store: ReturnType<typeof useDashboardStore>

  beforeEach(() => {
    vi.clearAllMocks()
    store = useDashboardStore()
  })

  describe('loadTrafficHistory', () => {
    it('应该正确加载历史数据', async () => {
      // Given: Mock API 返回历史数据
      const mockResult: TrafficHistoryResult = {
        points: [
          { time: '10:01:00', timestamp: 1000, count: 100, successCount: 95, failedCount: 5, peakQps: 10 },
          { time: '10:02:00', timestamp: 2000, count: 150, successCount: 145, failedCount: 5, peakQps: 15 },
        ],
        totalRequests: 250,
        peakQps: 15,
      }
      vi.mocked(monitorApi.getTrafficHistory).mockResolvedValue(mockResult)

      // When: 加载历史数据
      await store.loadTrafficHistory({ granularity: 'MINUTE' })

      // Then: 验证 store 状态
      expect(store.trafficHistory).toHaveLength(2)
      expect(store.trafficHistory![0]!.count).toBe(100)
      expect(store.trafficGranularity).toBe('MINUTE')
      expect(store.trafficHistoryLoading).toBe(false)
      expect(store.trafficHistoryError).toBeNull()
    })

    it('API 失败时应设置错误状态', async () => {
      // Given: Mock API 抛出错误
      vi.mocked(monitorApi.getTrafficHistory).mockRejectedValue(new Error('Network error'))

      // When: 加载历史数据
      await store.loadTrafficHistory({ granularity: 'MINUTE' })

      // Then: 验证错误状态
      expect(store.trafficHistory).toHaveLength(0)
      expect(store.trafficHistoryError).toBe('加载失败')
      expect(store.trafficHistoryLoading).toBe(false)
    })

    it('无数据时应返回空数组', async () => {
      // Given: Mock API 返回空数据
      vi.mocked(monitorApi.getTrafficHistory).mockResolvedValue({
        points: [],
        totalRequests: 0,
        peakQps: 0,
      })

      // When: 加载历史数据
      await store.loadTrafficHistory()

      // Then: 验证空数组
      expect(store.trafficHistory).toHaveLength(0)
    })

    it('应使用当前状态作为默认参数', async () => {
      // Given: 设置当前状态
      store.trafficTimeRange = { startTime: 1000, endTime: 2000 }
      store.trafficGranularity = 'HOUR'
      vi.mocked(monitorApi.getTrafficHistory).mockResolvedValue({
        points: [],
        totalRequests: 0,
        peakQps: 0,
      })

      // When: 加载历史数据（无参数）
      await store.loadTrafficHistory()

      // Then: 验证使用了当前状态
      expect(monitorApi.getTrafficHistory).toHaveBeenCalledWith({
        startTime: 1000,
        endTime: 2000,
        granularity: 'HOUR',
      })
    })
  })

  describe('handleDashboardData - SSE 推送处理', () => {
    it('应该在分钟级模式下追加实时数据', () => {
      // Given: 初始化历史数据和状态
      store.trafficHistory = [{ time: '10:00:00', timestamp: 1000, count: 50, successCount: 48, failedCount: 2, peakQps: 5 }]
      store.trafficGranularity = 'MINUTE'
      store.trafficTimeRange = {} // 无时间范围限制

      const payload: DashboardDataPayload = {
        statistics: createStatisticsInfo(),
        instances: [],
        latestTraffic: { time: '10:01:00', count: 30, timestamp: 2000 },
        timestamp: 2000,
      }

      // When: 处理 SSE 数据
      store.handleDashboardData(payload)

      // Then: 验证数据追加
      expect(store.trafficHistory).toHaveLength(2)
      expect(store.trafficHistory![1]!.count).toBe(30)
      expect(store.statistics.totalRequests).toBe(100)
    })

    it('小时级模式下不应追加实时数据', () => {
      // Given: 小时级模式
      store.trafficGranularity = 'HOUR'
      store.trafficHistory = []

      const payload: DashboardDataPayload = {
        statistics: createStatisticsInfo(),
        instances: [],
        latestTraffic: { time: '10:01:00', count: 30, timestamp: 2000 },
        timestamp: 2000,
      }

      // When: 处理 SSE 数据
      store.handleDashboardData(payload)

      // Then: 验证未追加数据
      expect(store.trafficHistory).toHaveLength(0)
    })

    it('有时间范围限制时不应追加实时数据', () => {
      // Given: 设置了时间范围
      store.trafficGranularity = 'MINUTE'
      store.trafficTimeRange = { startTime: 1000, endTime: 2000 }
      store.trafficHistory = []

      const payload: DashboardDataPayload = {
        statistics: createStatisticsInfo(),
        instances: [],
        latestTraffic: { time: '10:01:00', count: 30, timestamp: 3000 },
        timestamp: 3000,
      }

      // When: 处理 SSE 数据
      store.handleDashboardData(payload)

      // Then: 验证未追加数据
      expect(store.trafficHistory).toHaveLength(0)
    })

    it('数据超过60条时应移除最早的数据', () => {
      // Given: 初始化 60 条数据
      store.trafficGranularity = 'MINUTE'
      store.trafficTimeRange = {}
      store.trafficHistory = Array.from({ length: 60 }, (_, i) => ({
        time: `10:${i}:00`,
        timestamp: i * 1000,
        count: i,
        successCount: i,
        failedCount: 0,
        peakQps: 0,
      }))

      const payload: DashboardDataPayload = {
        statistics: createStatisticsInfo(),
        instances: [],
        latestTraffic: { time: '11:00:00', count: 30, timestamp: 60000 },
        timestamp: 60000,
      }

      // When: 处理 SSE 数据
      store.handleDashboardData(payload)

      // Then: 验证数据保持在 60 条
      expect(store.trafficHistory).toHaveLength(60)
      expect(store.trafficHistory![0]!.time).toBe('10:1:00') // 第一条被移除
    })

    it('latestTraffic 为空时应跳过追加', () => {
      // Given: 空的 latestTraffic
      store.trafficGranularity = 'MINUTE'
      store.trafficHistory = []

      const payload: DashboardDataPayload = {
        statistics: createStatisticsInfo(),
        instances: [],
        latestTraffic: undefined as any,
        timestamp: 2000,
      }

      // When: 处理 SSE 数据
      store.handleDashboardData(payload)

      // Then: 验证未追加数据
      expect(store.trafficHistory).toHaveLength(0)
    })
  })

  describe('setGranularity', () => {
    it('应该切换粒度并重新加载', async () => {
      // Given: Mock API
      vi.mocked(monitorApi.getTrafficHistory).mockResolvedValue({
        points: [{ time: '10:00', timestamp: 1000, count: 100, successCount: 95, failedCount: 5, peakQps: 10 }],
        totalRequests: 100,
        peakQps: 10,
      })

      // When: 切换粒度
      await store.setGranularity('HOUR')

      // Then: 验证粒度切换和重新加载
      expect(store.trafficGranularity).toBe('HOUR')
      expect(monitorApi.getTrafficHistory).toHaveBeenCalledWith({
        startTime: undefined,
        endTime: undefined,
        granularity: 'HOUR',
      })
    })
  })

  describe('setTimeRange', () => {
    it('设置时间范围时应加载历史数据', async () => {
      // Given: Mock API
      vi.mocked(monitorApi.getTrafficHistory).mockResolvedValue({
        points: [],
        totalRequests: 0,
        peakQps: 0,
      })

      // When: 设置时间范围
      const range = { startTime: 1000, endTime: 2000 }
      await store.setTimeRange(range)

      // Then: 验证时间范围和加载
      expect(store.trafficTimeRange.startTime).toBe(1000)
      expect(store.trafficTimeRange.endTime).toBe(2000)
      expect(monitorApi.getTrafficHistory).toHaveBeenCalled()
    })

    it('清空时间范围时应恢复默认模式', async () => {
      // Given: 设置了时间范围
      store.trafficTimeRange = { startTime: 1000, endTime: 2000 }
      vi.mocked(monitorApi.getTrafficHistory).mockResolvedValue({
        points: [],
        totalRequests: 0,
        peakQps: 0,
      })

      // When: 清空时间范围
      await store.setTimeRange({})

      // Then: 验证恢复默认
      expect(store.trafficTimeRange.startTime).toBeUndefined()
      expect(monitorApi.getTrafficHistory).toHaveBeenCalledWith({
        startTime: undefined,
        endTime: undefined,
        granularity: 'MINUTE',
      })
    })
  })

  describe('clearData', () => {
    it('应该清空所有数据', () => {
      // Given: 设置一些数据
      store.trafficHistory = [{ time: '10:00', timestamp: 1000, count: 100, successCount: 95, failedCount: 5, peakQps: 10 }]
      store.trafficTimeRange = { startTime: 1000, endTime: 2000 }
      store.trafficGranularity = 'HOUR'

      // When: 清空数据
      store.clearData()

      // Then: 验证数据已清空
      expect(store.trafficHistory).toHaveLength(0)
      expect(store.trafficTimeRange.startTime).toBeUndefined()
      expect(store.trafficGranularity).toBe('MINUTE')
    })
  })
})