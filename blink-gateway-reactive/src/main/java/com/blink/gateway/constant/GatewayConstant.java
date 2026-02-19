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

    Integer LENGTH_LIMIT_4096 = 4096 ;

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
     * 渠道token校验 token 类型 -1 不校验 0jwt 1 固定token
     */
    Byte CHANNEL_NOT_CHECK_TOKEN = -1;

    Byte CHANNEL_CHECK_BY_JWT = 0;

    Byte CHANNEL_CHECK_BY_TOKEN = 1;

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

    /**
     * 请求客户端ip key
     */
    String CLIENT_IP =  "clientIp";

    /**
     * 密钥文件nacos上的dataid
     */
    String SECRET_CONFIG_DATA_ID = "secretConfig.json";

    /**
     * 密钥文件nacos上的 group
     */
    String SECRET_CONFIG_GROUP= "DEFAULT_GROUP";

    /**
     * 本地缓存名称 常规有一致性要求的缓存
     */
    String CONSISTENT_CACHE = "consistentCache";

    /**
     * 静态数据缓存
     */
    String STATICDATA_CACHE = "staticDataCache";

    /**
     *  值经常变动的缓存
     */
    String FREQUENTLY_CHANGED_CACHE = "frequentlyChangedCache";

    /**
     * 渠道认证 jwt
     */
    Byte CHANNEL_AUTH_TYPE_JWT = 1;

    /**
     * 渠道认证 固定token
     */
    Byte CHANNEL_AUTH_TYPE_FIX_TOKEN = 0;


    /**
     * 渠道认证关闭（内部系统）
     */
    Byte CHANNEL_AUTH_TYPE_CLOSE = -1;



}
