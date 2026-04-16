package com.blink.job.core.alarm;

import com.blink.job.api.dto.JobInfo;
import com.blink.job.api.dto.JobLog;
import com.blink.job.api.enums.AlarmType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 日志告警处理器（默认实现）
 * 将告警信息输出到日志
 *
 * @author binblink
 */
@Slf4j
@Component
public class LogAlarmHandler implements JobAlarmHandler {

    @Override
    public void alarm(JobInfo jobInfo, JobLog jobLog, AlarmType alarmType) {
        String alarmTypeDesc = alarmType != null ? alarmType.getDesc() : "执行失败";

        log.error("[JobAlarm] 任务告警 | jobName: {}, jobGroup: {}, alarmType: {}, duration: {}ms, message: {}",
                jobInfo.getName(),
                jobInfo.getGroup(),
                alarmTypeDesc,
                jobLog.getDuration() != null ? jobLog.getDuration() : 0,
                jobLog.getErrorMessage());
    }
}
