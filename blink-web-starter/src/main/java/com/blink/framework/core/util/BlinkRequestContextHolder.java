package com.blink.framework.core.util;

import com.blink.framework.core.data.BlinkRequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.time.LocalDate;

/**
 *  注意 当使用@Async 或者异步环境下 慎用此类
 * @Author binblink
 * @Date 2025/8/26
 */
@Slf4j
public class BlinkRequestContextHolder {

    private static final ThreadLocal<BlinkRequestContext> contextHolder = new ThreadLocal<>();

    // 私有构造方法防止实例化
    private BlinkRequestContextHolder() {

    }

    public static void setContext(BlinkRequestContext context) {
        Assert.notNull(context, "Only non-null RequestContext instances are permitted");
        contextHolder.set(context);
    }

    public static BlinkRequestContext getContext() {
        BlinkRequestContext context = contextHolder.get();
        if (context == null) {
            context = createEmptyContext();
            contextHolder.set(context);
        }
        return context;
    }

    private static BlinkRequestContext createEmptyContext() {
        return new BlinkRequestContext();
    }


    /**
     * 清除上下文
     */
    public static void clearContext() {
        log.debug("Clear RequestContext");
        contextHolder.remove();
    }

    /**
     * 请求ID
     */
    public static String getRequestId() {
        return getContext().getRequestId();
    }

    /**
     * 追踪ID
     */
    public static String getTraceId() {
        return getContext().getTraceId();
    }

    /**
     * 当前登入用户ID
     */
    public static String getUserId() {
        return getContext().getUserId();
    }

    /**
     * 当前登入用户名
     */
    public static String getLoginName() {
        return getContext().getLoginName();
    }


    /**
     * ip
     */
    public static String getClientIp() {
        return getContext().getClientIp();
    }

    /**
     * 语言环境
     */
    public static String getLanguage() {
        return getContext().getLanguage();
    }


    /**
     * 应用名
     */
    public static String getAppName() {
        return getContext().getAppName();
    }


    /**
     * 请求时间
     */
    public static LocalDate getRequestDate() {
        return getContext().getRequestDate();
    }

    /**
     * 渠道
     */
    public static String getChannel() {
        return getContext().getChannel();
    }



}
