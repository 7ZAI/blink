package com.blink.framework.core.interceptor;

import com.blink.framework.common.utils.ApplicationContextUtil;
import com.blink.framework.core.data.BlinkRequestContext;
import com.blink.framework.core.data.CoreConstant;
import com.blink.framework.core.util.BlinkRequestContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.HandlerInterceptor;


import java.time.LocalDate;

/**
 * @Author binblink
 * @Date 2025/8/26
 */
public class BlinkRequestContextInterceptor implements HandlerInterceptor {

    private final Logger logger = LoggerFactory.getLogger(BlinkRequestContextInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        logger.debug("Creating BlinkRequestContext");

        // 创建并初始化上下文
        BlinkRequestContext context = new BlinkRequestContext();

        // 设置内容
        context.setRequestId(request.getHeader(CoreConstant.X_BLINK_REQUEST_ID));
        context.setClientIp(request.getHeader(CoreConstant.X_BLINK_CLIENTIP));
        context.setChannel(request.getHeader(CoreConstant.X_BLINK_CHANNEL));
        context.setUserId(request.getHeader(CoreConstant.X_BLINK_USRID));
        context.setLoginName(request.getHeader(CoreConstant.X_BLINK_LOGINNAME));
        context.setTraceId(request.getHeader(CoreConstant.X_BLINK_TRACE_ID));
        context.setLanguage(request.getHeader(CoreConstant.X_BLINK_LOCALE));
        context.setRequestDate(LocalDate.now());
        context.setAppName(getAppName());

        // 设置到上下文持有器
        BlinkRequestContextHolder.setContext(context);
        return true;
    }

    private String getAppName(){
        Environment environment = ApplicationContextUtil.getBean(Environment.class);
        return environment.getProperty(CoreConstant.APP_NAME_PROPERTY);
    }

  

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求完成后清理上下文，防止内存泄漏
        BlinkRequestContextHolder.clearContext();
        logger.debug("Clear BlinkRequestContext");
    }


    
}
