package com.blink.gateway.config;

import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.gateway.component.MultiLevelCacheComponent;
import com.blink.gateway.config.prop.BlinkGatewayProperties;
import com.blink.gateway.util.GateWayUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamReceiver;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

import static com.blink.gateway.constant.GatewayConstant.GATEWAY_STREAM_EVENT;

/**
 * redis stream 消息消费相关配置
 *
 * @Author binblink
 */
@Slf4j
@Configuration
public class RedisStreamConsumerConfig {

    @Resource
    private ReactiveRedisConnectionFactory connectionFactory;

    @Resource
    private BlinkGatewayProperties properties;

    @Resource
    private ReactiveRedisClient redisClient;

    @Resource
    private ApplicationEventPublisher publisher;

    @Resource
    private MultiLevelCacheComponent cacheComponent;

    @Value("${blink.gateway.instance-id}")
    private String instanceId;

    @Value("${spring.application.name}")
    private String appName;

    /**
     * 应用启动时自动检查并创建消费者组
     *
     * @PostConstruct
     */
    @PostConstruct
    public void initConsumerGroup() {

    }


    /**
     * 公共的消息监听与连接
     * redis stream event
     *
     * @return Flux
     */
    @Bean
    public Flux<MapRecord<String,String,Object>> commonEventFlux() {

        String streamKey = GATEWAY_STREAM_EVENT;
        String groupName = appName + ":" + instanceId;

        StreamReceiver.StreamReceiverOptions<String, MapRecord<String,String,Object>> options = StreamReceiver.StreamReceiverOptions.builder()
                .batchSize(1)
                .pollTimeout(Duration.ofSeconds(2))
                //下面两个方法的顺序影响 返回类型泛型
                .hashValueSerializer(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .hashKeySerializer(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .onErrorResume(e -> {
                    log.error(e.getMessage());
                    return null;
                }).build();

        StreamReceiver<String, MapRecord<String,String,Object>> streamReceiver = StreamReceiver.create(connectionFactory, options);


        StreamOffset<String> streamOffset = StreamOffset.create(streamKey, ReadOffset.lastConsumed());
        Consumer consumer = Consumer.from(groupName, "event-consumer");

        //检查缓存stream 和消费group 无则创建
        Flux<MapRecord<String,String,Object>> flux = GateWayUtil.createStreamAndGroup(redisClient, streamKey, groupName).flux().flatMap(bool -> {
            if (bool) {
                return streamReceiver.receive(consumer, streamOffset).doOnNext(record -> {
                    log.info("收到来自redis stream {}的事件消息 record：{}", streamKey, record);

                    Map<String,Object> map = record.getValue();
                    String rid = record.getId().getValue();

//                    EventStreamMessage.convertMapToMessage(map,EventStreamMessage.class);
//                    EventStreamMessage eventStreamMessage = (EventStreamMessage) streamMessage;

//                    if(EventType.CACHE_SYNC.equals(eventStreamMessage.getEventType())) {
//                        log.warn("EventType:" + eventStreamMessage.getEventType());
//                    }
//
//                    if(EventType.ROUTE_SYNC.equals(eventStreamMessage.getEventType())) {
//                        log.warn("EventType:" + eventStreamMessage.getEventType());
//                    }

//                    CacheMsgDTO cacheMsgDTO = streamMessage.getPayload();
                    //删除本地缓存
//                    cacheComponent.evictLocalCache(cacheMsgDTO.getKey());
                    //手动ack
//                    redisClient.xAck(streamKey, groupName, rid).subscribe();

                }).onErrorContinue((e, r) -> {

                    log.error("消费同步缓存消息出错！", e);

                });
            }

            return Mono.error(new BlinkException("创建消息stream失败！"));
        });
        return flux;
    }


}
