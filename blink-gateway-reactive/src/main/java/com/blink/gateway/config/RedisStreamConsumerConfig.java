package com.blink.gateway.config;

import com.blink.base.dto.CacheMsgDTO;
import com.blink.base.dto.RouteSyncMsgDTO;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.framework.redis.mq.StreamMessage;
import com.blink.gateway.component.MultiLevelCacheComponent;
import com.blink.gateway.config.prop.BlinkGatewayProperties;
import com.blink.gateway.util.GateWayUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamReceiver;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;

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

    /**
     * 应用启动时自动检查并创建消费者组
     * @PostConstruct
     */
    @PostConstruct
    public void initConsumerGroup() {

    }

    /**
     * 公共的消息监听与连接
     *
     * @return StreamReceiver
     */
    @Bean
    public StreamReceiver<String, MapRecord<String, String, Object>> streamReceiver() {

        String streamKey = properties.getCache().getStreamKey();
        String groupName = properties.getCache().getStreamGroupName();


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

        StreamReceiver<String, MapRecord<String, String, Object>> streamReceiver = StreamReceiver.create(connectionFactory, options);

        return streamReceiver;
    }

    /**
     * 本地缓存同步
     *
     * @param streamReceiver
     * @return
     */
    @Bean
    @ConditionalOnProperty(prefix = "blink.gateway.dynamicroute",
            name = "mode",
            havingValue = "nacos")
    public Flux<MapRecord<String,String,Object>>  localCacheFlux(StreamReceiver<String, MapRecord<String, String, Object>> streamReceiver){

        String streamKey = properties.getCache().getStreamKey();
        String groupName = properties.getCache().getStreamGroupName();

        //检查缓存stream 和消费group 无则创建
        GateWayUtil.createStreamAndGroup(redisClient, streamKey, groupName).subscribe(
                result -> log.info("Stream：{} 和 group：{} 已存在！", streamKey, groupName),
                error -> log.error("创建Stream：{} 和 group：{} 失败！{}", streamKey, groupName, error.getMessage())
        );

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

    /**
     * 动态路由 Redis模式下 路由消息修改同步
     * @param streamReceiver
     * @return
     */
    @Bean
    @ConditionalOnProperty(prefix = "blink.gateway.dynamicroute",
            name = "mode",
            havingValue = "redis")
    public Flux<MapRecord<String,String,Object>> redisRouteFlux(StreamReceiver<String, MapRecord<String, String, Object>> streamReceiver){

        String streamKey = properties.getDynamicroute().getRedis().getStreamkey();
        String groupName = properties.getDynamicroute().getRedis().getGroupId();

        GateWayUtil.createStreamAndGroup(redisClient, streamKey, groupName).subscribe(
                result -> log.info("Stream：{} 和 group：{} 已存在！", streamKey, groupName),
                error -> log.error("创建Stream：{} 和 group：{} 失败！{}", streamKey, groupName, error.getMessage())
        );

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




/**
 * 同步写法 不符合 reactive
 * 配置redis stream 监听容器
 * 一个Container 可以注册绑定多个 stream
 * @param redisConnectionFactory
 * @return
 */
//    @Bean
//    public StreamMessageListenerContainer<String, MapRecord<String, Object, Object>> streamMessageListenerContainer(
//            RedisConnectionFactory redisConnectionFactory) {
//
//        // 创建容器配置选项
//        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, Object, Object>> options =
//                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
//                        // 一次性最多拉取多少条消息
//                        .batchSize(10)
//                        // 超时时间
//                        .pollTimeout(Duration.ofSeconds(2))
//                        .errorHandler(errorHandler())
//                        .hashKeySerializer(new StringRedisSerializer())
//                        .hashValueSerializer(new GenericJackson2JsonRedisSerializer())
////                        .executor()
//                        .build();
//
//        // 创建监听容器
//        StreamMessageListenerContainer<String, MapRecord<String, Object, Object>> container =
//                StreamMessageListenerContainer.create(redisConnectionFactory, options);
//
//        return container;
//    }

/**
 * redis stream 异常处理
 * @return
 */
//    @Bean
//    public ErrorHandler errorHandler(){
//
//        ErrorHandler errorHandler = new ErrorHandler() {
//            @Override
//            public void handleError(Throwable t) {
//                log.error("Redis Stream StreamMessageListenerContainer error!",t);
//            }
//        };
//
//        return errorHandler;
//    }

/**
 * 配置监听 同步阻塞消费
 *
 * @param container
 * @param cacheComponent
 * @return
 */
//    @Bean
//    public StreamListener localCacheUpdateStreamListener(StreamMessageListenerContainer container,MultiLevelCacheComponent cacheComponent){
//
//        Consumer consumer = Consumer.from("groupLocalCache", "gateway_consumer");
//        // 启动前先创建组
////        createConsumerGroupIfNotExists();
//
//
//        StreamMessageListenerContainer.StreamReadRequest<String> streamRequest =
//                StreamMessageListenerContainer.StreamReadRequest
//                        .builder(StreamOffset.create("blink:stream:gateway:cache", ReadOffset.lastConsumed()))
//                        .consumer(consumer)
//                        .autoAcknowledge(true) // 手动确认
//                        .build();
//
//        //自定义的监听对象
//        LocalCacheUpdateStreamListener cacheUpdateStreamListener = new LocalCacheUpdateStreamListener(cacheComponent);
//        container.register(streamRequest, cacheUpdateStreamListener);
//
//        return cacheUpdateStreamListener;
//    }
}
