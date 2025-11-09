package com.blink.gateway.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 *
 * @Author binblink 
 * @Date 2025/11/8
 */
@Configuration
public class WebClientConfig {


    /**
     * WebClient配置 这是实现负载均衡的关键配置
     * 调用第三方服务
     *
     */
    @LoadBalanced
    @Bean
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
}
