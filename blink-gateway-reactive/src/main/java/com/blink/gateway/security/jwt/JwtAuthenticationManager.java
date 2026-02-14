package com.blink.gateway.security.jwt;

import com.blink.framework.common.jwt.JwtInfo;
import com.blink.framework.common.jwt.JwtProvider;
import com.blink.gateway.component.ChannelSecretCache;
import com.blink.gateway.component.GateWayCacheComponent;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * jwt 认证器
 *
 * @Author binblink
 * @Date 2026/2/2
 */
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    private ChannelSecretCache channelSecretCache;

    private GateWayCacheComponent cacheComponent;


    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {

        String jwtToken = (String) authentication.getPrincipal();
        //1.验证有效性
        String appKey = (String) authentication.getCredentials();

//        jwtProvider.validateTokenDetailed()

        //2.获取权限信息
        return Mono.justOrEmpty(appKey).flatMap(channel -> {

            JwtProvider jwtProvider = channelSecretCache.getJwtProviders().get(appKey);
            JwtInfo jwtInfo = jwtProvider.getJwtInfo(jwtToken);
            String userId = (String) jwtInfo.getCustomData().get("userId");
            Integer userIdInt = Integer.parseInt(userId);
            return cacheComponent.getPermissionsByUserId(userIdInt).flatMap(perm -> {
                Set<String> permissions = perm.getPermissions();
                Authentication authenticated = UsernamePasswordAuthenticationToken
                        .authenticated(userId, jwtToken, permissions.stream().map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList()));

                return Mono.just(authenticated);
            });

        });
    }


}
