package com.blink.job.api.annotation;

import java.lang.annotation.*;

/**
 * Blink 定时任务注解
 * 支持在任意 Spring Bean 方法上标记，框架自动注册为定时任务
 *
 * @author binblink
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BlinkScheduled {

    /**
     * 任务名称（唯一标识）
     */
    String name();

    /**
     * Cron 表达式
     */
    String cron();

    /**
     * 任务描述
     */
    String description() default "";

    /**
     * 是否启用
     */
    boolean enabled() default true;

    /**
     * 超时时间（毫秒），-1 表示不超时
     */
    long timeout() default -1;

    /**
     * 失败重试次数
     */
    int retryCount() default 0;

    /**
     * 重试间隔（毫秒）
     */
    long retryInterval() default 1000;

    /**
     * 任务分组（用于分类管理）
     */
    String group() default "default";
}
