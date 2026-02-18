package com.blink.gateway.security.jwt;

import com.blink.framework.common.data.ChannelInfoRedisDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static com.blink.framework.common.constrant.SysConstant.X_BLINK_APPKEY;
import static com.blink.framework.common.constrant.SysConstant.X_BLINK_TOKEN;
import static com.blink.gateway.constant.GatewayConstant.CHANNEL_AUTH_TYPE_JWT;
import static com.blink.gateway.constant.GatewayConstant.CHANNEL_INFO;


/**
 * jwt转换器 从请求中拿到jwt
 *
 * @Author binblink
 */
@Slf4j
public class JwtAuthenticationConverter implements ServerAuthenticationConverter {

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {

        //初始未认证的jwt
        return Mono.justOrEmpty((ChannelInfoRedisDO) exchange.getAttributes().get(CHANNEL_INFO))
                .filter(channel -> CHANNEL_AUTH_TYPE_JWT.equals(channel.getTokenType()))
                .flatMap(channel -> {
                    String token = exchange.getRequest().getHeaders().getFirst(X_BLINK_TOKEN);
                    String appKey = exchange.getRequest().getHeaders().getFirst(X_BLINK_APPKEY);
                    var authToken = UsernamePasswordAuthenticationToken.unauthenticated(appKey, token);
                    return Mono.just(authToken);
                });
    }

}
