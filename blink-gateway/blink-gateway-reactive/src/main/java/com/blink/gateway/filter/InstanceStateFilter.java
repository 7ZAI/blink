package com.blink.gateway.filter;

import com.blink.gateway.component.GatewayInstanceStateManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 实例状态过滤器
 *
 * 在网关优雅下线时，拒绝新请求
 * 必须在其他业务过滤器之前执行
 *
 * @author binblink
 * @since 2026-04-16
 */
@Component
@Slf4j
public class InstanceStateFilter implements GlobalFilter, Ordered {

    /**
     * 过滤器优先级：最高优先级，在所有其他过滤器之前执行
     */
    private static final int ORDER = Integer.MIN_VALUE + 100;

    /**
     * 实例正在下线的 HTTP 状态码
     */
    private static final int SERVICE_UNAVAILABLE = 503;

    @Resource
    private GatewayInstanceStateManager instanceStateManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 检查实例是否正在接收请求
        if (!instanceStateManager.shouldAcceptRequest()) {
            log.warn("[InstanceStateFilter] 实例正在下线，拒绝请求 | path: {}, instance: {}",
                    exchange.getRequest().getPath().value(),
                    instanceStateManager.getInstanceIdentifier());

            return buildServiceUnavailableResponse(exchange);
        }

        // 正常放行请求
        return chain.filter(exchange);
    }

    /**
     * 构建服务不可用响应
     */
    private Mono<Void> buildServiceUnavailableResponse(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatusCode.valueOf(SERVICE_UNAVAILABLE));
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> result = new HashMap<>();
        result.put("code", SERVICE_UNAVAILABLE);
        result.put("msg", "服务正在下线，请稍后重试");
        result.put("data", null);

        String body;
        try {
            body = objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            body = "{\"code\":503,\"msg\":\"服务正在下线，请稍后重试\",\"data\":null}";
        }

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
