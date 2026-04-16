package com.blink.base.dto.req;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 新增任务请求
 *
 * @author binblink
 */
@Data
public class AddSysJobReq {

    @NotBlank(message = "任务名称不能为空")
    private String jobName;

    private String jobGroup = "default";

    private String jobDescription;

    @NotBlank(message = "Cron表达式不能为空")
    private String cronExpression;

    private String targetBean;

    private String targetMethod;

    private Byte jobType = 1;

    private Byte enabled = 1;

    private Long timeout = -1L;

    private Integer retryCount = 0;

    private Long retryInterval = 1000L;

    private String parameters;
}
