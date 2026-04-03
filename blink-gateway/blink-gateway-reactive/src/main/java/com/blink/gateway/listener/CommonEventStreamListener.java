package com.blink.gateway.listener;

import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.framework.redis.mq.StreamMessage;
import com.blink.gateway.component.GateWayCacheComponent;
import com.blink.gateway.component.MultiLevelCacheComponent;
import com.blink.gateway.config.prop.BlinkGatewayProperties;
import com.blink.gateway.constant.GatewayConstant;
import com.blink.gateway.dto.CacheMsg;
import com.blink.gateway.dto.RouteSyncMsg;
import com.blink.gateway.event.EnableStreamEvent;
import com.blink.gateway.util.GateWayUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamReceiver;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;

import static com.blink.gateway.constant.RedisConstans.GATEWAY_STREAM_EVENT;

/**
 * @Author binblink
 */
@Slf4j
@Component
public class CommonEventStreamListener implements CommandLineRunner {

    @Resource
    private BlinkGatewayProperties properties;

    @Resource
    private ReactiveRedisConnectionFactory connectionFactory;

    @Resource
    private ReactiveRedisClient redisClient;

    @Resource
    private ApplicationEventPublisher publisher;

    @Resource
    private MultiLevelCacheComponent cacheComponent;

    @Resource
    private GateWayCacheComponent gateWayCacheComponent;

    @Value("${blink.gateway.instance-id:01}")
    private String instanceId;

    @Value("${spring.application.name}")
    private String appName;

    private Disposable disposable;

    private static Boolean initialized = false;

    private Flux<StreamMsgRecord> commonEventFlux;

    /**
     * 启动完成后
     *
     * @param args
     * @throws Exception
     */
    @Override
    public void run(String... args) throws Exception {
        // 默认关闭监听stream缓存同步消息
        if (properties.getEventStreamEnable()) {
            this.start();
        }
        CommonEventStreamListener.initialized = true;
    }

    /**
     * 初始化
     */
    @PostConstruct
    public void startCheck() {
        this.commonEventFlux = commonEventFlux();
    }

    @PreDestroy
    public void destroy() {
        // 取消订阅
        this.stop();
    }


    private void start() {

        log.info("<=== 开启 redis stream 连接，进行消息监听与消费");
        //开启 stream 消费
        this.disposable = commonEventFlux.subscribe();

    }

    private void stop() {
        log.info("===> 关闭 redis stream 连接 ！终止stream消息消费");
        if (Objects.nonNull(this.disposable)) {
            this.disposable.dispose();
        }
    }


    /**
     * 监听配置项变动事件 运行时动态开关 redis stream消费
     *
     * @param event 变动事件
     */
    @EventListener
    public void handleConfigChange(EnableStreamEvent event) {

        log.info("eventStreamEnable 配置项变动 值：{}", event.getNewValue());

        if (!initialized) {
            log.debug("应用启动 首次变动 不做任何处理！");
            return;
        }
        if (event.getNewValue()) {
            start();
        } else {
            stop();
        }

    }


    /**
     * 公共的消息监听与连接
     * redis stream event
     *
     * @return Flux
     */
    private Flux<StreamMsgRecord> commonEventFlux() {

        String streamKey = GATEWAY_STREAM_EVENT;
        String groupName = appName + ":" + instanceId;

        StreamReceiver.StreamReceiverOptions<String, ObjectRecord<String, StreamMessage>> options =
                StreamReceiver.StreamReceiverOptions.builder()
                        .batchSize(1)
                        .pollTimeout(Duration.ofSeconds(2))
                        .targetType(StreamMessage.class)
                        //下面两个方法的顺序影响 返回类型泛型
                        .onErrorResume(e -> {
                            log.error("stream error!{}", e.getMessage(), e);
                            return Mono.empty();
                        }).build();

        StreamReceiver<String, ObjectRecord<String, StreamMessage>> streamReceiver = StreamReceiver.create(connectionFactory, options);
        //读取最新值
        StreamOffset<String> streamOffset = StreamOffset.create(streamKey, ReadOffset.lastConsumed());

        Consumer consumer = Consumer.from(groupName, "event-consumer");
        //检查缓存stream 和消费group 无则创建
        //stream已经成功创建
        //事件处理 失败！
        //只记录 不停止监听 如果抛出异常会停止链接
        //stream不存在并创建失败

        return GateWayUtil.createStreamAndGroup(redisClient, streamKey, groupName)
                .flux()
                .flatMap(bool -> {
                    //stream已经成功创建
                    if (bool) {
                        return streamReceiver.receive(consumer, streamOffset)
                                .flatMap(record -> {
                                    log.info("收到来自redis stream {}的事件消息 record：{}", streamKey, record);

                                    String rid = record.getId().getValue();
                                    StreamMessage<?> message = record.getValue();
                                    StreamMsgRecord streamMsgRecord = new StreamMsgRecord(rid, streamKey, groupName, message);

                                    return handlerEvent(streamMsgRecord).flatMap(result -> {
                                        //事件处理 失败！
                                        if (!result.getHandledResult()) {
                                            return Mono.error(new BlinkException("消费失败"));
                                        }
                                        return ack(result).map(r -> streamMsgRecord);
                                    });

                                }).onErrorContinue((e, r) -> {
                                    //只记录 不停止监听 如果抛出异常会停止链接
                                    log.error("处理消费同步缓存消息出错！", e);

                                });
                    }
                    //stream不存在并创建失败
                    return Mono.error(new BlinkException("创建消息stream失败！"));
                });

    }

    //获取事件消息后 处理事件
    private Mono<StreamMsgRecord> handlerEvent(StreamMsgRecord smr) {

        StreamMessage<?> message = smr.getStreamMessage();

        //本地缓存同步
        if (message.getPayload() instanceof CacheMsg cacheMsg) {
            //删除本地缓存 手动ack
            return cacheMsgHandler(cacheMsg)
                    .flatMap(r -> {
                        smr.setHandledResult(r);
                        return Mono.just(smr);
                    }).switchIfEmpty(Mono.defer(() -> {
                        smr.setHandledResult(false);
                        return Mono.just(smr);
                    }));
        }
        //路由更新同步
        if (message.getPayload() instanceof RouteSyncMsg routeEvent) {
            try {
                //发布事件 更新路由
                publisher.publishEvent(new RefreshRoutesEvent(this));
                smr.setHandledResult(true);
                return Mono.just(smr);
            } catch (Exception e) {
                log.error("路由发布刷新事件失败！{}", e.getMessage(), e);
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
                .doOnSuccess(l -> log.info("stream消息消费 ack结果:{}", l))
                .onErrorContinue((ex, r) -> log.error("处理失败，消息将留在 PEL 中等待重试: {}", ex.getMessage(), ex));
    }

    /**
     * 缓存同步事件处理
     *
     * @param cacheMsg 缓存消息
     * @return 成功与否
     */
    private Mono<Boolean> cacheMsgHandler(CacheMsg cacheMsg) {

        String operator = cacheMsg.getOperator();
        String key = cacheMsg.getKey();
        String keyPrefix = key.substring(0, key.lastIndexOf(":"));
        String cacheName = gateWayCacheComponent.getLocalCacheKeyMapping().get(keyPrefix);

        //删除缓存 重建缓存
        if (GatewayConstant.CACHE_OPERATOR_DELETE.equals(operator)) {
            return cacheComponent.evictLocalCache(cacheName, key);
        }

        //直接更新缓存
        if (GatewayConstant.CACHE_OPERATOR_ADD.equals(operator) || GatewayConstant.CACHE_OPERATOR_MODIFY.equals(operator)) {
            return cacheComponent.setLocalCache(cacheName, key,cacheMsg.getValue());
        }

        return Mono.just(true);
    }

    //TODO 定期扫描pel重新投递或者放入死信队列


}
