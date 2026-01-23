package com.blink.gateway.security;

import com.alibaba.fastjson2.JSON;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.constant.GateWayErrMsgCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


/**
 * 授权异常处理
 * @Author binblink
 * @Date 2025/8/26
 */
public class BlinkAccessDeniedHandler implements ServerAccessDeniedHandler {

    private Logger logger = LoggerFactory.getLogger(BlinkAccessDeniedHandler.class);

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
        denied.printStackTrace();
        logger.error("access denied {}",denied.getMessage());
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        ResponseDTO<EmptyBody> errorResponse = ResponseDTO.newFailInstance();
        errorResponse.setMsgCode(GateWayErrMsgCode.ACCESSDENIED);
        errorResponse.setMsgInfo("无访问权限");

        // 这里可以返回统一的 JSON 响应
        try {
            byte[] jsonBytes = JSON.toJSONBytes(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(jsonBytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }
}
