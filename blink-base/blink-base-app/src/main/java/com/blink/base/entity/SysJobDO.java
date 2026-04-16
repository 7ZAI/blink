package com.blink.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 定时任务实体
 *
 * @author binblink
 */
@Data
@TableName("sys_job")
public class SysJobDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
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
