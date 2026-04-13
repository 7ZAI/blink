package com.blink.base.dto.req;

import com.blink.framework.common.data.Page;
import lombok.Data;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提交请假申请请求
 *
 * @author binblink
 */
@Data
public class SubmitLeaveReq {

    /**
     * 请假类型：annual-年假/sick-病假/personal-事假/compensatory-调休
     */
    @NotBlank(message = "请假类型不能为空")
    private String leaveType;

    /**
     * 开始时间
     */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startDate;

    /**
     * 结束时间
     */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endDate;

    /**
     * 请假天数
     */
    @NotNull(message = "请假天数不能为空")
    @DecimalMin(value = "0.5", message = "请假天数最少0.5天")
    private BigDecimal days;

    /**
     * 请假原因
     */
    @NotBlank(message = "请假原因不能为空")
    private String reason;
}
