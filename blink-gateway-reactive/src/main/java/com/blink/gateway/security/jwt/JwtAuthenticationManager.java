package com.blink.gateway.security.jwt;

import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.jwt.JwtProvider;
import com.blink.gateway.component.ChannelSecretCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * jwt 认证器
 *
 * @Author binblink
 * @Date 2026/2/2
 */
@Slf4j
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    private final ChannelSecretCache channelSecretCache;

    public JwtAuthenticationManager(ChannelSecretCache channelSecretCache) {
        this.channelSecretCache = channelSecretCache;
    }


    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {

        String jwtToken = (String) authentication.getPrincipal();

        //2.获取权限信息
        return Mono.justOrEmpty(authentication.getCredentials())
                .flatMap(appKey -> {
                    String appKeyStr = appKey.toString();
                    JwtProvider jwtProvider = channelSecretCache.getJwtProviders().get(appKeyStr);

                    if (Objects.isNull(jwtProvider)) {
                        log.error("获取JwtProvider失败 appkey:{}", appKeyStr);
                        return Mono.error(new BlinkException());
                    }
                    //能拿到JwtInfo 无异常则验证通过
                    return Mono.just(jwtProvider.getJwtInfo(jwtToken));
                }).doOnError(e -> {
                    log.error("jwt验证失败{}", e.getMessage(), e);
                }).flatMap(jwtInfo -> {
                    String userId = (String) jwtInfo.getCustomData().get("userId");
                    Integer userIdInt = Integer.parseInt(userId);
                    Authentication authenticated = UsernamePasswordAuthenticationToken
                            .authenticated(userIdInt, jwtToken, null);
                    return Mono.just(authenticated);
                }).switchIfEmpty(Mono.just(authentication));
    }


}
