package com.blink.framework.redis.mq;

import cn.hutool.core.bean.BeanUtil;
import com.blink.framework.redis.entity.EventType;
import com.blink.framework.redis.entity.MessageType;

import java.util.Map;

/**
 * 事件消息
 * @Author binblink
 */
public class EventStreamMessage<T> extends StreamMessage<T> {

    private EventType eventType;

    public EventStreamMessage(EventType eventType){
        super();
        this.eventType = eventType;
    }

    public EventStreamMessage(EventType eventType,String topic, T payload){
        super(topic,MessageType.EVENT, payload);
        this.eventType = eventType;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    /**
     * 将消息对象转换为Map
     */
    public static Map<String, Object> convertMessageToMap(EventStreamMessage<?> message) {
        return BeanUtil.beanToMap(message, false, false);
    }


    /**
     * 将Map转换为消息对象
     */
    public static <T> EventStreamMessage<T> convertMapToMessage(Map<String, Object> map, Class<T> tClass) {
        // 1. 将map转换为StreamMessage，此时data字段是一个Map
        EventStreamMessage<T> message = BeanUtil.mapToBean(map, EventStreamMessage.class, false);
        // 2. 从map中获取data字段（是一个Map），然后转换为T
        Object dataMap = map.get("payload");
        T data = BeanUtil.toBean(dataMap, tClass);

        // 3. 设置data到message
        message.setPayload(data);

        return message;
    }

    @Override
    public String toString() {
        return "EventStreamMessage{" +
                "eventType=" + eventType +
                '}';
    }
}
