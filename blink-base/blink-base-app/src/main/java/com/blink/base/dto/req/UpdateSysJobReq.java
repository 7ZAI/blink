package com.blink.base.dto.req;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 更新任务请求
 *
 * @author binblink
 */
@Data
public class UpdateSysJobReq {

    @NotNull(message = "任务ID不能为空")
    private Long jobId;

    private String jobName;

    private String jobGroup;

    private String jobDescription;

    private String cronExpression;

    private Byte enabled;

    private Long timeout;

    private Integer retryCount;

    private Long retryInterval;

    private String parameters;
}
