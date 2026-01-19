package com.blink.framework.common.config;

import com.blink.framework.common.utils.ApplicationContextUtil;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自动配置
 * 主要是为了 ApplicationContextUtil 注册为bean
 * @Author binblink
 */
@AutoConfigureOrder
@Configuration
public class CommonUtilsAutoConfig {

    @Bean
    @ConditionalOnMissingBean
    public ApplicationContextUtil applicationContextUtil(ApplicationContext applicationContext){
        ApplicationContextUtil util = new ApplicationContextUtil();
        util.setApplicationContext(applicationContext);
        return util;
    }

}
