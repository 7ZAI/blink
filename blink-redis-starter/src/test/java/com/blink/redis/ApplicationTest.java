package com.blink.redis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * @Author binblink
 */
@SpringBootApplication
@ActiveProfiles("test")
@ComponentScan(basePackages = "com.blink.framework.redis")
@Import(GeneratorIdTestController.class)
public class ApplicationTest {



    public static void main(String[] args) {

        SpringApplication.run(ApplicationTest.class, args);
    }
}
