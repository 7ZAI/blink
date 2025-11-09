package com.blink.gateway.security;

import com.alibaba.fastjson2.JSON;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.constant.GateErrMsgCode;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import reactor.core.publisher.Mono;

/**
 * 认证失败
 * @Author binblink
 * @Date 2025/8/28
 */
public class BlinkAuthenticationFailureHandler implements ServerAuthenticationFailureHandler {
    @Override
    public Mono<Void> onAuthenticationFailure(WebFilterExchange webFilterExchange, AuthenticationException exception) {
        ServerHttpResponse response = webFilterExchange.getExchange().getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ResponseDTO<EmptyBody> errorResponse = ResponseDTO.newFailInstance();

        errorResponse.setMsgCode(GateErrMsgCode.UNAUTHORIZED);
        errorResponse.setMsgInfo(exception.getMessage());

        try {
            byte[] jsonBytes = JSON.toJSONBytes(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(jsonBytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
