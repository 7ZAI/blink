package com.blink.gateway.security.token;

import com.blink.framework.common.data.ChannelInfoRedisDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static com.blink.framework.common.constrant.SysConstant.X_BLINK_TOKEN;
import static com.blink.gateway.constant.GatewayConstant.CHANNEL_AUTH_TYPE_CLOSE;
import static com.blink.gateway.constant.GatewayConstant.CHANNEL_INFO;


/**
 * token转换器
 * 内部系统 带状态的token验证
 * 从请求中拿到token
 *
 * @Author binblink
 */
@Slf4j
public class TokenAuthenticationConverter implements ServerAuthenticationConverter {


    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {

        //初始未认证的token
        return Mono.justOrEmpty((ChannelInfoRedisDO) exchange.getAttributes().get(CHANNEL_INFO))
                .filter(channel -> CHANNEL_AUTH_TYPE_CLOSE.equals(channel.getTokenType()))
                .flatMap(channel -> Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(X_BLINK_TOKEN)))
                .filter(token -> !token.isEmpty())
                .flatMap(token -> Mono.just(UsernamePasswordAuthenticationToken.unauthenticated(null, token)));

    }
}
