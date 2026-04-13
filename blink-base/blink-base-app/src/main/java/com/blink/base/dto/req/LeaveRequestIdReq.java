package com.blink.base.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 请假申请ID请求
 *
 * @author binblink
 */
@Data
public class LeaveRequestIdReq {

    /**
     * 请假申请ID
     */
    @NotNull(message = "请假申请ID不能为空")
    private Integer id;
}
