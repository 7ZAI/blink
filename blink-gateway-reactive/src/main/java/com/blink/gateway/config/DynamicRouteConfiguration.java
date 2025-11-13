package com.blink.gateway.config;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.gateway.config.prop.GatewayProperties;
import com.blink.gateway.listener.RouteSyncFlux;
import com.blink.gateway.route.NacosDynamicRouteListener;
import com.blink.gateway.route.NacosRouteDefinitionRepository;
import com.blink.gateway.route.RedisRouteDefinitionRepository;
import com.blink.gateway.util.GateWayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamReceiver;

import java.time.Duration;
import java.util.Map;


/**
 * 动态路由配置 根据配置文件 按条件加载
 * 兼容spring boot的路由配置方式
 *
 * @author binblink
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(GatewayProperties.class)
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
                                                                   GatewayProperties properties,
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
                GatewayProperties properties,
                ReactiveRedisClient redisClient) {
            return new RedisRouteDefinitionRepository(properties.getDynamicroute().getRedis(), redisClient);
        }


        @Bean
        public RouteSyncFlux routeSyncFlux(ReactiveRedisClient redisClient, GatewayProperties properties){

            String streamKey = properties.getDynamicroute().getRedis().getStreamkey();
            String groupName = properties.getDynamicroute().getRedis().getGroupId();

            GateWayUtil.createStreamAndGroup(redisClient, streamKey, groupName).subscribe(
                    result -> log.info("Stream：{} 和 group：{} 已存在！", streamKey, groupName),
                    error -> log.error("创建Stream：{} 和 group：{} 失败！{}", streamKey, groupName, error.getMessage())
            );

            return new RouteSyncFlux();
        }
//        /**
//         * stream 路由消息监听与连接
//         *
//         */
//        @Bean(name = "routeStreamReceiver")
//        public StreamReceiver<String, MapRecord<String, String, String>> routeStreamReceiver(ReactiveRedisConnectionFactory connectionFactory,
//                                                                                        ReactiveRedisClient redisClient, GatewayProperties properties) {
//
//            StreamReceiver.StreamReceiverOptions<String, MapRecord<String, String, String>> options = StreamReceiver.StreamReceiverOptions.builder()
//                    .batchSize(1)
//                    .pollTimeout(Duration.ofSeconds(2))
//                    .onErrorResume(e -> {
//
//                        log.error(e.getMessage());
//                        return null;
//                    }).build();
//
//            StreamReceiver<String, MapRecord<String, String, String>> streamReceiver = StreamReceiver.create(connectionFactory, options);
//
//            String streamKey = properties.getDynamicroute().getRedis().getStreamkey();
//            String groupName = properties.getDynamicroute().getRedis().getGroupId();
//
//
//            GateWayUtil.createStreamAndGroup(redisClient, streamKey, groupName).subscribe(
//                    result -> log.info("Stream：{} 和 group：{} 已存在！", streamKey, groupName),
//                    error -> log.error("创建Stream：{} 和 group：{} 失败！{}", streamKey, groupName, error.getMessage())
//            );
//
//
//            return streamReceiver;
//        }

        /**
         * 给Container 绑定 stream 并注册监听器 实现消息消费 同步路由
         * 同步配置 不适合响应式
         *
         * @param container StreamMessageListenerContainer
         * @param properties 配置类
         * @param publisher 事件发布
         * @return
         */
//        @Bean
//        @ConditionalOnMissingBean
//        public StreamListener routeStreamListener(StreamMessageListenerContainer container, GatewayProperties properties, ApplicationEventPublisher publisher) {
//            GatewayProperties.Dynamicroute.Redis redis = properties.getDynamicroute().getRedis();
//            // 为第一个Stream "stream" 配置并注册监听
//            StreamMessageListenerContainer.StreamReadRequest<String> routeStreamReadRequest =
//                    StreamMessageListenerContainer.StreamReadRequest
//                            .builder(StreamOffset.create(redis.getStreamkey(), ReadOffset.lastConsumed()))
//                            .consumer(Consumer.from(redis.getGroupId(), "consumer1"))
//                            .autoAcknowledge(true) // 自动确认
//                            .build();
//            //自定义的监听对象
//            RouteUpdateStreamListener routeUpdateStreamListener = new RouteUpdateStreamListener(publisher);
//            container.register(routeStreamReadRequest, routeUpdateStreamListener);
//
//            return routeUpdateStreamListener;
//        }
    }
}