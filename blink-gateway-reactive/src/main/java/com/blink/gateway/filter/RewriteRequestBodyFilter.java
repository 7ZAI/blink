package com.blink.gateway.filter;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.blink.framework.common.data.ChannelInfoRedisDO;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.redis.id.ReactiveIdGenerator;
import com.blink.gateway.constant.GatewayConstant;
import com.blink.gateway.util.GateWayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Objects;

import static com.blink.gateway.constant.GatewayConstant.*;


/**
 * 全局过滤器 生成请求ID和链路ID
 * 放入请求头
 * 报文填入值
 *
 * @author binblink
 */

public class RewriteRequestBodyFilter implements GlobalFilter, Ordered {

    private Logger logger = LoggerFactory.getLogger(RewriteRequestBodyFilter.class);
    private ReactiveIdGenerator idGenerator;

   public RewriteRequestBodyFilter(ReactiveIdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String bodyStr = (String) exchange.getAttributes().get(CACHED_REQUEST_BODY_ATTR);
        //文件提交 bodyStr会为空
        if( StrUtil.isNotBlank(bodyStr)){
            return idGenerator.generateRequestId()
                    .zipWith(idGenerator.generateTraceId())
                    .switchIfEmpty(Mono.error(new BlinkException("999999", false)))
                    .flatMap(tuple -> {
                        String requestId = tuple.getT1();
                        String traceId = tuple.getT2();
                        logger.info("generate requestId {} tarceId {}", requestId, traceId);


                        ChannelInfoRedisDO channelInfo  = exchange.getAttribute(CHANNEL_INFO);
                        if( Objects.isNull(channelInfo)){
                            return Mono.error(new BlinkException("系统错误!"));
                        }
                        RequestDTO requestDTO = JSON.parseObject(bodyStr,RequestDTO.class);
                        requestDTO.setReqDate(LocalDate.now());
                        requestDTO.setChannel(channelInfo.getChannelName());
                        requestDTO.setRequestId(requestId);
                        requestDTO.setTraceId(traceId);
                        requestDTO.setClientIp(GateWayUtil.getClientIp(exchange.getRequest()));
                        requestDTO.setLoginName(exchange.getRequest().getHeaders().getFirst(X_BLINK_LOGINNAME));
                        requestDTO.setSource("gateway");
                        requestDTO.setToken(exchange.getRequest().getHeaders().getFirst(X_BLINK_TOKEN));


                        String modifiedBody = JSON.toJSONString(requestDTO);
                        // 创建新的请求，添加ID到Header
                        // 将修改后的body转换为DataBuffer
                        byte[] modifiedBodyBytes = modifiedBody.getBytes(StandardCharsets.UTF_8);
                        DataBuffer bodyDataBuffer = exchange.getResponse().bufferFactory().wrap(modifiedBodyBytes);

                        // 重新构造请求，并更新content-length
                        ServerHttpRequest modifiedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
                            @Override
                            public Flux<DataBuffer> getBody() {
                                return Flux.just(bodyDataBuffer);
                            }

                            @Override
                            public HttpHeaders getHeaders() {
                                HttpHeaders headers = new HttpHeaders();
                                headers.putAll(exchange.getRequest().getHeaders());
                                // 由于修改了body，所以需要重新设置content-length
                                headers.setContentLength(modifiedBodyBytes.length);
                                // 移除transfer-encoding，因为我们已经设置了content-length
                                headers.remove(HttpHeaders.TRANSFER_ENCODING);

                                headers.put(X_BLINK_REQUEST_ID, Collections.singletonList(requestId));
                                headers.put(X_BLINK_TRACE_ID,Collections.singletonList(traceId));

                                return headers;
                            }
                        };

                        // 继续过滤器链
                        return chain.filter(exchange.mutate().request(modifiedRequest).build());
                    });
        }

        return chain.filter(exchange);

    }

    @Override
    public int getOrder() {
        return GatewayConstant.ORDER_LOWEST_ADD_FOUR;
    }
}
