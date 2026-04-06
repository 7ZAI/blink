package com.blink.gateway.constant;

import com.blink.framework.common.constrant.RedisCacheKeyConstant;

/**
 * Redis 常量
 * 使用 RedisCacheKeyConstant 共享定义
 *
 * @author binblink
 */
public interface RedisConstans {

    /**
     * 渠道 key prefix
     */
    String BLINK_CHANNEL_PREFIX = RedisCacheKeyConstant.CHANNEL_CACHE_PREFIX;

    /**
     * 认证 token
     */
    String USER_TOKEN = "user:token:";

    /**
     * 被踢下线的旧 token 标记前缀
     */
    String USER_TOKEN_OLD = "user:token:old:";

    /**
     * 用户多设备会话 ZSet key 前缀
     */
    String USER_TOKENS = "user:tokens:";

    /**
     * 错误信息 key 前缀
     */
    String ERR_MSG_PREFIX = RedisCacheKeyConstant.ERR_MSG_PREFIX;

    /**
     * 用户登入信息
     */
    String USER_INFO = "user:info:";

    /**
     * 请求随机数验证 key
     */
    String REQ_NONCE_PREFIX = "req:nonce:";

    /**
     * 用户权限
     */
    String URL_PERMISSION = RedisCacheKeyConstant.URL_PERMISSION_PREFIX;

    /**
     * 缓存 key 前缀
     */
    String GATEWAY_CONFIG_KEY_PREFIX = RedisCacheKeyConstant.GATEWAY_CONFIG_PREFIX;

    /**
     * 保存路由的 key 前缀
     */
    String GATEWAY_DYNAMIC_ROUTES = RedisCacheKeyConstant.GATEWAY_DYNAMIC_ROUTES_PREFIX;

    /**
     * 缓存 Stream Group 名称
     */
    String CAHCE_STREAM_GROUP_NAME = "groupLocalCache";

    /**
     * Gateway 同步 Stream Key
     */
    String GATEWAY_STREAM_EVENT = RedisCacheKeyConstant.GATEWAY_STREAM_EVENT;
}