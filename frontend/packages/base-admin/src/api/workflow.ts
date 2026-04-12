import request from '@/utils/request'
import type { PageResult } from '@/types'

// ==================== 类型定义 ====================

/**
 * 流程定义信息
 */
export interface ProcessDefinitionInfo {
  processDefinitionId: string
  processDefinitionKey: string
  processDefinitionName: string
  description?: string
  version: number
  deploymentId: string
  deploymentTime: string
  suspended: boolean
  latestVersion: boolean
}

/**
 * 流程实例信息
 */
export interface ProcessInstanceInfo {
  processInstanceId: string
  processDefinitionId: string
  processDefinitionKey: string
  processDefinitionName: string
  businessKey?: string
  currentActivityName?: string
  startTime: string
  endTime?: string
  startUserId: string
  startUserName?: string
  status: 'running' | 'completed' | 'terminated'
  processVariables?: Record<string, any>
}

/**
 * 任务信息
 */
export interface TaskInfo {
  taskId: string
  taskName: string
  taskDescription?: string
  processInstanceId: string
  processDefinitionId: string
  processName?: string
  assignee?: string
  createTime: string
  dueDate?: string
  priority?: number
  processVariables?: Record<string, any>
}

/**
 * 历史任务信息
 */
export interface HistoricTaskInfo {
  taskId: string
  taskName: string
  taskDescription?: string
  processInstanceId: string
  processDefinitionId: string
  processDefinitionKey?: string
  processName?: string
  assignee?: string
  assigneeName?: string
  createTime: string
  endTime?: string
  durationInMillis?: number
  deleteReason?: string
  priority?: number
}

/**
 * 流程历史节点信息
 */
export interface ProcessHistoryInfo {
  activityId: string
  activityName: string
  activityType: string
  taskId?: string
  assignee?: string
  startTime: string
  endTime?: string
  durationInMillis?: number
  comment?: string
  status: 'completed' | 'pending'
}

// ==================== 请求参数类型 ====================

/** 查询流程定义参数 */
export interface QueryProcessDefinitionParams {
  pageNum?: number
  pageSize?: number
  name?: string
  key?: string
  latestVersion?: boolean
}

/** 查询流程实例参数 */
export interface QueryProcessInstanceParams {
  pageNum?: number
  pageSize?: number
  processDefinitionKey?: string
  startUserId?: string
  status?: 'running' | 'completed' | 'all'
}

/** 查询任务参数 */
export interface QueryTaskParams {
  pageNum?: number
  pageSize?: number
  userId?: string
  taskName?: string
  processDefinitionKey?: string
}

/** 部署流程参数 */
export interface DeployProcessParams {
  processName: string
  processKey: string
  bpmnXmlContent: string
  description?: string
}

/** 启动流程参数 */
export interface StartProcessParams {
  processDefinitionKey: string
  businessKey?: string
  variables?: Record<string, any>
}

/** 请假审批参数 */
export interface LeaveApprovalParams {
  applicantId: string
  applicantName: string
  leaveType: 1 | 2 | 3 | 4 // 1-事假 2-病假 3-年假 4-调休
  startDate: string
  endDate: string
  leaveDays: number
  reason: string
}

/** 完成任务参数 */
export interface CompleteTaskParams {
  taskId: string
  userId: string
  comment?: string
  approved?: boolean
  variables?: Record<string, any>
}

/** 委托任务参数 */
export interface DelegateTaskParams {
  taskId: string
  currentUserId: string
  targetUserId: string
}

/** 回退流程参数 */
export interface RollbackProcessParams {
  processInstanceId: string
  targetActivityId: string
  reason: string
}

/** 撤回任务参数 */
export interface WithdrawTaskParams {
  taskId: string
  userId: string
  reason?: string
}

/** 导入XML流程参数 */
export interface ImportXmlProcessParams {
  processName: string
  bpmnXmlContent: string
  description?: string
}

// ==================== 流程定义管理 API ====================

/**
 * 部署流程定义
 */
export const deployProcess = (params: DeployProcessParams): Promise<string> => {
  return request.post('/workflow/deployProcess', { body: params }) as Promise<string>
}

/**
 * 分页查询流程定义列表
 */
export const getProcessDefinitionList = (
  params?: QueryProcessDefinitionParams
): Promise<PageResult<ProcessDefinitionInfo>> => {
  return request.post('/workflow/getProcessDefinitionList', { body: params || {} }) as Promise<
    PageResult<ProcessDefinitionInfo>
  >
}

/**
 * 获取流程图XML
 */
export const getProcessDiagramXml = (processDefinitionId: string): Promise<string> => {
  return request.post('/workflow/getProcessDiagramXml', {
    body: { processDefinitionId },
  }) as Promise<string>
}

/**
 * 获取流程实例的流程图图片（高亮当前节点）
 */
export const getProcessDiagramImage = (processInstanceId: string): Promise<string> => {
  return request.post('/workflow/getProcessDiagramImage', {
    body: { processInstanceId },
  }) as Promise<string>
}

/**
 * 挂起流程定义
 */
export const suspendProcessDefinition = (processDefinitionId: string): Promise<void> => {
  return request.post('/workflow/suspendProcessDefinition', {
    body: { processDefinitionId },
  }) as Promise<void>
}

/**
 * 激活流程定义
 */
export const activateProcessDefinition = (processDefinitionId: string): Promise<void> => {
  return request.post('/workflow/activateProcessDefinition', {
    body: { processDefinitionId },
  }) as Promise<void>
}

/**
 * 删除流程定义
 */
export const deleteProcessDefinition = (deploymentId: string, cascade?: boolean): Promise<void> => {
  return request.post('/workflow/deleteProcessDefinition', {
    body: { deploymentId, cascade },
  }) as Promise<void>
}

// ==================== 流程实例管理 API ====================

/**
 * 启动流程实例
 */
export const startProcess = (params: StartProcessParams): Promise<ProcessInstanceInfo> => {
  return request.post('/workflow/startProcess', { body: params }) as Promise<ProcessInstanceInfo>
}

/**
 * 启动请假审批流程
 */
export const startLeaveProcess = (params: LeaveApprovalParams): Promise<ProcessInstanceInfo> => {
  return request.post('/workflow/startLeaveProcess', {
    body: params,
  }) as Promise<ProcessInstanceInfo>
}

/**
 * 分页查询流程实例列表
 */
export const getProcessInstanceList = (
  params?: QueryProcessInstanceParams
): Promise<PageResult<ProcessInstanceInfo>> => {
  return request.post('/workflow/getProcessInstanceList', { body: params || {} }) as Promise<
    PageResult<ProcessInstanceInfo>
  >
}

/**
 * 查询用户发起的流程实例
 */
export const getMyProcessInstances = (
  userId: string,
  status?: 'running' | 'completed' | 'all'
): Promise<ProcessInstanceInfo[]> => {
  return request.post('/workflow/getMyProcessInstances', {
    body: { userId, status: status || 'all' },
  }) as Promise<ProcessInstanceInfo[]>
}

/**
 * 查询流程实例详情
 */
export const getProcessInstanceDetail = (
  processInstanceId: string
): Promise<ProcessInstanceInfo> => {
  return request.post('/workflow/getProcessInstanceDetail', {
    body: { processInstanceId },
  }) as Promise<ProcessInstanceInfo>
}

/**
 * 删除流程实例
 */
export const deleteProcessInstance = (
  processInstanceId: string,
  reason?: string
): Promise<void> => {
  return request.post('/workflow/deleteProcessInstance', {
    body: { processInstanceId, reason },
  }) as Promise<void>
}

/**
 * 回退流程到指定节点
 */
export const rollbackProcess = (params: RollbackProcessParams): Promise<void> => {
  return request.post('/workflow/rollbackProcess', { body: params }) as Promise<void>
}

// ==================== 流程导入 API ====================

/**
 * 导入BPMN XML流程定义
 */
export const importProcessFromXml = (params: ImportXmlProcessParams): Promise<string> => {
  return request.post('/workflow/importProcessFromXml', { body: params }) as Promise<string>
}

// ==================== 任务管理 API ====================

/**
 * 查询用户待办任务
 */
export const getUserTasks = (userId: string): Promise<PageResult<TaskInfo>> => {
  return request.post('/workflow/getUserTasks', { body: { userId } }) as Promise<
    PageResult<TaskInfo>
  >
}

/**
 * 分页查询待办任务
 */
export const getPendingTasks = (params?: QueryTaskParams): Promise<PageResult<TaskInfo>> => {
  return request.post('/workflow/getPendingTasks', { body: params || {} }) as Promise<
    PageResult<TaskInfo>
  >
}

/**
 * 分页查询已办任务
 */
export const getCompletedTasks = (
  params?: QueryTaskParams
): Promise<PageResult<HistoricTaskInfo>> => {
  return request.post('/workflow/getCompletedTasks', { body: params || {} }) as Promise<
    PageResult<HistoricTaskInfo>
  >
}

/**
 * 完成任务
 */
export const completeTask = (params: CompleteTaskParams): Promise<void> => {
  return request.post('/workflow/completeTask', { body: params }) as Promise<void>
}

/**
 * 委托任务
 */
export const delegateTask = (params: DelegateTaskParams): Promise<void> => {
  return request.post('/workflow/delegateTask', { body: params }) as Promise<void>
}

/**
 * 认领任务
 */
export const claimTask = (taskId: string, userId: string): Promise<void> => {
  return request.post('/workflow/claimTask', { body: { taskId, userId } }) as Promise<void>
}

/**
 * 取消认领任务
 */
export const unclaimTask = (taskId: string): Promise<void> => {
  return request.post('/workflow/unclaimTask', { body: { taskId } }) as Promise<void>
}

/**
 * 撤回任务（发起人撤回未处理的任务）
 */
export const withdrawTask = (params: WithdrawTaskParams): Promise<void> => {
  return request.post('/workflow/withdrawTask', { body: params }) as Promise<void>
}

// ==================== 流程历史 API ====================

/**
 * 查询流程历史
 */
export const getProcessHistory = (processInstanceId: string): Promise<ProcessHistoryInfo[]> => {
  return request.post('/workflow/getProcessHistory', { body: { processInstanceId } }) as Promise<
    ProcessHistoryInfo[]
  >
}
