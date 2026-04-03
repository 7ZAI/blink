package com.blink.framework.mq.componet;


import com.blink.framework.common.utils.JacksonUtil;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;

/**
 * 基于 Fastjson2 的 RabbitMQ 消息转换器
 */
public class Fastjson2MessageConverter implements MessageConverter {

    // 默认的 ContentType（标识消息体为 JSON 格式）
    private static final String DEFAULT_CONTENT_TYPE = "application/json";
    private static final String DEFAULT_CHARSET = StandardCharsets.UTF_8.name();

    @Override
    public Message toMessage(Object object, MessageProperties messageProperties) throws MessageConversionException {
        Assert.notNull(object, "待序列化的对象不能为空");
        Assert.notNull(messageProperties, "MessageProperties 不能为空");

        // 1. 设置消息属性：Content-Type + 字符编码
        messageProperties.setContentType(DEFAULT_CONTENT_TYPE);
        messageProperties.setContentEncoding(DEFAULT_CHARSET);

        // 2. Fastjson2 序列化 Java 对象为 JSON 字节数组
        byte[] body;
        try {
            body = JacksonUtil.toJson(object).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new MessageConversionException("Fastjson2 序列化对象失败", e);
        }

        // 3. 构建 RabbitMQ Message 并返回
        return new Message(body, messageProperties);
    }

    @Override
    public Object fromMessage(Message message) throws MessageConversionException {
        Assert.notNull(message, "消息不能为空");
        MessageProperties properties = message.getMessageProperties();
        Assert.notNull(properties, "消息属性不能为空");

        // 1. 校验 ContentType（非必须，可兼容）
        String contentType = properties.getContentType();
        if (contentType == null || !contentType.contains("json")) {
            throw new MessageConversionException("不支持的 ContentType：" + contentType);
        }

        // 2. 获取消息体字节数组并转换为字符串
        String content;
        try {
            String encoding = properties.getContentEncoding() == null ? DEFAULT_CHARSET : properties.getContentEncoding();
            content = new String(message.getBody(), encoding);
        } catch (Exception e) {
            throw new MessageConversionException("解析消息体字节数组失败", e);
        }

        // 3. 从消息属性中获取目标类型（关键：避免反序列化类型丢失）
        String targetClassName = properties.getHeader("target-class");
        if (targetClassName == null) {
            throw new MessageConversionException("消息头缺少 target-class，无法反序列化");
        }

        // 4. Fastjson2 反序列化 JSON 为指定类型对象
        try {
            Class<?> targetClass = Class.forName(targetClassName);
            return JacksonUtil.parseMessyJson(content, targetClass);
        } catch (ClassNotFoundException e) {
            throw new MessageConversionException("找不到目标类型：" + targetClassName, e);
        } catch (Exception e) {
            throw new MessageConversionException("Fastjson2 反序列化失败", e);
        }
    }

    // 辅助方法：发送消息时手动设置目标类型到消息头（生产者用）
    public static void setTargetClassHeader(MessageProperties properties, Class<?> clazz) {
        properties.setHeader("target-class", clazz.getName());
    }
}