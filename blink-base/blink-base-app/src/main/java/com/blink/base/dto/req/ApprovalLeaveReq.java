package com.blink.base.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审批请假请求
 *
 * @author binblink
 */
@Data
public class ApprovalLeaveReq {

    /**
     * 请假申请ID
     */
    @NotNull(message = "请假申请ID不能为空")
    private Integer leaveRequestId;

    /**
     * 审批结果：approved-通过/rejected-拒绝
     */
    @NotBlank(message = "审批结果不能为空")
    private String approvalResult;

    /**
     * 审批意见
     */
    private String approvalComment;
}
