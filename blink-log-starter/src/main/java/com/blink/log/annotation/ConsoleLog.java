package com.blink.log.annotation;

import java.lang.annotation.*;

/**
 *  日志注解 用在方法上 细粒度可控的
 *  入参 或者 出参 全内容记录 不会省略 用在核心业务接口
 *  优先级高于默认的controller切面日志 即如果有注解存在 则切面日志失效
 *
 * @author binblink
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConsoleLog {

    /**
     * 是否记录请求参数
     */
    boolean logRequest() default true;

    /**
     * 是否记录响应结果
     */
    boolean logResponse() default true;

    /**
     * 是否记录耗时
     */
    boolean logCostTime() default true;

    /**
     * 日志级别
     */
    LogLevel level() default LogLevel.INFO;

    enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
}
