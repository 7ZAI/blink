package com.blink.job.spring.config;

import com.blink.job.core.executor.JobExecutor;
import com.blink.job.core.registry.JobRegistry;
import com.blink.job.spring.initializer.SpringJobInitializer;
import com.blink.job.spring.scheduler.SpringJobScheduler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Spring 原生调度自动配置
 *
 * @author binblink
 */
@AutoConfiguration
@ConditionalOnClass(ThreadPoolTaskScheduler.class)
public class SpringJobAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "blinkTaskScheduler")
    public ThreadPoolTaskScheduler blinkTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("blink-job-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.initialize();
        return scheduler;
    }

    @Bean
    public SpringJobScheduler springJobScheduler(
            ThreadPoolTaskScheduler blinkTaskScheduler,
            JobExecutor jobExecutor,
            JobRegistry jobRegistry) {
        return new SpringJobScheduler(blinkTaskScheduler, jobExecutor, jobRegistry);
    }

    @Bean
    public SpringJobInitializer springJobInitializer(
            SpringJobScheduler springJobScheduler,
            JobRegistry jobRegistry,
            java.util.List<com.blink.job.api.job.BlinkJob> blinkJobs) {
        return new SpringJobInitializer(springJobScheduler, jobRegistry, blinkJobs);
    }
}
