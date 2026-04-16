package com.blink.base.dto.rsp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务信息 VO
 *
 * @author binblink
 */
@Data
public class SysJobVO {

    private Long jobId;

    private String jobName;

    private String jobGroup;

    private String jobDescription;

    private String cronExpression;

    private Byte jobStatus;

    private Byte jobType;

    private String targetBean;

    private String targetMethod;

    private Byte enabled;

    private Long timeout;

    private Integer retryCount;

    private Long retryInterval;

    private String parameters;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
