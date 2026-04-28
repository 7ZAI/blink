package com.blink.gateway.admin.notification.handler;

import com.blink.framework.common.utils.JacksonUtil;
import com.blink.gateway.admin.notification.entity.NotificationFailureLogDO;
import com.blink.gateway.admin.notification.mapper.NotificationFailureLogMapper;
import com.blink.gateway.admin.notification.model.NotificationMessage;
import com.blink.gateway.admin.notification.model.SendResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 通知发送失败处理器
 *
 * @author binblink
 * @since 2026-04-28
 */
@Component
@Slf4j
public class NotificationFailureHandler {

    private final NotificationFailureLogMapper failureLogMapper;

    public NotificationFailureHandler(NotificationFailureLogMapper failureLogMapper) {
        this.failureLogMapper = failureLogMapper;
    }

    /**
     * 处理发送失败的通知，持久化到数据库
     *
     * @param message 通知消息
     * @param result  发送结果
     */
    public void handleFailure(NotificationMessage message, SendResult result) {
        log.warn("[NotificationFailure] 通知发送失败 | channel={}, error={}",
            result.getChannelType().getName(), result.getErrorMessage());

        NotificationFailureLogDO failureLog = new NotificationFailureLogDO();
        failureLog.setChannelType(result.getChannelType().getCode());
        failureLog.setNotificationType(message.getNotificationType());
        failureLog.setBusinessId(message.getBusinessId());
        failureLog.setTitle(message.getTitle());
        failureLog.setContent(message.getContent());
        failureLog.setRecipients(message.getRecipients() != null ? JacksonUtil.toJson(message.getRecipients()) : null);
        failureLog.setErrorCode(result.getErrorCode());
        failureLog.setErrorMessage(result.getErrorMessage());
        failureLog.setRetryCount(0);
        failureLog.setStatus((byte) 0); // 待重试
        failureLog.setCreateTime(LocalDateTime.now());
        failureLog.setUpdateTime(LocalDateTime.now());

        failureLogMapper.insert(failureLog);
    }
}
