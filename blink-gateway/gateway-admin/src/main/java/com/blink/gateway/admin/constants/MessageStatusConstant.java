package com.blink.gateway.admin.constants;

/**
 * 消息状态常量
 *
 * @author binblink
 */
public interface MessageStatusConstant {

    /**
     * REDIS 消息状态码 未读
     */
    String REDIS_MSG_STATUS_UNREADED = "0";

    /**
     * REDIS 消息状态码 已读
     */
    String REDIS_MSG_STATUS_READED = "1";

    /**
     * REDIS 消息状态码 发送失败
     */
    String REDIS_MSG_STATUS_SEND_FAILED = "2";

    /**
     * REDIS 消息状态码 确认消费
     */
    String REDIS_MSG_STATUS_ACK = "3";
}