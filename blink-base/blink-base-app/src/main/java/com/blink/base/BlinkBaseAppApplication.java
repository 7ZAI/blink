package com.blink.base;

import com.blink.framework.common.utils.ApplicationContextUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan({"com.blink"})
public class BlinkBaseAppApplication {

    public static void main(String[] args) throws ClassNotFoundException {

       SpringApplication.run(BlinkBaseAppApplication.class, args);

//       String name = context.getEnvironment().getProperty("user.name");
//       String age = context.getEnvironment().getProperty("user.age");
//
//       System.out.println("name :" + name + " age:" + age);

//       Class clazz =  Thread.currentThread().getContextClassLoader().loadClass("com.blink.base.component.CachePreHeating");
//
//       Object obj = ApplicationContextUtil.getBean(clazz);

    }

}
