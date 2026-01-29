package com.blink.framework.core.config;

import com.blink.framework.core.config.prop.BlinkWebAppConfigProperties;
import com.blink.framework.core.interceptor.BlinkRequestContextInterceptor;
import com.blink.framework.core.interceptor.LogMdcInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;

import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.*;

import java.util.List;

/**
 * 自动配置类
 *
 * @author binblink
 */

@EnableWebMvc
public class BlinkWebMvcConfigurer implements WebMvcConfigurer {

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private BlinkWebAppConfigProperties properties;

    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {

    }

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
                jacksonConverter.setObjectMapper(objectMapper);
                break; // 通常只有一个，找到即可
            }
        }
    }

}