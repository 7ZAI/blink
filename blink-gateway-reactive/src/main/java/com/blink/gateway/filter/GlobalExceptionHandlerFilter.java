package com.blink.gateway.filter;

import com.blink.gateway.constant.GatewayConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

public class GlobalExceptionHandlerFilter implements WebExceptionHandler,Ordered {

    private Logger logger = LoggerFactory.getLogger(GlobalExceptionHandlerFilter.class);
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {

        logger.error(" Gateway GlobalException {}",ex.getMessage());
        ex.printStackTrace();
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        response.getHeaders().set("Content-Type", "application/json");

        String errorMessage = "{\"message\": \"系统异常，请稍后重试\"}";
        DataBufferFactory bufferFactory = response.bufferFactory();
        return response.writeWith(Mono.just(bufferFactory.wrap(errorMessage.getBytes(StandardCharsets.UTF_8))));
    }

    @Override
    public int getOrder() {
        return GatewayConstant.ORDER_LOWEST;
    }
}
