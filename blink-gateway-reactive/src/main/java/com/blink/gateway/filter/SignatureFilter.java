package com.blink.gateway.filter;

import com.blink.framework.common.data.ChannelInfoRedisDO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.gateway.component.GateWayCacheComponent;
import com.blink.gateway.constant.GatewayConstant;
import com.blink.gateway.signature.HmacSignatureService;
import com.blink.gateway.signature.SignatureServiceFactory;
import com.blink.gateway.util.GateWayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.blink.gateway.constant.GatewayConstant.*;

/**
 * 签名验证 包含防止请求重放功能
 *
 * @Author binblink
 * @Date 2025/9/3
 */
public class SignatureFilter implements GlobalFilter, Ordered {

    private final Logger logger = LoggerFactory.getLogger(SignatureFilter.class);

    private final SignatureServiceFactory signatureServiceFactory;

    private final ReactiveRedisClient redisClient;

    private final GateWayCacheComponent cacheComponent;

    public SignatureFilter(ReactiveRedisClient redisClient, SignatureServiceFactory signatureServiceFactory, GateWayCacheComponent cacheComponent) {
        this.signatureServiceFactory = signatureServiceFactory;
        this.redisClient = redisClient;
        this.cacheComponent = cacheComponent;
    }

    /**
     * 仅仅验证 必填请求头完整性
     * through the given {@link GatewayFilterChain}.
     *
     * @param exchange the current server exchange
     * @param chain    provides a way to delegate to the next filter
     * @return {@code Mono<Void>} to indicate when request processing is complete
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        return cacheComponent.getGateWayConfigFromCache(SIGNTURE_SWITCH_KEY)
                .defaultIfEmpty(GateWayUtil.getDefaultConfig(SIGNTURE_SWITCH_KEY))
                .flatMap(conf -> {
                    //关闭签名验证 直接继续执行过滤链
                    if (SWITCH_OFF.equals(Byte.valueOf(conf.getConfigValue()))) {
                        return chain.filter(exchange);
                    }

                    return signVerify(exchange)
                            .filter(isValid -> isValid)
                            .switchIfEmpty(Mono.error(new BlinkException("签名验证失败")))
                            .doOnSuccess(v -> logger.debug("signature verify succeed 签名验证成功！"))
                            // 验签成功 进行防止请求重发校验
                            .then(cacheComponent.getGateWayConfigFromCache(REQUEST_REPLAY_DEFEND_SWITCH)
                                    .defaultIfEmpty(GateWayUtil.getDefaultConfig(REQUEST_REPLAY_DEFEND_SWITCH))
                                    // 检查开关是否开启
                                    .flatMap(config -> SWITCH_ON.equals(Byte.valueOf(config.getConfigValue()))
                                            ? processWithDefend(chain, exchange)
                                            : chain.filter(exchange)
                                    )
                            );
                });

    }

    /**
     * 签名验证
     *
     * @param exchange
     * @return
     */
    private Mono<Boolean> signVerify(ServerWebExchange exchange) {

        HttpHeaders headers = exchange.getRequest().getHeaders();

        ChannelInfoRedisDO channelDO = (ChannelInfoRedisDO) exchange.getAttributes().get(CHANNEL_INFO);
        if (Objects.isNull(channelDO)) {
            return Mono.error(new BlinkException("校验错误"));
        }

        String appKey = headers.getFirst(X_BLINK_APPKEY);
        String sign = headers.getFirst(X_BLINK_SIGN);
        String timeStamp = headers.getFirst(X_BLINK_TIMESTAMP);
        String nonce = headers.getFirst(X_BLINK_NONCE);
        String loginName = headers.getFirst(X_BLINK_LOGINNAME);

        HmacSignatureService signatureService = (HmacSignatureService) signatureServiceFactory.getDefaultService();
        Map<String, Object> parameMap = new HashMap<>();
        parameMap.put(KEY_TIMESTAMP, timeStamp);
        parameMap.put(KEY_NONCE, nonce);
        parameMap.put(KEY_LOGINNAME, loginName);
        parameMap.put(KEY_APPKEY, appKey);

        //非加密验证请求头数据 + body数据
        //加密情况下 只验证请求头数据 AES GCM算法保证完整性
        String data = "";
        // 优先验证签名是因为 防止nonce被恶意爆刷进redis
        MediaType mediaType = exchange.getRequest().getHeaders().getContentType();
        //post application/json请求
        if (MediaType.APPLICATION_JSON.equals(mediaType)) {
            // 非加密情况 验证body + 请求头
            data = (String) exchange.getAttributes().get(CACHED_REQUEST_BODY_ATTR);

        }

        return Mono.just(signatureService.verify(data, channelDO.getAppSecret(), sign, parameMap));
    }


    // 提取独立方法处理开关开启的情况
    private Mono<Void> processWithDefend(GatewayFilterChain chain, ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();

        String timeStamp = headers.getFirst(X_BLINK_TIMESTAMP);
        String nonce = headers.getFirst(X_BLINK_NONCE);
        String loginName = headers.getFirst(X_BLINK_LOGINNAME);
        assert timeStamp != null;

        long currentTime = System.currentTimeMillis();
        return isValidTimestamp(Long.parseLong(timeStamp), currentTime)
                .filter(isValid -> isValid)
                //请求时间校验不通过 抛异常
                .switchIfEmpty(Mono.error(new BlinkException("非法请求！ 请求过期")))
                // 验证时间戳是否有效
                .flatMap(r -> checkDuplicateRequest(nonce, loginName, chain, exchange));

    }

    //nonce 提取独立方法处理重复请求检查，包含配置化过期时间
    private Mono<Void> checkDuplicateRequest(String nonce, String loginName,
                                             GatewayFilterChain chain, ServerWebExchange exchange) {
        // 从配置获取过期时间
        return cacheComponent.getGateWayConfigFromCache(REQ_NONCE_EXPIRE_TIME_KEY)
                .defaultIfEmpty(GateWayUtil.getDefaultConfig(REQ_NONCE_EXPIRE_TIME_KEY))
                // 解析配置的过期时间
                .map(expireConfig -> Duration.ofMillis(Long.parseLong(expireConfig.getConfigValue())))
                // 使用配置的过期时间设置值
                .flatMap(expireDuration -> redisClient.setIfAbsentWithExpire(REQ_NONCE_PREFIX + nonce, loginName, expireDuration))
                //设置结果判断
                .flatMap(isSet -> isSet ? chain.filter(exchange) : Mono.error(new BlinkException("请求重复")));
    }


    /**
     * 当前时间 - 请求时间 大于配置的有效时间 认定为无效请求
     *
     * @param timestamp
     * @return
     */
    private Mono<Boolean> isValidTimestamp(long timestamp, long currentTime) {

        return cacheComponent.getGateWayConfigFromCache(REQ_TIMESTAMP_EFFECT_TIME_KEY)
                //缓存拿不到 默认5分钟
                .defaultIfEmpty(GateWayUtil.getDefaultConfig(REQ_TIMESTAMP_EFFECT_TIME_KEY))
                .map(conf -> {
                    long configTime = Long.parseLong(conf.getConfigValue());
                    // 时间窗口
                    return Math.abs(currentTime - timestamp) <= configTime;
                });
    }

    @Override
    public int getOrder() {
        return GatewayConstant.ORDER_LOWEST_ADD_TWO;
    }


}
