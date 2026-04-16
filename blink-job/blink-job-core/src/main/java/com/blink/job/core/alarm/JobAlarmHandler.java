package com.blink.job.core.alarm;

import com.blink.job.api.dto.JobInfo;
import com.blink.job.api.dto.JobLog;
import com.blink.job.api.enums.AlarmType;

/**
 * 任务告警处理器接口（SPI 扩展点）
 *
 * @author binblink
 */
public interface JobAlarmHandler {

    /**
     * 发送告警
     *
     * @param jobInfo   任务信息
     * @param jobLog    执行日志
     * @param alarmType 告警类型: FAILURE/TIMEOUT/RETRY_EXHAUSTED
     */
    void alarm(JobInfo jobInfo, JobLog jobLog, AlarmType alarmType);
}
