package com.blink.framework.core.config;

import com.blink.framework.core.exception.DefaultErrMsgProvider;
import com.blink.framework.core.exception.ErrMsgProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Web Starter 自动配置类
 *
 * @author binblink
 */
@Configuration
public class WebStarterAutoConfiguration {

    /**
     * 默认错误信息提供者
     * 当业务服务没有提供自己的实现时使用
     *
     * @return DefaultErrMsgProvider 实例
     */
    @Bean
    @ConditionalOnMissingBean(ErrMsgProvider.class)
    public ErrMsgProvider defaultErrMsgProvider() {
        return new DefaultErrMsgProvider();
    }
}