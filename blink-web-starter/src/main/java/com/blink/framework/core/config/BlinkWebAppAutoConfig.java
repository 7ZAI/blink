package com.blink.framework.core.config;

import com.blink.framework.core.aop.BlinkControllerLogAspect;
import com.blink.framework.core.aop.LogExecutionAspect;
import com.blink.framework.core.aop.RedisCacheUpdateAspect;
import com.blink.framework.core.component.RedisCachePreHeatRunner;
import com.blink.framework.core.config.prop.BlinkWebAppConfigProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@EnableConfigurationProperties(BlinkWebAppConfigProperties.class)
public class BlinkWebAppAutoConfig {


    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 注册模块
        mapper.registerModule(new JavaTimeModule());
        // 支持构造函数参数名
        mapper.registerModule(new ParameterNamesModule());

        // 配置
        // 注册 Java 8 时间模块（必须！）
        mapper.registerModule(new JavaTimeModule());

        // 禁用日期时间戳格式（使用ISO格式）
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 忽略未知属性（反序列化时）
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 空值处理
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 美化输出（开发环境）
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        return mapper;
    }

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
