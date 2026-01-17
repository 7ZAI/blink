package com.blink.gateway.security;

import com.blink.framework.common.data.ChannelInfoRedisDO;
import com.blink.gateway.config.prop.BlinkGatewayProperties;
import com.blink.gateway.constant.GatewayConstant;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static com.blink.gateway.constant.GatewayConstant.CHANNEL_INFO;


/**
 * token转换器
 * 从请求中拿到token
 * @Author binblink
 * @Date 2025/8/27
 */
public class TokenServerAuthenticationConverter implements ServerAuthenticationConverter {


    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {

        ChannelInfoRedisDO reqChannel = (ChannelInfoRedisDO) exchange.getAttributes().get(CHANNEL_INFO);

//        reqChannel.getChannelName().equals();



        String token = exchange.getRequest().getHeaders().getFirst(GatewayConstant.X_BLINK_TOKEN);
        String userId = exchange.getRequest().getHeaders().getFirst(GatewayConstant.X_BLINK_USRID);
        //初始未认证的token
        return Mono.just(UsernamePasswordAuthenticationToken.unauthenticated(userId,token));
    }
}
