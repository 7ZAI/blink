package com.blink.gateway.security.filter;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;

import com.blink.framework.common.data.ChannelInfoRedisDO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.gateway.component.GateWayCacheComponent;
import com.blink.gateway.config.prop.BlinkGatewayConfigProperties;
import com.blink.gateway.util.GateWayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

import static com.blink.framework.common.constrant.SysConstant.*;
import static com.blink.gateway.constant.GateWayErrMsgCode.*;
import static com.blink.gateway.constant.GatewayConstant.*;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR;

/**
 * 全局路由过滤 针对请求头请求类型进行合法性校验
 * 规定只能通过 method 为post  ContentType 为application/json 和文件上传类型
 * 校验请求头必输项
 * 校验请求体必输项长度
 * <p>
 * 最后如果缓存 body 内容
 * TODO 文件上传 未实现 目前一律放过
 *
 * @author binblink
 */
@Slf4j
public class RequestValidateFilter implements WebFilter {

    private final GateWayCacheComponent cacheComponent;
    private final BlinkGatewayConfigProperties gatewayConfigProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RequestValidateFilter(GateWayCacheComponent cacheComponent, BlinkGatewayConfigProperties gatewayConfigProperties) {
        this.cacheComponent = cacheComponent;
        this.gatewayConfigProperties = gatewayConfigProperties;
    }

    /**
     * 合法性校验过滤器 第一个执行
     * 校验内容 包括： 请求头必填项 content-length长度检验 请求方法 类型 渠道校验
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        var httpRequest = exchange.getRequest();
        var headers = httpRequest.getHeaders();
        var path = httpRequest.getPath().value();

        log.info("===> 开始校验请求合法性 path: {}", path);

      

        return headerValidate(httpRequest)
                .filter(isValid -> isValid)
                .switchIfEmpty(Mono.error(new BlinkException(ILLEGAL_REQUEST)))
                .flatMap(r -> checkChannel(exchange, headers))
                .filter(isValid -> isValid)
                .switchIfEmpty(Mono.error(new BlinkException(ILLEGAL_REQUEST)))
                .flatMap(r -> cacheRequestBody(exchange, chain));
    }

    /**
     * 检查URL是否在忽略拦截列表中
     */
    private boolean isIgnoredUrl(String path) {
        List<String> ignoreUrls = gatewayConfigProperties.getIgnoreInterceptUrl();
        if (ignoreUrls == null || ignoreUrls.isEmpty()) {
            return false;
        }
        return ignoreUrls.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 校验必填请求头 和 约束http提交方法为POST Content-Type为application/json
     */
    private Mono<Boolean> headerValidate(ServerHttpRequest httpRequest) {

        return Mono.justOrEmpty(httpRequest.getHeaders())
                .flatMap(httpHeaders -> {
                    if (!checkRequestType(httpHeaders, httpRequest.getMethod())) {
                        log.warn("请求类型校验失败！");
                        return Mono.just(false);
                    }

                    if (!checkHeadersData(httpHeaders, httpRequest.getPath().value())) {
                        log.warn("必填请求头或者请求头内容长度校验失败！");
                        return Mono.just(false);
                    }
                    return Mono.just(true);
                })
                .defaultIfEmpty(false);
    }


    /**
     * 校验渠道信息
     */
    private Mono<Boolean> checkChannel(ServerWebExchange exchange, HttpHeaders headers) {

        String appKey = headers.getFirst(X_BLINK_APPKEY);
        return cacheComponent.getChannelInfoFromCache(appKey)
                .switchIfEmpty(Mono.error(new BlinkException(FAILED_TO_GET_CHANNEL)))
                .flatMap(channelDO -> {
                    if (SWITCH_OFF.equals(channelDO.getEnable())) {
                        log.warn("渠道:{}已关闭！拒绝访问！", channelDO.getChannelName());
                        return Mono.error(new BlinkException(CHANNEL_CLOSED));
                    }

                    if (SWITCH_ON.equals(channelDO.getEncryptionSwitch())) {
                        String key = headers.getFirst(X_BLINK_KEY);
                        String iv = headers.getFirst(X_BLINK_IV);
                        if (StrUtil.isBlank(key) || StrUtil.isBlank(iv)) {
                            log.warn("缺失必要请求头 channel:{}", channelDO.getChannelName());
                            return Mono.error(new BlinkException(ILLEGAL_REQUEST));
                        }
                    }
                    exchange.getAttributes().put(CHANNEL_INFO, channelDO);
                    return Mono.just(true);
                });


    }

    /**
     * 缓存 body 到 Attributes 中
     */
    private Mono<Void> cacheRequestBody(ServerWebExchange exchange, WebFilterChain chain) {
        if (GateWayUtil.shouldCacheRequestBody(exchange.getRequest())) {
            return cacheRequestBodyToAttributes(exchange)
                    .flatMap(mutatedRequest -> chain.filter(exchange.mutate().request(mutatedRequest).build()));
        }
        return chain.filter(exchange);
    }


    /**
     * 校验请求类型  只允许 method 为post  ContentType 为application/json和文件上传类型 通过
     */
    private Boolean checkRequestType(HttpHeaders headers, HttpMethod method) {

        MediaType currentType = headers.getContentType();
        long contentLength = headers.getContentLength();
        if (Objects.isNull(currentType) || contentLength == -1) {
            log.warn("请求未设置ContentType");
            return false;
        }
        if (MediaType.MULTIPART_FORM_DATA_VALUE.equalsIgnoreCase(currentType.getType())) {
            if (contentLength > -1) {
                return contentLength <= MAX_UPLOAD;
            }
            return true;
        }

        if (MediaType.APPLICATION_JSON.equalsTypeAndSubtype(currentType)) {
            if (!method.matches(HttpMethod.POST.name())) {
                return false;
            }
            if (contentLength > -1) {
                return contentLength <= MAX_API;
            }
            return true;
        }
        return false;
    }

    /**
     * 非空请求头校验 包含长度校验
     */
    private Boolean checkHeadersData(HttpHeaders headers, String path) {

        String appKey = headers.getFirst(X_BLINK_APPKEY);
        String sign = headers.getFirst(X_BLINK_SIGN);
        String token = headers.getFirst(X_BLINK_TOKEN);
        String timeStamp = headers.getFirst(X_BLINK_TIMESTAMP);
        String nonce = headers.getFirst(X_BLINK_NONCE);

        if (!(StrUtil.isNotBlank(appKey) && StrUtil.isNotBlank(timeStamp)
                && StrUtil.isNotBlank(sign) && StrUtil.isNotBlank(nonce))) {

            log.debug("缺失必要请求头！");
            return false;
        }
        try {
            Long.parseLong(timeStamp);
        } catch (NumberFormatException e) {
            log.error("非法请求 格式错误！{}", e.getMessage());
            return false;
        }
    
         // 检查是否在忽略拦截URL列表中
        if (!isIgnoredUrl(path)) {
            //非忽略url token必填
            if (StrUtil.isBlank(token)) {
                return false;
            }
        }

        if (!checkHeaderLength(appKey, sign, token, timeStamp, nonce)) {
            log.debug("请求头长度校验失败！");
            return false;
        }

        return true;
    }

    private boolean checkHeaderLength(String appKey, String sign, String token, String timeStamp, String nonce) {

        if (appKey.length() > LENGTH_LIMIT_128) {
            return false;
        }

        if (sign.length() > LENGTH_LIMIT_1024) {
            return false;
        }

        if (token != null && token.length() > LENGTH_LIMIT_4096) {
            return false;
        }

        if (timeStamp.length() > String.valueOf(Long.MAX_VALUE).length()) {
            return false;
        }
        if (nonce.length() > LENGTH_LIMIT_128) {
            return false;
        }

        return true;

    }

    /**
     * 缓存body内容 转为字符串 重新包装 ServerHttpRequest
     */
    private Mono<ServerHttpRequest> cacheRequestBodyToAttributes(ServerWebExchange exchange) {
        return ServerWebExchangeUtils.cacheRequestBodyAndRequest(exchange, (serverHttpRequest) -> {
            final ServerRequest serverRequest = ServerRequest
                    .create(exchange.mutate().request(serverHttpRequest).build(), HandlerStrategies.withDefaults().messageReaders());
            return serverRequest.bodyToMono(String.class).doOnNext(objectValue -> {

                ChannelInfoRedisDO channelInfoRedisDO = exchange.getAttribute(CHANNEL_INFO);
                String jsonString = objectValue;
                if (channelInfoRedisDO != null && SWITCH_OFF.equals(channelInfoRedisDO.getEncryptionSwitch())) {
                    try {
                        jsonString = JacksonUtil.normalizeJson(jsonString);
                    } catch (RuntimeException e) {
                        throw new BlinkException(e,"");
                    }

                }
                log.info("===> 缓存请求体:{}", jsonString);
                exchange.getAttributes().put(CACHED_REQUEST_BODY_ATTR, jsonString);
            }).then(Mono.defer(() -> {
                ServerHttpRequest cachedRequest = exchange.getAttribute(CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR);
                Assert.notNull(cachedRequest, "cache request shouldn't be null");
                exchange.getAttributes().remove(CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR);
                return Mono.just(cachedRequest);
            }));
        });
    }


}
