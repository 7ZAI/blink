package com.blink.job.spring.scheduler;

import com.blink.job.api.dto.JobContext;
import com.blink.job.api.dto.JobInfo;
import com.blink.job.api.enums.JobStatus;
import com.blink.job.core.executor.JobExecutor;
import com.blink.job.core.registry.JobRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Spring 原生调度器
 * 基于 ThreadPoolTaskScheduler 实现任务调度
 *
 * @author binblink
 */
@Slf4j
public class SpringJobScheduler {

    private final ThreadPoolTaskScheduler taskScheduler;
    private final JobExecutor jobExecutor;
    private final JobRegistry jobRegistry;

    /**
     * 已调度的任务映射（用于取消任务）
     */
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public SpringJobScheduler(ThreadPoolTaskScheduler taskScheduler,
                              JobExecutor jobExecutor,
                              JobRegistry jobRegistry) {
        this.taskScheduler = taskScheduler;
        this.jobExecutor = jobExecutor;
        this.jobRegistry = jobRegistry;
    }

    /**
     * 调度任务
     *
     * @param jobInfo 任务信息
     */
    public void schedule(JobInfo jobInfo) {
        if (!Boolean.TRUE.equals(jobInfo.getEnabled())) {
            log.info("[SpringJobScheduler] 任务未启用，跳过调度 | jobName: {}", jobInfo.getName());
            return;
        }

        if (jobInfo.getStatus() == JobStatus.PAUSED) {
            log.info("[SpringJobScheduler] 任务已暂停，跳过调度 | jobName: {}", jobInfo.getName());
            return;
        }

        String key = buildKey(jobInfo);

        // 取消已存在的调度
        cancel(jobInfo);

        // 创建任务
        Runnable task = () -> {
            try {
                JobContext context = jobExecutor.buildContext(jobInfo);
                jobExecutor.execute(jobInfo, context);
            } catch (Exception e) {
                log.error("[SpringJobScheduler] 任务执行异常 | jobName: {}", jobInfo.getName(), e);
            }
        };

        // 创建 Cron 触发器
        Trigger trigger = new Trigger() {
            @Override
            public Date nextExecutionTime(TriggerContext triggerContext) {
                // 获取任务最新状态（可能被动态修改）
                JobInfo latestJob = jobRegistry.getJob(jobInfo.getName(), jobInfo.getGroup());
                if (latestJob != null && latestJob.getStatus() == JobStatus.PAUSED) {
                    return null; // 暂停任务
                }

                // 解析 Cron 表达式
                org.springframework.scheduling.support.CronExpression cronExpression =
                        org.springframework.scheduling.support.CronExpression.parse(jobInfo.getCron());

                LocalDateTime lastTime = triggerContext.lastCompletionTime() != null
                        ? LocalDateTime.ofInstant(triggerContext.lastCompletionTime().toInstant(), ZoneId.systemDefault())
                        : LocalDateTime.now();

                LocalDateTime nextTime = cronExpression.next(lastTime);
                if (nextTime == null) {
                    return null;
                }
                return Date.from(nextTime.atZone(ZoneId.systemDefault()).toInstant());
            }
        };

        // 调度任务
        ScheduledFuture<?> future = taskScheduler.schedule(task, trigger);
        scheduledTasks.put(key, future);

        log.info("[SpringJobScheduler] 任务已调度 | jobName: {}, cron: {}", jobInfo.getName(), jobInfo.getCron());
    }

    /**
     * 取消调度
     *
     * @param jobInfo 任务信息
     */
    public void cancel(JobInfo jobInfo) {
        String key = buildKey(jobInfo);
        ScheduledFuture<?> future = scheduledTasks.remove(key);
        if (future != null) {
            future.cancel(false);
            log.info("[SpringJobScheduler] 任务已取消 | jobName: {}", jobInfo.getName());
        }
    }

    /**
     * 暂停任务
     */
    public void pause(JobInfo jobInfo) {
        jobRegistry.updateStatus(jobInfo.getName(), jobInfo.getGroup(), JobStatus.PAUSED);
        cancel(jobInfo);
        log.info("[SpringJobScheduler] 任务已暂停 | jobName: {}", jobInfo.getName());
    }

    /**
     * 恢复任务
     */
    public void resume(JobInfo jobInfo) {
        jobRegistry.updateStatus(jobInfo.getName(), jobInfo.getGroup(), JobStatus.NORMAL);
        JobInfo latestJob = jobRegistry.getJob(jobInfo.getName(), jobInfo.getGroup());
        if (latestJob != null) {
            schedule(latestJob);
        }
        log.info("[SpringJobScheduler] 任务已恢复 | jobName: {}", jobInfo.getName());
    }

    /**
     * 立即执行一次
     */
    public void trigger(JobInfo jobInfo) {
        taskScheduler.execute(() -> {
            try {
                JobContext context = jobExecutor.buildContext(jobInfo);
                jobExecutor.execute(jobInfo, context);
            } catch (Exception e) {
                log.error("[SpringJobScheduler] 手动触发执行异常 | jobName: {}", jobInfo.getName(), e);
            }
        });
        log.info("[SpringJobScheduler] 任务已手动触发 | jobName: {}", jobInfo.getName());
    }

    private String buildKey(JobInfo jobInfo) {
        return jobInfo.getGroup() + ":" + jobInfo.getName();
    }
}
