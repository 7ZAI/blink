import request from '@/utils/request'
import type { LeaveRequestVO, LeaveRequestRsp, QueryLeaveReq, SubmitLeaveReq, ApprovalLeaveReq } from './types/leave'

/**
 * 提交请假申请
 */
export function submitLeave(data: SubmitLeaveReq) {
  return request.post('/leave/submit', { body: data })
}

/**
 * 查询我的请假列表
 */
export function getMyLeaveList(data: QueryLeaveReq) {
  return request.post<LeaveRequestRsp>('/leave/getMyList', { body: data })
}

/**
 * 查询待审批列表
 */
export function getPendingList(data: QueryLeaveReq) {
  return request.post<LeaveRequestRsp>('/leave/getPendingList', { body: data })
}

/**
 * 查询请假详情
 */
export function getLeaveDetail(id: number) {
  return request.post<LeaveRequestVO>('/leave/getDetail', { body: { id } })
}

/**
 * 审批请假
 */
export function approvalLeave(data: ApprovalLeaveReq) {
  return request.post('/leave/approval', { body: data })
}

/**
 * 取消请假
 */
export function cancelLeave(id: number) {
  return request.post('/leave/cancel', { body: { id } })
}

/**
 * 管理员查询所有请假列表
 */
export function getAllLeaveList(data: QueryLeaveReq) {
  return request.post<LeaveRequestRsp>('/leave/getAllList', { body: data })
}
