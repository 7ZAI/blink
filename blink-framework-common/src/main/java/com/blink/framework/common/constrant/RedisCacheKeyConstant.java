package com.blink.framework.common.constrant;

/**
 * Redis 缓存 Key 常量
 * 用于 gateway-admin 和 gateway-reactive 共享缓存 Key 定义
 *
 * @author binblink
 */
public interface RedisCacheKeyConstant {

    /**
     * blink 前缀
     */
    String BLINK_PREFIX = "blink:";

    /**
     * 渠道信息缓存 Key 前缀
     * 完整格式: blink:channel:{appKey}
     */
    String CHANNEL_CACHE_PREFIX = BLINK_PREFIX + "channel:";

    /**
     * 网关配置缓存 Key 前缀
     * 完整格式: blink:config:gateway:{configKey}
     */
    String GATEWAY_CONFIG_PREFIX = BLINK_PREFIX + "config:gateway:";

    /**
     * 用户权限缓存 Key 前缀
     * 完整格式: permission:identity:{path}
     */
    String URL_PERMISSION_PREFIX = "permission:identity:";

    /**
     * 错误信息缓存 Key 前缀
     * 完整格式: system:err:msg:{locale}:{errCode}
     */
    String ERR_MSG_PREFIX = "system:err:msg:";

    /**
     * 用户 Token 缓存 Key 前缀
     * 完整格式: user:token:{loginName}
     */
    String USER_TOKEN_PREFIX = "user:token:";

    /**
     * 用户信息缓存 Key 前缀
     * 完整格式: user:info:{userId}
     */
    String USER_INFO_PREFIX = "user:info:";

    /**
     * 网关动态路由缓存 Key 前缀
     * 完整格式: blink:gateway:routes:{routeId}
     */
    String GATEWAY_DYNAMIC_ROUTES_PREFIX = BLINK_PREFIX + "gateway:routes:";

    /**
     * Gateway 同步 Stream Key
     * 用于缓存变更通知
     */
    String GATEWAY_STREAM_EVENT = BLINK_PREFIX + "stream:gateway:event";
}