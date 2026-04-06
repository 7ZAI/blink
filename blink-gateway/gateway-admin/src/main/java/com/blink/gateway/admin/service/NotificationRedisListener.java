package com.blink.gateway.admin.service;

import com.blink.framework.common.utils.JacksonUtil;
import com.blink.gateway.admin.constants.RedisKeyConstant;
import com.blink.gateway.admin.constants.TargetTypeConstant;
import com.blink.gateway.admin.sse.NotificationMsg;
import com.blink.gateway.admin.sse.SseConnectionPool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

/**
 * Redis消息监听器
 * 监听通知频道，接收消息后推送给SSE连接
 *
 * @author binblink
 * @since 2026-04-06
 */
@Slf4j
@Service
public class NotificationRedisListener implements MessageListener {

    @Resource
    private SseConnectionPool sseConnectionPool;

    @Resource
    private RedisMessageListenerContainer redisMessageListenerContainer;

    /**
     * 应用启动后自动注册监听器到Redis频道
     */
    @EventListener(ApplicationReadyEvent.class)
    public void register() {
        String channel = RedisKeyConstant.NOTIFICATION_CHANNEL;
        ChannelTopic topic = new ChannelTopic(channel);
        redisMessageListenerContainer.addMessageListener(this, topic);
        log.info("[NotificationListener] 已订阅Redis频道: {}", channel);
    }

    /**
     * 处理接收到的Redis消息
     *
     * @param message 消息内容
     * @param pattern 匹配模式（对于channel订阅，pattern为null）
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String messageBody = new String(message.getBody());
            NotificationMsg msg = JacksonUtil.fromJson(messageBody, NotificationMsg.class);
            handleNotification(msg);
        } catch (Exception e) {
            log.error("[NotificationListener] 解析消息失败: {}", new String(message.getBody()), e);
        }
    }

    /**
     * 处理通知消息，根据目标类型分发
     *
     * @param msg 通知消息
     */
    private void handleNotification(NotificationMsg msg) {
        log.info("[NotificationListener] 收到消息, notificationId: {}, targetType: {}",
            msg.getNotificationId(), msg.getTargetType());

        if (TargetTypeConstant.ALL.equals(msg.getTargetType())) {
            // 全局广播
            sseConnectionPool.broadcast(msg);
        } else if (TargetTypeConstant.USER.equals(msg.getTargetType())) {
            // 指定用户推送
            Integer targetUserId = msg.getTargetUserId();
            if (targetUserId != null) {
                sseConnectionPool.sendToUser(targetUserId, msg);
            }
        }
    }
}