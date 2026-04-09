package com.blink.gateway.base.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.blink.framework.common.exception.BlinkException;
import com.blink.gateway.base.constants.BaseErrCodeConstant;
import com.blink.gateway.base.interceptor.PermissionInterceptor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * SA-Token 配置类
 *
 * @author binblink
 */
@Configuration
@Slf4j
public class SaTokenConfig implements WebMvcConfigurer {

    @Resource
    private PermissionInterceptor permissionInterceptor;

    /**
     * 注册SA-Token拦截器和权限拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 注册 Sa-Token 登录拦截器
        registry.addInterceptor(new SaInterceptor(handle -> {
                    try {
                        StpUtil.checkLogin();
                    } catch (NotLoginException e) {
                        log.error("检查认证错误{}", e.getMessage(), e);
                        // 将 Sa-Token 未登录异常转换为 BlinkException
                        // 由 GlobalExceptionHandler 统一处理
                        BlinkException.throwBusinessException(BaseErrCodeConstant.TOKEN_EXPIRED);
                    }
                }))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",
                        "/auth/captcha",
                        "/auth/getLoginConfig",
                        "/captcha/**",
                        "/error",
                        "/actuator/**",
                        "/doc.html",
                        "/webjars/**",
                        "/swagger-resources/**",
                        "/v3/api-docs/**"
                )
                .order(1);

        // 2. 注册权限拦截器（在登录拦截器之后执行）
        // 注意：BlinkRequestContextInterceptor 由 blink-web-starter 自动注册（order=0）
        // 从请求头获取 userId、loginName 设置到 BlinkRequestContextHolder
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",
                        "/auth/captcha",
                        "/auth/getLoginConfig",
                        "/captcha/**",
                        "/auth/logout",
                        "/auth/getUserInfo",
                        "/auth/firstTimeResetPassword",
                        "/error",
                        "/actuator/**",
                        "/doc.html",
                        "/webjars/**",
                        "/swagger-resources/**",
                        "/v3/api-docs/**"
                )
                .order(10);
    }
}