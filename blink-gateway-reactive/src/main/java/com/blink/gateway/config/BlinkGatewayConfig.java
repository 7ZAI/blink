package com.blink.gateway.config;


import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.framework.redis.id.ReactiveIdGenerator;
import com.blink.gateway.component.GateWayCacheComponent;
import com.blink.gateway.component.MultiLevelCacheComponent;
import com.blink.gateway.filter.*;
import com.blink.gateway.listener.LocalCacheUpdateStreamListener;
import com.blink.gateway.listener.RouteUpdateStreamListener;
import com.blink.gateway.service.BaseAppService;
import com.blink.gateway.signature.SignatureServiceFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.util.ErrorHandler;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * filter 执行顺序
 * <p>
 * RequestValidateFilter(合法性校验) --> SignatureFilter(签名) --> RequestBodyTransformFliter（转换报文）
 * <p>
 * <p>
 *
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




    /**
     * 全局异常处理
     * 全局异常GlobalExceptionHandlerFilter
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
        return new SignatureFilter(redisClient,signatureServiceFactory,cacheComponent);
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
     * 配置redis stream 监听容器
     * 一个Container 可以注册绑定多个 stream
     * @param redisConnectionFactory
     * @return
     */
    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, Object, Object>> streamMessageListenerContainer(
            RedisConnectionFactory redisConnectionFactory) {

        // 创建容器配置选项
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, Object, Object>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        // 一次性最多拉取多少条消息
                        .batchSize(10)
                        // 超时时间
                        .pollTimeout(Duration.ofSeconds(2))
                        .errorHandler(errorHandler())
                        .hashKeySerializer(new StringRedisSerializer())
                        .hashValueSerializer(new GenericJackson2JsonRedisSerializer())
//                        .executor()
                        .build();

        // 创建监听容器
        StreamMessageListenerContainer<String, MapRecord<String, Object, Object>> container =
                StreamMessageListenerContainer.create(redisConnectionFactory, options);

        return container;
    }

    /**
     * redis stream 异常处理
     * @return
     */
    @Bean
    public ErrorHandler errorHandler(){

        ErrorHandler errorHandler = new ErrorHandler() {
            @Override
            public void handleError(Throwable t) {
                log.error("Redis Stream StreamMessageListenerContainer error!",t);
            }
        };

        return errorHandler;
    }

    /**
     * 配置监听
     *
     * @param container
     * @param cacheComponent
     * @return
     */
    @Bean
    public StreamListener localCacheUpdateStreamListener(StreamMessageListenerContainer container,MultiLevelCacheComponent cacheComponent){

        StreamMessageListenerContainer.StreamReadRequest<String> streamRequest =
                StreamMessageListenerContainer.StreamReadRequest
                        .builder(StreamOffset.create("blink:stream:gateway:cache", ReadOffset.lastConsumed()))
                        .consumer(Consumer.from("groupLocalCache", "gateway_consumer"))
                        .autoAcknowledge(true) // 手动确认
                        .build();

        //自定义的监听对象
        LocalCacheUpdateStreamListener cacheUpdateStreamListener = new LocalCacheUpdateStreamListener(cacheComponent);
        container.register(streamRequest, cacheUpdateStreamListener);

        return cacheUpdateStreamListener;
    }

}
