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
export function getJobList(data: QuerySysJobReq) {
  return request<ApiResponse<PageResult<SysJobVO>>>({
    url: '/job/getJobList',
    method: 'post',
    data
  })
}

/** 新增任务 */
export function addJob(data: AddSysJobReq) {
  return request<ApiResponse<void>>({
    url: '/job/addJob',
    method: 'post',
    data
  })
}

/** 更新任务 */
export function updateJob(data: UpdateSysJobReq) {
  return request<ApiResponse<void>>({
    url: '/job/updateJob',
    method: 'post',
    data
  })
}

/** 删除任务 */
export function deleteJob(data: DeleteSysJobReq) {
  return request<ApiResponse<void>>({
    url: '/job/deleteJob',
    method: 'post',
    data
  })
}

/** 暂停任务 */
export function pauseJob(data: JobIdReq) {
  return request<ApiResponse<void>>({
    url: '/job/pauseJob',
    method: 'post',
    data
  })
}

/** 恢复任务 */
export function resumeJob(data: JobIdReq) {
  return request<ApiResponse<void>>({
    url: '/job/resumeJob',
    method: 'post',
    data
  })
}

/** 立即执行 */
export function triggerJob(data: JobIdReq) {
  return request<ApiResponse<void>>({
    url: '/job/triggerJob',
    method: 'post',
    data
  })
}

/** 获取日志列表 */
export function getLogList(data: QuerySysJobLogReq) {
  return request<ApiResponse<PageResult<SysJobLogVO>>>({
    url: '/job/getLogList',
    method: 'post',
    data
  })
}
