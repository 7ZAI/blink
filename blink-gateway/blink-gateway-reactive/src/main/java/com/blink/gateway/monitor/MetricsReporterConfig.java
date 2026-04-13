package com.blink.gateway.monitor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 指标上报异步配置
 *
 * @author binblink
 * @since 2026-04-14
 */
@Configuration
@EnableScheduling
@EnableAsync
@Slf4j
public class MetricsReporterConfig {

    /**
     * 指标上报专用线程池
     */
    @Bean("metricsReporterExecutor")
    public Executor metricsReporterExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(10);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("metrics-reporter-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        log.info("[MetricsReporterConfig] 指标上报线程池初始化完成");
        return executor;
    }
}
