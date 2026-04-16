package com.blink.job.core.constants;

/**
 * 任务调度配置常量
 *
 * @author binblink
 */
public interface JobConstant {

    /**
     * 默认重试间隔（毫秒）
     */
    Long DEFAULT_RETRY_INTERVAL_MS = 1000L;

    /**
     * 默认执行超时时间（毫秒）
     */
    Long DEFAULT_TIMEOUT_MS = 30000L;

    /**
     * 默认重试次数
     */
    Integer DEFAULT_RETRY_COUNT = 3;
}