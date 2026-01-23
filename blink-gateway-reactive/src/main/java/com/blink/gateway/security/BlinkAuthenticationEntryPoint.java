package com.blink.gateway.security;


import com.alibaba.fastjson2.JSON;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.constant.GateWayErrMsgCode;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 认证失败异常处理
 * @Author binblink
 * @Date 2025/8/26
 */
public class BlinkAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {


    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ResponseDTO<EmptyBody> errorResponse = ResponseDTO.newFailInstance();

        errorResponse.setMsgCode(GateWayErrMsgCode.UNAUTHORIZED);
        errorResponse.setMsgInfo("认证失败 token过期失效");

        try {
            byte[] jsonBytes = JSON.toJSONBytes(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(jsonBytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}