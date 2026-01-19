package com.blink.framework.openfeign.interceptor;


import org.slf4j.MDC;
import feign.RequestInterceptor;
import feign.RequestTemplate;

import static com.blink.framework.common.constrant.SysConstant.*;

/**
 * Feign 全局拦截器 设置请求头
 * 请求DTO 元数据替代类 请求头功能 不需要再拦截设置请求头类
 * @author binblink
 */
public class FeignHeaderInterceptor implements RequestInterceptor {

    //初始放至MDC 在web模块 LogMdcInterceptor 中
    private static final String TRACE_ID_MDC = "traceId";

    @Override
    public void apply(RequestTemplate template) {
        // 从 MDC 获取当前线程的 traceId，放入请求头
        String traceId = MDC.get(TRACE_ID_MDC);

        if (traceId != null && !traceId.isEmpty()) {
            template.header(X_BLINK_TRACE_ID, traceId);
        }
        //来源于feign 调用
//        template.header(X_BLINK_SOURCE,"feign");
//        template.header(X_BLINK_LOGINNAME, BlinkRequestContextHolder.getLoginName());
//        template.header(, BlinkRequestContextHolder.getAppName());
    }
}