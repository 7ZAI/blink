package com.blink.gateway.component;


import com.blink.base.dto.rsp.QueryErrMsgRsp;
import com.blink.base.dto.rsp.QueryUserPermissionRsp;
import com.blink.framework.common.data.ChannelInfoRedisDO;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.data.SysConfigCacheDO;
import com.blink.gateway.constant.GatewayConstant;
import com.blink.gateway.dto.req.QueryChannelConfigReq;
import com.blink.gateway.dto.req.QueryOneChannelReq;
import com.blink.gateway.dubbo.service.GatewayAdminDubboService;
import com.blink.gateway.service.BaseAppRemoteService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.blink.gateway.constant.RedisConstans.*;


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
    private BaseAppRemoteService baseAppRemoteService;

    @Resource
    private GatewayAdminDubboService gatewayAdminDubboService;

    private final HashMap<String, String> localCacheKeyMapping;

    public GateWayCacheComponent() {
        localCacheKeyMapping = new HashMap<>();
        localCacheKeyMapping.put(GATEWAY_CONFIG_KEY_PREFIX, GatewayConstant.CONSISTENT_CACHE);
        localCacheKeyMapping.put(BLINK_CHANNEL_PREFIX, GatewayConstant.CONSISTENT_CACHE);
        localCacheKeyMapping.put(URL_PERMISSION, GatewayConstant.STATICDATA_CACHE);
        localCacheKeyMapping.put(ERR_MSG_PREFIX, GatewayConstant.STATICDATA_CACHE);

    }


    /**
     * 从缓存中获取配置参数
     *
     * @param configKey 配置key
     * @return Mono<SysConfigCacheDO> SysConfigCacheDO对象
     */
    public Mono<SysConfigCacheDO> getGateWayConfigFromCache(String configKey) {

        String cacheKey = GATEWAY_CONFIG_KEY_PREFIX + configKey;

        return multiLevelCacheComponent.get(GatewayConstant.CONSISTENT_CACHE,
                        cacheKey,
                        SysConfigCacheDO.class,
                        (key, clazz) -> callGetChannelConfig(key))
                .onErrorResume(e -> Mono.empty());
    }

    /**
     * 封装调用dubbo获取配置信息
     *
     * @param key 配置key
     * @return Mono<SysConfigCacheDO>
     */
    private Mono<SysConfigCacheDO> callGetChannelConfig(String key) {
        // 去掉前缀
        if (key.contains(GATEWAY_CONFIG_KEY_PREFIX)) {
            key = key.replace(GATEWAY_CONFIG_KEY_PREFIX, "");
        }

        QueryChannelConfigReq req = new QueryChannelConfigReq();
        req.setConfigKey(key);
        RequestDTO<QueryChannelConfigReq> reqDto = new RequestDTO<>();
        reqDto.setBody(req);

        CompletableFuture<ResponseDTO<SysConfigCacheDO>> future = gatewayAdminDubboService.getChannelConfigAsync(reqDto);

        return Mono.fromFuture(future)
                .mapNotNull(response -> {
                    if (response == null) {
                        log.error("获取配置信息失败: response is null");
                        return null;
                    }
                    return response.getBody();
                })
                .doOnError(e -> log.error("获取配置信息失败", e))
                .onErrorResume(e -> Mono.empty());
    }

    /**
     * 从缓存中获取渠道信息
     *
     * @param appKey 渠道appkey
     * @return Mono<ChannelInfoRedisDO>
     */
    public Mono<ChannelInfoRedisDO> getChannelInfoFromCache(String appKey) {

        String cacheKey = BLINK_CHANNEL_PREFIX + appKey;

        return multiLevelCacheComponent.get(GatewayConstant.CONSISTENT_CACHE,
                        cacheKey,
                        ChannelInfoRedisDO.class,
                        (key, clazz) -> callGatewayDubbo(key))
                .onErrorResume(e -> Mono.empty());
    }

    /**
     * 封装调用dubbo 转换返回值
     *
     * @param key 参数
     * @return Mono<ChannelInfoRedisDO>
     */
    private Mono<ChannelInfoRedisDO> callGatewayDubbo(String key) {
        QueryOneChannelReq req = new QueryOneChannelReq();
        req.setAppKey(key);
        RequestDTO<QueryOneChannelReq> reqDto = new RequestDTO<>();
        reqDto.setBody(req);
        CompletableFuture<ResponseDTO<ChannelInfoRedisDO>> future = gatewayAdminDubboService.getChannelInfoAsync(reqDto);

        return Mono.fromFuture(future)
                .mapNotNull(response -> {
                    if (response == null) {
                        log.error("获取渠道信息失败: response is null");
                        return null;
                    }
                    return response.getBody();
                })
                .doOnError(e -> log.error("获取渠道信息失败", e))
                .onErrorResume(e -> Mono.empty());

    }

    /**
     * 从缓存中获取错误码信息表
     *
     * @param errCode 错误码
     * @param local   语言
     * @return 错误消息
     */
    public Mono<String> getErrorMsgInfoFromCache(String errCode, String local) {

        String cacheKey = ERR_MSG_PREFIX + local + ":" + errCode;
        return multiLevelCacheComponent.get(GatewayConstant.STATICDATA_CACHE,
                        cacheKey,
                        String.class,
                        (key, clazz) -> baseAppRemoteService.getErrorMsgInfo(errCode, local).map(QueryErrMsgRsp::getMsgInfo))
                .onErrorResume(e -> Mono.empty());
    }

    /**
     * 多级缓存获取用户权限信息
     *
     * @param userId 用户id
     * @return Set<String> 权限标识集合
     */
    public Mono<QueryUserPermissionRsp> getPermissionsByUserId(Integer userId) {
        String cacheKey = ERR_MSG_PREFIX + userId + ":";
        return multiLevelCacheComponent.get(GatewayConstant.CONSISTENT_CACHE,
                        cacheKey,
                        QueryUserPermissionRsp.class,
                        (key, clazz) -> baseAppRemoteService.getUserPermissionsByUerId(userId))
                .onErrorResume(e -> Mono.empty());

    }

    /**
     * 获取所有接口权限
     *
     * @param requestPath 请求路径
     * @return Mono<QueryErrMsgRspDTO>
     */
    public Mono<String> getPermissionsByRequestPath(String requestPath) {

        String cacheKey = URL_PERMISSION + requestPath;
        return multiLevelCacheComponent.get(GatewayConstant.CONSISTENT_CACHE,
                        cacheKey,
                        String.class,
                        (key, clazz) -> baseAppRemoteService.getUserPermissionsByPath(requestPath).mapNotNull(r -> {
                            Optional<String> optional = r.getPermissions().stream().findFirst();
                            return optional.orElse(null);
                        }))
                .onErrorResume(e -> Mono.empty());

    }

    public HashMap<String, String> getLocalCacheKeyMapping() {
        return localCacheKeyMapping;
    }
}
