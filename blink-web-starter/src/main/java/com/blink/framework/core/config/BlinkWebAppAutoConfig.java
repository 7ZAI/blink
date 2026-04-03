package com.blink.framework.core.config;


import com.blink.framework.core.config.prop.BlinkWebAppConfigProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * web应用自动配置类
 *
 * @Author binblink
 */
@AutoConfiguration
@ConditionalOnWebApplication
@EnableConfigurationProperties({BlinkWebAppConfigProperties.class})
@ComponentScan(basePackages = "com.blink.framework.core.config")
public class BlinkWebAppAutoConfig {

    /**
     * 如果外部配置了新的EnableWebMvc 覆盖自动配置
     */
    @Bean
    @ConditionalOnMissingBean(annotation = EnableWebMvc.class)
    public BlinkWebMvcConfigurer blinkWebMvcConfigurer() {
        return new BlinkWebMvcConfigurer();
    }





}
