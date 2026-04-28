package com.blink.gateway.admin.notification.channel;

import com.blink.gateway.admin.notification.model.ChannelType;
import com.blink.gateway.admin.notification.model.NotificationMessage;
import com.blink.gateway.admin.notification.model.SendResult;
import lombok.extern.slf4j.Slf4j;

/**
 * 通知渠道抽象基类
 *
 * 提供模板方法模式，处理通用的发送流程：
 * 1. 检查渠道可用性
 * 2. 参数校验
 * 3. 执行发送
 * 4. 异常处理
 *
 * @author binblink
 * @since 2026-04-28
 */
@Slf4j
public abstract class AbstractNotificationChannel implements NotificationChannel {

    @Override
    public SendResult send(NotificationMessage message) {
        // 1. 检查渠道可用性
        if (!isAvailable()) {
            log.warn("[{}] 渠道未配置或不可用", getChannelName());
            return SendResult.unavailable(getChannelType());
        }

        // 2. 参数校验
        SendResult validationResult = validate(message);
        if (!validationResult.isSuccess()) {
            log.warn("[{}] 参数校验失败 | error={}", getChannelName(), validationResult.getErrorMessage());
            return validationResult;
        }

        // 3. 执行发送
        try {
            SendResult result = doSend(message);
            if (result.isSuccess()) {
                log.info("[{}] 通知发送成功 | title={}", getChannelName(), message.getTitle());
            } else {
                log.warn("[{}] 通知发送失败 | error={}", getChannelName(), result.getErrorMessage());
            }
            return result;
        } catch (Exception e) {
            log.error("[{}] 通知发送异常 | title={}", getChannelName(), message.getTitle(), e);
            return SendResult.failure(getChannelType(), e.getMessage());
        }
    }

    /**
     * 参数校验，子类可重写
     *
     * @param message 通知消息
     * @return 校验结果
     */
    protected SendResult validate(NotificationMessage message) {
        return SendResult.success(getChannelType());
    }

    /**
     * 子类实现具体发送逻辑
     *
     * @param message 通知消息
     * @return 发送结果
     */
    protected abstract SendResult doSend(NotificationMessage message);
}
