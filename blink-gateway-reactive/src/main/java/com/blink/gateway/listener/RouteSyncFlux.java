package com.blink.gateway.listener;

import com.blink.base.dto.CacheMsgDTO;
import com.blink.base.dto.RouteSyncMsgDTO;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.framework.redis.mq.StreamMessage;
import com.blink.gateway.component.MultiLevelCacheComponent;
import com.blink.gateway.config.prop.GatewayProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
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
public class RouteSyncFlux {

    @Resource
    private StreamReceiver<String, MapRecord<String,String,Object>> streamReceiver;

    @Resource
    private GatewayProperties properties;

    @Resource
    private ReactiveRedisClient redisClient;

    @Resource
    private ApplicationEventPublisher publisher;

    @PostConstruct
    public void start(){
        receive().subscribe();
    }


    private Flux<MapRecord<String,String,Object>>  receive(){

        String streamKey = properties.getDynamicroute().getRedis().getStreamkey();
        String groupName = properties.getDynamicroute().getRedis().getGroupId();

        StreamOffset<String> streamOffset = StreamOffset.create(streamKey, ReadOffset.lastConsumed());
        Consumer consumer = Consumer.from(groupName, "route-consumer-1");

        Flux<MapRecord<String,String,Object>> flux = streamReceiver.receive(consumer, streamOffset).doOnNext(record -> {
            log.info("收到来自redis stream {}的缓存消息 record：{}", streamKey, record);
            Map<String, Object> map = record.getValue();
            String rid = record.getId().getValue();
            StreamMessage<RouteSyncMsgDTO> streamMessage =  StreamMessage.convertMapToMessage(map, RouteSyncMsgDTO.class);

            RouteSyncMsgDTO routeMsg = streamMessage.getPayload();
            // 发布 刷新路由事件
            publisher.publishEvent(new RefreshRoutesEvent(this));
            //手动ack
            redisClient.xAck(streamKey, groupName, rid).subscribe();
        }).onErrorContinue((e,r)->{

            log.error("消费同步路由消息出错！",e);

        });

        return flux;
    }


}
