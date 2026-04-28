package com.blink.gateway.admin.notification.channel;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.blink.gateway.admin.notification.entity.NotificationChannelConfigDO;
import com.blink.gateway.admin.notification.mapper.NotificationChannelConfigMapper;
import com.blink.gateway.admin.notification.model.ChannelType;
import com.blink.gateway.admin.notification.model.NotificationMessage;
import com.blink.gateway.admin.notification.model.SendResult;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Webhook通知渠道
 *
 * @author binblink
 * @since 2026-04-28
 */
@Component
@Slf4j
public class WebhookNotificationChannel extends AbstractNotificationChannel {

    private final NotificationChannelConfigMapper configMapper;
    private WebhookConfig webhookConfig;

    public WebhookNotificationChannel(NotificationChannelConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    @PostConstruct
    public void init() {
        loadConfig();
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.WEBHOOK;
    }

    @Override
    public boolean isAvailable() {
        return webhookConfig != null && StrUtil.isNotBlank(webhookConfig.getUrl());
    }

    @Override
    protected SendResult doSend(NotificationMessage message) {
        try {
            // 构建请求体
            Map<String, Object> payload = new HashMap<>();
            payload.put("title", message.getTitle());
            payload.put("content", message.getContent());
            payload.put("type", message.getNotificationType());
            payload.put("severity", message.getSeverity());
            payload.put("timestamp", LocalDateTime.now().toString());
            payload.put("businessId", message.getBusinessId());
            if (message.getExtra() != null) {
                payload.put("extra", message.getExtra());
            }

            // 发送HTTP请求
            HttpRequest request = "POST".equalsIgnoreCase(webhookConfig.getMethod())
                ? HttpRequest.post(webhookConfig.getUrl())
                : HttpRequest.get(webhookConfig.getUrl());

            // 设置请求头
            if (webhookConfig.getHeaders() != null) {
                for (Map.Entry<String, String> entry : webhookConfig.getHeaders().entrySet()) {
                    request.header(entry.getKey(), entry.getValue());
                }
            }

            request.timeout(webhookConfig.getTimeout() != null ? webhookConfig.getTimeout() : 5000);
            request.body(JSONUtil.toJsonStr(payload), "application/json");

            HttpResponse response = request.execute();

            if (response.isOk()) {
                log.info("[WebhookChannel] 通知发送成功 | url={}, status={}",
                    webhookConfig.getUrl(), response.getStatus());
                return SendResult.success(getChannelType());
            } else {
                log.warn("[WebhookChannel] 通知发送失败 | url={}, status={}",
                    webhookConfig.getUrl(), response.getStatus());
                return SendResult.failure(getChannelType(),
                    "HTTP状态码: " + response.getStatus());
            }
        } catch (Exception e) {
            log.error("[WebhookChannel] 通知发送异常", e);
            return SendResult.failure(getChannelType(), e.getMessage());
        }
    }

    /**
     * 从数据库加载配置
     */
    private void loadConfig() {
        NotificationChannelConfigDO config = configMapper.selectByChannelType("webhook");
        if (config == null || config.getEnabled() == null || config.getEnabled() != 1) {
            log.info("[WebhookChannel] Webhook渠道未配置或未启用");
            return;
        }

        try {
            webhookConfig = JSONUtil.toBean(config.getConfigJson(), WebhookConfig.class);
            log.info("[WebhookChannel] Webhook渠道初始化成功 | url={}", webhookConfig.getUrl());
        } catch (Exception e) {
            log.error("[WebhookChannel] Webhook配置解析失败", e);
        }
    }

    /**
     * Webhook配置
     */
    @Data
    public static class WebhookConfig {
        private String url;
        private String method;
        private Map<String, String> headers;
        private Integer timeout;
        private Integer retryTimes;
    }
}
