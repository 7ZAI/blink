package com.blink.job.quartz.scheduler;

import cn.hutool.json.JSONUtil;
import com.blink.job.api.dto.JobInfo;
import com.blink.job.api.enums.JobStatus;
import com.blink.job.core.registry.JobRegistry;
import com.blink.job.quartz.job.BlinkQuartzJob;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;

/**
 * Quartz 调度器
 * 基于 Quartz Scheduler 实现任务调度
 *
 * @author binblink
 */
@Slf4j
public class QuartzJobScheduler {

    private final Scheduler scheduler;
    private final JobRegistry jobRegistry;

    public QuartzJobScheduler(Scheduler scheduler, JobRegistry jobRegistry) {
        this.scheduler = scheduler;
        this.jobRegistry = jobRegistry;
    }

    /**
     * 调度任务
     *
     * @param jobInfo 任务信息
     */
    public void schedule(JobInfo jobInfo) {
        if (!Boolean.TRUE.equals(jobInfo.getEnabled())) {
            log.info("[QuartzJobScheduler] 任务未启用，跳过调度 | jobName: {}", jobInfo.getName());
            return;
        }

        try {
            JobKey jobKey = JobKey.jobKey(jobInfo.getName(), jobInfo.getGroup());

            // 如果已存在，先删除
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }

            // 创建 JobDetail
            JobDetail jobDetail = JobBuilder.newJob(BlinkQuartzJob.class)
                    .withIdentity(jobKey)
                    .withDescription(jobInfo.getDescription())
                    .usingJobData(BlinkQuartzJob.JOB_INFO_KEY, JSONUtil.toJsonStr(jobInfo))
                    .storeDurably(true)
                    .build();

            // 创建 Trigger
            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(jobInfo.getName(), jobInfo.getGroup())
                    .withSchedule(CronScheduleBuilder.cronSchedule(jobInfo.getCron())
                            .withMisfireHandlingInstructionDoNothing())
                    .build();

            // 调度任务
            scheduler.scheduleJob(jobDetail, trigger);

            log.info("[QuartzJobScheduler] 任务已调度 | jobName: {}, cron: {}",
                    jobInfo.getName(), jobInfo.getCron());

        } catch (SchedulerException e) {
            log.error("[QuartzJobScheduler] 任务调度失败 | jobName: {}", jobInfo.getName(), e);
        }
    }

    /**
     * 取消调度
     */
    public void cancel(JobInfo jobInfo) {
        try {
            JobKey jobKey = JobKey.jobKey(jobInfo.getName(), jobInfo.getGroup());
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
                log.info("[QuartzJobScheduler] 任务已取消 | jobName: {}", jobInfo.getName());
            }
        } catch (SchedulerException e) {
            log.error("[QuartzJobScheduler] 取消任务失败 | jobName: {}", jobInfo.getName(), e);
        }
    }

    /**
     * 暂停任务
     */
    public void pause(JobInfo jobInfo) {
        try {
            JobKey jobKey = JobKey.jobKey(jobInfo.getName(), jobInfo.getGroup());
            scheduler.pauseJob(jobKey);
            jobRegistry.updateStatus(jobInfo.getName(), jobInfo.getGroup(), JobStatus.PAUSED);
            log.info("[QuartzJobScheduler] 任务已暂停 | jobName: {}", jobInfo.getName());
        } catch (SchedulerException e) {
            log.error("[QuartzJobScheduler] 暂停任务失败 | jobName: {}", jobInfo.getName(), e);
        }
    }

    /**
     * 恢复任务
     */
    public void resume(JobInfo jobInfo) {
        try {
            JobKey jobKey = JobKey.jobKey(jobInfo.getName(), jobInfo.getGroup());
            scheduler.resumeJob(jobKey);
            jobRegistry.updateStatus(jobInfo.getName(), jobInfo.getGroup(), JobStatus.NORMAL);
            log.info("[QuartzJobScheduler] 任务已恢复 | jobName: {}", jobInfo.getName());
        } catch (SchedulerException e) {
            log.error("[QuartzJobScheduler] 恢复任务失败 | jobName: {}", jobInfo.getName(), e);
        }
    }

    /**
     * 立即执行一次
     */
    public void trigger(JobInfo jobInfo) {
        try {
            JobKey jobKey = JobKey.jobKey(jobInfo.getName(), jobInfo.getGroup());
            scheduler.triggerJob(jobKey);
            log.info("[QuartzJobScheduler] 任务已手动触发 | jobName: {}", jobInfo.getName());
        } catch (SchedulerException e) {
            log.error("[QuartzJobScheduler] 手动触发失败 | jobName: {}", jobInfo.getName(), e);
        }
    }
}
