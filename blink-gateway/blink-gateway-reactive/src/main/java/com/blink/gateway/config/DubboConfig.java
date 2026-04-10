package com.blink.gateway.config;

import com.blink.base.dubbo.service.BaseDubboService;
import com.blink.gateway.dubbo.service.GatewayAdminDubboService;
import com.blink.gateway.service.BaseAppDubboService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

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

    /**
     * 注入 gatewayAdminDubboService
     */
    @DubboReference(
            timeout = 10000,
            check = false
    )
    private GatewayAdminDubboService gatewayAdminDubboService;

    /**
     * 暴露 GatewayAdminDubboService Bean
     */
    @Bean
    public GatewayAdminDubboService gatewayAdminDubboService() {
        return gatewayAdminDubboService;
    }

    /**
     * 创建 BaseAppDubboService 实例
     * 使用 Dubbo 3.x 原生异步接口（CompletableFuture），无需额外线程池
     */
    @Bean
    @Primary
    public BaseAppDubboService baseAppDubboService() {
        return new BaseAppDubboService(baseDubboService);
    }

}
