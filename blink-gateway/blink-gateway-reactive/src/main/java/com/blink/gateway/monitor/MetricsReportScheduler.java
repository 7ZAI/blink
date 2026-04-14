package com.blink.gateway.monitor;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

/**
 * 监控指标上报调度器
 *
 * 使用编程式调度，支持动态修改上报间隔
 *
 * @author binblink
 * @since 2026-04-14
 */
@Component
@Slf4j
public class MetricsReportScheduler {

    private final MetricsReporterImpl metricsReporter;
    private final MonitorConfigHolder configHolder;
    private final ThreadPoolTaskScheduler taskScheduler;

    private ScheduledFuture<?> scheduledFuture;

    public MetricsReportScheduler(MetricsReporterImpl metricsReporter,
                                  MonitorConfigHolder configHolder) {
        this.metricsReporter = metricsReporter;
        this.configHolder = configHolder;
        this.taskScheduler = createTaskScheduler();
    }

    /**
     * 创建任务调度器
     */
    private ThreadPoolTaskScheduler createTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("metrics-reporter-");
        scheduler.setDaemon(true);
        scheduler.initialize();
        return scheduler;
    }

    @PostConstruct
    public void start() {
        if (!configHolder.isEnabled()) {
            log.info("[MetricsScheduler] 监控已禁用，跳过调度启动");
            return;
        }

        scheduleReport();
        log.info("[MetricsScheduler] 调度启动 | intervalMs: {}, initialDelayMs: {}",
                configHolder.getIntervalMs(), configHolder.getInitialDelayMs());
    }

    /**
     * 调度指标上报任务
     */
    private void scheduleReport() {
        PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofMillis(configHolder.getIntervalMs()));
        trigger.setInitialDelay(Duration.ofMillis(configHolder.getInitialDelayMs()));

        scheduledFuture = taskScheduler.schedule(() -> {
            try {
                if (configHolder.isEnabled()) {
                    metricsReporter.reportMetrics();
                }
            } catch (Exception e) {
                log.error("[MetricsScheduler] 指标上报异常 | error: {}", e.getMessage(), e);
            }
        }, trigger);
    }

    /**
     * 停止调度
     */
    public void stop() {
        if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
            scheduledFuture.cancel(false);
            log.info("[MetricsScheduler] 调度已停止");
        }
    }

    /**
     * 重启调度（配置变更时调用）
     */
    public void restart() {
        stop();
        if (configHolder.isEnabled()) {
            scheduleReport();
            log.info("[MetricsScheduler] 调度已重启 | intervalMs: {}", configHolder.getIntervalMs());
        }
    }

    @PreDestroy
    public void destroy() {
        stop();
        taskScheduler.shutdown();
        log.info("[MetricsScheduler] 调度器已销毁");
    }
}
