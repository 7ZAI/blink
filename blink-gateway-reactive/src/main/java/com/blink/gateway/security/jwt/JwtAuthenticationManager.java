package com.blink.gateway.security.jwt;

import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

/**
 * jwt 认证器
 * @Author binblink
 * @Date 2026/2/2
 */
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {


    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {

        String jwt = (String) authentication.getPrincipal();
        //1.验证有效性

        //2.获取权限信息
        return null;
    }
}
