package com.blink.gateway.security;


import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.gateway.constant.GatewayConstant;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class BlinkAuthenticationSuccessHandler implements ServerAuthenticationSuccessHandler {

    private final ReactiveRedisClient redisClient;

    public BlinkAuthenticationSuccessHandler(ReactiveRedisClient redisClient) {
        this.redisClient = redisClient;
    }

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        ServerWebExchange exchange = webFilterExchange.getExchange();
        String redisKey = GatewayConstant.USER_TOKEN + authentication.getCredentials();

        log.debug("认证成功 token续期检查, redisKey: {}", redisKey);
        return redisClient.ttl(redisKey)
                .flatMap(ttl -> {
                    // TTL 检查
                    if (ttl == null || ttl.toSeconds() < 0) {
                        log.warn("token不存在或已过期, redisKey: {}", redisKey);
                    }
                    // 是否需要续期 5分钟仍然活跃
                    if (ttl.toSeconds() <= GatewayConstant.RENEW_THRESHOLD.toSeconds()) {
                        // 自动续期 30 分钟o
                        return renewToken(redisKey).then(webFilterExchange.getChain().filter(exchange));
                    }
                    //不需要续期
                    return webFilterExchange.getChain().filter(exchange);
                }).onErrorResume(error -> {
                    log.error("续期检查 redis 请求失败, redisKey: {}, error: {}",redisKey, error.getMessage(), error);
                    // 继续执行 过滤链
                    return webFilterExchange.getChain().filter(exchange);
                });
    }

    /**
     * 续期token
     */
    private Mono<Boolean> renewToken(String redisKey) {
        return redisClient.expire(redisKey, GatewayConstant.TOKEN_TTL)
                .doOnSuccess(result -> {
                    if (result) {
                        log.info("token续期成功, redisKey: {}", redisKey);
                    } else {
                        log.error("token续期失败, redisKey: {}", redisKey);
                    }
                })
                .onErrorResume(error -> {
                    log.error("token续期异常, redisKey: {}, error: {}",
                            redisKey, error.getMessage(), error);
                    return Mono.just(false);
                });
    }


}
