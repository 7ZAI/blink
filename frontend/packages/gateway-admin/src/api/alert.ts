import request from '@/utils/request'

// ==================== 告警相关类型定义 ====================

/**
 * 告警条件
 */
export interface AlertCondition {
  metricName: string // 指标名称: cpuUsage, memoryUsage, p95ResponseTime, p99ResponseTime, errorRate
  operator: string // 操作符: gt, lt, gte, lte
  threshold: number // 阈值
  durationMinutes: number // 持续时间 (分钟)
}

/**
 * 告警规则
 */
export interface AlertRule {
  id: number
  ruleName: string
  ruleType: string // RESOURCE/PERFORMANCE/ERROR/INSTANCE
  conditions: AlertCondition[]
  severity: string // INFO/WARNING/ERROR
  notifyChannels: string[] // IN_APP, EMAIL, WEBHOOK
  notifyTemplate: string
  suppressMinutes: number
  enabled: number // 0-禁用 1-启用
  createTime: string
  updateTime: string
}

/**
 * 添加/更新告警规则参数
 */
export interface AddAlertRuleParams {
  id?: number // 更新时必填
  ruleName: string
  ruleType: string
  conditions: AlertCondition[]
  severity: string
  notifyChannels: string[]
  notifyTemplate?: string
  suppressMinutes?: number
  enabled?: number
}

/**
 * 查询告警规则参数
 */
export interface QueryAlertRuleParams {
  pageNum?: number
  pageSize?: number
  ruleType?: string
  enabled?: number
}

/**
 * 告警规则列表结果
 */
export interface AlertRuleListResult {
  total: number
  rules: AlertRule[]
}

/**
 * 告警历史
 */
export interface AlertHistory {
  id: number
  ruleId: number
  ruleName: string
  instanceId: string
  alertTitle: string
  alertContent: string
  severity: string
  status: string // FIRING/RESOLVED/ACKNOWLEDGED
  firedTime: string
  resolvedTime?: string
  acknowledgedTime?: string
  acknowledgedBy?: number
}

/**
 * 查询告警历史参数
 */
export interface QueryAlertHistoryParams {
  pageNum?: number
  pageSize?: number
  status?: string // FIRING/RESOLVED/ACKNOWLEDGED
  severity?: string // INFO/WARNING/ERROR
  ruleId?: number
  startTime?: string
  endTime?: string
}

/**
 * 告警历史列表结果
 */
export interface AlertHistoryListResult {
  total: number
  rows: AlertHistory[]
}

// ==================== 告警 API 方法 ====================

/**
 * 查询告警规则列表
 */
export const getRules = (params: QueryAlertRuleParams = {}): Promise<AlertRuleListResult> => {
  return request.post('/alert/getRules', { body: params })
}

/**
 * 新增告警规则
 */
export const addRule = (params: AddAlertRuleParams): Promise<void> => {
  return request.post('/alert/addRule', { body: params })
}

/**
 * 更新告警规则
 */
export const updateRule = (params: AddAlertRuleParams): Promise<void> => {
  return request.post('/alert/updateRule', { body: params })
}

/**
 * 删除告警规则
 */
export const deleteRule = (id: number): Promise<void> => {
  return request.post('/alert/deleteRule', { body: { id } })
}

/**
 * 切换告警规则启用状态
 */
export const toggleRule = (id: number, enabled: number): Promise<void> => {
  return request.post('/alert/toggleRule', { body: { id, enabled } })
}

/**
 * 查询告警历史
 */
export const getHistory = (params: QueryAlertHistoryParams = {}): Promise<AlertHistoryListResult> => {
  return request.post('/alert/getHistory', { body: params })
}

/**
 * 获取当前触发中的告警
 */
export const getFiring = (): Promise<AlertHistory[]> => {
  return request.post('/alert/getFiring', { body: {} })
}

/**
 * 确认告警
 */
export const acknowledge = (id: number): Promise<void> => {
  return request.post('/alert/acknowledge', { body: { id } })
}

// ==================== 告警 API 对象 ====================

export const alertApi = {
  getRules,
  addRule,
  updateRule,
  deleteRule,
  toggleRule,
  getHistory,
  getFiring,
  acknowledge,
}

// ==================== 辅助常量 ====================

/**
 * 规则类型选项
 */
export const RULE_TYPE_OPTIONS = [
  { value: 'RESOURCE', label: '资源告警' },
  { value: 'PERFORMANCE', label: '性能告警' },
  { value: 'ERROR', label: '错误告警' },
  { value: 'INSTANCE', label: '实例告警' },
]

/**
 * 监控指标选项
 */
export const METRIC_OPTIONS = [
  { value: 'cpuUsage', label: 'CPU使用率 (%)' },
  { value: 'memoryUsage', label: '内存使用率 (%)' },
  { value: 'p95ResponseTime', label: 'P95响应时间 (ms)' },
  { value: 'p99ResponseTime', label: 'P99响应时间 (ms)' },
  { value: 'errorRate', label: '错误率 (%)' },
]

/**
 * 操作符选项
 */
export const OPERATOR_OPTIONS = [
  { value: 'gt', label: '大于' },
  { value: 'lt', label: '小于' },
  { value: 'gte', label: '大于等于' },
  { value: 'lte', label: '小于等于' },
]

/**
 * 严重程度选项
 */
export const SEVERITY_OPTIONS = [
  { value: 'INFO', label: '信息' },
  { value: 'WARNING', label: '警告' },
  { value: 'ERROR', label: '严重' },
]

/**
 * 告警状态选项
 */
export const STATUS_OPTIONS = [
  { value: 'FIRING', label: '触发中', color: 'danger' },
  { value: 'RESOLVED', label: '已恢复', color: 'success' },
  { value: 'ACKNOWLEDGED', label: '已确认', color: 'info' },
]

/**
 * 通知渠道选项
 */
export const NOTIFY_CHANNEL_OPTIONS = [
  { value: 'IN_APP', label: '站内通知' },
  { value: 'EMAIL', label: '邮件通知' },
  { value: 'WEBHOOK', label: 'Webhook' },
]