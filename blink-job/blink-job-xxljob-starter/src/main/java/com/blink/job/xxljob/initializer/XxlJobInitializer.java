package com.blink.job.xxljob.initializer;

import cn.hutool.core.collection.CollUtil;
import com.blink.job.api.dto.JobInfo;
import com.blink.job.api.enums.JobType;
import com.blink.job.api.job.BlinkJob;
import com.blink.job.core.registry.JobRegistry;
import com.blink.job.xxljob.handler.BlinkXxlJobHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * XXL-Job 调度初始化器
 * 在所有单例 Bean 初始化完成后，注册任务到 Handler
 *
 * @author binblink
 */
@Slf4j
@Component
public class XxlJobInitializer implements SmartInitializingSingleton {

    private final BlinkXxlJobHandler xxlJobHandler;
    private final JobRegistry jobRegistry;
    private final List<BlinkJob> blinkJobs;

    public XxlJobInitializer(BlinkXxlJobHandler xxlJobHandler,
                             JobRegistry jobRegistry,
                             List<BlinkJob> blinkJobs) {
        this.xxlJobHandler = xxlJobHandler;
        this.jobRegistry = jobRegistry;
        this.blinkJobs = blinkJobs;
    }

    @Override
    public void afterSingletonsInstantiated() {
        log.info("[XxlJobInitializer] 开始初始化定时任务...");

        // 1. 注册注解任务到 Handler
        registerAnnotatedJobs();

        // 2. 注册 BlinkJob 接口实现
        registerBlinkJobs();

        log.info("[XxlJobInitializer] 定时任务初始化完成，共 {} 个任务", jobRegistry.getAllJobs().size());
    }

    /**
     * 注册注解任务到 Handler
     */
    private void registerAnnotatedJobs() {
        List<JobInfo> jobs = jobRegistry.getAllJobs();
        for (JobInfo jobInfo : jobs) {
            xxlJobHandler.registerJob(jobInfo);
        }
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
                log.debug("[XxlJobInitializer] 任务已存在，跳过注册 | jobName: {}", blinkJob.getName());
                xxlJobHandler.registerBlinkJob(blinkJob);
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
            xxlJobHandler.registerJob(jobInfo);
            xxlJobHandler.registerBlinkJob(blinkJob);

            log.info("[XxlJobInitializer] 注册 BlinkJob | jobName: {}, bean: {}",
                    blinkJob.getName(), beanName);
        }
    }
}
