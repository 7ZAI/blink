package com.blink.base.controller;

import com.blink.base.dto.req.ApprovalLeaveReq;
import com.blink.base.dto.req.LeaveRequestIdReq;
import com.blink.base.dto.req.QueryLeaveReq;
import com.blink.base.dto.req.SubmitLeaveReq;
import com.blink.base.dto.rsp.LeaveRequestRsp;
import com.blink.base.dto.vo.LeaveRequestVO;
import com.blink.base.service.LeaveRequestService;
import com.blink.framework.common.context.BlinkRequestContextHolder;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 请假申请 Controller
 *
 * @author binblink
 */
@Slf4j
@RestController
@RequestMapping("/leave")
@Validated
public class LeaveRequestController {

    @Resource
    private LeaveRequestService leaveRequestService;

    /**
     * 提交请假申请
     *
     * @param reqDto 请求参数
     * @return 响应
     */
    @PostMapping("/submit")
    public ResponseDTO<EmptyBody> submitLeave(@RequestBody @Valid RequestDTO<SubmitLeaveReq> reqDto) {
        log.info("[LeaveRequest] 收到提交请假申请请求 | applicant: {}",
                BlinkRequestContextHolder.getLoginName());
        leaveRequestService.submitLeave(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 查询我的请假列表
     *
     * @param reqDto 请求参数
     * @return 请假列表
     */
    @PostMapping("/getMyList")
    public ResponseDTO<LeaveRequestRsp> getMyLeaveList(@RequestBody RequestDTO<QueryLeaveReq> reqDto) {
        LeaveRequestRsp rsp = leaveRequestService.getMyLeaveList(reqDto.getBody());
        return ResponseDTO.newSuccessInstance(rsp);
    }

    /**
     * 查询待审批列表
     *
     * @param reqDto 请求参数
     * @return 待审批列表
     */
    @PostMapping("/getPendingList")
    public ResponseDTO<LeaveRequestRsp> getPendingApprovalList(@RequestBody RequestDTO<QueryLeaveReq> reqDto) {
        LeaveRequestRsp rsp = leaveRequestService.getPendingApprovalList(reqDto.getBody());
        return ResponseDTO.newSuccessInstance(rsp);
    }

    /**
     * 查询请假详情
     *
     * @param reqDto 请求参数
     * @return 请假详情
     */
    @PostMapping("/getDetail")
    public ResponseDTO<LeaveRequestVO> getLeaveDetail(@RequestBody @Valid RequestDTO<LeaveRequestIdReq> reqDto) {
        LeaveRequestVO vo = leaveRequestService.getLeaveDetail(reqDto.getBody().getId());
        return ResponseDTO.newSuccessInstance(vo);
    }

    /**
     * 审批请假
     *
     * @param reqDto 请求参数
     * @return 响应
     */
    @PostMapping("/approval")
    public ResponseDTO<EmptyBody> approvalLeave(@RequestBody @Valid RequestDTO<ApprovalLeaveReq> reqDto) {
        log.info("[LeaveRequest] 收到审批请求 | id: {}, result: {}",
                reqDto.getBody().getLeaveRequestId(), reqDto.getBody().getApprovalResult());
        leaveRequestService.approvalLeave(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 取消请假
     *
     * @param reqDto 请求参数
     * @return 响应
     */
    @PostMapping("/cancel")
    public ResponseDTO<EmptyBody> cancelLeave(@RequestBody @Valid RequestDTO<LeaveRequestIdReq> reqDto) {
        log.info("[LeaveRequest] 收到取消请求 | id: {}", reqDto.getBody().getId());
        leaveRequestService.cancelLeave(reqDto.getBody().getId());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 管理员查询所有请假列表
     *
     * @param reqDto 请求参数
     * @return 请假列表
     */
    @PostMapping("/getAllList")
    public ResponseDTO<LeaveRequestRsp> getAllLeaveList(@RequestBody RequestDTO<QueryLeaveReq> reqDto) {
        LeaveRequestRsp rsp = leaveRequestService.getAllLeaveList(reqDto.getBody());
        return ResponseDTO.newSuccessInstance(rsp);
    }
}
