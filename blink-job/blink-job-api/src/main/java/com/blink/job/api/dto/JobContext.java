package com.blink.job.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 任务执行上下文
 * 提供任务执行时的环境信息
 *
 * @author binblink
 */
@Getter
@Builder
public class JobContext {

    /** 任务ID（本次执行唯一标识） */
    private final String jobId;

    /** 任务名称 */
    private final String jobName;

    /** 任务分组 */
    private final String jobGroup;

    /** 触发时间 */
    private final LocalDateTime triggerTime;

    /** 上次执行时间 */
    private final LocalDateTime lastExecuteTime;

    /** 任务参数（来自配置或手动触发时传入） */
    private final Map<String, Object> parameters;

    /** 执行次数（第几次重试，0表示首次执行） */
    private final int executeCount;

    /**
     * 创建新的上下文（用于重试）
     */
    public JobContext withExecuteCount(int executeCount) {
        return JobContext.builder()
                .jobId(this.jobId)
                .jobName(this.jobName)
                .jobGroup(this.jobGroup)
                .triggerTime(this.triggerTime)
                .lastExecuteTime(this.lastExecuteTime)
                .parameters(this.parameters)
                .executeCount(executeCount)
                .build();
    }

    /**
     * 获取参数（带默认值）
     */
    public Object getParameter(String key, Object defaultValue) {
        if (parameters == null) {
            return defaultValue;
        }
        return parameters.getOrDefault(key, defaultValue);
    }
}
