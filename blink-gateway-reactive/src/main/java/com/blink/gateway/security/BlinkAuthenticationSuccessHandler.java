package com.blink.gateway.security;


import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.gateway.constant.GatewayConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 认证成功后续处理 自动续期token
 *
 * @Author binblink
 */
public class BlinkAuthenticationSuccessHandler implements ServerAuthenticationSuccessHandler {

    private final Logger logger = LoggerFactory.getLogger(BlinkAuthenticationSuccessHandler.class);

    private final ReactiveRedisClient redisClient;

    public BlinkAuthenticationSuccessHandler(ReactiveRedisClient redisClient) {
        this.redisClient = redisClient;
    }

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        ServerWebExchange exchange = webFilterExchange.getExchange();
        String redisKey = GatewayConstant.USER_TOKEN + authentication.getCredentials();
//        return webFilterExchange.getChain().filter(exchange);
        return redisClient.ttl(redisKey)
                .flatMap(ttl -> {
                    if (ttl.toSeconds() <= GatewayConstant.RENEW_THRESHOLD.toSeconds()) {
                        // 自动续期 30 分钟o
                        return redisClient.expire(redisKey, GatewayConstant.TOKEN_TTL)
                                .flatMap(renewResult -> {
                            if (renewResult) {
                                logger.info("token续期成功 redisKey{}", redisKey);

                            } else {
                                logger.error("token续期失败 redisKey{}", redisKey);
                            }
                            return webFilterExchange.getChain().filter(exchange);
                        });
                    }
                    return webFilterExchange.getChain().filter(exchange);
                }).switchIfEmpty(Mono.defer(() -> webFilterExchange.getChain().filter(exchange)));

    }


}
