package com.blink.gateway.security.jwt;

import com.blink.framework.common.data.ChannelInfoRedisDO;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.blink.framework.common.constrant.SysConstant.X_BLINK_TOKEN;
import static com.blink.gateway.constant.GatewayConstant.CHANNEL_INFO;


/**
 * jwt转换器 从请求中拿到jwt
 * @Author binblink
 */
public class JwtAuthenticationConverter implements ServerAuthenticationConverter {

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {

        //初始未认证的token
        return Mono.justOrEmpty((ChannelInfoRedisDO) exchange.getAttributes().get(CHANNEL_INFO))
                .filter(channel-> 1 == channel.getTokenType())
                .flatMap(channel -> Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(X_BLINK_TOKEN)))
                .filter(header -> !header.isEmpty())
                .map(token -> UsernamePasswordAuthenticationToken.unauthenticated(null,token));
    }

}
