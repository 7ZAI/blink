package com.blink.gateway.security.token;


import com.blink.framework.common.data.UserInfoRedisDO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.gateway.constant.GatewayConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

import static com.blink.gateway.constant.RedisConstans.USER_TOKEN;
import static com.blink.gateway.constant.RedisConstans.USER_TOKENS;

/**
 * 认证成功后续处理 自动续期token
 *
 * @Author binblink
 */
@Slf4j
public class TokenAuthenticationSuccessHandler implements ServerAuthenticationSuccessHandler {

    private final ReactiveRedisClient redisClient;

    public TokenAuthenticationSuccessHandler(ReactiveRedisClient redisClient) {
        this.redisClient = redisClient;
    }

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        ServerWebExchange exchange = webFilterExchange.getExchange();

        //登入请求
        if(GatewayConstant.LOGIN_PATH.equals(exchange.getRequest().getPath().value())){
            log.debug("================>登入请求经过!");
            return webFilterExchange.getChain().filter(exchange);
        }

        String token = (String) authentication.getCredentials();
        String redisKey = USER_TOKEN + token;

        //传递 登录用户信息 用来后续元数据组装使用
        UserInfoRedisDO userInfo = (UserInfoRedisDO) authentication.getPrincipal();
        if(Objects.nonNull(userInfo)) {
            exchange.getAttributes().put(GatewayConstant.LOGIN_USER_KEY, userInfo);
        }

        return redisClient.ttl(redisKey)
                 //处理为空的情况
                .switchIfEmpty(Mono.error(new BlinkException("检查过期时间失败 key:"+redisKey)))
                //抛出异常 不会执行flatmap
                .flatMap(ttl -> {
                    log.debug("认证成功 token续期检查, redisKey: {}", redisKey);
                    // TTL 检查
                    if (ttl == null || ttl.toSeconds() < 0) {
                        log.warn("token不存在或已过期, redisKey: {}", redisKey);
                    }
                    // 是否需要续期 5分钟仍然活跃
                    if (ttl.toSeconds() <= GatewayConstant.RENEW_THRESHOLD.toSeconds()) {
                        // 自动续期 30 分钟，同时续期 USER_TOKEN 和 USER_TOKENS
                        return renewToken(redisKey, userInfo).then(webFilterExchange.getChain().filter(exchange));
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
     * 同时续期 USER_TOKEN 和 USER_TOKENS 两个 key，确保踢出逻辑正常工作
     *
     * @param redisKey USER_TOKEN 的 redis key
     * @param userInfo 用户信息，用于获取 userId 来构建 USER_TOKENS key
     * @return 是否续期成功
     */
    private Mono<Boolean> renewToken(String redisKey, UserInfoRedisDO userInfo) {
        String userTokensKey = USER_TOKENS + userInfo.getUserId();

        return Mono.zip(
                        // 续期 USER_TOKEN
                        redisClient.expire(redisKey, GatewayConstant.TOKEN_TTL)
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
                                }),
                        // 续期 USER_TOKENS（ZSet）
                        redisClient.expire(userTokensKey, GatewayConstant.TOKEN_TTL)
                                .doOnSuccess(result -> {
                                    if (result) {
                                        log.debug("USER_TOKENS续期成功, userId: {}", userInfo.getUserId());
                                    } else {
                                        log.warn("USER_TOKENS续期失败(可能已过期), userId: {}", userInfo.getUserId());
                                    }
                                })
                                .onErrorResume(error -> {
                                    log.error("USER_TOKENS续期异常, userId: {}, error: {}",
                                            userInfo.getUserId(), error.getMessage(), error);
                                    return Mono.just(false);
                                })
                )
                .map(tuple -> tuple.getT1() && tuple.getT2());
    }


}
