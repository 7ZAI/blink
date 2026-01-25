package com.blink.gateway;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import reactor.core.publisher.Hooks;


/**
 * 启动类
 *
 * @author binblink
 */
@SpringBootApplication
@EnableDiscoveryClient
@ConfigurationPropertiesScan
public class BlinkReactiveGatewayApplication {


    public static void main(String[] args) {

        // 启用 Reactor 调试代理
//        Hooks.onOperatorDebug();
       SpringApplication.run(BlinkReactiveGatewayApplication.class, args);
    }

}
