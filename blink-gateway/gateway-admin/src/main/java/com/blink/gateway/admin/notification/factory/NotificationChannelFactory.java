package com.blink.gateway.admin.notification.factory;

import com.blink.gateway.admin.notification.channel.NotificationChannel;
import com.blink.gateway.admin.notification.model.ChannelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 通知渠道工厂
 *
 * 管理所有通知渠道实例，提供渠道获取和查询功能
 *
 * @author binblink
 * @since 2026-04-28
 */
@Component
@Slf4j
public class NotificationChannelFactory {

    /**
     * 渠道映射表
     */
    private final Map<ChannelType, NotificationChannel> channelMap = new ConcurrentHashMap<>();

    /**
     * 构造函数，Spring 自动注入所有 NotificationChannel 实现
     *
     * @param channels 所有渠道实现列表
     */
    public NotificationChannelFactory(List<NotificationChannel> channels) {
        if (channels != null) {
            for (NotificationChannel channel : channels) {
                channelMap.put(channel.getChannelType(), channel);
                log.info("[NotificationChannelFactory] 注册渠道: {}", channel.getChannelType());
            }
        }
    }

    /**
     * 获取指定渠道
     *
     * @param type 渠道类型
     * @return 渠道实例，未找到返回null
     */
    public NotificationChannel getChannel(ChannelType type) {
        return channelMap.get(type);
    }

    /**
     * 获取所有可用渠道
     *
     * @return 可用渠道列表
     */
    public List<NotificationChannel> getAvailableChannels() {
        return channelMap.values().stream()
            .filter(NotificationChannel::isAvailable)
            .collect(Collectors.toList());
    }

    /**
     * 获取所有已注册渠道类型
     *
     * @return 渠道类型列表
     */
    public List<ChannelType> getRegisteredChannelTypes() {
        return new ArrayList<>(channelMap.keySet());
    }
}
