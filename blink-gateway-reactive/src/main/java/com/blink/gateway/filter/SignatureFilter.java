package com.blink.gateway.filter;

import com.blink.framework.common.data.ChannelInfoRedisDO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.gateway.component.GateWayCacheComponent;
import com.blink.gateway.constant.GateWayErrMsgCode;
import com.blink.gateway.signature.HmacSignatureService;
import com.blink.gateway.signature.SignatureServiceFactory;
import com.blink.gateway.util.GateWayUtil;
import lombok.extern.slf4j.Slf4j;
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

import static com.blink.framework.common.constrant.SysConstant.*;
import static com.blink.gateway.constant.GatewayConstant.*;

/**
 * 签名验证 包含防止请求重放功能
 *
 * @Author binblink
 */
@Slf4j
public class SignatureFilter implements GlobalFilter, Ordered {

    private final SignatureServiceFactory signatureServiceFactory;


    private final GateWayCacheComponent cacheComponent;

    public SignatureFilter(SignatureServiceFactory signatureServiceFactory, GateWayCacheComponent cacheComponent) {
        this.signatureServiceFactory = signatureServiceFactory;
        this.cacheComponent = cacheComponent;
    }

    /**
     * 签名过滤器
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        log.info("----------- 开始签名校验 -----------");
        //缓存参数 开关判断
        return cacheComponent.getGateWayConfigFromCache(SIGNTURE_SWITCH_KEY)
                .defaultIfEmpty(GateWayUtil.getDefaultConfig(SIGNTURE_SWITCH_KEY))
                .flatMap(conf -> {
                    //关闭签名验证 直接继续执行过滤链
                    if (SWITCH_OFF.equals(Byte.valueOf(conf.getConfigValue()))) {
                        return chain.filter(exchange);
                    }

                    return signVerify(exchange)
                            .filter(isValid -> isValid)
                            .switchIfEmpty(Mono.error(new BlinkException(GateWayErrMsgCode.ILLEGAL_REQUEST)))
                            //成功继续执行
                            .flatMap(r-> chain.filter(exchange))
                            // 验签成功
                            .doOnSuccess(v -> log.info("signature verify succeed 签名验证成功！"));
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

        HmacSignatureService signatureService = (HmacSignatureService) signatureServiceFactory.getDefaultService();
        Map<String, Object> parameMap = new HashMap<>();
        parameMap.put(KEY_TIMESTAMP, timeStamp);
        parameMap.put(KEY_NONCE, nonce);
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


    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 102;
    }

}
