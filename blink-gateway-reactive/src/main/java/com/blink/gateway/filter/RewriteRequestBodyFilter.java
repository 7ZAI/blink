package com.blink.gateway.filter;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.blink.framework.common.data.ChannelInfoRedisDO;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.UserInfoRedisDO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.utils.ApplicationContextUtil;
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
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Objects;

import static com.blink.framework.common.constrant.SysConstant.*;
import static com.blink.gateway.constant.GatewayConstant.CACHED_REQUEST_BODY_ATTR;
import static com.blink.gateway.constant.GatewayConstant.CHANNEL_INFO;


/**
 *
 * 生成请求ID和链路ID放入请求头
 * 完善报文填入值
 * 注意order 顺序
 * 此过滤器在 认证授权完成后执行 请查看类 {@link SecurityWebFiltersOrder}  后设置
 *
 * @author binblink
 */

public class RewriteRequestBodyFilter implements GlobalFilter, Ordered {

    private final Logger logger = LoggerFactory.getLogger(RewriteRequestBodyFilter.class);
    private final ReactiveIdGenerator idGenerator;

    public RewriteRequestBodyFilter(ReactiveIdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String bodyStr = (String) exchange.getAttributes().get(CACHED_REQUEST_BODY_ATTR);
        //文件提交 bodyStr会为空
        if (StrUtil.isNotBlank(bodyStr)) {
            return idGenerator.generateRequestId()
                    .zipWith(idGenerator.generateTraceId())
                    .switchIfEmpty(Mono.error(new BlinkException("999999", false)))
                    .flatMap(tuple -> {
                        String requestId = tuple.getT1();
                        String traceId = tuple.getT2();
                        logger.info("generate requestId {} tarceId {}", requestId, traceId);


                        ChannelInfoRedisDO channelInfo = exchange.getAttribute(CHANNEL_INFO);
                        if (Objects.isNull(channelInfo)) {
                            return Mono.error(new BlinkException("系统错误!"));
                        }


                        RequestDTO requestDTO = JSON.parseObject(bodyStr, RequestDTO.class);
                        //组装元数据
                        this.assembleReqDTO(requestDTO, channelInfo, requestId, traceId, exchange);

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
                                headers.put(X_BLINK_TRACE_ID, Collections.singletonList(traceId));

                                return headers;
                            }
                        };

                        // 继续过滤器链
                        return chain.filter(exchange.mutate().request(modifiedRequest).build());
                    });
        }

        return chain.filter(exchange);

    }

    /**
     * 组装元数据
     * @param requestDTO json字符串转换后的请求DTO
     * @param channelInfo 渠道信息
     * @param requestId 请求id
     * @param traceId 追踪id
     * @param exchange 请求
     * @return RequestDTO
     */
    private RequestDTO assembleReqDTO(RequestDTO requestDTO,ChannelInfoRedisDO channelInfo, String requestId, String traceId, ServerWebExchange exchange) {

        UserInfoRedisDO userInfo = exchange.getAttribute(GatewayConstant.LOGIN_USER_KEY);

        if(Objects.nonNull(userInfo)) {
            requestDTO.setLoginName(userInfo.getLoginName());
            requestDTO.setUserId(String.valueOf(userInfo.getUserId()));
        }

        requestDTO.setReqDate(LocalDate.now());
        requestDTO.setChannel(channelInfo.getChannelName());
        requestDTO.setRequestId(requestId);
        requestDTO.setTraceId(traceId);
        requestDTO.setClientIp(GateWayUtil.getClientIp(exchange.getRequest()));
        //来自网关
        requestDTO.setSource(ApplicationContextUtil.getApplicationContext().getApplicationName());
        //初始spanid 统一设置为00 parentId为空
        requestDTO.setSpanId(GatewayConstant.SPAN_ID_ORIGINAL);
        requestDTO.setToken(exchange.getRequest().getHeaders().getFirst(X_BLINK_TOKEN));

        return requestDTO;
    }

    @Override
    public int getOrder() {
        return SecurityWebFiltersOrder.AUTHENTICATION.getOrder() + 100;
    }
}
