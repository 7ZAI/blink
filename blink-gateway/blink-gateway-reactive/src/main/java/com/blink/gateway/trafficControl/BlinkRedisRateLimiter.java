package com.blink.gateway.trafficControl;

import cn.hutool.core.util.StrUtil;
import com.blink.gateway.constant.GateWayErrMsgCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 自定义限流判断
 * 修改逻辑 达到限流条件提前抛出异常 而不是原本的直接返回响应429
 * 其他不变
 * @Author binblink
 */
@Slf4j
public class BlinkRedisRateLimiter extends RedisRateLimiter {


    public BlinkRedisRateLimiter(ReactiveStringRedisTemplate redisTemplate, RedisScript<List<Long>> script, ConfigurationService configurationService) {
        super(redisTemplate, script, configurationService);
    }

    /**
     * This creates an instance with default static configuration, useful in Java DSL.
     *
     * @param defaultReplenishRate how many tokens per second in token-bucket algorithm.
     * @param defaultBurstCapacity how many tokens the bucket can hold in token-bucket
     *                             algorithm.
     */
    public BlinkRedisRateLimiter(int defaultReplenishRate, int defaultBurstCapacity) {
        super(defaultReplenishRate, defaultBurstCapacity);
    }

    /**
     * This creates an instance with default static configuration, useful in Java DSL.
     *
     * @param defaultReplenishRate   how many tokens per second in token-bucket algorithm.
     * @param defaultBurstCapacity   how many tokens the bucket can hold in token-bucket
     *                               algorithm.
     * @param defaultRequestedTokens how many tokens are requested per request.
     */
    public BlinkRedisRateLimiter(int defaultReplenishRate, int defaultBurstCapacity, int defaultRequestedTokens) {
        super(defaultReplenishRate, defaultBurstCapacity, defaultRequestedTokens);
    }

    @Override
    public Mono<Response> isAllowed(String routeId, String id) {
        return super.isAllowed(routeId, id)
                .flatMap(response -> {

                    log.debug("===> Redis限流返回值 resp: {}", response);
                    // 如果被限流（allowed = false），抛出自定义异常
                    int burstCapacity = 0, rate = 0;
                    String rateStr = response.getHeaders().get(REPLENISH_RATE_HEADER);
                    String burstCapacityStr = response.getHeaders().get(BURST_CAPACITY_HEADER);
                    response.getHeaders().get(BURST_CAPACITY_HEADER);

                    if (StrUtil.isNotBlank(rateStr)) {
                        rate = Integer.parseInt(rateStr);
                    }
                    if (StrUtil.isNotBlank(burstCapacityStr)) {
                        burstCapacity = Integer.parseInt(burstCapacityStr);
                    }

                    if (!response.isAllowed()) {
                        log.warn("请求过多 限流拦截 路由id:{} 触发维度key:{},生成速率:{},容量:{}", routeId, id, rate, burstCapacity);
                        return Mono.error(new RateLimitExceededException(GateWayErrMsgCode.TOO_MANY_REQUESTS));
                    }
                    // 如果允许通过，正常返回
                    return Mono.just(response);
                });
    }
}
