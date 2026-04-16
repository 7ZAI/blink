/**
 * Alert API 单元测试
 *
 * 测试告警管理 API 调用
 *
 * @author binblink
 * @since 2026-04-15
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '@/utils/request'
import {
  getRules,
  addRule,
  updateRule,
  deleteRule,
  toggleRule,
  getHistory,
  getFiring,
  acknowledge,
  alertApi,
  RULE_TYPE_OPTIONS,
  METRIC_OPTIONS,
  OPERATOR_OPTIONS,
  SEVERITY_OPTIONS,
  STATUS_OPTIONS,
  NOTIFY_CHANNEL_OPTIONS,
} from '../alert'
import type {
  AlertRule,
  AlertCondition,
  QueryAlertRuleParams,
  AddAlertRuleParams,
  QueryAlertHistoryParams,
  AlertRuleListResult,
  AlertHistoryListResult,
  AlertHistory,
} from '../alert'

// Mock request
vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('Alert API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getRules', () => {
    it('应该正确调用 API 并返回告警规则列表', async () => {
      // Given: Mock API 返回数据
      const mockResult: AlertRuleListResult = {
        total: 2,
        rules: [
          {
            id: 1,
            ruleName: 'CPU告警',
            ruleType: 'RESOURCE',
            conditions: [{ metricName: 'cpuUsage', operator: 'gt', threshold: 80, durationMinutes: 3 }],
            severity: 'WARNING',
            notifyChannels: ['IN_APP'],
            notifyTemplate: '',
            suppressMinutes: 5,
            enabled: 1,
            createTime: '2026-04-15 10:00:00',
            updateTime: '2026-04-15 10:00:00',
          },
          {
            id: 2,
            ruleName: '内存告警',
            ruleType: 'RESOURCE',
            conditions: [{ metricName: 'memoryUsage', operator: 'gt', threshold: 90, durationMinutes: 5 }],
            severity: 'ERROR',
            notifyChannels: ['IN_APP', 'EMAIL'],
            notifyTemplate: '',
            suppressMinutes: 10,
            enabled: 1,
            createTime: '2026-04-15 11:00:00',
            updateTime: '2026-04-15 11:00:00',
          },
        ],
      }
      vi.mocked(request.post).mockResolvedValue(mockResult)

      // When: 调用 getRules
      const params: QueryAlertRuleParams = { pageNum: 1, pageSize: 10 }
      const result = await getRules(params)

      // Then: 验证调用和结果
      expect(request.post).toHaveBeenCalledWith('/alert/getRules', { body: params })
      expect(result.total).toBe(2)
      expect(result.rules).toHaveLength(2)
      expect(result.rules![0]!.ruleName).toBe('CPU告警')
    })

    it('应该支持按类型筛选', async () => {
      // Given: Mock API
      vi.mocked(request.post).mockResolvedValue({ total: 1, rules: [] })

      // When: 按类型筛选
      const params: QueryAlertRuleParams = { ruleType: 'PERFORMANCE' }
      await getRules(params)

      // Then: 验证参数传递
      expect(request.post).toHaveBeenCalledWith('/alert/getRules', { body: params })
    })

    it('应该支持按启用状态筛选', async () => {
      // Given: Mock API
      vi.mocked(request.post).mockResolvedValue({ total: 0, rules: [] })

      // When: 按状态筛选
      const params: QueryAlertRuleParams = { enabled: 0 }
      await getRules(params)

      // Then: 验证参数传递
      expect(request.post).toHaveBeenCalledWith('/alert/getRules', { body: { enabled: 0 } })
    })

    it('默认参数应为空对象', async () => {
      // Given: Mock API
      vi.mocked(request.post).mockResolvedValue({ total: 0, rules: [] })

      // When: 无参数调用
      await getRules()

      // Then: 验证默认参数
      expect(request.post).toHaveBeenCalledWith('/alert/getRules', { body: {} })
    })
  })

  describe('addRule', () => {
    it('应该正确调用新增规则 API', async () => {
      // Given: Mock API
      vi.mocked(request.post).mockResolvedValue(undefined)

      // When: 新增规则
      const params: AddAlertRuleParams = {
        ruleName: '新告警规则',
        ruleType: 'RESOURCE',
        conditions: [{ metricName: 'cpuUsage', operator: 'gt', threshold: 80, durationMinutes: 3 }],
        severity: 'WARNING',
        notifyChannels: ['IN_APP'],
      }
      await addRule(params)

      // Then: 验证调用
      expect(request.post).toHaveBeenCalledWith('/alert/addRule', { body: params })
    })
  })

  describe('updateRule', () => {
    it('应该正确调用更新规则 API', async () => {
      // Given: Mock API
      vi.mocked(request.post).mockResolvedValue(undefined)

      // When: 更新规则
      const params: AddAlertRuleParams = {
        id: 1,
        ruleName: '更新后的规则',
        ruleType: 'PERFORMANCE',
        conditions: [{ metricName: 'p99ResponseTime', operator: 'gt', threshold: 500, durationMinutes: 5 }],
        severity: 'ERROR',
        notifyChannels: ['IN_APP', 'EMAIL'],
      }
      await updateRule(params)

      // Then: 验证调用
      expect(request.post).toHaveBeenCalledWith('/alert/updateRule', { body: params })
    })
  })

  describe('deleteRule', () => {
    it('应该正确调用删除规则 API', async () => {
      // Given: Mock API
      vi.mocked(request.post).mockResolvedValue(undefined)

      // When: 删除规则
      await deleteRule(1)

      // Then: 验证调用
      expect(request.post).toHaveBeenCalledWith('/alert/deleteRule', { body: { id: 1 } })
    })
  })

  describe('toggleRule', () => {
    it('应该正确调用启用规则 API', async () => {
      // Given: Mock API
      vi.mocked(request.post).mockResolvedValue(undefined)

      // When: 启用规则
      await toggleRule(1, 1)

      // Then: 验证调用
      expect(request.post).toHaveBeenCalledWith('/alert/toggleRule', { body: { id: 1, enabled: 1 } })
    })

    it('应该正确调用禁用规则 API', async () => {
      // Given: Mock API
      vi.mocked(request.post).mockResolvedValue(undefined)

      // When: 禁用规则
      await toggleRule(1, 0)

      // Then: 验证调用
      expect(request.post).toHaveBeenCalledWith('/alert/toggleRule', { body: { id: 1, enabled: 0 } })
    })
  })

  describe('getHistory', () => {
    it('应该正确调用告警历史 API', async () => {
      // Given: Mock API 返回数据
      const mockResult: AlertHistoryListResult = {
        total: 1,
        rows: [
          {
            id: 1,
            ruleId: 1,
            ruleName: 'CPU告警',
            instanceId: 'instance-001',
            alertTitle: 'CPU使用率超过阈值',
            alertContent: 'CPU告警触发',
            severity: 'WARNING',
            status: 'FIRING',
            firedTime: '2026-04-15 10:00:00',
          },
        ],
      }
      vi.mocked(request.post).mockResolvedValue(mockResult)

      // When: 查询告警历史
      const params: QueryAlertHistoryParams = { pageNum: 1, pageSize: 10 }
      const result = await getHistory(params)

      // Then: 验证调用和结果
      expect(request.post).toHaveBeenCalledWith('/alert/getHistory', { body: params })
      expect(result.total).toBe(1)
      expect(result.rows).toHaveLength(1)
      expect(result.rows![0]!.status).toBe('FIRING')
    })

    it('应该支持按状态筛选', async () => {
      // Given: Mock API
      vi.mocked(request.post).mockResolvedValue({ total: 0, rows: [] })

      // When: 按状态筛选
      await getHistory({ status: 'RESOLVED' })

      // Then: 验证参数传递
      expect(request.post).toHaveBeenCalledWith('/alert/getHistory', { body: { status: 'RESOLVED' } })
    })

    it('应该支持按严重程度筛选', async () => {
      // Given: Mock API
      vi.mocked(request.post).mockResolvedValue({ total: 0, rows: [] })

      // When: 按严重程度筛选
      await getHistory({ severity: 'ERROR' })

      // Then: 验证参数传递
      expect(request.post).toHaveBeenCalledWith('/alert/getHistory', { body: { severity: 'ERROR' } })
    })
  })

  describe('getFiring', () => {
    it('应该正确获取触发中的告警', async () => {
      // Given: Mock API 返回数据
      const mockAlerts: AlertHistory[] = [
        {
          id: 1,
          ruleId: 1,
          ruleName: 'CPU告警',
          instanceId: 'instance-001',
          alertTitle: 'CPU使用率超过阈值',
          alertContent: 'CPU告警触发',
          severity: 'WARNING',
          status: 'FIRING',
          firedTime: '2026-04-15 10:00:00',
        },
      ]
      vi.mocked(request.post).mockResolvedValue(mockAlerts)

      // When: 获取触发中的告警
      const result = await getFiring()

      // Then: 验证调用和结果
      expect(request.post).toHaveBeenCalledWith('/alert/getFiring', { body: {} })
      expect(result).toHaveLength(1)
      expect(result![0]!.status).toBe('FIRING')
    })

    it('无触发中的告警时应返回空数组', async () => {
      // Given: Mock API 返回空数组
      vi.mocked(request.post).mockResolvedValue([])

      // When: 获取触发中的告警
      const result = await getFiring()

      // Then: 验证空数组
      expect(result).toHaveLength(0)
    })
  })

  describe('acknowledge', () => {
    it('应该正确调用确认告警 API', async () => {
      // Given: Mock API
      vi.mocked(request.post).mockResolvedValue(undefined)

      // When: 确认告警
      await acknowledge(1)

      // Then: 验证调用
      expect(request.post).toHaveBeenCalledWith('/alert/acknowledge', { body: { id: 1 } })
    })
  })

  describe('alertApi 对象', () => {
    it('应该包含所有 API 方法', () => {
      expect(alertApi.getRules).toBe(getRules)
      expect(alertApi.addRule).toBe(addRule)
      expect(alertApi.updateRule).toBe(updateRule)
      expect(alertApi.deleteRule).toBe(deleteRule)
      expect(alertApi.toggleRule).toBe(toggleRule)
      expect(alertApi.getHistory).toBe(getHistory)
      expect(alertApi.getFiring).toBe(getFiring)
      expect(alertApi.acknowledge).toBe(acknowledge)
    })
  })

  describe('辅助常量', () => {
    it('RULE_TYPE_OPTIONS 应包含正确的规则类型', () => {
      expect(RULE_TYPE_OPTIONS).toHaveLength(4)
      expect(RULE_TYPE_OPTIONS![0]!.value).toBe('RESOURCE')
      expect(RULE_TYPE_OPTIONS![1]!.value).toBe('PERFORMANCE')
      expect(RULE_TYPE_OPTIONS![2]!.value).toBe('ERROR')
      expect(RULE_TYPE_OPTIONS![3]!.value).toBe('INSTANCE')
    })

    it('METRIC_OPTIONS 应包含正确的监控指标', () => {
      expect(METRIC_OPTIONS).toHaveLength(5)
      expect(METRIC_OPTIONS![0]!.value).toBe('cpuUsage')
      expect(METRIC_OPTIONS![1]!.value).toBe('memoryUsage')
      expect(METRIC_OPTIONS![2]!.value).toBe('p95ResponseTime')
      expect(METRIC_OPTIONS![3]!.value).toBe('p99ResponseTime')
      expect(METRIC_OPTIONS![4]!.value).toBe('errorRate')
    })

    it('OPERATOR_OPTIONS 应包含正确的操作符', () => {
      expect(OPERATOR_OPTIONS).toHaveLength(4)
      expect(OPERATOR_OPTIONS![0]!.value).toBe('gt')
      expect(OPERATOR_OPTIONS![1]!.value).toBe('lt')
      expect(OPERATOR_OPTIONS![2]!.value).toBe('gte')
      expect(OPERATOR_OPTIONS![3]!.value).toBe('lte')
    })

    it('SEVERITY_OPTIONS 应包含正确的严重程度', () => {
      expect(SEVERITY_OPTIONS).toHaveLength(3)
      expect(SEVERITY_OPTIONS![0]!.value).toBe('INFO')
      expect(SEVERITY_OPTIONS![1]!.value).toBe('WARNING')
      expect(SEVERITY_OPTIONS![2]!.value).toBe('ERROR')
    })

    it('STATUS_OPTIONS 应包含正确的告警状态', () => {
      expect(STATUS_OPTIONS).toHaveLength(3)
      expect(STATUS_OPTIONS![0]!.value).toBe('FIRING')
      expect(STATUS_OPTIONS![1]!.value).toBe('RESOLVED')
      expect(STATUS_OPTIONS![2]!.value).toBe('ACKNOWLEDGED')
    })

    it('NOTIFY_CHANNEL_OPTIONS 应包含正确的通知渠道', () => {
      expect(NOTIFY_CHANNEL_OPTIONS).toHaveLength(3)
      expect(NOTIFY_CHANNEL_OPTIONS![0]!.value).toBe('IN_APP')
      expect(NOTIFY_CHANNEL_OPTIONS![1]!.value).toBe('EMAIL')
      expect(NOTIFY_CHANNEL_OPTIONS![2]!.value).toBe('WEBHOOK')
    })
  })
})