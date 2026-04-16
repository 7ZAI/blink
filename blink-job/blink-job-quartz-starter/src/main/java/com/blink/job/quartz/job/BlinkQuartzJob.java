package com.blink.job.quartz.job;

import cn.hutool.json.JSONUtil;
import com.blink.job.api.dto.JobContext;
import com.blink.job.api.dto.JobInfo;
import com.blink.job.core.executor.JobExecutor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

/**
 * Quartz Job 包装类
 * 将 Quartz Job 调用委托给 Blink JobExecutor
 *
 * @author binblink
 */
@Slf4j
@Component
public class BlinkQuartzJob implements Job {

    /**
     * JobDataMap 中存储 JobInfo 的 Key
     */
    public static final String JOB_INFO_KEY = "JOB_INFO";

    private static org.springframework.context.ApplicationContext applicationContext;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        String jobInfoJson = dataMap.getString(JOB_INFO_KEY);

        if (jobInfoJson == null || jobInfoJson.isEmpty()) {
            log.error("[BlinkQuartzJob] 任务信息为空，跳过执行");
            return;
        }

        JobInfo jobInfo = JSONUtil.toBean(jobInfoJson, JobInfo.class);

        // 获取 JobExecutor
        JobExecutor jobExecutor = applicationContext.getBean(JobExecutor.class);

        // 构建执行上下文
        JobContext jobContext = JobContext.builder()
                .jobId(UUID.randomUUID().toString())
                .jobName(jobInfo.getName())
                .jobGroup(jobInfo.getGroup())
                .triggerTime(LocalDateTime.now())
                .lastExecuteTime(getLastFireTime(context))
                .executeCount(0)
                .build();

        log.info("[BlinkQuartzJob] 开始执行任务 | jobName: {}, jobGroup: {}",
                jobInfo.getName(), jobInfo.getGroup());

        // 执行任务
        jobExecutor.execute(jobInfo, jobContext);
    }

    /**
     * 获取上次执行时间
     */
    private LocalDateTime getLastFireTime(JobExecutionContext context) {
        Date previousFireTime = context.getPreviousFireTime();
        if (previousFireTime != null) {
            return LocalDateTime.ofInstant(previousFireTime.toInstant(),
                    ZoneId.systemDefault());
        }
        return null;
    }

    /**
     * 初始化 Spring 上下文
     */
    public static void init(org.springframework.context.ApplicationContext ctx) {
        applicationContext = ctx;
    }
}
