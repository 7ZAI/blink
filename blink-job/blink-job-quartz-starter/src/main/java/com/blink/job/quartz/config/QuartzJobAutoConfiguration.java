package com.blink.job.quartz.config;

import com.blink.job.core.executor.JobExecutor;
import com.blink.job.core.registry.JobRegistry;
import com.blink.job.quartz.initializer.QuartzJobInitializer;
import com.blink.job.quartz.scheduler.QuartzJobScheduler;
import org.quartz.Scheduler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Quartz 调度自动配置
 *
 * @author binblink
 */
@AutoConfiguration
@ConditionalOnClass(Scheduler.class)
public class QuartzJobAutoConfiguration {

    @Bean
    public QuartzJobScheduler quartzJobScheduler(Scheduler scheduler, JobRegistry jobRegistry) {
        return new QuartzJobScheduler(scheduler, jobRegistry);
    }

    @Bean
    public QuartzJobInitializer quartzJobInitializer(
            QuartzJobScheduler quartzJobScheduler,
            JobRegistry jobRegistry,
            List<com.blink.job.api.job.BlinkJob> blinkJobs,
            ApplicationContext applicationContext) {
        return new QuartzJobInitializer(quartzJobScheduler, jobRegistry, blinkJobs, applicationContext);
    }
}
