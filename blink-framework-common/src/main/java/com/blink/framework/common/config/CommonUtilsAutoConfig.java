package com.blink.framework.common.config;

import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 通用工具类自动配置
 *
 * 扫描并注册 ApplicationContextUtil 等工具类
 *
 * 使用最高优先级，确保在其他自动配置之前加载，
 * 避免 CacheComponent 等组件在初始化时调用 ApplicationContextUtil 导致失败
 *
 * @author binblink
 */
@Configuration
@ComponentScan(basePackages = "com.blink.framework.common.utils")
@AutoConfigureOrder(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public class CommonUtilsAutoConfig {

}
