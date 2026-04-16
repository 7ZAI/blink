package com.blink.job.api.dto;

import com.blink.job.api.enums.JobStatus;
import com.blink.job.api.enums.JobType;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务信息（运行时元数据）
 *
 * @author binblink
 */
@Data
public class JobInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 任务ID（持久化用） */
    private Long id;

    /** 任务名称 */
    private String name;

    /** 任务分组 */
    private String group;

    /** 任务描述 */
    private String description;

    /** Cron 表达式 */
    private String cron;

    /** 任务状态 */
    private JobStatus status;

    /** 执行目标类型: METHOD(注解方法) / BEAN(接口实现) */
    private JobType type;

    /** 执行目标: Bean名称 */
    private String targetBean;

    /** 执行目标: 方法名（仅 METHOD 类型） */
    private String targetMethod;

    /** 是否启用 */
    private Boolean enabled;

    /** 超时时间（毫秒） */
    private Long timeout;

    /** 重试次数 */
    private Integer retryCount;

    /** 重试间隔（毫秒） */
    private Long retryInterval;

    /** 任务参数（JSON 格式） */
    private String parameters;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
