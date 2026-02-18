package com.blink.gateway.constant;

/**
 * redis常量
 *
 * @author binblink
 */
public interface RedisConstans {


    /**
     * ---------------------------------------------------Redis relate start--------------------------------------------------------
     **/

    String BLINK_PREFIX = "blink:";
    /**
     * 渠道key prefix
     */
    String BLINK_CHANNEL_PREFIX = BLINK_PREFIX + "channel:";

    /**
     * 认证token
     */
    String USER_TOKEN = "user:token:";

    /**
     * 错误信息key 前缀
     */
    String ERR_MSG_PREFIX = "system:err:msg:";

    /**
     * 用户登入信息
     */
    String USER_INFO = "user:info:";
    /**
     * 请求随机数 验证 key
     */
    String REQ_NONCE_PREFIX = "req:nonce:";

    /**
     * 用户权限
     */
    String URL_PERMISSION = "permission:identity:";

    /**
     * 缓存key前缀
     */
    String GATEWAY_CONFIG_KEY_PREFIX = BLINK_PREFIX + "config:gateway:";


    /**
     * 保存路由的key前缀
     */
    String GATEWAY_DYNAMIC_ROUTES = BLINK_PREFIX + "gateway:routes:";

    /**
     *
     */
    String CAHCE_STREAM_GROUP_NAME = "groupLocalCache";


    /**
     * gateway同步 stream key
     */
    String GATEWAY_STREAM_EVENT = BLINK_PREFIX + "stream:gateway:event";


    /**---------------------------------------------------Redis relate end--------------------------------------------------------**/
}
