package com.blink.redis;// 在测试包中创建
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.blink.framework.redis") // 扫描你的主包
public class TestApplicationConfig {
    // 空类即可
}