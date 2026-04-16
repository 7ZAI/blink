package com.blink.job.xxljob.config;

import com.blink.job.core.executor.JobExecutor;
import com.blink.job.core.registry.JobRegistry;
import com.blink.job.xxljob.handler.BlinkXxlJobHandler;
import com.blink.job.xxljob.initializer.XxlJobInitializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * XXL-Job 调度自动配置
 *
 * @author binblink
 */
@AutoConfiguration
@ConditionalOnClass(name = "com.xxl.job.core.handler.annotation.XxlJob")
public class XxlJobAutoConfiguration {

    @Bean
    public BlinkXxlJobHandler blinkXxlJobHandler(JobExecutor jobExecutor) {
        return new BlinkXxlJobHandler(jobExecutor);
    }

    @Bean
    public XxlJobInitializer xxlJobInitializer(
            BlinkXxlJobHandler blinkXxlJobHandler,
            JobRegistry jobRegistry,
            List<com.blink.job.api.job.BlinkJob> blinkJobs) {
        return new XxlJobInitializer(blinkXxlJobHandler, jobRegistry, blinkJobs);
    }
}
