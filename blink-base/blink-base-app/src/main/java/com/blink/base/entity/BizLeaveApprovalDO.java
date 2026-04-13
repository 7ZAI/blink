package com.blink.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 请假审批记录实体类
 *
 * @author binblink
 */
@Data
@TableName("biz_leave_approval")
public class BizLeaveApprovalDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 请假申请ID
     */
    private Integer leaveRequestId;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 任务ID
     */
    private String taskId;

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
     * 审批结果：approved-通过/rejected-拒绝
     */
    private String approvalResult;

    /**
     * 审批意见
     */
    private String approvalComment;

    /**
     * 审批时间
     */
    private LocalDateTime approvalTime;
}
