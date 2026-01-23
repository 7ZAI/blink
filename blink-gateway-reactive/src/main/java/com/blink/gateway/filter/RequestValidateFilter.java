package com.blink.gateway.filter;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.blink.framework.common.data.ChannelInfoRedisDO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.gateway.component.GateWayCacheComponent;
import com.blink.gateway.util.GateWayUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

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
public class RequestValidateFilter implements GlobalFilter, Ordered {


    private final GateWayCacheComponent cacheComponent;

    public RequestValidateFilter(GateWayCacheComponent cacheComponent) {
        this.cacheComponent = cacheComponent;
    }

    /**
     * 合法性校验过滤器 第一个执行
     * 校验内容 包括： 请求头必填项 content-length长度检验 请求方法 类型 渠道校验
     *
     * @param exchange
     * @param chain
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {


        var httpRequest = exchange.getRequest();
        var headers = httpRequest.getHeaders();


        log.info("----------- 开始校验请求合法性 -----------");

        //获取客户端渠道信息key
        String appKey = headers.getFirst(X_BLINK_APPKEY);

        return headerValidate(httpRequest).flatMap(isValid -> isValid
                ? processChecking(exchange, chain, headers, appKey)
                : Mono.error(new BlinkException(ILLEGAL_REQUEST))
        );

    }

    /**
     * 校验必填请求头 和 约束http提交方法为POST Content-Type为application/json
     */
    private Mono<Boolean> headerValidate(ServerHttpRequest httpRequest) {

        HttpHeaders headers = httpRequest.getHeaders();

        if (!checkRequestType(headers, httpRequest.getMethod())) {
            log.error("请求类型校验失败！");
            return Mono.just(false);
        }

        if (!checkHeadersData(headers, httpRequest.getPath().value())) {
            log.error("请求头数据校验失败！");
            return Mono.just(false);
        }

        return Mono.just(true);
    }

    /**
     * 继续校验 组装程序处理模块
     * 拆分为多个方法避免return嵌套 虽然实际上还是 return 回调return
     * 但是函数单一职责封装 提高了代码可读性
     *
     * @param exchange
     * @param chain
     * @param headers
     * @param appKey
     * @return
     */
    private Mono<Void> processChecking(ServerWebExchange exchange, GatewayFilterChain chain, HttpHeaders headers, String appKey) {
        return checkChannel(exchange, headers, appKey).flatMap(isValid -> isValid
                ? cacheRequestBody(exchange, chain)
                : Mono.error(new BlinkException(ILLEGAL_REQUEST)));
    }

    /**
     * 校验渠道信息
     *
     * @param exchange 请求服务
     * @param headers  请求头
     * @param appKey   渠道key
     * @return
     */
    private Mono<Boolean> checkChannel(ServerWebExchange exchange, HttpHeaders headers, String appKey) {

        //从缓存组件中获取参数
        return cacheComponent.getChannelInfoFromCache(appKey)
                //抛异常 后续flatMap 不再执行
                .switchIfEmpty(Mono.error(new BlinkException(FAILED_TO_GET_CHANNEL)))
                .flatMap(channelDO -> {
                    //渠道开关校验
                    if (SWITCH_OFF.equals(channelDO.getEnable())) {
                        //渠道已关闭
                        return Mono.error(new BlinkException(CHANNEL_CLOSED));
                    }
                    // 开关校验加密必须项目
                    if (SWITCH_ON.equals(channelDO.getEncryptionSwitch())) {
                        String key = headers.getFirst(X_BLINK_KEY);
                        String iv = headers.getFirst(X_BLINK_IV);
                        //缺失必要请求头 抛非法请求异常
                        if (StrUtil.isBlank(key) || StrUtil.isBlank(iv)) {
                            log.warn("缺失必要请求头 channel:{}", channelDO.getChannelName());
                            return Mono.error(new BlinkException(ILLEGAL_REQUEST));
                        }
                    }
                    // 方便后续使用 减少redis访问
                    exchange.getAttributes().put(CHANNEL_INFO, channelDO);
                    return Mono.just(true);
                });
    }

    /**
     * 缓存 body 到 Attributes 中
     * 后续 签名 加密 填充报文 均会用到 所以缓存下来
     *
     * @param exchange
     * @param chain
     * @return
     */
    private Mono<Void> cacheRequestBody(ServerWebExchange exchange, GatewayFilterChain chain) {
        //只针对post application/json请求类型 缓存body
        if (GateWayUtil.shouldCacheRequestBody(exchange.getRequest())) {
            //缓存body
            return cacheRequestBodyToAttributes(exchange).flatMap(mutatedRequest ->
                            //跟换装饰后的request请求 继续执行过滤链
                            chain.filter(exchange.mutate().request(mutatedRequest).build())
//                            .doFinally(s -> {
//
//                        Object backupCachedBody = exchange.getAttributes()
//                                .get(CACHED_ORIGINAL_REQUEST_BODY_BACKUP_ATTR);
//                        if (backupCachedBody instanceof DataBuffer dataBuffer) {
//                            DataBufferUtils.release(dataBuffer);
//                        }
//                    })
            );
        }
        // 继续执行过滤链
        return chain.filter(exchange);
    }


    /**
     * 校验请求类型  只允许 method 为post  ContentType 为application/json和文件上传类型 通过
     *
     * @param headers 请求头
     * @param method  请求方法
     * @return
     */
    private Boolean checkRequestType(HttpHeaders headers, HttpMethod method) {

        MediaType currentType = headers.getContentType();
        long contentLength = headers.getContentLength();
        //getContentType 非空
        if (Objects.isNull(currentType) || contentLength == -1) {
            return false;
        }
        //文件上传 放过
        if (MediaType.MULTIPART_FORM_DATA_VALUE.equalsIgnoreCase(currentType.getType())) {
            //文件上传 大小限制
            if (contentLength > -1) {
                return contentLength <= MAX_UPLOAD;
            }
            return true;
        }

        //请求接口为json
        if (MediaType.APPLICATION_JSON.equalsTypeAndSubtype(currentType)) {
            //不是POST 返回false
            if (!method.matches(HttpMethod.POST.name())) {
                return false;
            }
            //普通api 大小限制
            if (contentLength > -1) {
                return contentLength <= MAX_API;
            }
            return true;
        }
        //其他请求方法均拒绝
        return false;
    }

    /**
     * 非空请求头校验 包含长度校验
     *
     * @param headers 请求头
     * @param path    请求路径
     * @return
     */
    private Boolean checkHeadersData(HttpHeaders headers, String path) {


        // 调用方appkey channel sign必填
        String appKey = headers.getFirst(X_BLINK_APPKEY);
        String sign = headers.getFirst(X_BLINK_SIGN);
        String token = headers.getFirst(X_BLINK_TOKEN);
        String timeStamp = headers.getFirst(X_BLINK_TIMESTAMP);
        String nonce = headers.getFirst(X_BLINK_NONCE);

        //非空项目
        if (!(StrUtil.isNotBlank(appKey) && StrUtil.isNotBlank(timeStamp)
                && StrUtil.isNotBlank(sign) && StrUtil.isNotBlank(nonce))) {

            log.debug("缺失必要请求头！");
            return false;
        }
        //校验时间戳格式 为long类型
        try {
            Long.parseLong(timeStamp);
        } catch (NumberFormatException e) {
            //数据格式错误 转换失败异常
            log.error("非法请求 格式错误！{}", e.getMessage());
            return false;
        }
        //请求路径非登入请求 loginName和token 必填
        if (!LOGIN_PATH.equals(path)) {
            if (StrUtil.isBlank(token)) {
                return false;
            }
        }

        //内容长度校验
        if (!checkHeaderLength(appKey, sign, token, timeStamp, nonce)) {
            log.debug("请求头长度校验失败！");
            return false;
        }

        return true;
    }

    /**
     * @param appKey    接入应用key
     * @param sign      签名
     * @param token     登入凭证
     * @param timeStamp 时间戳
     * @param nonce     随机数
     * @return
     */
    private boolean checkHeaderLength(String appKey, String sign, String token, String timeStamp, String nonce) {

        if (appKey.length() > LENGTH_LIMIT_128) {
            return false;
        }

        if (sign.length() > LENGTH_LIMIT_1024) {
            return false;
        }

        if (token.length() > LENGTH_LIMIT_128) {
            return false;
        }

        if (timeStamp.length() > String.valueOf(Long.MAX_VALUE).length()) {
            return false;
        }
        if(nonce.length() > LENGTH_LIMIT_128){
            return false;
        }

        return true;

    }

    /**
     * 缓存body内容 转为字符串 重新包装 ServerHttpRequest
     * 参考来自至@link{CacheRequestBodyGatewayFilterFactory}的代码 略作修改
     */
    private Mono<ServerHttpRequest> cacheRequestBodyToAttributes(ServerWebExchange exchange) {
        //cacheRequestBodyAndRequest 内部缓存并返回装饰类
        return ServerWebExchangeUtils.cacheRequestBodyAndRequest(exchange, (serverHttpRequest) -> {
            // ServerHttpRequest转换为ServerRequest
            final ServerRequest serverRequest = ServerRequest
                    .create(exchange.mutate().request(serverHttpRequest).build(), HandlerStrategies.withDefaults().messageReaders());
            //利用原生bodyToMono方法 读取body内容转换为设置类型
            return serverRequest.bodyToMono(String.class).doOnNext(objectValue -> {

                ChannelInfoRedisDO channelInfoRedisDO = exchange.getAttribute(CHANNEL_INFO);
                String jsonString = objectValue;
                //如果关闭加密 前端json字符串可能带有转义字符 导致验证签名不通过
                if (SWITCH_OFF.equals(channelInfoRedisDO.getEncryptionSwitch())) {
                    // 规范化json 去掉/r/n 等转义字符
                    JSONObject jsonObject = JSON.parseObject(objectValue);
                    jsonString = JSON.toJSONString(jsonObject);
                }
                log.info("请求体:{}", jsonString);
                //缓存请求body 字符串到 请求域
                Object previousCachedBody = exchange.getAttributes()
                        .put(CACHED_REQUEST_BODY_ATTR, jsonString);
//                if (previousCachedBody != null) {
//                    // store previous cached body
//                    exchange.getAttributes().put(CACHED_ORIGINAL_REQUEST_BODY_BACKUP_ATTR, previousCachedBody);
//                }
            }).then(Mono.defer(() -> {
                //cacheRequestBodyAndRequest方法中 中缓存了 装饰类
                ServerHttpRequest cachedRequest = exchange
                        .getAttribute(CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR);
                Assert.notNull(cachedRequest, "cache request shouldn't be null");
                exchange.getAttributes().remove(CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR);
                return Mono.just(cachedRequest);
            }));
        });
    }


    /**
     * order值 越小越优先
     *
     * @return
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 11;
    }


}

