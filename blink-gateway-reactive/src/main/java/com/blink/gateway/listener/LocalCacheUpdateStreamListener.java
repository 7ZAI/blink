package com.blink.gateway.listener;

import com.blink.gateway.component.MultiLevelCacheComponent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;

import java.util.Map;

/**
 * 同步gateway本地缓存（即删除本地缓存）
 * 同步写法 不符合 Reactive
 *
 * @Author binblink
 */
@Slf4j
@Deprecated
public class LocalCacheUpdateStreamListener implements StreamListener<String, MapRecord<String,String,Object>> {

    private final MultiLevelCacheComponent cacheComponent;

    public LocalCacheUpdateStreamListener(MultiLevelCacheComponent multiLevelCacheComponent){
        this.cacheComponent = multiLevelCacheComponent;
    }

    /**
     * 删除本地缓存
     *
     * @param message 消息.
     */
    @Override
    public void onMessage(MapRecord<String, String, Object> message) {

        log.info("接收到来自redis stream的缓存同步消息！message: {}",message);

        Map<String,Object> map = message.getValue();

//        cacheComponent.evictLocalCache("");
    }
}
