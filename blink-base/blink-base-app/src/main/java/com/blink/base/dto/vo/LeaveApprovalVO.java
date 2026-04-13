package com.blink.base.dto.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 请假审批记录VO
 *
 * @author binblink
 */
@Data
public class LeaveApprovalVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Integer id;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 审批人ID
     */
    private Integer approverId;

    /**
     * 审批人姓名
     */
    private String approverName;

    /**
     * 审批结果
     */
    private String approvalResult;

    /**
     * 审批结果名称
     */
    private String approvalResultName;

    /**
     * 审批意见
     */
    private String approvalComment;

    /**
     * 审批时间
     */
    private LocalDateTime approvalTime;
}
