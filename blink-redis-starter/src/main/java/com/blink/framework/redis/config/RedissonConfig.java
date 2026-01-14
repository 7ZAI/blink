package com.blink.framework.redis.config;

import com.blink.framework.redis.lock.RedisDistributeLock;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "blink.redis.redissonEnable", havingValue = "true")
public class RedissonConfig {

    private static final String REDISSON_PREFIX = "redis://";

    @Value("${spring.data.redis.host:localhost}")
    private String redisAddr;

    @Value("${spring.data.redis.port:6379}")
    private String port;

    @Value("${spring.data.redis.password:123456}")
    private String password;



    @Bean
    @ConditionalOnMissingBean
    public RedissonClient redisson(){

        Config config = new Config();

        config.useSingleServer()
                .setAddress(REDISSON_PREFIX + redisAddr + ":"+ port)
                .setPassword(password);

        return  Redisson.create(config);
    }

    @Bean
    public RedisDistributeLock redisDistributeLock(RedissonClient redisson){

        return new RedisDistributeLock(redisson);
    }


}
