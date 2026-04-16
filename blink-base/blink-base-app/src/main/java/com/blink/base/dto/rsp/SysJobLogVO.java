package com.blink.base.dto.rsp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务日志 VO
 *
 * @author binblink
 */
@Data
public class SysJobLogVO {

    private Long logId;

    private Long jobId;

    private String jobName;

    private String jobGroup;

    private LocalDateTime triggerTime;

    private LocalDateTime finishTime;

    private Long duration;

    private Byte status;

    private Integer executeCount;

    private String resultMessage;

    private String errorMessage;

    private LocalDateTime createTime;
}
