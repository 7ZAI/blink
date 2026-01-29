package com.blink.gateway.constant;

import java.time.Duration;

/**
 * 常量类
 * @author binblink
 */
public interface GatewayConstant {




    /**
     * 用户信息本地缓存 key
     */
    String CHANNEL_INFO = "channelInfo";

    /**
     * token 失效时间
     */
    Duration TOKEN_TTL = Duration.ofMinutes(30);

    /**
     * token 触发自定续期 时间
     */
    Duration RENEW_THRESHOLD = Duration.ofMinutes(5);


    /**
     * 超级管理员权限
     */
    String SUPER_ADMIN_PERMISSION = "*:**";

    /**
     * 开关 0开启
     */
    Byte SWITCH_ON = 0;

    /**
     * 关闭 1关闭
     */
    Byte SWITCH_OFF = 1;

    /**
     * 登入url
     */
    String LOGIN_PATH = "/base/auth/login";

    /**
     * 定义不同API的最大Content-Length（单位：字节）
     * 10MB
     */
    long MAX_GENERAL = 10 * 1024 * 1024;
    /**
     * 50MB
     */

    long MAX_UPLOAD = 50 * 1024 * 1024;
    /**
     * 2MB
     */

    long MAX_API = 2 * 1024 * 1024;
    /**
     * 长度限制 64
     */
    Integer LENGTH_LIMIT_128 = 128 ;

    Integer LENGTH_LIMIT_1024 = 1024 ;

    /**
     * 长度限制 32
     */
    Integer LENGTH_LIMIT_32 = 32 ;

    /**
     * 请求有效时间 5 分钟 防重放
     */
    long REQ_DEFAULT_EFFECT_TIME = 5 * 60 * 1000;

    /**
     *
     * 随机数失效时间 10 分钟 防重放
     */
    long REQ_NONCE_EXPIRE_TIME = 10 * 60 * 1000;


    String CACHED_REQUEST_BODY_ATTR = "blinkCachedRequestBody";

    String CACHED_ORIGINAL_REQUEST_BODY_BACKUP_ATTR = "cachedOriginalRequestBodyBackup";

    /**
     * 防止重放配置key
     */
    String REQUEST_REPLAY_DEFEND_SWITCH = "request_replay_defend_switch";

    /**
     * 防止重放请求有效时间 超过判定为无效请求
     */
    String REQ_TIMESTAMP_EFFECT_TIME_KEY = "request_replay_defend_effect_time";

    /**
     * 防止重放请求随机数保存时间 key
     */
    String REQ_NONCE_EXPIRE_TIME_KEY = "request_replay_defend_nonce_expire_time";

    /**
     * 签名开关 key
     */
    String SIGNTURE_SWITCH_KEY = "signture_switch";

    String KEY_TIMESTAMP = "timeStamp";

    String KEY_NONCE = "nonce";

    String KEY_LOGINNAME = "loginName";

    String KEY_APPKEY = "appKey";


    String NACOS_GATEWAY_ROUTES_DEFAULT_DATAID = "gateway-routes";

    String NACOS_GATEWAY_ROUTES_DEFAULT_GROUP = "DEFAULT_GROUP";

    /**
     * 当前登入用户信息 认证成功后的用户信息
     * 会写入attribute
     */
    String LOGIN_USER_KEY = "current:userInfo";


    /**
     * spanId 源头00
     */
    String SPAN_ID_ORIGINAL = "00";

    /**
     * 元数据source来源
     */
    String SOURCE_GATEWAY = "gateway";

    /**
     * 默认语言
     */
    String DEFAULT_LANG_CN = "zh_cn";

    /**---------------------------------------------------Redis relate start--------------------------------------------------------**/

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
     *  保存路由的key前缀
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
