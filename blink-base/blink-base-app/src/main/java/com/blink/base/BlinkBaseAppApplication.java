package com.blink.base;

import com.blink.framework.common.utils.ApplicationContextUtil;
import com.blink.framework.core.config.BlinkWebMvcConfigurer;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
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
@EnableDubbo
public class BlinkBaseAppApplication {

    public static void main(String[] args) {

        ConfigurableApplicationContext context = SpringApplication.run(BlinkBaseAppApplication.class, args);

        // 检查CustomWebMvcConfig是否存在
//        if (context.containsBean("blinkWebMvcConfigurer")) {
//            System.out.println("CustomWebMvcConfig 已加载");
//        } else {
//            System.out.println("CustomWebMvcConfig 未加载");
//        }
//        // 检查GlobalControllerAdvice是否存在
//        if (context.containsBean("globalExceptionHandler")) {
//            System.out.println("GlobalControllerAdvice 已加载");
//        } else {
//            System.out.println("GlobalControllerAdvice 未加载");
//        }

    }

}
