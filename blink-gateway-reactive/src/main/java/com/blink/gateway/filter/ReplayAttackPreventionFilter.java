package com.blink.gateway.filter;

import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.gateway.component.GateWayCacheComponent;
import com.blink.gateway.constant.GateWayErrMsgCode;
import com.blink.gateway.util.GateWayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static com.blink.framework.common.constrant.SysConstant.X_BLINK_NONCE;
import static com.blink.framework.common.constrant.SysConstant.X_BLINK_TIMESTAMP;
import static com.blink.gateway.constant.GatewayConstant.*;

/**
 * 防止请求重放攻击
 *
 * @Author binblink
 * @Date 2026/2/2
 */
@Slf4j
public class ReplayAttackPreventionFilter implements GlobalFilter, Ordered {

    private final ReactiveRedisClient redisClient;

    private final GateWayCacheComponent cacheComponent;

    public ReplayAttackPreventionFilter(ReactiveRedisClient redisClient, GateWayCacheComponent cacheComponent) {
        this.redisClient = redisClient;
        this.cacheComponent = cacheComponent;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //缓存组件 获取参数开关
        return cacheComponent.getGateWayConfigFromCache(REQUEST_REPLAY_DEFEND_SWITCH)
                .defaultIfEmpty(GateWayUtil.getDefaultConfig(REQUEST_REPLAY_DEFEND_SWITCH))
                // 检查开关是否开启
                .flatMap(config -> SWITCH_ON.equals(Byte.valueOf(config.getConfigValue())) ?
                        processWithDefend(chain, exchange) : chain.filter(exchange));

    }

    // 提取成独立方法处理开关开启的情况
    private Mono<Void> processWithDefend(GatewayFilterChain chain, ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();

        String timeStamp = headers.getFirst(X_BLINK_TIMESTAMP);
        String nonce = headers.getFirst(X_BLINK_NONCE);
        long currentTime = System.currentTimeMillis();

        return isValidTimestamp(Long.parseLong(timeStamp), currentTime)
                .filter(isValid -> isValid)
                //请求时间校验不通过 抛异常
                .switchIfEmpty(Mono.error(new BlinkException(GateWayErrMsgCode.ILLEGAL_REQUEST)))
                // 验证时间戳是否有效
                .flatMap(r -> checkDuplicateRequest(nonce, chain, exchange));

    }


    //nonce 提取成独立方法处理重复请求检查，包含配置化过期时间
    private Mono<Void> checkDuplicateRequest(String nonce,
                                             GatewayFilterChain chain, ServerWebExchange exchange) {
        // 从配置获取过期时间
        return cacheComponent.getGateWayConfigFromCache(REQ_NONCE_EXPIRE_TIME_KEY)
                .defaultIfEmpty(GateWayUtil.getDefaultConfig(REQ_NONCE_EXPIRE_TIME_KEY))
                // 解析配置的过期时间
                .map(expireConfig -> Duration.ofMillis(Long.parseLong(expireConfig.getConfigValue())))
                // 使用配置的过期时间设置值
                .flatMap(expireDuration -> redisClient.setIfAbsentWithExpire(REQ_NONCE_PREFIX + nonce, nonce, expireDuration))
                //设置结果判断
                .flatMap(isSet -> isSet ? chain.filter(exchange) : Mono.error(new BlinkException(GateWayErrMsgCode.ILLEGAL_REQUEST)));
    }

    /**
     * 当前时间 - 请求时间 大于配置的有效时间 认定为无效请求
     *
     * @param timestamp
     * @return
     */
    private Mono<Boolean> isValidTimestamp(long timestamp, long currentTime) {

        return cacheComponent.getGateWayConfigFromCache(REQ_TIMESTAMP_EFFECT_TIME_KEY)
                //缓存拿不到 默认5分钟
                .defaultIfEmpty(GateWayUtil.getDefaultConfig(REQ_TIMESTAMP_EFFECT_TIME_KEY))
                .map(conf -> {
                    long configTime = Long.parseLong(conf.getConfigValue());
                    // 时间窗口
                    return Math.abs(currentTime - timestamp) <= configTime;
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 103;
    }
}
