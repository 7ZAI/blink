package com.blink.job.api.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务执行日志
 *
 * @author binblink
 */
@Data
public class JobLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 日志ID */
    private Long logId;

    /** 任务ID */
    private Long jobId;

    /** 任务名称 */
    private String jobName;

    /** 任务分组 */
    private String jobGroup;

    /** 触发时间 */
    private LocalDateTime triggerTime;

    /** 完成时间 */
    private LocalDateTime finishTime;

    /** 执行耗时（毫秒） */
    private Long duration;

    /** 执行状态: 0-执行中, 1-成功, 2-失败 */
    private Byte status;

    /** 执行次数（重试计数） */
    private Integer executeCount;

    /** 执行结果消息 */
    private String resultMessage;

    /** 异常信息 */
    private String errorMessage;

    /** 创建时间 */
    private LocalDateTime createTime;
}
