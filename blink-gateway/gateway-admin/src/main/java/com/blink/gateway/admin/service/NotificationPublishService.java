package com.blink.gateway.admin.service;

import cn.hutool.core.bean.BeanUtil;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.constants.NotificationSeverityConstant;
import com.blink.gateway.admin.constants.NotificationTypeConstant;
import com.blink.gateway.admin.constants.RedisKeyConstant;
import com.blink.gateway.admin.constants.TargetTypeConstant;
import com.blink.gateway.admin.entity.SysNotificationDO;
import com.blink.gateway.admin.mapper.SysNotificationMapper;
import com.blink.gateway.admin.sse.NotificationMsg;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 消息通知发布服务
 *
 * @author binblink
 * @since 2026-04-06
 */
@Slf4j
@Service
public class NotificationPublishService {

    @Resource
    private SysNotificationMapper notificationMapper;

    @Resource
    private RedisClient redisClient;

    /**
     * 发送全局广播消息
     */
    public void broadcast(String title, String content, String type, String severity, String sourceRef) {
        SysNotificationDO notification = createNotification(
            title, content, type, severity, TargetTypeConstant.ALL, null, sourceRef
        );
        notificationMapper.insert(notification);
        publishToChannel(notification);
        log.info("[Notification] 广播消息已发送, title: {}", title);
    }

    /**
     * 发送定向用户消息
     */
    public void sendToUser(Integer userId, String title, String content, String type, String severity, String sourceRef) {
        SysNotificationDO notification = createNotification(
            title, content, type, severity, TargetTypeConstant.USER, userId, sourceRef
        );
        notificationMapper.insert(notification);
        incrementUnreadCount(userId);
        publishToChannel(notification);
        log.info("[Notification] 定向消息已发送, userId: {}, title: {}", userId, title);
    }

    /**
     * 发送操作成功通知
     */
    public void sendOperationSuccess(Integer userId, String title, String content, String sourceRef) {
        sendToUser(userId, title, content, NotificationTypeConstant.OPERATION, NotificationSeverityConstant.SUCCESS, sourceRef);
    }

    /**
     * 发送操作失败通知
     */
    public void sendOperationError(Integer userId, String title, String content, String sourceRef) {
        sendToUser(userId, title, content, NotificationTypeConstant.OPERATION, NotificationSeverityConstant.ERROR, sourceRef);
    }

    /**
     * 发送告警通知
     */
    public void sendAlert(String title, String content, String severity) {
        broadcast(title, content, NotificationTypeConstant.ALERT, severity, null);
    }

    private SysNotificationDO createNotification(String title, String content, String type, String severity,
                                                   String targetType, Integer targetUserId, String sourceRef) {
        SysNotificationDO notification = new SysNotificationDO();
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setSeverity(severity);
        notification.setTargetType(targetType);
        notification.setTargetUserId(targetUserId);
        notification.setSourceRef(sourceRef);
        notification.setCreatedTime(LocalDateTime.now());

        if (NotificationTypeConstant.ALERT.equals(type)) {
            notification.setExpireTime(LocalDateTime.now().plusHours(24));
        } else {
            notification.setExpireTime(LocalDateTime.now().plusDays(7));
        }

        return notification;
    }

    private void publishToChannel(SysNotificationDO notification) {
        String channel = RedisKeyConstant.NOTIFICATION_CHANNEL;
        NotificationMsg msg = BeanUtil.copyProperties(notification, NotificationMsg.class);
        redisClient.publish(channel, JacksonUtil.toJson(msg));
    }

    private void incrementUnreadCount(Integer userId) {
        String key = RedisKeyConstant.NOTIFICATION_USER_UNREAD + userId;
        redisClient.increment(key);
        redisClient.expire(key, Duration.ofDays(7));
    }
}