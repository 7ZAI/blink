package com.blink.gateway.component;


import com.blink.base.dto.rsp.QueryErrMsgRsp;
import com.blink.base.dto.rsp.QueryUserPermissionRsp;
import com.blink.framework.common.data.ChannelInfoRedisDO;
import com.blink.framework.common.data.SysConfigCacheDO;
import com.blink.gateway.constant.GatewayConstant;
import com.blink.gateway.service.BaseAppService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

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
    private BaseAppService baseAppService;


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
                        (key, clazz) -> baseAppService.getOneConfig(key))
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
                        (key, clazz) -> baseAppService.getChannelInfo(key))
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
        //使用静态的缓存对象
        return multiLevelCacheComponent.get(GatewayConstant.STATICDATA_CACHE,
                        cacheKey,
                        String.class,
                        (key, clazz) -> baseAppService.getErrorMsgInfo(errCode, local).map(QueryErrMsgRsp::getMsgInfo))
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
                        (key, clazz) -> baseAppService.getUserPermissionsByUerId(userId))
                .onErrorResume(e -> Mono.empty());

    }

    /**
     * 获取所有接口权限
     * @param requestPath 请求路径
     * @return Mono<QueryErrMsgRspDTO>
     */
    public Mono<String> getPermissionsByRequestPath(String requestPath) {

        String cacheKey = URL_PERMISSION + requestPath;
        return multiLevelCacheComponent.get(GatewayConstant.CONSISTENT_CACHE,
                        cacheKey,
                        String.class,
                        (key, clazz) -> baseAppService.getUserPermissionsByPath(requestPath).mapNotNull(r->{
                            Optional<String> optional = r.getPermissions().stream().findFirst();
                            return optional.orElse(null);
                        }))
                .onErrorResume(e -> Mono.empty());

    }




}
