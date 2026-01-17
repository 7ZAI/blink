package com.blink.framework.mq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication
@ComponentScan({"com.blink.*"})
public class BlinkFrameworkMqApplication {


    public static void main(String[] args) {

      ConfigurableApplicationContext applicationContext = SpringApplication.run(BlinkFrameworkMqApplication.class, args);

    }

}
