/**
 * 请假申请相关类型定义
 */

/**
 * 请假类型
 */
export type LeaveType = 'annual' | 'sick' | 'personal' | 'compensatory' | 'marriage' | 'maternity'

/**
 * 请假状态
 */
export type LeaveStatus = 'draft' | 'pending' | 'approved' | 'rejected' | 'cancelled'

/**
 * 审批结果
 */
export type ApprovalResult = 'approved' | 'rejected'

/**
 * 请假申请VO
 */
export interface LeaveRequestVO {
  id: number
  processInstanceId: string
  applicantId: number
  applicantName: string
  deptId: number
  deptName: string
  leaveType: LeaveType
  leaveTypeName: string
  startDate: string
  endDate: string
  days: number
  reason: string
  status: LeaveStatus
  statusName: string
  currentTask: string
  createTime: string
  updateTime: string
  approvalList: LeaveApprovalVO[]
  canCancel: boolean
  canApprove: boolean
}

/**
 * 审批记录VO
 */
export interface LeaveApprovalVO {
  id: number
  taskName: string
  approverId: number
  approverName: string
  approvalResult: ApprovalResult
  approvalResultName: string
  approvalComment: string
  approvalTime: string
}

/**
 * 请假列表响应
 */
export interface LeaveRequestRsp {
  rows: LeaveRequestVO[]
  total: number
  pageNum: number
  pageSize: number
}

/**
 * 查询请假请求
 */
export interface QueryLeaveReq {
  pageNum: number
  pageSize: number
  status?: LeaveStatus
  leaveType?: LeaveType
  applicantId?: number
  applicantName?: string
  deptId?: number
  startDateBegin?: string
  startDateEnd?: string
}

/**
 * 提交请假请求
 */
export interface SubmitLeaveReq {
  leaveType: LeaveType
  startDate: string
  endDate: string
  days: number
  reason: string
}

/**
 * 审批请假请求
 */
export interface ApprovalLeaveReq {
  leaveRequestId: number
  approvalResult: ApprovalResult
  approvalComment?: string
}

/**
 * 请假类型选项
 */
export const LEAVE_TYPE_OPTIONS = [
  { value: 'annual', label: '年假', listClass: 'success' },
  { value: 'sick', label: '病假', listClass: 'warning' },
  { value: 'personal', label: '事假', listClass: 'info' },
  { value: 'compensatory', label: '调休', listClass: 'primary' },
  { value: 'marriage', label: '婚假', listClass: 'danger' },
  { value: 'maternity', label: '产假', listClass: 'success' },
]

/**
 * 请假状态选项
 */
export const LEAVE_STATUS_OPTIONS = [
  { value: 'draft', label: '草稿', listClass: 'info' },
  { value: 'pending', label: '待审批', listClass: 'warning' },
  { value: 'approved', label: '已通过', listClass: 'success' },
  { value: 'rejected', label: '已拒绝', listClass: 'danger' },
  { value: 'cancelled', label: '已取消', listClass: 'info' },
]

/**
 * 获取请假类型标签
 */
export function getLeaveTypeLabel(type: LeaveType): string {
  const option = LEAVE_TYPE_OPTIONS.find((item) => item.value === type)
  return option?.label || type
}

/**
 * 获取请假状态标签
 */
export function getLeaveStatusLabel(status: LeaveStatus): string {
  const option = LEAVE_STATUS_OPTIONS.find((item) => item.value === status)
  return option?.label || status
}

/**
 * 获取请假状态标签类型
 */
export function getLeaveStatusType(status: LeaveStatus): string {
  const option = LEAVE_STATUS_OPTIONS.find((item) => item.value === status)
  return option?.listClass || ''
}
