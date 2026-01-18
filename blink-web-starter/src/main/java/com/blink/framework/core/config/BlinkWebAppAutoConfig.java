package com.blink.framework.core.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * web应用自动配置类
 *
 * @Author binblink
 */
@AutoConfiguration
@ConditionalOnWebApplication
public class BlinkWebAppAutoConfig {


    /**
     * 如果外部配置了新的ControllerAdvice 覆盖自动配置
     */
    @Bean
    @ConditionalOnMissingBean(annotation = ControllerAdvice.class)
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    /**
     * 如果外部配置了新的EnableWebMvc 覆盖自动配置
     */
    @Bean
    @ConditionalOnMissingBean(annotation = EnableWebMvc.class)
    public BlinkWebMvcConfigurer blinkWebMvcConfigurer() {
        return new BlinkWebMvcConfigurer();
    }
}
