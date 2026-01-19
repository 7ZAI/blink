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
