package com.blink.gateway.component;

import com.blink.base.dto.rsp.QueryErrMsgRspDTO;
import com.blink.framework.common.data.ChannelInfoRedisDO;
import com.blink.framework.common.data.SysConfigCacheDO;
import com.blink.gateway.service.BaseAppService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static com.blink.gateway.constant.GatewayConstant.*;

/**
 * 缓存组件
 *
 * @author binblink
 */
@Component
@Slf4j
public class GateWayCacheComponent {


    @Resource
    private MultiLevelCacheComponent multiLevelCacheComponent;

    @Resource
    private BaseAppService baseAppService;


    /**
     * 从缓存中获取配置参数
     *
     * @param configKey
     * @return
     */
    public Mono<SysConfigCacheDO> getGateWayConfigFromCache(String configKey) {

        String cacheKey = GATEWAY_CONFIG_KEY_PREFIX + configKey;

        return multiLevelCacheComponent.get(cacheKey, SysConfigCacheDO.class,(key, clazz) -> baseAppService.getOneConfig(key))
                .onErrorResume(e -> Mono.empty());
    }

    /**
     * 从缓存中获取渠道信息
     *
     * @param appKey
     * @return
     */
    public Mono<ChannelInfoRedisDO> getChannelInfoFromCache(String appKey) {

        String cacheKey = BLINK_CHANNEL_PREFIX + appKey;

        return multiLevelCacheComponent.get(cacheKey, ChannelInfoRedisDO.class,(key, clazz) -> baseAppService.getChannelInfo(key))
                .onErrorResume(e -> Mono.empty());
    }

    /**
     * 从缓存中获取错误码信息表
     *
     * @param errCode 错误码
     * @param local   语言
     * @return
     */
    public Mono<String> getErrorMsgInfoFromCache(String errCode, String local) {

        String cacheKey = ERR_MSG_PREFIX + local + ":" + errCode;

        return multiLevelCacheComponent.get(cacheKey, String.class,
                        (key, clazz) -> baseAppService.getErrorMsgInfo(errCode, local).map(QueryErrMsgRspDTO::getMsgInfo))
                .onErrorResume(e -> Mono.empty());
    }


}
