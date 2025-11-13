package com.blink.gateway.constant;

import java.time.Duration;

public interface GatewayConstant {


    /**
     * 登入用户id 除了认证url 必填
     * custom request header
     */
    String X_BLINK_USRID = "x-blink-usrId";

    /**
     * 登入用户名 除了认证url 必填
     */
    String X_BLINK_LOGINNAME = "x-blink-loginName";

    /**
     * 来源
     */
    String X_BLINK_SOURCE = "x-blink-source";

    /**
     * 客户端ip地址
     */
    String X_BLINK_CLIENTIP = "x-blink-clientIp";

    /**
     * 请求id 网关填充
     */
    String X_BLINK_REQUEST_ID = "x-blink-requestId";

    /**
     * 跟踪id 网关填充
     */
    String X_BLINK_TRACE_ID = "x-blink-traceId";

    /**
     * 用户token 必填
     */
    String X_BLINK_TOKEN = "x-blink-token";

    /**
     * 应用key 必填
     */
    String X_BLINK_APPKEY = "x-blink-appKey";

    /**
     * key(16位随机数) 加密时必填
     */
    String X_BLINK_KEY = "x-blink-key";

    /**
     * 偏移量 加密时必填
     */
    String X_BLINK_IV = "x-blink-iv";

    /**
     * 语言 可以不填 默认 zh cn
     */
    String X_BLINK_LOCALE = "x-blink-locale";

    /**
     * 时间戳 必填
     */
    String X_BLINK_TIMESTAMP = "x-blink-timestamp";

    /**
     * 随机数 必填
     */
    String X_BLINK_NONCE = "x-blink-nonce";

    /**
     * 签名 必填
     */
    String X_BLINK_SIGN = "x-blink-sign";


    /**
     * spring order 执行顺序最高 值越低越高
     */
    Integer ORDER_LOWEST = Integer.MIN_VALUE;
    /**
     * spring order 执行顺序 第一
     */
    Integer ORDER_LOWEST_ADD_ONE = Integer.MIN_VALUE + 1;

    /**
     * spring order 执行顺序 第二
     */
    Integer ORDER_LOWEST_ADD_TWO = Integer.MIN_VALUE + 2;

    Integer ORDER_LOWEST_ADD_THREE = Integer.MIN_VALUE + 3;

    Integer ORDER_LOWEST_ADD_FOUR = Integer.MIN_VALUE + 4;
    /**
     * spring order 执行顺序最低 末尾 值越低越高
     */
    Integer ORDER_HEIGHEST = Integer.MAX_VALUE - 1;

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
    Integer LENGTH_LIMIT_64 = 64 ;

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
     * 系统config 请求url
     */
    String GET_GATEWAY_CONFIG_URL = "/sysConfig/getOneConfig";

    /**
     * 渠道信息 请求url
     */
    String GET_CHANNEL_URL = "/channel/getChannel";


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
     * 同步缓存stream key
     */
    String REDIS_STREAM_CACHE_KEY = BLINK_PREFIX + "stream:gateway:cache";

    /**
     *  保存路由的key前缀
     */
    String GATEWAY_DYNAMIC_ROUTES = BLINK_PREFIX + "gateway:routes:";

    /**
     *
     */
    String CAHCE_STREAM_GROUP_NAME = "groupLocalCache";


    /**---------------------------------------------------Redis relate end--------------------------------------------------------**/



}
