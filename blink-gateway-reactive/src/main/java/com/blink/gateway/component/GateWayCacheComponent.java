package com.blink.gateway.component;

import com.blink.framework.common.data.ChannelInfoRedisDO;
import com.blink.framework.common.data.SysConfigCacheDO;
import com.blink.gateway.service.BaseAppService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static com.blink.gateway.constant.GatewayConstant.BLINK_CHANNEL_PREFIX;
import static com.blink.gateway.constant.GatewayConstant.GATEWAY_CONFIG_KEY_PREFIX;

/**
 * 缓存组件
 * @Author binblink
 * @Date 2025/10/15
 */
@Component
public class GateWayCacheComponent {

    private final Logger logger = LoggerFactory.getLogger(GateWayCacheComponent.class);

    @Resource
    private MultiLevelCacheComponent multiLevelCacheComponent;

    @Resource
    private BaseAppService baseAppService;


    /**
     * 从缓存中获取配置参数
     * @param configKey
     * @return
     */
    public Mono<SysConfigCacheDO> getGateWayConfigFromCache(String configKey) {

        String cacheKey = GATEWAY_CONFIG_KEY_PREFIX + configKey;

        return multiLevelCacheComponent.get(cacheKey, SysConfigCacheDO.class,
                        (key, clazz) -> baseAppService.getOneConfig(key)
                                .doOnNext(value -> setCache(cacheKey, value)))
                .onErrorResume(e -> Mono.empty());
    }

    /**
     * 从缓存中获取渠道信息
     * @param appKey
     * @return
     */
    public Mono<ChannelInfoRedisDO> getChannelInfoFromCache(String appKey) {

        String cacheKey = BLINK_CHANNEL_PREFIX + appKey;

        return multiLevelCacheComponent.get(cacheKey, ChannelInfoRedisDO.class,
                        (key, clazz) -> baseAppService.getChannelInfo(key)
                                .doOnNext(value -> setCache(cacheKey, value)))
                .onErrorResume(e -> Mono.empty());
    }


    private void setCache(String key, Object value) {

        logger.info("从远程服务获取参数 成功！key:{},value:{}", key, value);
        multiLevelCacheComponent.setRedisCache(key, value).subscribe();
        multiLevelCacheComponent.setLocalCache(key, value);
    }


}
