package com.blink;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Gateway Admin 后台管理系统启动类
 * 网关运维管理平台，实现渠道管理、路由管理、配置管理和监控
 *
 * @author binblink
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableDubbo
@EnableScheduling
@EnableAsync
public class GatewayAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayAdminApplication.class, args);
    }
}