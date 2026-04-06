package com.blink.gateway.admin.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis Pub/Sub 配置类
 * 提供消息监听容器用于订阅Redis频道
 *
 * @author binblink
 * @since 2026-04-06
 */
@Slf4j
@Configuration
public class RedisPubSubConfig {

    /**
     * 创建Redis消息监听容器
     *
     * @param redisConnectionFactory Redis连接工厂
     * @return RedisMessageListenerContainer 容器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory redisConnectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        log.info("[RedisPubSub] Redis消息监听容器已初始化");
        return container;
    }
}