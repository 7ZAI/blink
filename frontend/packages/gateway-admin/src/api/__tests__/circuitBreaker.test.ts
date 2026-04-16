/**
 * CircuitBreaker API 单元测试
 *
 * 测试熔断器监控 API 调用
 *
 * @author binblink
 * @since 2026-04-15
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '@/utils/request'
import {
  getOverview,
  getConfig,
  circuitBreakerApi,
  STATE_OPTIONS,
  WINDOW_TYPE_OPTIONS,
} from '../circuitBreaker'
import type {
  CircuitBreakerOverview,
  CircuitBreakerConfig,
  CircuitBreakerStatus,
} from '../circuitBreaker'

// Mock request
vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('CircuitBreaker API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getOverview', () => {
    it('应该正确调用 API 并返回熔断器总览', async () => {
      // Given: Mock API 返回数据
      const mockOverview: CircuitBreakerOverview = {
        circuitBreakers: [
          {
            name: 'myCircuitBreaker',
            baseConfig: 'default',
            slidingWindowType: 'COUNT_BASED',
            slidingWindowSize: 10,
            minimumNumberOfCalls: 5,
            failureRateThreshold: 50.0,
            slowCallRateThreshold: 100.0,
            slowCallDurationThreshold: 8000,
            waitDurationInOpenState: 60,
            permittedNumberOfCallsInHalfOpenState: 3,
            automaticTransitionFromOpenToHalfOpenEnabled: true,
            instanceStatuses: [
              {
                name: 'myCircuitBreaker',
                state: 'CLOSED',
                failureRate: 0.0,
                slowCallRate: 0.0,
                numberOfCalls: 0,
                numberOfFailedCalls: 0,
                numberOfSlowCalls: 0,
                numberOfSuccessfulCalls: 0,
                instanceId: 'instance-001',
                timestamp: Date.now(),
              },
            ],
          },
          {
            name: 'strictCircuitBreaker',
            baseConfig: 'strict',
            slidingWindowType: 'COUNT_BASED',
            slidingWindowSize: 10,
            minimumNumberOfCalls: 5,
            failureRateThreshold: 30.0,
            slowCallRateThreshold: 100.0,
            slowCallDurationThreshold: 8000,
            waitDurationInOpenState: 120,
            permittedNumberOfCallsInHalfOpenState: 2,
            automaticTransitionFromOpenToHalfOpenEnabled: true,
          },
        ],
        totalCircuitBreakers: 7,
        openCount: 0,
        closedCount: 7,
        halfOpenCount: 0,
        totalInstances: 1,
      }
      vi.mocked(request.post).mockResolvedValue(mockOverview)

      // When: 调用 getOverview
      const result = await getOverview()

      // Then: 验证调用和结果
      expect(request.post).toHaveBeenCalledWith('/circuitBreaker/getOverview', { body: {} })
      expect(result.totalCircuitBreakers).toBe(7)
      expect(result.circuitBreakers).toHaveLength(2)
      expect(result.totalInstances).toBe(1)
    })

    it('应该正确返回多实例状态', async () => {
      // Given: Mock API 返回多实例数据
      const mockOverview: CircuitBreakerOverview = {
        circuitBreakers: [],
        totalCircuitBreakers: 7,
        openCount: 2,
        closedCount: 4,
        halfOpenCount: 1,
        totalInstances: 3,
      }
      vi.mocked(request.post).mockResolvedValue(mockOverview)

      // When: 调用 getOverview
      const result = await getOverview()

      // Then: 验证结果
      expect(result.totalInstances).toBe(3)
      expect(result.openCount).toBe(2)
      expect(result.halfOpenCount).toBe(1)
    })
  })

  describe('getConfig', () => {
    it('应该正确获取指定熔断器配置', async () => {
      // Given: Mock API 返回数据
      const mockConfig: CircuitBreakerConfig = {
        name: 'protectedCircuitBreaker',
        baseConfig: 'default',
        slidingWindowType: 'COUNT_BASED',
        slidingWindowSize: 10,
        minimumNumberOfCalls: 5,
        failureRateThreshold: 55.0, // 覆盖后的阈值
        slowCallRateThreshold: 100.0,
        slowCallDurationThreshold: 8000,
        waitDurationInOpenState: 60,
        permittedNumberOfCallsInHalfOpenState: 3,
        automaticTransitionFromOpenToHalfOpenEnabled: true,
        instanceStatuses: [
          {
            name: 'protectedCircuitBreaker',
            state: 'CLOSED',
            failureRate: 0.0,
            slowCallRate: 0.0,
            numberOfCalls: 100,
            numberOfFailedCalls: 5,
            numberOfSlowCalls: 2,
            numberOfSuccessfulCalls: 93,
            instanceId: 'instance-001',
            timestamp: Date.now(),
          },
        ],
      }
      vi.mocked(request.post).mockResolvedValue(mockConfig)

      // When: 获取配置
      const result = await getConfig('protectedCircuitBreaker')

      // Then: 验证调用和结果
      expect(request.post).toHaveBeenCalledWith('/circuitBreaker/getConfig', { body: { name: 'protectedCircuitBreaker' } })
      expect(result.name).toBe('protectedCircuitBreaker')
      expect(result.failureRateThreshold).toBe(55.0)
    })

    it('应该正确获取 strict 配置', async () => {
      // Given: Mock API 返回 strict 配置
      const mockConfig: CircuitBreakerConfig = {
        name: 'strictCircuitBreaker',
        baseConfig: 'strict',
        slidingWindowType: 'COUNT_BASED',
        slidingWindowSize: 10,
        minimumNumberOfCalls: 5,
        failureRateThreshold: 30.0,
        slowCallRateThreshold: 100.0,
        slowCallDurationThreshold: 8000,
        waitDurationInOpenState: 120,
        permittedNumberOfCallsInHalfOpenState: 2,
        automaticTransitionFromOpenToHalfOpenEnabled: true,
      }
      vi.mocked(request.post).mockResolvedValue(mockConfig)

      // When: 获取配置
      const result = await getConfig('strictCircuitBreaker')

      // Then: 验证配置
      expect(result.baseConfig).toBe('strict')
      expect(result.failureRateThreshold).toBe(30.0)
      expect(result.waitDurationInOpenState).toBe(120)
    })

    it('应该正确获取 lenient 配置', async () => {
      // Given: Mock API 返回 lenient 配置
      const mockConfig: CircuitBreakerConfig = {
        name: 'lenientCircuitBreaker',
        baseConfig: 'lenient',
        slidingWindowType: 'COUNT_BASED',
        slidingWindowSize: 20,
        minimumNumberOfCalls: 10,
        failureRateThreshold: 70.0,
        slowCallRateThreshold: 100.0,
        slowCallDurationThreshold: 8000,
        waitDurationInOpenState: 30,
        permittedNumberOfCallsInHalfOpenState: 5,
        automaticTransitionFromOpenToHalfOpenEnabled: true,
      }
      vi.mocked(request.post).mockResolvedValue(mockConfig)

      // When: 获取配置
      const result = await getConfig('lenientCircuitBreaker')

      // Then: 验证配置
      expect(result.baseConfig).toBe('lenient')
      expect(result.failureRateThreshold).toBe(70.0)
      expect(result.waitDurationInOpenState).toBe(30)
    })
  })

  describe('circuitBreakerApi 对象', () => {
    it('应该包含所有 API 方法', () => {
      expect(circuitBreakerApi.getOverview).toBe(getOverview)
      expect(circuitBreakerApi.getConfig).toBe(getConfig)
    })

    it('应该能通过对象调用 API', async () => {
      // Given: Mock API
      vi.mocked(request.post).mockResolvedValue({ circuitBreakers: [], totalCircuitBreakers: 7 })

      // When: 通过对象调用
      await circuitBreakerApi.getOverview()

      // Then: 验证调用
      expect(request.post).toHaveBeenCalled()
    })
  })

  describe('辅助常量', () => {
    it('STATE_OPTIONS 应包含正确的熔断器状态', () => {
      expect(STATE_OPTIONS).toHaveLength(3)
      expect(STATE_OPTIONS![0]!.value).toBe('CLOSED')
      expect(STATE_OPTIONS![0]!.color).toBe('success')
      expect(STATE_OPTIONS![1]!.value).toBe('OPEN')
      expect(STATE_OPTIONS![1]!.color).toBe('danger')
      expect(STATE_OPTIONS![2]!.value).toBe('HALF_OPEN')
      expect(STATE_OPTIONS![2]!.color).toBe('warning')
    })

    it('WINDOW_TYPE_OPTIONS 应包含正确的滑动窗口类型', () => {
      expect(WINDOW_TYPE_OPTIONS).toHaveLength(2)
      expect(WINDOW_TYPE_OPTIONS![0]!.value).toBe('COUNT_BASED')
      expect(WINDOW_TYPE_OPTIONS![0]!.label).toBe('基于计数')
      expect(WINDOW_TYPE_OPTIONS![1]!.value).toBe('TIME_BASED')
      expect(WINDOW_TYPE_OPTIONS![1]!.label).toBe('基于时间')
    })
  })

  describe('类型定义验证', () => {
    it('CircuitBreakerStatus 应包含所有必要字段', () => {
      const status: CircuitBreakerStatus = {
        name: 'test',
        state: 'CLOSED',
        failureRate: 0.0,
        slowCallRate: 0.0,
        numberOfCalls: 100,
        numberOfFailedCalls: 5,
        numberOfSlowCalls: 2,
        numberOfSuccessfulCalls: 93,
        instanceId: 'instance-001',
        timestamp: Date.now(),
      }

      expect(status.name).toBe('test')
      expect(status.state).toBe('CLOSED')
      expect(status.numberOfCalls).toBe(100)
    })

    it('CircuitBreakerConfig 应包含所有必要字段', () => {
      const config: CircuitBreakerConfig = {
        name: 'test',
        slidingWindowType: 'COUNT_BASED',
        slidingWindowSize: 10,
        minimumNumberOfCalls: 5,
        failureRateThreshold: 50.0,
        slowCallRateThreshold: 100.0,
        slowCallDurationThreshold: 8000,
        waitDurationInOpenState: 60,
        permittedNumberOfCallsInHalfOpenState: 3,
        automaticTransitionFromOpenToHalfOpenEnabled: true,
      }

      expect(config.name).toBe('test')
      expect(config.slidingWindowSize).toBe(10)
      expect(config.failureRateThreshold).toBe(50.0)
    })
  })
})