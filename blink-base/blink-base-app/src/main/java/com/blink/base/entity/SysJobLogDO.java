package com.blink.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 定时任务日志实体
 *
 * @author binblink
 */
@Data
@TableName("sys_job_log")
public class SysJobLogDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
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
