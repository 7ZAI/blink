/**
 * Monitor API 单元测试
 *
 * 测试监控中心 API 调用
 *
 * @author binblink
 * @since 2026-04-15
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '@/utils/request'
import {
  getGatewayInstances,
  getStatistics,
  getHealthStatus,
  getInstanceList,
  getInstanceDetail,
  getMonitorInstanceDetail,
  offlineInstance,
  onlineInstance,
  getTrafficHistory,
  monitorApi,
} from '../monitor'
import type {
  MonitorQuery,
  InstanceInfo,
  InstanceListResult,
  InstanceDetailInfo,
  StatisticsInfo,
  TrafficHistoryQuery,
  TrafficHistoryResult,
  OfflineInstanceParams,
  OnlineInstanceParams,
} from '../monitor'

// Mock request
vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('Monitor API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getGatewayInstances', () => {
    it('应该正确获取网关实例列表', async () => {
      // Given: Mock API 返回数据
      const mockResult: InstanceListResult = {
        total: 2,
        instances: [
          {
            instanceId: 'instance-001',
            serviceId: 'gateway-app',
            host: '192.168.1.1',
            port: 8002,
            uri: '/gateway',
            status: 0,
            statusDesc: '在线',
            healthy: true,
            lastHeartbeat: '2026-04-15 10:00:00',
          },
          {
            instanceId: 'instance-002',
            serviceId: 'gateway-app',
            host: '192.168.1.2',
            port: 8002,
            uri: '/gateway',
            status: 0,
            statusDesc: '在线',
            healthy: true,
            lastHeartbeat: '2026-04-15 10:00:00',
          },
        ],
      }
      vi.mocked(request.post).mockResolvedValue(mockResult)

      // When: 调用 getGatewayInstances
      const params: MonitorQuery = { instanceId: 'instance-001' }
      const result = await getGatewayInstances(params)

      // Then: 验证调用和结果
      expect(request.post).toHaveBeenCalledWith('/monitor/getGatewayInstances', { body: params })
      expect(result.total).toBe(2)
      expect(result.instances).toHaveLength(2)
      expect(result.instances![0]!.healthy).toBe(true)
    })

    it('默认参数应为空对象', async () => {
      // Given: Mock API
      vi.mocked(request.post).mockResolvedValue({ total: 0, instances: [] })

      // When: 无参数调用
      await getGatewayInstances()

      // Then: 验证默认参数
      expect(request.post).toHaveBeenCalledWith('/monitor/getGatewayInstances', { body: {} })
    })
  })

  describe('getStatistics', () => {
    it('应该正确获取统计数据', async () => {
      // Given: Mock API 返回数据
      const mockStats: StatisticsInfo = {
        totalInstances: 3,
        healthyInstances: 2,
        totalRequests: 1000,
        successRequests: 950,
        failedRequests: 50,
        avgResponseTime: 150,
      }
      vi.mocked(request.post).mockResolvedValue(mockStats)

      // When: 调用 getStatistics
      const result = await getStatistics()

      // Then: 验证调用和结果
      expect(request.post).toHaveBeenCalledWith('/monitor/getStatistics', { body: {} })
      expect(result.totalInstances).toBe(3)
      expect(result.totalRequests).toBe(1000)
      expect(result.avgResponseTime).toBe(150)
    })
  })

  describe('getHealthStatus', () => {
    it('应该正确获取健康状态', async () => {
      // Given: Mock API
      vi.mocked(request.post).mockResolvedValue({ status: 'UP' })

      // When: 调用 getHealthStatus
      const result = await getHealthStatus({ instanceId: 'instance-001' })

      // Then: 验证调用
      expect(request.post).toHaveBeenCalledWith('/monitor/getHealthStatus', { body: { instanceId: 'instance-001' } })
    })
  })

  describe('getInstanceList', () => {
    it('应该正确获取实例列表（管理用）', async () => {
      // Given: Mock API
      vi.mocked(request.post).mockResolvedValue({ total: 2, instances: [] })

      // When: 调用 getInstanceList
      await getInstanceList()

      // Then: 验证调用
      expect(request.post).toHaveBeenCalledWith('/gatewayInstance/getGatewayInstances', { body: {} })
    })
  })

  describe('getInstanceDetail', () => {
    it('应该正确获取实例详情', async () => {
      // Given: Mock API 返回数据
      const mockDetail: InstanceInfo = {
        instanceId: 'instance-001',
        serviceId: 'gateway-app',
        host: '192.168.1.1',
        port: 8002,
        uri: '/gateway',
        status: 0,
        statusDesc: '在线',
        healthy: true,
      }
      vi.mocked(request.post).mockResolvedValue(mockDetail)

      // When: 调用 getInstanceDetail
      const result = await getInstanceDetail('instance-001')

      // Then: 验证调用和结果
      expect(request.post).toHaveBeenCalledWith('/gatewayInstance/getGatewayInstanceDetail', { body: { instanceId: 'instance-001' } })
      expect(result.instanceId).toBe('instance-001')
    })
  })

  describe('getMonitorInstanceDetail', () => {
    it('应该正确获取实例监控详情（完整 JVM/GC/HTTP 指标）', async () => {
      // Given: Mock API 返回完整监控数据
      const mockDetail: InstanceDetailInfo = {
        instanceId: 'instance-001',
        serviceId: 'gateway-app',
        host: '192.168.1.1',
        port: 8002,
        uri: '/gateway',
        healthStatus: 'UP',
        statusDesc: '在线',
        timestamp: Date.now(),
        heapUsed: 524288000,
        heapMax: 1073741824,
        heapUsagePercent: 49.0,
        nonHeapUsed: 10485760,
        cpuUsage: 25.5,
        memoryUsage: 60.0,
        youngGcCount: 100,
        youngGcTime: 500,
        oldGcCount: 5,
        oldGcTime: 2000,
        totalGcCount: 105,
        totalGcTime: 2500,
        liveThreads: 150,
        peakThreads: 200,
        daemonThreads: 25,
        totalRequests: 50000,
        successRequests: 49500,
        failedRequests: 500,
        successRate: 99.0,
        avgResponseTime: 150,
        p50ResponseTime: 100,
        p95ResponseTime: 200,
        p99ResponseTime: 350,
        maxResponseTime: 500,
        currentQps: 1000,
        activeConnections: 10,
      }
      vi.mocked(request.post).mockResolvedValue(mockDetail)

      // When: 调用 getMonitorInstanceDetail
      const result = await getMonitorInstanceDetail('instance-001')

      // Then: 验证调用和结果
      expect(request.post).toHaveBeenCalledWith('/monitor/getInstanceDetail', { body: { instanceId: 'instance-001' } })
      expect(result.instanceId).toBe('instance-001')
      expect(result.heapUsagePercent).toBe(49.0)
      expect(result.cpuUsage).toBe(25.5)
      expect(result.totalRequests).toBe(50000)
      expect(result.successRate).toBe(99.0)
    })
  })

  describe('offlineInstance', () => {
    it('应该正确调用下线实例 API', async () => {
      // Given: Mock API
      vi.mocked(request.post).mockResolvedValue(undefined)

      // When: 下线实例
      const params: OfflineInstanceParams = {
        instanceId: 'instance-001',
        reason: '维护',
      }
      await offlineInstance(params)

      // Then: 验证调用
      expect(request.post).toHaveBeenCalledWith('/gatewayInstance/offlineInstance', { body: params })
    })
  })

  describe('onlineInstance', () => {
    it('应该正确调用上线实例 API', async () => {
      // Given: Mock API
      vi.mocked(request.post).mockResolvedValue(undefined)

      // When: 上线实例
      const params: OnlineInstanceParams = {
        instanceId: 'instance-001',
      }
      await onlineInstance(params)

      // Then: 验证调用
      expect(request.post).toHaveBeenCalledWith('/gatewayInstance/onlineInstance', { body: params })
    })
  })

  describe('getTrafficHistory', () => {
    it('应该正确获取流量历史数据', async () => {
      // Given: Mock API 返回流量历史数据
      const mockResult: TrafficHistoryResult = {
        points: [
          { time: '10:01:00', timestamp: 1000, count: 100, successCount: 95, failedCount: 5, peakQps: 10 },
          { time: '10:02:00', timestamp: 2000, count: 150, successCount: 145, failedCount: 5, peakQps: 15 },
        ],
        totalRequests: 250,
        peakQps: 15,
      }
      vi.mocked(request.post).mockResolvedValue(mockResult)

      // When: 调用 getTrafficHistory
      const params: TrafficHistoryQuery = {
        startTime: 1000,
        endTime: 2000,
        granularity: 'MINUTE',
      }
      const result = await getTrafficHistory(params)

      // Then: 验证调用和结果
      expect(request.post).toHaveBeenCalledWith('/monitor/getTrafficHistory', { body: params })
      expect(result.points).toHaveLength(2)
      expect(result.totalRequests).toBe(250)
      expect(result.peakQps).toBe(15)
    })

    it('应该支持小时级粒度', async () => {
      // Given: Mock API
      vi.mocked(request.post).mockResolvedValue({ points: [], totalRequests: 0, peakQps: 0 })

      // When: 使用小时级粒度
      await getTrafficHistory({ granularity: 'HOUR' })

      // Then: 验证参数传递
      expect(request.post).toHaveBeenCalledWith('/monitor/getTrafficHistory', { body: { granularity: 'HOUR' } })
    })

    it('默认参数应为空对象', async () => {
      // Given: Mock API
      vi.mocked(request.post).mockResolvedValue({ points: [], totalRequests: 0, peakQps: 0 })

      // When: 无参数调用
      await getTrafficHistory()

      // Then: 验证默认参数
      expect(request.post).toHaveBeenCalledWith('/monitor/getTrafficHistory', { body: {} })
    })

    it('无数据时应返回空数组', async () => {
      // Given: Mock API 返回空数据
      vi.mocked(request.post).mockResolvedValue({ points: [], totalRequests: 0, peakQps: 0 })

      // When: 调用 getTrafficHistory
      const result = await getTrafficHistory()

      // Then: 验证空数组
      expect(result.points).toHaveLength(0)
      expect(result.totalRequests).toBe(0)
    })
  })

  describe('monitorApi 对象', () => {
    it('应该包含所有 API 方法', () => {
      expect(monitorApi.getGatewayInstances).toBe(getGatewayInstances)
      expect(monitorApi.getStatistics).toBe(getStatistics)
      expect(monitorApi.getHealthStatus).toBe(getHealthStatus)
      expect(monitorApi.getInstanceList).toBe(getInstanceList)
      expect(monitorApi.getInstanceDetail).toBe(getInstanceDetail)
      expect(monitorApi.getMonitorInstanceDetail).toBe(getMonitorInstanceDetail)
      expect(monitorApi.offlineInstance).toBe(offlineInstance)
      expect(monitorApi.onlineInstance).toBe(onlineInstance)
      expect(monitorApi.getTrafficHistory).toBe(getTrafficHistory)
    })

    it('应该能通过对象调用 API', async () => {
      // Given: Mock API
      vi.mocked(request.post).mockResolvedValue({ total: 0, instances: [] })

      // When: 通过对象调用
      await monitorApi.getInstanceList()

      // Then: 验证调用
      expect(request.post).toHaveBeenCalled()
    })
  })

  describe('类型定义验证', () => {
    it('InstanceInfo 应包含所有必要字段', () => {
      const info: InstanceInfo = {
        instanceId: 'test',
        serviceId: 'gateway-app',
        host: '192.168.1.1',
        port: 8002,
        uri: '/gateway',
        status: 0,
        statusDesc: '在线',
        healthy: true,
      }

      expect(info.instanceId).toBe('test')
      expect(info.port).toBe(8002)
      expect(info.healthy).toBe(true)
    })

    it('InstanceDetailInfo 应包含完整监控指标', () => {
      const detail: InstanceDetailInfo = {
        instanceId: 'test',
        serviceId: 'gateway-app',
        host: '192.168.1.1',
        port: 8002,
        uri: '/gateway',
        healthStatus: 'UP',
        statusDesc: '在线',
        timestamp: Date.now(),
        heapUsed: 524288000,
        heapMax: 1073741824,
        heapUsagePercent: 49.0,
        nonHeapUsed: 10485760,
        cpuUsage: 25.5,
        memoryUsage: 60.0,
        youngGcCount: 100,
        youngGcTime: 500,
        oldGcCount: 5,
        oldGcTime: 2000,
        totalGcCount: 105,
        totalGcTime: 2500,
        liveThreads: 150,
        peakThreads: 200,
        daemonThreads: 25,
        totalRequests: 50000,
        successRequests: 49500,
        failedRequests: 500,
        successRate: 99.0,
        avgResponseTime: 150,
        p50ResponseTime: 100,
        p95ResponseTime: 200,
        p99ResponseTime: 350,
        maxResponseTime: 500,
        currentQps: 1000,
        activeConnections: 10,
      }

      // JVM 指标
      expect(detail.heapUsagePercent).toBe(49.0)
      expect(detail.cpuUsage).toBe(25.5)

      // GC 指标
      expect(detail.totalGcCount).toBe(105)
      expect(detail.totalGcTime).toBe(2500)

      // HTTP 指标
      expect(detail.successRate).toBe(99.0)
      expect(detail.avgResponseTime).toBe(150)
    })

    it('TrafficHistoryPoint 应包含正确的字段', () => {
      const point = {
        time: '10:00:00',
        timestamp: 1000,
        count: 100,
        successCount: 95,
        failedCount: 5,
        peakQps: 10,
      }

      expect(point.time).toBe('10:00:00')
      expect(point.count).toBe(100)
      expect(point.peakQps).toBe(10)
    })
  })
})