package com.blink.redis;

import com.blink.framework.redis.config.prop.BlinkRedisProperties;
import com.blink.framework.redis.config.prop.DistributedLockProperties;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableConfigurationProperties({BlinkRedisProperties.class, DistributedLockProperties.class})
@ComponentScan(basePackages = "com.blink.framework.redis")
public class TestApplicationConfig {
}
