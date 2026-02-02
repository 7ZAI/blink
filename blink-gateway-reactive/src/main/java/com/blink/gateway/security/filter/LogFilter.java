package com.blink.gateway.security.filter;

import com.blink.gateway.util.GateWayUtil;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 网关日志记录过滤器
 * 记录请求信息、响应信息和耗时统计
 *
 * @author binblink
 */
@Slf4j
public class LogFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // 记录开始时间
        long startTime = System.currentTimeMillis();

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();
        String requestParams = request.getURI().getQuery();

        String clientIp = GateWayUtil.getClientIp(request);
        var headers = request.getHeaders();

        // 记录请求信息 请求体会在初步的合法性验证后 打印
        log.info("===> 请求开始 请求路径: {},请求参数:{}, 请求方法: {}, 客户端IP: {}", path, requestParams,method, clientIp);
        log.info("请求头: {}", headers);


        // 装饰响应对象以捕获响应体
        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();
        //响应装饰类
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;

                    return super.writeWith(fluxBody.buffer().map(dataBuffers -> {
                        // 合并多个 DataBuffer
                        DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
                        DataBuffer join = dataBufferFactory.join(dataBuffers);
                        byte[] content = new byte[join.readableByteCount()];
                        join.read(content);
                        DataBufferUtils.release(join);

                        // 记录响应信息
                        String responseBody = new String(content, StandardCharsets.UTF_8);
                        long endTime = System.currentTimeMillis();
                        long duration = endTime - startTime;

                        int statusCode = originalResponse.getStatusCode() != null ?
                                originalResponse.getStatusCode().value() : 0;

                        log.info("<=== 请求结束: {} {}, 状态码: {}, 耗时: {}ms", method, path, statusCode, duration);

                        HttpHeaders rspHeaders = originalResponse.getHeaders();

                        log.info("响应头: {}", rspHeaders);
                        // 可选：记录响应体（注意大小）
                        if (responseBody.length() < 3000) {
                            log.debug("响应体: {}", responseBody);
                        } else {
                            log.debug("响应体大小: {} bytes (内容过大，已省略)", responseBody.length());
                        }

                        return bufferFactory.wrap(content);
                    }));
                }
                return super.writeWith(body);
            }
        };

        // 使用装饰后的响应对象
        return chain.filter(exchange.mutate().response(decoratedResponse).build())
                .doOnError(throwable -> {
                    // 记录异常
                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;
                    log.error("<=== 请求异常: {} {}, 耗时: {}ms, 异常信息: {}",method, path, duration, throwable.getMessage());

                })
                .then(Mono.fromRunnable(() -> {
                    // 如果没有响应体，在这里记录完成信息
                    if (originalResponse.getStatusCode() != null &&
                            (originalResponse.getStatusCode().value() == 304 ||
                                    originalResponse.getStatusCode().is3xxRedirection())) {
                        long endTime = System.currentTimeMillis();
                        long duration = endTime - startTime;
                        log.info("<=== 请求结束: {} {}, 状态码: {}, 耗时: {}ms", method, path,originalResponse.getStatusCode().value(), duration);
                    }
                }));
    }

}