package com.blink.gateway.security;


import lombok.extern.slf4j.Slf4j;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


/**
 * 授权异常处理 抛出异常给全局异常统一处理
 * @Author binblink
 */
@Slf4j
public class BlinkAccessDeniedHandler implements ServerAccessDeniedHandler {


    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {

        log.error("Access denied",denied);

        return Mono.error(denied);
    }


}
