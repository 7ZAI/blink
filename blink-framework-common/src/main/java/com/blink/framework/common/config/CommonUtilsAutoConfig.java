package com.blink.framework.common.config;

import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 通用工具类自动配置
 * 
 * 扫描并注册 ApplicationContextUtil 等工具类
 * 
 * @author binblink
 */
@AutoConfigureOrder
@Configuration
@ComponentScan(basePackages = "com.blink.framework.common.utils")
public class CommonUtilsAutoConfig {

}
