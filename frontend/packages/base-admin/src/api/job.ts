import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types'

// ==================== 类型定义 ====================

/** 任务信息 */
export interface SysJobVO {
  jobId: number
  jobName: string
  jobGroup: string
  jobDescription: string
  cronExpression: string
  jobStatus: number
  jobType: number
  targetBean: string
  targetMethod: string
  enabled: number
  timeout: number
  retryCount: number
  retryInterval: number
  parameters: string
  createTime: string
  updateTime: string
}

/** 任务日志信息 */
export interface SysJobLogVO {
  logId: number
  jobId: number
  jobName: string
  jobGroup: string
  triggerTime: string
  finishTime: string
  duration: number
  status: number
  executeCount: number
  resultMessage: string
  errorMessage: string
  createTime: string
}

/** 查询任务列表请求 */
export interface QuerySysJobReq {
  pageNum: number
  pageSize: number
  jobName?: string
  jobGroup?: string
  jobStatus?: number
}

/** 新增任务请求 */
export interface AddSysJobReq {
  jobName: string
  jobGroup?: string
  jobDescription?: string
  cronExpression: string
  targetBean?: string
  targetMethod?: string
  jobType?: number
  enabled?: number
  timeout?: number
  retryCount?: number
  retryInterval?: number
  parameters?: string
}

/** 更新任务请求 */
export interface UpdateSysJobReq {
  jobId: number
  jobName?: string
  jobGroup?: string
  jobDescription?: string
  cronExpression?: string
  enabled?: number
  timeout?: number
  retryCount?: number
  retryInterval?: number
  parameters?: string
}

/** 删除任务请求 */
export interface DeleteSysJobReq {
  jobIds: number[]
}

/** 任务ID请求 */
export interface JobIdReq {
  jobId: number
}

/** 查询日志请求 */
export interface QuerySysJobLogReq {
  pageNum: number
  pageSize: number
  jobId?: number
  jobName?: string
  status?: number
  triggerTimeStart?: string
  triggerTimeEnd?: string
}

// ==================== API 接口 ====================

/** 获取任务列表 */
export function getJobList(data: QuerySysJobReq): Promise<PageResult<SysJobVO>> {
  return request.post('/job/getJobList', { body: data }) as Promise<PageResult<SysJobVO>>
}

/** 新增任务 */
export function addJob(data: AddSysJobReq): Promise<void> {
  return request.post('/job/addJob', { body: data }) as Promise<void>
}

/** 更新任务 */
export function updateJob(data: UpdateSysJobReq): Promise<void> {
  return request.post('/job/updateJob', { body: data }) as Promise<void>
}

/** 删除任务 */
export function deleteJob(data: DeleteSysJobReq): Promise<void> {
  return request.post('/job/deleteJob', { body: data }) as Promise<void>
}

/** 暂停任务 */
export function pauseJob(data: JobIdReq): Promise<void> {
  return request.post('/job/pauseJob', { body: data }) as Promise<void>
}

/** 恢复任务 */
export function resumeJob(data: JobIdReq): Promise<void> {
  return request.post('/job/resumeJob', { body: data }) as Promise<void>
}

/** 立即执行 */
export function triggerJob(data: JobIdReq): Promise<void> {
  return request.post('/job/triggerJob', { body: data }) as Promise<void>
}

/** 获取日志列表 */
export function getLogList(data: QuerySysJobLogReq): Promise<PageResult<SysJobLogVO>> {
  return request.post('/job/getLogList', { body: data }) as Promise<PageResult<SysJobLogVO>>
}
