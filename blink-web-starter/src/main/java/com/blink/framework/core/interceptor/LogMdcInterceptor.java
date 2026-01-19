package com.blink.framework.core.interceptor;

import com.blink.framework.core.data.CoreConstant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

import static com.blink.framework.common.constrant.SysConstant.*;

/**
 * 日志MDC设置变量
 *
 * @Author binblink
 */
public class LogMdcInterceptor implements HandlerInterceptor {

    // 请求开始时设置 MDC 上下文
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 设置 traceId（优先从请求头取，没有则生成）
        String traceId = request.getHeader(X_BLINK_TRACE_ID);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put("traceId", traceId);

        // 2. 设置 userId（从 Token 解析，示例用固定值）
        String userName = request.getHeader(X_BLINK_LOGINNAME);
        MDC.put("userName", userName);

        return true;
    }

    // 请求结束后清空 MDC（关键：避免线程池复用导致脏数据）
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        MDC.clear();
    }

}
