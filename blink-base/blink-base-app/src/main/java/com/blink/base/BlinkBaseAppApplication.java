package com.blink.base;

import com.blink.framework.common.utils.ApplicationContextUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/**
 * 启动类
 * @author binblink
 */
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan({"com.blink"})
public class BlinkBaseAppApplication {

    public static void main(String[] args) {

       SpringApplication.run(BlinkBaseAppApplication.class, args);

    }

}
