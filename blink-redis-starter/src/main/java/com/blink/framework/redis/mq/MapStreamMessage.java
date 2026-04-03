package com.blink.framework.redis.mq;

import cn.hutool.core.bean.BeanUtil;
import com.blink.framework.common.utils.JacksonUtil;

import java.util.Map;

/**
 * StreamMessage的Map格式
 * @Author binblink
 */
public class MapStreamMessage<T> extends StreamMessage<T> {

    private Map<String,Object> data;

    public MapStreamMessage(StreamMessage<T> streamMessage) {
        BeanUtil.copyProperties(streamMessage, this);
        this.data = transformToMap(streamMessage);
    }

    public MapStreamMessage() {
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    private Map<String,Object> transformToMap(StreamMessage<T> streamMessage){
        T obj = streamMessage.getPayload();
        Map<String, Object> map = BeanUtil.beanToMap(streamMessage, false, false);
        map.put("payload", JacksonUtil.toJson(obj));
        return map;
    }

}
