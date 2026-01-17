package com.blink.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 *
 * ip黑名单 白名单 过滤
 *
 *
 * TODO 支持ipv4 ipv6 支持网段过滤
 * @Author binblink
 * @Date 2025/12/20
 */
public class IpFilter implements GlobalFilter, Ordered {


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        return null;
    }

    private Mono<Boolean> whiteIps(String ip){
        return Mono.just(true);
    }


    private Mono<Boolean> blackIps(String ip){
        return Mono.just(true);
    }


    @Override
    public int getOrder() {
        return 0;
    }
}
