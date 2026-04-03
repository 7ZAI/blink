package com.blink.base.constants;

/**
 * @author binblink
 */
public interface RedisKeyConstans {


    String BASE_APP = "base-app:";

    String USER_TOKEN = "user:token:";

    String USER_TOKEN_OLD = "user:token:old:";

    /**
     * 用户多设备会话 ZSet key前缀
     * 用于存储用户所有活跃会话的token及登录时间
     * 完整key: user:tokens:{loginName}
     * value: token, score: 登录时间戳
     */
    String USER_TOKENS = "user:tokens:";

    String USER_INFO = "user:info:";

    String BLINK_PREFIX = "blink:";

    String CHANNEL_INFO = BLINK_PREFIX + "channel:";

    String URL_PERMISSION =  "permission:identity:";

    String GATEWAY_CONFIG_PREFIX = BLINK_PREFIX + "config:gateway:";

    String GATEWAY_DYNAMIC_ROUTES = BLINK_PREFIX +"gateway:routes";

    /**
     * gateway同步 stream key
     */
    String GATEWAY_STREAM_EVENT = BLINK_PREFIX + "stream:gateway:event";


    String CHANNEL_PERMISSION = BLINK_PREFIX + "permission:";

    /**
     * 系统配置 配置缓存key
     */
    String SYSTEM_CONFIG = BLINK_PREFIX + "base:system:config";

    /**
     * 数据范围权限缓存 key前缀
     * 完整key: blink:data_scope:role:{roleId}
     */
    String DATA_SCOPE_ROLE = BLINK_PREFIX + "data_scope:role:";

    /**
     * 用户数据范围权限缓存 key前缀
     * 完整key: blink:data_scope:user:{userId}
     * 缓存 UserDataScopeInfo 对象
     */
    String DATA_SCOPE_USER = BLINK_PREFIX + "data_scope:user:";

}
