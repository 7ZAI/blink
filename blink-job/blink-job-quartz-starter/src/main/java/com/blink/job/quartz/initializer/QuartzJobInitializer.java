package com.blink.job.quartz.initializer;

import cn.hutool.core.collection.CollUtil;
import com.blink.job.api.dto.JobInfo;
import com.blink.job.api.enums.JobType;
import com.blink.job.api.job.BlinkJob;
import com.blink.job.core.registry.JobRegistry;
import com.blink.job.quartz.job.BlinkQuartzJob;
import com.blink.job.quartz.scheduler.QuartzJobScheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Quartz 调度初始化器
 * 在所有单例 Bean 初始化完成后，注册并调度所有任务
 *
 * @author binblink
 */
@Slf4j
@Component
public class QuartzJobInitializer implements SmartInitializingSingleton {

    private final QuartzJobScheduler quartzJobScheduler;
    private final JobRegistry jobRegistry;
    private final List<BlinkJob> blinkJobs;
    private final ApplicationContext applicationContext;

    public QuartzJobInitializer(QuartzJobScheduler quartzJobScheduler,
                                JobRegistry jobRegistry,
                                List<BlinkJob> blinkJobs,
                                ApplicationContext applicationContext) {
        this.quartzJobScheduler = quartzJobScheduler;
        this.jobRegistry = jobRegistry;
        this.blinkJobs = blinkJobs;
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        log.info("[QuartzJobInitializer] 开始初始化定时任务...");

        // 初始化 BlinkQuartzJob 的 Spring 上下文
        BlinkQuartzJob.init(applicationContext);

        // 1. 注册 BlinkJob 接口实现
        registerBlinkJobs();

        // 2. 调度所有任务
        scheduleAllJobs();

        log.info("[QuartzJobInitializer] 定时任务初始化完成，共 {} 个任务", jobRegistry.getAllJobs().size());
    }

    /**
     * 注册 BlinkJob 接口实现
     */
    private void registerBlinkJobs() {
        if (CollUtil.isEmpty(blinkJobs)) {
            return;
        }

        for (BlinkJob blinkJob : blinkJobs) {
            String beanName = blinkJob.getClass().getSimpleName();
            beanName = Character.toLowerCase(beanName.charAt(0)) + beanName.substring(1);

            // 检查是否已注册
            if (jobRegistry.isRegistered(blinkJob.getName(), blinkJob.getGroup())) {
                log.debug("[QuartzJobInitializer] 任务已存在，跳过注册 | jobName: {}", blinkJob.getName());
                continue;
            }

            // 构建任务信息
            JobInfo jobInfo = new JobInfo();
            jobInfo.setName(blinkJob.getName());
            jobInfo.setGroup(blinkJob.getGroup());
            jobInfo.setDescription(blinkJob.getDescription());
            jobInfo.setType(JobType.BEAN);
            jobInfo.setTargetBean(beanName);
            jobInfo.setEnabled(true);
            jobInfo.setCreateTime(LocalDateTime.now());
            jobInfo.setUpdateTime(LocalDateTime.now());

            jobRegistry.register(jobInfo);
            log.info("[QuartzJobInitializer] 注册 BlinkJob | jobName: {}, bean: {}",
                    blinkJob.getName(), beanName);
        }
    }

    /**
     * 调度所有任务
     */
    private void scheduleAllJobs() {
        List<JobInfo> jobs = jobRegistry.getAllJobs();
        for (JobInfo jobInfo : jobs) {
            if (jobInfo.getCron() != null && !jobInfo.getCron().isEmpty()) {
                quartzJobScheduler.schedule(jobInfo);
            }
        }
    }
}
