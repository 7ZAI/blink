package com.blink.base.dto.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 请假申请详情VO
 *
 * @author binblink
 */
@Data
public class LeaveRequestVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Integer id;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 申请人ID
     */
    private Integer applicantId;

    /**
     * 申请人姓名
     */
    private String applicantName;

    /**
     * 部门ID
     */
    private Integer deptId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 请假类型
     */
    private String leaveType;

    /**
     * 请假类型名称
     */
    private String leaveTypeName;

    /**
     * 开始时间
     */
    private LocalDateTime startDate;

    /**
     * 结束时间
     */
    private LocalDateTime endDate;

    /**
     * 请假天数
     */
    private BigDecimal days;

    /**
     * 请假原因
     */
    private String reason;

    /**
     * 状态
     */
    private String status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 当前任务节点
     */
    private String currentTask;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 审批记录列表
     */
    private List<LeaveApprovalVO> approvalList;

    /**
     * 是否可取消（待审批状态可取消）
     */
    private Boolean canCancel;

    /**
     * 是否可审批（当前用户是审批人时可审批）
     */
    private Boolean canApprove;
}
