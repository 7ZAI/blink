package com.blink.base.dto.req;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 任务ID请求
 *
 * @author binblink
 */
@Data
public class JobIdReq {

    @NotNull(message = "任务ID不能为空")
    private Long jobId;
}
