package com.blink.gateway.admin.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知发送结果
 *
 * @author binblink
 * @since 2026-04-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 渠道类型
     */
    private ChannelType channelType;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 第三方返回的消息ID
     */
    private String externalMessageId;

    /**
     * 创建成功结果
     *
     * @param channelType 渠道类型
     * @return 成功结果
     */
    public static SendResult success(ChannelType channelType) {
        return SendResult.builder()
            .success(true)
            .channelType(channelType)
            .build();
    }

    /**
     * 创建失败结果
     *
     * @param channelType  渠道类型
     * @param errorMessage 错误信息
     * @return 失败结果
     */
    public static SendResult failure(ChannelType channelType, String errorMessage) {
        return SendResult.builder()
            .success(false)
            .channelType(channelType)
            .errorMessage(errorMessage)
            .build();
    }

    /**
     * 创建带错误码的失败结果
     *
     * @param channelType  渠道类型
     * @param errorCode    错误码
     * @param errorMessage 错误信息
     * @return 失败结果
     */
    public static SendResult failure(ChannelType channelType, String errorCode, String errorMessage) {
        return SendResult.builder()
            .success(false)
            .channelType(channelType)
            .errorCode(errorCode)
            .errorMessage(errorMessage)
            .build();
    }

    /**
     * 创建不可用结果
     *
     * @param channelType 渠道类型
     * @return 不可用结果
     */
    public static SendResult unavailable(ChannelType channelType) {
        return SendResult.builder()
            .success(false)
            .channelType(channelType)
            .errorCode("CHANNEL_UNAVAILABLE")
            .errorMessage("渠道未配置或不可用")
            .build();
    }
}
