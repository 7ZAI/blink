package com.blink.gateway;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan({"com.blink.*"})
public class BlinkReactiveGatewayApplication {


    public static void main(String[] args) {
       SpringApplication.run(BlinkReactiveGatewayApplication.class, args);
    }

}
