package com.blink.gateway.route;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.gateway.config.prop.DynamicRouteProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


/**
 * Redis 存储路由
 *
 * @author binblink
 */
public class RedisRouteDefinitionRepository implements RouteDefinitionRepository {

    private static final Logger logger = LoggerFactory.getLogger(RedisRouteDefinitionRepository.class);

    private final DynamicRouteProperties.Redis redisProperties;
    private final ReactiveRedisClient redisClient;

    public RedisRouteDefinitionRepository(DynamicRouteProperties.Redis redisProperties,
                                          ReactiveRedisClient redisClient) {
        this.redisProperties = redisProperties;
        this.redisClient = redisClient;
//        initListener();
    }

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        return redisClient.hEntries(redisProperties.getRouteKey())
                .map(entry -> JSON.parseObject(entry.getValue().toString(), RouteDefinition.class));
    }

    /**
     * 初始化Redis消息监听[citation:5]
     */
//    private void initListener() {
//        redisClient.
//        redisClient.getConnectionFactory()
//            .getConnection()
//            .subscribe(
//                (message, pattern) -> {
//                    // 收到Redis路由变更消息时刷新路由
//                    String body = new String(message.getBody());
//                    if ("route_changed".equals(body)) {
//                        // 这里可以通过ApplicationEventPublisher发布RefreshRoutesEvent
//                        // 需要注入ApplicationEventPublisher
//                    }
//                },
//                ("gateway_route_channel").getBytes()
//            );
//    }
    @Override
    public Mono<Void> save(Mono<RouteDefinition> route) {
        // 保存到Redis
        return route.flatMap(routeDefinition -> redisClient.hPut(redisProperties.getRouteKey(), routeDefinition.getId(),
                        JSONObject.toJSONString(routeDefinition))
                .flatMap(r -> r ? Mono.empty() : Mono.error(new BlinkException("保存路由失败！路由id:" + routeDefinition.getId())))
        );
    }

    @Override
    public Mono<Void> delete(Mono<String> routeId) {
        return routeId.flatMap(id ->
                redisClient.hExists(redisProperties.getRouteKey(), id).flatMap(r -> {
                    if (r) {
                        //存在route id 删除
                        return redisClient.hDelete(redisProperties.getRouteKey(), id)
                                .flatMap(l -> l > 0 ? Mono.empty() : Mono.error(new BlinkException("删除路由失败！路由id:" + id)));
                    }
                    //不存在
                    logger.error("删除路由 路由id:{} 不存在", id);
                    return Mono.empty();
                })
        );
    }


}