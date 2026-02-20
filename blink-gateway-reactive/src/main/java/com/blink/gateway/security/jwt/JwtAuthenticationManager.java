package com.blink.gateway.security.jwt;

import com.blink.framework.common.data.UserInfoRedisDO;
import com.blink.framework.common.exception.BlinkErrorCodeEnum;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.jwt.JwtProvider;
import com.blink.gateway.component.ChannelSecretCache;
import com.blink.gateway.component.GateWayCacheComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * jwt 认证器
 *
 * @Author binblink
 */
@Slf4j
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    private final ChannelSecretCache channelSecretCache;

    private final GateWayCacheComponent cacheComponent;

    public JwtAuthenticationManager(ChannelSecretCache channelSecretCache, GateWayCacheComponent cacheComponent) {
        this.channelSecretCache = channelSecretCache;
        this.cacheComponent = cacheComponent;
    }


    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {

        String jwtToken = (String) authentication.getCredentials();

        //2.获取权限信息
        return Mono.justOrEmpty(authentication.getPrincipal())
                .flatMap(appKey -> {
                    String appKeyStr = appKey.toString();
                    JwtProvider jwtProvider = channelSecretCache.getJwtProviders().get(appKeyStr);

                    if (Objects.isNull(jwtProvider)) {
                        log.error("获取JwtProvider失败 appkey:{}", appKeyStr);
                        return Mono.error(new BlinkException());
                    }
                    //能拿到JwtInfo 无异常则验证通过
                    return Mono.just(jwtProvider.getJwtInfo(jwtToken));
                }).flatMap(jwtInfo -> {
                    String userId = (String) jwtInfo.getSubject();
                    Integer userIdInt = Integer.parseInt(userId);

                    return cacheComponent.getPermissionsByUserId(userIdInt).flatMap(perms -> {
                        //用户接口权限集合
                        Set<String> permittedList = Optional.ofNullable(perms.getPermissions()).orElseGet(HashSet::new);

                        UserInfoRedisDO userInfoRedisDO = new UserInfoRedisDO();
                        userInfoRedisDO.setUserId(userIdInt);
                        userInfoRedisDO.setPermissions(permittedList);

                        Authentication authenticated = UsernamePasswordAuthenticationToken
                                .authenticated(userInfoRedisDO, jwtToken, permittedList.stream().map(SimpleGrantedAuthority::new)
                                        .collect(Collectors.toList()));
                        return Mono.just(authenticated);
                    });

                })
                .switchIfEmpty(Mono.just(authentication))
                .onErrorResume(e -> {
                    if (e instanceof JwtProvider.InvalidTokenException) {
                        log.error("jwt验证失败{}", e.getMessage(), e);
                        return Mono.error(new BlinkException(BlinkErrorCodeEnum.BLINK_TOKEN_INVALID.getCode()));
                    }
                    return Mono.error(e);
                });
    }


}
