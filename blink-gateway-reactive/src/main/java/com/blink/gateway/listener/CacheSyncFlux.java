package com.blink.gateway.listener;

import com.blink.base.dto.CacheMsgDTO;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.framework.redis.mq.StreamMessage;
import com.blink.gateway.component.MultiLevelCacheComponent;
import com.blink.gateway.config.prop.GatewayProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamReceiver;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * @Author binblink
 * @Date 2025/11/11
 */
@Slf4j
@Component
public class CacheSyncFlux {

    @Resource
    private StreamReceiver<String, MapRecord<String,String,Object>> streamReceiver;

    @Resource
    private GatewayProperties properties;

    @Resource
    private ReactiveRedisClient redisClient;

    @Resource
    private MultiLevelCacheComponent cacheComponent;

    @PostConstruct
    public void start(){
        receive().subscribe();
    }


    private Flux<MapRecord<String,String,Object>>  receive(){

        String streamKey = properties.getCache().getStreamKey();
        String groupName = properties.getCache().getStreamGroupName();

        StreamOffset<String> streamOffset = StreamOffset.create(streamKey, ReadOffset.lastConsumed());
        Consumer consumer = Consumer.from(groupName, "cache-consumer-1");

        Flux<MapRecord<String,String,Object>> flux = streamReceiver.receive(consumer, streamOffset).doOnNext(record -> {
            log.info("收到来自redis stream {}的缓存消息 record：{}", streamKey, record);
            Map<String, Object> map = record.getValue();
            String rid = record.getId().getValue();
            StreamMessage<CacheMsgDTO> streamMessage =  StreamMessage.convertMapToMessage(map, CacheMsgDTO.class);

            CacheMsgDTO cacheMsgDTO = streamMessage.getPayload();
            //删除本地缓存
            cacheComponent.evictLocalCache(cacheMsgDTO.getKey());
            //手动ack
            redisClient.xAck(streamKey, groupName, rid).subscribe();
        }).onErrorContinue((e,r)->{

            log.error("消费同步缓存消息出错！",e);

        });

        return flux;
    }


}
