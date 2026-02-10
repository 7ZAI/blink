package com.blink.gateway.security.jwt;

import com.blink.framework.common.data.UserInfoRedisDO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.jwt.JwtConfig;
import com.blink.framework.common.jwt.JwtInfo;
import com.blink.framework.common.jwt.JwtProvider;
import com.blink.gateway.component.ChannelSecretCache;
import com.blink.gateway.component.GateWayCacheComponent;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

/**
 * jwt 认证器
 *
 * @Author binblink
 * @Date 2026/2/2
 */
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    private ChannelSecretCache channelSecretCache;


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

            //根据id获取配置用户拥有的角色的权限标识 缓存获取
//            cacheComponent.
            UserInfoRedisDO userInfo = new UserInfoRedisDO();
            Authentication authenticated = UsernamePasswordAuthenticationToken
                    .authenticated(userInfo, jwtToken, userInfo.getPermissions().stream().map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList()));
            return Mono.just(authenticated);
        });
    }



}
