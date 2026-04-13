package com.blink.base.service;

import com.blink.base.dto.req.ApprovalLeaveReq;
import com.blink.base.dto.req.QueryLeaveReq;
import com.blink.base.dto.req.SubmitLeaveReq;
import com.blink.base.dto.rsp.LeaveRequestRsp;
import com.blink.base.dto.vo.LeaveRequestVO;

/**
 * 请假申请服务接口
 *
 * @author binblink
 */
public interface LeaveRequestService {

    /**
     * 提交请假申请
     *
     * @param req 请假申请信息
     */
    void submitLeave(SubmitLeaveReq req);

    /**
     * 查询我的请假列表
     *
     * @param req 查询条件
     * @return 请假列表
     */
    LeaveRequestRsp getMyLeaveList(QueryLeaveReq req);

    /**
     * 查询待我审批的请假列表
     *
     * @param req 查询条件
     * @return 待审批列表
     */
    LeaveRequestRsp getPendingApprovalList(QueryLeaveReq req);

    /**
     * 查询请假详情
     *
     * @param id 请假申请ID
     * @return 请假详情
     */
    LeaveRequestVO getLeaveDetail(Integer id);

    /**
     * 审批请假
     *
     * @param req 审批信息
     */
    void approvalLeave(ApprovalLeaveReq req);

    /**
     * 取消请假
     *
     * @param id 请假申请ID
     */
    void cancelLeave(Integer id);

    /**
     * 管理员查询所有请假列表
     *
     * @param req 查询条件
     * @return 请假列表
     */
    LeaveRequestRsp getAllLeaveList(QueryLeaveReq req);
}
