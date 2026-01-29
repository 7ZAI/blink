package com.blink.framework.redis.mq;

import cn.hutool.core.bean.BeanUtil;
import com.blink.framework.common.mq.BlinkMessage;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * stream通用消息类
 *
 * @author binblink
 */
public class StreamMessage<T> implements Serializable, BlinkMessage {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    private String msgId;

    /**
     * 消息主题/Stream名称
     */
    private String topic;

    /**
     * 消息类型 N-通知 B-业务
     */
    private String msgType;

    /**
     * 消息状态 未读 0 已读 1 已消费 2
     */
    private String msgStatus = "0";

    /**
     * 消息体
     */
    private T payload;

    /**
     * 消息体类的全限定名 T
     */
    private String payloadClass;

    /**
     * 消息创建时间
     */
    private LocalDateTime createTime;

    /**
     * 消息版本
     */
    private String version = "1.0";

    /**
     * 消息发送方
     */
    private String sender;


    /**
     * 消息发送方
     */
    private String receiver;

    /**
     * 扩展字段
     */
    private Object extra;

    public String getMsgId() {
        return msgId;
    }

    public StreamMessage<T> setMsgId(String msgId) {
        this.msgId = msgId;
        return this;
    }

    public String getTopic() {
        return topic;
    }

    public StreamMessage<T> setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public String getPayloadClass() {
        return payloadClass;
    }

    public StreamMessage<T> setPayloadClass(String payloadClass) {
        this.payloadClass = payloadClass;
        return this;
    }

    public String getMsgType() {
        return msgType;
    }

    public StreamMessage<T> setMsgType(String msgType) {
        this.msgType = msgType;
        return this;
    }

    public String getMsgStatus() {
        return msgStatus;
    }

    public StreamMessage<T> setMsgStatus(String msgStatus) {
        this.msgStatus = msgStatus;
        return this;
    }

    public T getPayload() {
        return payload;
    }

    public StreamMessage<T> setPayload(T payload) {
        this.payload = payload;
        return this;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public StreamMessage<T> setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public StreamMessage<T> setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getSender() {
        return sender;
    }

    public StreamMessage<T> setSender(String sender) {
        this.sender = sender;
        return this;
    }

    public String getReceiver() {
        return receiver;
    }

    public StreamMessage<T> setReceiver(String receiver) {
        this.receiver = receiver;
        return this;
    }

    public Object getExtra() {
        return extra;
    }

    public StreamMessage<T> setExtra(Object extra) {
        this.extra = extra;
        return this;
    }

    @Override
    public String toString() {
        return "StreamMessage{" +
                "msgId='" + msgId + '\'' +
                ", topic='" + topic + '\'' +
                ", msgType='" + msgType + '\'' +
                ", msgStatus='" + msgStatus + '\'' +
                ", payload=" + payload +
                ", payloadClass='" + payloadClass + '\'' +
                ", createTime=" + createTime +
                ", version='" + version + '\'' +
                ", sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", extra=" + extra +
                '}';
    }

    /**
     * 构造函数
     */
    public StreamMessage() {
        this.msgId = UUID.randomUUID().toString();
        this.createTime = LocalDateTime.now();
    }

    public StreamMessage(String topic, String msgType, T payload) {
        this();
        this.topic = topic;
        this.msgType = msgType;
        this.payload = payload;
    }

    /**
     * 创建消息的便捷方法
     */
    public static <T> StreamMessage<T> of(String topic, String messageType, T payload) {
        return new StreamMessage<>(topic, messageType, payload);
    }

    /**
     * 创建消息的便捷方法 全部常规属性
     */
    public static <T> StreamMessage<T> of(String topic, String messageType, T payload, String sender, String receiver) {
        StreamMessage<T> message = new StreamMessage<>(topic, messageType, payload);
        message.setSender(sender);
        message.setReceiver(receiver);
        return message;
    }

    /**
     * 创建消息的便捷方法（带扩展字段）
     */
    public static <T> StreamMessage<T> of(String topic, String messageType, T payload, Object extra) {
        StreamMessage<T> message = new StreamMessage<>(topic, messageType, payload);
        message.setExtra(extra);
        return message;
    }

    /**
     * 将消息对象转换为Map
     */
    public static Map<String, Object> convertMessageToMap(StreamMessage<?> message) {
        return BeanUtil.beanToMap(message, false, false);
    }


    /**
     * 将Map转换为消息对象
     */
    public static <T> StreamMessage<T> convertMapToMessage(Map<String, Object> map, Class<T> tClass) {
        // 1. 将map转换为StreamMessage，此时data字段是一个Map
        StreamMessage<T> message = BeanUtil.mapToBean(map, StreamMessage.class, false);
        // 2. 从map中获取data字段（是一个Map），然后转换为T
        Object dataMap = map.get("payload");
        T data = BeanUtil.toBean(dataMap, tClass);

        // 3. 设置data到message
        message.setPayload(data);

        return message;
    }


}