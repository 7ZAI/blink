package com.blink.framework.core.interceptor;

import com.blink.framework.common.context.BlinkRequestContext;
import com.blink.framework.common.context.BlinkRequestContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.HandlerInterceptor;


import java.time.LocalDate;

import static com.blink.framework.common.constrant.SysConstant.*;

/**
 * @Author binblink
 * @Date 2025/8/26
 */
@Slf4j
public class BlinkRequestContextInterceptor implements HandlerInterceptor {

    @Value("${spring.application.name}")
    private String appName;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        log.debug("Creating BlinkRequestContext");
        // 创建并初始化上下文
        BlinkRequestContext context = new BlinkRequestContext();

        // 设置内容
        context.setRequestId(request.getHeader(X_BLINK_REQUEST_ID));
        context.setClientIp(request.getHeader(X_BLINK_CLIENTIP));
        context.setChannel(request.getHeader(X_BLINK_CHANNEL));
        context.setUserId(request.getHeader(X_BLINK_USRID));
        context.setLoginName(request.getHeader(X_BLINK_LOGINNAME));
        context.setTraceId(request.getHeader(X_BLINK_TRACE_ID));
        context.setLanguage(request.getHeader(X_BLINK_LOCALE));
        context.setRequestDate(LocalDate.now());
        context.setAppName(appName);

        // 设置到上下文持有器
        BlinkRequestContextHolder.setContext(context);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求完成后清理上下文，防止内存泄漏
        BlinkRequestContextHolder.clearContext();
        log.debug("Clear BlinkRequestContext");
    }


    
}
