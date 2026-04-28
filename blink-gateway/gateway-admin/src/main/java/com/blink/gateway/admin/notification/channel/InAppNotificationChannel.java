package com.blink.gateway.admin.notification.channel;

import cn.hutool.core.util.StrUtil;
import com.blink.gateway.admin.service.NotificationPublishService;
import com.blink.gateway.admin.notification.model.ChannelType;
import com.blink.gateway.admin.notification.model.NotificationMessage;
import com.blink.gateway.admin.notification.model.SendResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 站内通知渠道
 *
 * 整合现有的 NotificationPublishService，是系统中始终可用的默认通知渠道。
 *
 * @author binblink
 * @since 2026-04-28
 */
@Component
@Slf4j
public class InAppNotificationChannel extends AbstractNotificationChannel {

    private final NotificationPublishService notificationPublishService;

    public InAppNotificationChannel(NotificationPublishService notificationPublishService) {
        this.notificationPublishService = notificationPublishService;
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.IN_APP;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    protected SendResult doSend(NotificationMessage message) {
        try {
            String severity = StrUtil.isNotBlank(message.getSeverity())
                ? message.getSeverity()
                : "INFO";

            notificationPublishService.sendAlert(
                message.getTitle(),
                message.getContent(),
                severity
            );

            return SendResult.success(getChannelType());
        } catch (Exception e) {
            log.error("[InAppChannel] 站内通知发送失败", e);
            return SendResult.failure(getChannelType(), e.getMessage());
        }
    }
}
