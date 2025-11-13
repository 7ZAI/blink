package com.blink.gateway.config;


import com.blink.base.dto.CacheMsgDTO;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.framework.redis.id.ReactiveIdGenerator;
import com.blink.framework.redis.mq.StreamMessage;
import com.blink.gateway.component.GateWayCacheComponent;
import com.blink.gateway.config.prop.GatewayProperties;
import com.blink.gateway.filter.*;
import com.blink.gateway.signature.SignatureServiceFactory;
import com.blink.gateway.util.GateWayUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamReceiver;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;

/**
 * filter 执行顺序
 * <p>
 * RequestValidateFilter(合法性校验) --> SignatureFilter(签名) --> RequestBodyTransformFliter（转换报文）
 * <p>
 * <p>
 */
@Slf4j
@Configuration
public class BlinkGatewayConfig {

    @Autowired
    private ReactiveRedisClient redisClient;

    @Autowired
    private ReactiveIdGenerator reactiveIdGenerator;

    @Autowired
    private GateWayCacheComponent cacheComponent;

    @Autowired
    private SignatureServiceFactory signatureServiceFactory;

    @Autowired
    private ReactiveRedisConnectionFactory connectionFactory;

    @Resource
    private GatewayProperties properties;


    /**
     * 全局异常处理
     * 全局异常GlobalExceptionHandlerFilter
     *
     * @return
     */
    @Bean
    @Primary
    public GlobalExceptionHandlerFilter globalExceptionHandlerFilter() {
        return new GlobalExceptionHandlerFilter();
    }


    @Bean
    public RequestValidateFilter requestValidateFilter() {
        return new RequestValidateFilter(cacheComponent);
    }

    @Bean
    public SignatureFilter signatureFilter() {
        return new SignatureFilter(redisClient, signatureServiceFactory, cacheComponent);
    }

    @Bean
    public CryptFilter cryptFilter() {
        return new CryptFilter(signatureServiceFactory);
    }

    @Bean
    public RewriteRequestBodyFilter rewriteRequestBodyFilter() {
        return new RewriteRequestBodyFilter(reactiveIdGenerator);
    }

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
                //顺序影响 返回类型泛型
                .hashValueSerializer(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .hashKeySerializer(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .onErrorResume(e -> {
                    log.error(e.getMessage());
                    return null;
                }).build();

        StreamReceiver<String, MapRecord<String, String, Object>> streamReceiver = StreamReceiver.create(connectionFactory, options);
        //检查缓存stream 和消费group 无则创建
        GateWayUtil.createStreamAndGroup(redisClient, streamKey, groupName).subscribe(
                result -> log.info("Stream：{} 和 group：{} 已存在！", streamKey, groupName),
                error -> log.error("创建Stream：{} 和 group：{} 失败！{}", streamKey, groupName, error.getMessage())
        );
        return streamReceiver;
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
