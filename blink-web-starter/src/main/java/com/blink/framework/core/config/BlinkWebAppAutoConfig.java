package com.blink.framework.core.config;

import com.blink.framework.core.aop.BlinkControllerLogAspect;
import com.blink.framework.core.aop.LogExecutionAspect;
import com.blink.framework.core.aop.RedisCacheUpdateAspect;
import com.blink.framework.core.component.RedisCachePreHeatRunner;
import com.blink.framework.core.config.prop.BlinkWebAppConfigProperties;
import com.blink.framework.core.config.prop.ThreadPoolProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * web应用自动配置类
 *
 * @Author binblink
 */
@AutoConfiguration
@ConditionalOnWebApplication
@EnableConfigurationProperties({BlinkWebAppConfigProperties.class})
public class BlinkWebAppAutoConfig {

    /**
     * 如果外部配置了新的RestControllerAdvice 覆盖自动配置
     */
    @Bean
    @ConditionalOnMissingBean(annotation = RestControllerAdvice.class)
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

    /**
     * 注解@CacheDoubleDelete 延迟双删 redis缓存切面
     *
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisCacheUpdateAspect redisCacheUpdateAspect() {
        return new RedisCacheUpdateAspect();
    }

    /**
     * 预热数据加载
     *
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisCachePreHeatRunner redisCachePreHeatRunner() {
        return new RedisCachePreHeatRunner();

    }

    /**
     * 默认开启 controller AOP 日志
     *
     * @return
     */
    @Bean
    @ConditionalOnProperty(value = "blink.web.enable-controller-log", havingValue = "true", matchIfMissing = true)
    public BlinkControllerLogAspect controllerLogAspect(BlinkWebAppConfigProperties properties) {
        return new BlinkControllerLogAspect(properties);

    }

    /**
     * 注解@LogExecution 方法日志切面
     *
     * @return
     */
    @Bean
    public LogExecutionAspect logExecutionAspect() {
        return new LogExecutionAspect();

    }


}
