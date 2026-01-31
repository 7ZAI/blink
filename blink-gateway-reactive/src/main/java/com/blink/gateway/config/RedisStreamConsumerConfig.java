package com.blink.gateway.config;

import com.blink.base.dto.CacheMsgDTO;
import com.blink.base.dto.RouteSyncMsgDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.framework.redis.mq.StreamMessage;
import com.blink.gateway.component.MultiLevelCacheComponent;
import com.blink.gateway.config.prop.BlinkGatewayProperties;
import com.blink.gateway.listener.StreamMsgRecord;
import com.blink.gateway.util.GateWayUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamReceiver;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

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
    private ReactiveRedisClient redisClient;

    @Resource
    private ApplicationEventPublisher publisher;

    @Resource
    private MultiLevelCacheComponent cacheComponent;

    @Value("${blink.gateway.instance-id:03}")
    private String instanceId;

    @Value("${spring.application.name}")
    private String appName;

    /**
     * 公共的消息监听与连接
     * redis stream event
     *
     * @return Flux
     */
    @Bean
    public Flux<StreamMsgRecord> commonEventFlux() {

        String streamKey = GATEWAY_STREAM_EVENT;
        String groupName = appName + ":" + instanceId;

        StreamReceiver.StreamReceiverOptions<String, ObjectRecord<String, StreamMessage>> options = StreamReceiver.StreamReceiverOptions.builder()
                .batchSize(1)
                .pollTimeout(Duration.ofSeconds(2))
                .targetType(StreamMessage.class)
                //下面两个方法的顺序影响 返回类型泛型
                .onErrorResume(e -> {
                    log.error("stream error!"+ e.getMessage(),e);
                    return Mono.empty();
                }).build();

        StreamReceiver<String, ObjectRecord<String, StreamMessage>> streamReceiver = StreamReceiver.create(connectionFactory, options);
        //读取最新值
        StreamOffset<String> streamOffset = StreamOffset.create(streamKey, ReadOffset.lastConsumed());

        Consumer consumer = Consumer.from(groupName, "event-consumer");
        //检查缓存stream 和消费group 无则创建
        Flux<StreamMsgRecord> flux = GateWayUtil.createStreamAndGroup(redisClient, streamKey, groupName).flux().flatMap(bool -> {
            if (bool) {
                return streamReceiver.receive(consumer, streamOffset).flatMap(record -> {
                    log.info("收到来自redis stream {}的事件消息 record：{}", streamKey, record);

                    String rid = record.getId().getValue();
                    StreamMessage message = record.getValue();
                    StreamMsgRecord streamMsgRecord = new StreamMsgRecord(rid, streamKey, groupName, message);

                    return handlerEvent(streamMsgRecord).doOnSuccess(result->{
                        //事件处理 失败！
                        if (!result.getHandledResult()) {
                               throw new BlinkException("消费失败");
                        }
                        ack(result).subscribe();
                    });
//                    return Flux.just(streamMsgRecord);

                }).onErrorContinue((e, r) -> {
                    //只记录 不停止监听 如果抛出异常会停止链接
                    log.error("处理消费同步缓存消息出错！", e);

                });
            }
            return Mono.error(new BlinkException("创建消息stream失败！"));
        });

        return flux;
    }

    //获取事件消息后 处理事件
    private Mono<StreamMsgRecord> handlerEvent(StreamMsgRecord smr) {

        StreamMessage message = smr.getStreamMessage();

        //本地缓存同步
        if (message.getPayload() instanceof CacheMsgDTO cacheMsgDTO) {
            //删除本地缓存 手动ack
            return cacheComponent.evictTransactional(cacheMsgDTO.getKey())
                    .flatMap(r -> {
                        smr.setHandledResult(r);
                        return Mono.just(smr);
                    }).switchIfEmpty(Mono.defer(() -> {
                        smr.setHandledResult(false);
                        return Mono.just(smr);
                    }));
        }
        //路由更新同步
        if (message.getPayload() instanceof RouteSyncMsgDTO routeEvent) {
            try {
                //发布事件 更新路由
                publisher.publishEvent(new RefreshRoutesEvent(this));
                smr.setHandledResult(true);
                return Mono.just(smr);
            } catch (Exception e) {
                log.error("路由发布刷新事件失败！" + e.getMessage(), e);
                smr.setHandledResult(false);
                return Mono.just(smr);
            }
        }
        smr.setHandledResult(true);
        //废消息 返回true
        return Mono.just(smr);
    }

    //手动ack
    private Mono<Boolean> ack(StreamMsgRecord smr) {

        return redisClient.xAck(smr.getStreamKey(), smr.getGroupName(), smr.getId()).map(rs -> rs > 0)
                .doOnSuccess(l -> log.info("stream消息消费完成 并以确认 ack结果:{}", l))
                .onErrorContinue((ex, r) -> log.error("处理失败，消息将留在 PEL 中等待重试: {}", ex.getMessage(), ex));
    }

}
