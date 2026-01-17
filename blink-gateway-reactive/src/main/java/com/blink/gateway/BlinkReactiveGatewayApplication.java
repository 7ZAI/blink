package com.blink.gateway;


import com.blink.framework.redis.config.BlinkRedisAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * 启动类
 *
 * @author binblink
 */
@SpringBootApplication
@EnableDiscoveryClient
public class BlinkReactiveGatewayApplication {


    public static void main(String[] args) {
       SpringApplication.run(BlinkReactiveGatewayApplication.class, args);
    }

}
