package com.blink.gateway.admin.notification.dispatcher;

import com.blink.gateway.admin.notification.channel.NotificationChannel;
import com.blink.gateway.admin.notification.factory.NotificationChannelFactory;
import com.blink.gateway.admin.notification.handler.NotificationFailureHandler;
import com.blink.gateway.admin.notification.model.ChannelType;
import com.blink.gateway.admin.notification.model.NotificationMessage;
import com.blink.gateway.admin.notification.model.SendResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 通知统一分发器
 *
 * @author binblink
 * @since 2026-04-28
 */
@Component
@Slf4j
public class NotificationDispatcher {

    private final NotificationChannelFactory channelFactory;
    private final NotificationFailureHandler failureHandler;

    public NotificationDispatcher(NotificationChannelFactory channelFactory,
                                   NotificationFailureHandler failureHandler) {
        this.channelFactory = channelFactory;
        this.failureHandler = failureHandler;
    }

    /**
     * 同步发送通知到指定渠道
     *
     * @param message     通知消息
     * @param channelType 渠道类型
     * @return 发送结果
     */
    public SendResult dispatch(NotificationMessage message, ChannelType channelType) {
        NotificationChannel channel = channelFactory.getChannel(channelType);
        if (channel == null) {
            log.warn("[NotificationDispatcher] 渠道未注册: {}", channelType);
            SendResult result = SendResult.failure(channelType, "渠道未注册");
            failureHandler.handleFailure(message, result);
            return result;
        }

        SendResult result = channel.send(message);
        if (!result.isSuccess()) {
            failureHandler.handleFailure(message, result);
        }
        return result;
    }

    /**
     * 同步发送通知到多个渠道
     *
     * @param message      通知消息
     * @param channelTypes 渠道类型列表
     * @return 发送结果列表
     */
    public List<SendResult> dispatch(NotificationMessage message, List<ChannelType> channelTypes) {
        List<SendResult> results = new ArrayList<>();
        for (ChannelType type : channelTypes) {
            results.add(dispatch(message, type));
        }
        return results;
    }

    /**
     * 异步发送通知到指定渠道
     *
     * @param message     通知消息
     * @param channelType 渠道类型
     */
    @Async("notificationExecutor")
    public void dispatchAsync(NotificationMessage message, ChannelType channelType) {
        dispatch(message, channelType);
    }

    /**
     * 异步发送通知到多个渠道
     *
     * @param message      通知消息
     * @param channelTypes 渠道类型列表
     */
    @Async("notificationExecutor")
    public void dispatchAsync(NotificationMessage message, List<ChannelType> channelTypes) {
        dispatch(message, channelTypes);
    }
}
