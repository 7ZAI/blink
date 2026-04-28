package com.blink.gateway.admin.notification.channel;

import com.blink.gateway.admin.notification.model.ChannelType;
import com.blink.gateway.admin.notification.model.NotificationMessage;
import com.blink.gateway.admin.notification.model.SendResult;

/**
 * 通知渠道接口（顶层抽象）
 *
 * @author binblink
 * @since 2026-04-28
 */
public interface NotificationChannel {

    /**
     * 发送通知
     *
     * @param message 通知消息
     * @return 发送结果
     */
    SendResult send(NotificationMessage message);

    /**
     * 获取渠道类型
     *
     * @return 渠道类型
     */
    ChannelType getChannelType();

    /**
     * 渠道是否可用（检查配置）
     *
     * @return true-可用，false-不可用
     */
    boolean isAvailable();

    /**
     * 获取渠道名称（用于日志和展示）
     *
     * @return 渠道名称
     */
    default String getChannelName() {
        return getChannelType().getName();
    }
}
