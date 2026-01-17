package com.blink.gateway.config;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.gateway.config.prop.BlinkGatewayProperties;
import com.blink.gateway.listener.NacosDynamicRouteListener;
import com.blink.gateway.route.NacosRouteDefinitionRepository;
import com.blink.gateway.route.RedisRouteDefinitionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * 动态路由配置 根据配置文件 按条件加载
 * 兼容spring boot的路由配置方式
 *
 * @author binblink
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(BlinkGatewayProperties.class)
public class DynamicRouteConfiguration {

    /**
     * 当 mode = nacos 时激活Nacos动态路由
     */
    @Configuration
    @ConditionalOnProperty(prefix = "blink.gateway.dynamicroute",
            name = "mode",
            havingValue = "nacos")
    public static class NacosDynamicRouteConfig {

        @Bean
        @ConditionalOnMissingBean
        public RouteDefinitionRepository nacosRouteDefinitionRepository(NacosConfigManager configManager) {
            return new NacosRouteDefinitionRepository(configManager);
        }

        @Bean
        @ConditionalOnMissingBean
        public NacosDynamicRouteListener nacosDynamicRouteListener(NacosConfigManager configManager,
                                                                   BlinkGatewayProperties properties,
                                                                   ApplicationEventPublisher publisher) {
            return new NacosDynamicRouteListener(configManager, publisher, properties.getDynamicroute().getNacos());
        }
    }

    /**
     * 当 mode = redis 时激活Redis动态路由
     */
    @Configuration
    @ConditionalOnProperty(prefix = "blink.gateway.dynamicroute",
            name = "mode",
            havingValue = "redis")
    public static class RedisDynamicRouteConfig {


        @Bean
        @ConditionalOnMissingBean
        public RouteDefinitionRepository redisRouteDefinitionRepository(
                BlinkGatewayProperties properties,
                ReactiveRedisClient redisClient) {
            return new RedisRouteDefinitionRepository(properties.getDynamicroute().getRedis(), redisClient);
        }


//        @Bean
//        public RedisRouteSyncListener routeSyncFlux(ReactiveRedisClient redisClient, BlinkGatewayProperties properties){
//            return new RedisRouteSyncListener();
//        }
    }
}