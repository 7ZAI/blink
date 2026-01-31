package com.blink.gateway.route;

import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.gateway.config.prop.BlinkGatewayProperties;
import com.blink.gateway.util.JacksonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;


/**
 * Redis 存储路由
 *
 * @author binblink
 */
@Slf4j
public class RedisRouteDefinitionRepository implements RouteDefinitionRepository {


    private final BlinkGatewayProperties.DynamicRoute.Redis redisProperties;
    private final ReactiveRedisClient redisClient;

    public RedisRouteDefinitionRepository(BlinkGatewayProperties.DynamicRoute.Redis redisProperties,
                                          ReactiveRedisClient redisClient) {
        this.redisProperties = redisProperties;
        this.redisClient = redisClient;
    }


    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        String routesKey = redisProperties.getRouteKey();
        return redisClient.hEntries(routesKey).switchIfEmpty(Mono.error(new BlinkException("路由为空 key:" + routesKey)))
                .mapNotNull(routeDefinition -> {
                    log.info("从redis 获取的路由：{}",routeDefinition.getValue());
                    String jsonstr = (String) routeDefinition.getValue();
                    return JacksonUtil.fromJson(jsonstr,RouteDefinition.class);
                }).onErrorContinue((e, r) -> log.error("路由解析失败，跳过该条记录: {}", e.getMessage(),e));


    }

    @Override
    public Mono<Void> save(Mono<RouteDefinition> route) {
        // 保存到Redis
        return route.flatMap(routeDefinition -> redisClient.hPut(redisProperties.getRouteKey(), routeDefinition.getId(),
                        JacksonUtil.toJson(routeDefinition))
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
                    log.error("删除路由 路由id:{} 不存在", id);
                    return Mono.empty();
                })
        );
    }


}