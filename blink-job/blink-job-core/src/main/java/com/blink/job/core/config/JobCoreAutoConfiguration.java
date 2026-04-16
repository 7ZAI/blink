package com.blink.job.core.config;

import com.blink.job.core.alarm.JobAlarmHandler;
import com.blink.job.core.alarm.LogAlarmHandler;
import com.blink.job.core.executor.JobExecutor;
import com.blink.job.core.processor.JobAnnotationProcessor;
import com.blink.job.core.registry.DefaultJobRegistry;
import com.blink.job.core.registry.JobRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * 任务核心自动配置
 *
 * @author binblink
 */
@AutoConfiguration
@EnableConfigurationProperties(JobProperties.class)
public class JobCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JobRegistry jobRegistry() {
        return new DefaultJobRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public JobAlarmHandler jobAlarmHandler() {
        return new LogAlarmHandler();
    }

    @Bean
    public JobExecutor jobExecutor(JobRegistry jobRegistry,
                                   JobAlarmHandler alarmHandler,
                                   ApplicationContext applicationContext) {
        return new JobExecutor(jobRegistry, alarmHandler, applicationContext);
    }

    @Bean
    public JobAnnotationProcessor jobAnnotationProcessor(JobRegistry jobRegistry) {
        return new JobAnnotationProcessor(jobRegistry);
    }
}
