import request from '@/utils/request'
import type { LeaveRequestVO, LeaveRequestRsp, QueryLeaveReq, SubmitLeaveReq, ApprovalLeaveReq } from './types/leave'

/**
 * 提交请假申请
 */
export function submitLeave(data: SubmitLeaveReq): Promise<void> {
  return request.post('/leave/submit', { body: data })
}

/**
 * 查询我的请假列表
 */
export function getMyLeaveList(data: QueryLeaveReq): Promise<LeaveRequestRsp> {
  return request.post('/leave/getMyList', { body: data })
}

/**
 * 查询待审批列表
 */
export function getPendingList(data: QueryLeaveReq): Promise<LeaveRequestRsp> {
  return request.post('/leave/getPendingList', { body: data })
}

/**
 * 查询请假详情
 */
export function getLeaveDetail(id: number): Promise<LeaveRequestVO> {
  return request.post('/leave/getDetail', { body: { id } })
}

/**
 * 审批请假
 */
export function approvalLeave(data: ApprovalLeaveReq): Promise<void> {
  return request.post('/leave/approval', { body: data })
}

/**
 * 取消请假
 */
export function cancelLeave(id: number): Promise<void> {
  return request.post('/leave/cancel', { body: { id } })
}

/**
 * 管理员查询所有请假列表
 */
export function getAllLeaveList(data: QueryLeaveReq): Promise<LeaveRequestRsp> {
  return request.post('/leave/getAllList', { body: data })
}
