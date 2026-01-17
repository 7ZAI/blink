package com.blink.framework.core.data;

/**
 * redis key常量
 */
public interface CoreConstant {


    /**----------------------------------------------Redis key常量-----------------------------------------------------------**/

    /**
     * 数据字典缓存key
     */
     String DICT_KEY_PREFIX = "system:dict:";

    /**
     * 消息缓存key
     */
     String MSG_INFO_KEY_PREFIX = "system:msginfo:";

    String CHANNEL_INFO_KEY_PREFIX = "blink:channel:";


     /**----------------------------------------------请求头常量-----------------------------------------------------------**/

    /**
     * 登入用户id
     *  custom request header
     */
    String X_BLINK_USRID = "x-blink-usrId";

    /**
     * 登入用户名
     */
    String X_BLINK_LOGINNAME = "x-blink-loginName";

    /**
     * 来源
     */
    String X_BLINK_SOURCE = "x-blink-source";

    /**
     * 是不是加密报文 0 否 1是
     */
    String X_BLINK_ENCRYPTED = "x-blink-encrypted";

    /**
     * 客户端ip地址
     */
    String X_BLINK_CLIENTIP = "x-blink-clientIp";

    /**
     * 渠道
     */
    String X_BLINK_CHANNEL = "x-blink-channel";
    /**
     * 请求id
     */
    String X_BLINK_REQUEST_ID = "x-blink-requestId";

    /**
     * 跟踪id
     */
    String X_BLINK_TRACE_ID = "x-blink-traceId";

    /**
     * 用户token
     */
    String X_BLINK_TOKEN = "x-blink-token";

    /**
     * 应用key
     */
    String X_BLINK_APPKEY = "x-blink-appKey";

    /**
     * key(16位随机数)
     */
    String X_BLINK_KEY = "x-blink-key";

    /**
     * 语言
     */
    String X_BLINK_LOCALE = "x-blink-locale";

    /**
     * 偏移量
     */
    String X_BLINK_IV = "x-blink-iv";
    /**
     * 签名
     */
    String X_BLINK_SIGN = "x-blink-sign";




    String APP_NAME_PROPERTY = "spring.application.name";

    String LANG_CN = "zh_cn";

    /**
     * 来自网关
     */
    String SOURCE_GATEWAY = "gateway";
    /**
     * 来自内部调用
     */
    String SOURCE_INTERNAL = "interal";

    Byte SWITCH_ON = 0;

    Byte SWITCH_OFF = 1;

    String BODY_ENCRYPTED = "1";

    String BODY_PLAINTEXT = "0";

    /**
     * 协议版本
     */
    String PROTOCOL_VERSION = "v1";
}
