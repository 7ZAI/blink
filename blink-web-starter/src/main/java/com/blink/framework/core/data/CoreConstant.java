package com.blink.framework.core.data;

/**
 * redis key常量
 */
public interface CoreConstant {


    /**----------------------------------------------Redis key常量-----------------------------------------------------------**/

    /**
     * 字段约束缓存key
     */
    String FIELD_CONSTRAINT_KEY_PREFIX = "system:field:constraint:";

    /**
     * 消息缓存key
     */
     String MSG_INFO_KEY_PREFIX = "system:err:msg:";

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

    /**
     * CPU密集型线程池bean 名称
     */
    String CPU_THREADPOOL = "cpuIntensiveThreadPool";
    /**
     * IO密集型线程池bean 名称
     */
    String IO_THREADPOOL = "ioIntensiveThreadPool";
    /**
     * 定时线程池bean 名称
     */
    String SCHEDULED_THREADPOOL = "scheduledThreadPool";
}
