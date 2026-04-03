package com.blink.framework.core.config;

import com.blink.framework.core.config.prop.BlinkWebAppConfigProperties;
import com.blink.framework.core.interceptor.BlinkRequestContextInterceptor;
import com.blink.framework.core.interceptor.LogMdcInterceptor;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import jakarta.annotation.Resource;


import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 自动配置类
 *
 * @author binblink
 */

@EnableWebMvc
public class BlinkWebMvcConfigurer implements WebMvcConfigurer {

    @Resource
    private BlinkWebAppConfigProperties properties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        if(properties.getEnableContextHolder()){
            // 应用于所有路径
            registry.addInterceptor(new BlinkRequestContextInterceptor()).addPathPatterns("/**");
        }

        registry.addInterceptor(new LogMdcInterceptor()).addPathPatterns("/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**");
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (HttpMessageConverter<?> converter : converters) {
            // 找到默认的Jackson消息转换器
            if (converter instanceof MappingJackson2HttpMessageConverter jacksonConverter) {
                // 将自定义的ObjectMapper设置进去
                jacksonConverter.setObjectMapper(objectMapperForMvc());
                break; // 通常只有一个，找到即可
            }
        }
    }


    private static ObjectMapper objectMapperForMvc() {
        ObjectMapper mapper = new ObjectMapper();
        var javaTimeModule = new JavaTimeModule();
        // 关键配置：为 LocalDateTime 注册序列化和反序列化器
        javaTimeModule.addSerializer(
                LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
        javaTimeModule.addDeserializer(
                LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
        // 注册模块
        mapper.registerModule(javaTimeModule);
        // 支持构造函数参数名
        mapper.registerModule(new ParameterNamesModule());

        // 禁用日期时间戳格式（使用ISO格式）
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 忽略未知属性（反序列化时）
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 空值处理
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 美化输出（开发环境）
//        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        return mapper;
    }

}