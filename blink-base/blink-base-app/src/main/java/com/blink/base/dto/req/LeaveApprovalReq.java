package com.blink.base.dto.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 请假审批请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
@ToString
public class LeaveApprovalReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 申请人ID
     */
    @NotBlank(message = "申请人ID不能为空")
    private String applicantId;

    /**
     * 申请人姓名
     */
    @NotBlank(message = "申请人姓名不能为空")
    private String applicantName;

    /**
     * 请假类型（1-事假 2-病假 3-年假 4-调休）
     */
    @NotNull(message = "请假类型不能为空")
    private Integer leaveType;

    /**
     * 开始日期
     */
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    /**
     * 结束日期
     */
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    /**
     * 请假天数
     */
    @NotNull(message = "请假天数不能为空")
    @Min(value = 1, message = "请假天数至少为1天")
    private Integer leaveDays;

    /**
     * 请假原因
     */
    @NotBlank(message = "请假原因不能为空")
    private String reason;
}
