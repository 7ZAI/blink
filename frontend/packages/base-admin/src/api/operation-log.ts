import request from '@/utils/request'
import type { PageResult } from './user'

// 日志类型枚举
export enum LogType {
  LOGIN = 'LOGIN',
  SYSTEM = 'SYSTEM',
  OPERATION = 'OPERATION',
}

// 日志类型标签映射
export const logTypeLabels: Record<LogType, string> = {
  [LogType.LOGIN]: '登入日志',
  [LogType.SYSTEM]: '系统日志',
  [LogType.OPERATION]: '操作日志',
}

// 执行状态标签映射
export const executeStatusLabels: Record<number, string> = {
  0: '成功',
  1: '失败',
}

// 日志类型选项
export const logTypeOptions = Object.values(LogType).map((type) => ({
  label: logTypeLabels[type],
  value: type,
}))

// 执行状态选项
export const executeStatusOptions = [
  { label: '成功', value: 0 },
  { label: '失败', value: 1 },
]

export interface QueryOperationLogParams {
  pageNum: number
  pageSize: number
  loginName?: string
  logType?: string
  executeStatus?: number
  startTime?: string
  endTime?: string
  keyword?: string
  orderBy?: string
}

export interface OperationLogInfo {
  logId: number
  userId: number
  loginName: string
  logType: string
  logTypeDesc: string
  description: string
  requestUrl: string
  requestMethod: string
  executeStatus: number
  executeStatusDesc: string
  executeTimeMs: number
  ipAddress: string
  userAgent: string
  operationTime: string
}

export interface OperationLogDetail {
  logId: number
  userId: number
  loginName: string
  logType: string
  logTypeDesc: string
  description: string
  requestUrl: string
  requestMethod: string
  requestParams: string
  responseData: string
  executeStatus: number
  executeStatusDesc: string
  errorMsg: string
  executeTimeMs: number
  ipAddress: string
  userAgent: string
  operationTime: string
}

export const getOperationLogList = (
  params: QueryOperationLogParams
): Promise<PageResult<OperationLogInfo>> => {
  return request.post('/sysOperationLog/getOperationLogList', { body: params }) as Promise<
    PageResult<OperationLogInfo>
  >
}

export const getOperationLogDetail = (logId: number): Promise<OperationLogDetail> => {
  return request.post('/sysOperationLog/getOperationLogDetail', {
    body: { logId },
  }) as Promise<OperationLogDetail>
}
