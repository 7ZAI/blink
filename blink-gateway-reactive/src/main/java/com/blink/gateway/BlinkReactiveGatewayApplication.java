package com.blink.gateway;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
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
        ConfigurableApplicationContext context = SpringApplication.run(BlinkReactiveGatewayApplication.class, args);
        // 检查CustomWebMvcConfig是否存在
//        if (context.containsBean("blinkRedisRateLimiter")) {
//            System.out.println("blinkRedisRateLimiter 已加载");
//
//            Object bean =  context.getBean("blinkRedisRateLimiter");
//
//            System.out.println(bean.getClass().getName());
//        }else {
//            System.out.println("blinkRedisRateLimiter 未加载");
//        }

    }

}
