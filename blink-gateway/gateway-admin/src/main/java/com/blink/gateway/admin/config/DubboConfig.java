package com.blink.gateway.admin.config;

import com.blink.base.dubbo.service.BaseDubboService;

import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.context.annotation.Configuration;

/**
 * Dubbo 配置类
 * 管理 Dubbo 服务引用和相关配置
 *
 * @Author blink
 * @Date 2026/03/04
 */
@Configuration
public class DubboConfig {

    /**
     * 注入 BaseDubboService
     */
    @DubboReference(
            timeout = 10000,
            check = false
    )
    private BaseDubboService baseDubboService;



}
