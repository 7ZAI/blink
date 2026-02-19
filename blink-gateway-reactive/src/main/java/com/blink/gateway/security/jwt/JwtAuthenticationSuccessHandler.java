package com.blink.gateway.security.jwt;

import com.blink.framework.common.data.ChannelInfoRedisDO;
import com.blink.framework.common.data.UserInfoRedisDO;
import com.blink.gateway.constant.GatewayConstant;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

import static com.blink.gateway.constant.GatewayConstant.CHANNEL_INFO;
import static com.blink.gateway.constant.RedisConstans.USER_TOKEN;

/**
 * @Author binblink
 * @Date 2026/2/2
 */
public class JwtAuthenticationSuccessHandler implements ServerAuthenticationSuccessHandler {
    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {

        ServerWebExchange exchange = webFilterExchange.getExchange();


        ChannelInfoRedisDO channelInfoRedisDO = (ChannelInfoRedisDO) exchange.getAttributes().get(CHANNEL_INFO);
        //传递 登录用户信息 用来后续元数据组装使用
        UserInfoRedisDO userInfo = (UserInfoRedisDO) authentication.getPrincipal();
        if(Objects.nonNull(userInfo)) {
            userInfo.setUsername("channel-"+channelInfoRedisDO.getChannelName());
            exchange.getAttributes().put(GatewayConstant.LOGIN_USER_KEY, userInfo);
        }
        return webFilterExchange.getChain().filter(exchange);
    }
}
