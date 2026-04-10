package com.blink.gateway.listener;

import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.framework.redis.mq.StreamMessage;
import com.blink.gateway.component.GateWayCacheComponent;
import com.blink.gateway.component.MultiLevelCacheComponent;
import com.blink.gateway.config.prop.BlinkGatewayProperties;
import com.blink.gateway.constant.GatewayConstant;
import com.blink.gateway.dto.CacheMsg;
import com.blink.gateway.dto.req.MessageAckReq;
import com.blink.gateway.dto.RouteSyncMsg;
import com.blink.gateway.dubbo.service.GatewayAdminDubboService;
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
import java.util.List;
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

    @Resource
    private GatewayAdminDubboService gatewayAdminDubboService;

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
        // 每个实例使用独立的消费者组，实现广播模式（所有实例都收到消息）
        String groupName = appName + ":" + instanceId;
        String consumerName = "event-consumer";

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

        Consumer consumer = Consumer.from(groupName, consumerName);
        //检查缓存stream 和消费group 无则创建
        //stream已经成功创建
        //事件处理 失败！
        //只记录 不停止监听 如果抛出异常会停止链接
        //stream不存在并创建失败

        // PEL 空闲时间阈值：超过 1 分钟的历史消息将被清理
        long pelIdleThresholdMs = 60_000L;

        return GateWayUtil.createStreamAndGroup(redisClient, streamKey, groupName)
                // 先清理 PEL 中的历史消息，避免重复消费
                .flatMap(bool -> GateWayUtil.cleanupPel(redisClient, streamKey, groupName, consumerName, pelIdleThresholdMs)
                        .map(count -> bool))
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
                // 检查推送模式
                String pushMode = routeEvent.getPushMode();
                if ("specified".equals(pushMode)) {
                    // 指定实例模式：检查当前实例是否在目标列表中
                    String currentInstanceId = appName + ":" + instanceId;
                    List<String> targetInstanceIds = routeEvent.getTargetInstanceIds();
                    if (targetInstanceIds == null || !targetInstanceIds.contains(currentInstanceId)) {
                        log.info("[RouteSync] 跳过同步，当前实例不在目标列表中 | instanceId: {}, targetIds: {}",
                                currentInstanceId, targetInstanceIds);
                        smr.setHandledResult(true);
                        return Mono.just(smr);
                    }
                    log.info("[RouteSync] 指定实例推送，当前实例在目标列表中 | instanceId: {}", currentInstanceId);
                }

                // 发布事件更新路由
                publisher.publishEvent(new RefreshRoutesEvent(this));
                smr.setHandledResult(true);
                log.info("[RouteSync] 路由刷新事件已发布 | storageMode: {}, pushMode: {}",
                        routeEvent.getStorageMode(), pushMode);
                return Mono.just(smr);
            } catch (Exception e) {
                log.error("[RouteSync] 路由发布刷新事件失败 | error: {}", e.getMessage(), e);
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
                .doOnSuccess(redisAcked -> {
                    log.info("stream消息消费 ack结果:{}", redisAcked);

                    // Redis Stream ACK 成功后，异步调用 Dubbo ACK 反馈消费结果
                    // 不阻塞主流程，失败仅记录日志
                    notifyAckResult(smr, true, null);
                })
                .onErrorResume(ex -> {
                    log.error("处理失败，消息将留在 PEL 中等待重试: {}", ex.getMessage(), ex);
                    // Redis ACK 失败，也通知 gateway-admin 消费失败
                    notifyAckResult(smr, false, ex.getMessage());
                    return Mono.just(false);
                });
    }

    /**
     * 异步通知消费结果给 gateway-admin
     * 不阻塞主流程，失败仅记录日志
     *
     * @param smr     消息记录
     * @param success 是否成功
     * @param errorMsg 错误信息
     */
    private void notifyAckResult(StreamMsgRecord smr, boolean success, String errorMsg) {
        try {
            MessageAckReq ackReq = new MessageAckReq();
            ackReq.setStreamId(smr.getId());
            ackReq.setMsgId(smr.getStreamMessage().getMsgId());
            ackReq.setSuccess(success);
            ackReq.setConsumer(appName + ":" + instanceId);
            ackReq.setErrorMsg(errorMsg);

            // 从 CacheMsg 中获取操作人信息
            Object payload = smr.getStreamMessage().getPayload();
            if (payload instanceof CacheMsg cacheMsg) {
                ackReq.setOperatorUser(cacheMsg.getOperatorUser());
                ackReq.setOperatorName(cacheMsg.getOperatorName());
            }

            RequestDTO<MessageAckReq> reqDto = new RequestDTO<>();
            reqDto.setBody(ackReq);

            // 异步调用 Dubbo ACK，不阻塞主流程
            gatewayAdminDubboService.ackMessageAsync(reqDto)
                    .whenComplete((rsp, ex) -> {
                        if (ex != null) {
                            log.warn("[DubboACK] 异步通知失败 | streamId: {}, error: {}", smr.getId(), ex.getMessage());
                        } else if (rsp != null && rsp.getBody() != null) {
                            log.debug("[DubboACK] 异步通知成功 | streamId: {}, acked: {}", smr.getId(), rsp.getBody().getAcked());
                        }
                    });
        } catch (Exception e) {
            log.warn("[DubboACK] 构造 ACK 请求失败 | streamId: {}, error: {}", smr.getId(), e.getMessage());
        }
    }

    /**
     * 缓存同步事件处理
     * 支持版本号检查，防止消息乱序
     *
     * @param cacheMsg 缓存消息
     * @return 成功与否
     */
    private Mono<Boolean> cacheMsgHandler(CacheMsg cacheMsg) {

        String operator = cacheMsg.getOperator();
        String key = cacheMsg.getKey();
        Integer incomingVersion = cacheMsg.getVersion();

        // 获取 key 前缀和缓存名称
        // 提取格式: "blink:channel:appKey" -> "blink:channel:"
        int lastColonIndex = key.lastIndexOf(":");
        String keyPrefix = lastColonIndex > 0 ? key.substring(0, lastColonIndex + 1) : key;
        String cacheName = gateWayCacheComponent.getLocalCacheKeyMapping().get(keyPrefix);

        // cacheName 为空时，无法操作本地缓存，记录警告并跳过本地缓存同步
        if (cacheName == null) {
            log.warn("[CacheSync] 未找到缓存映射 | keyPrefix: {}, key: {}, 跳过本地缓存同步", keyPrefix, key);
            return Mono.just(true);
        }

        // 删除缓存（同时删除本地和 Redis）
        if (GatewayConstant.CACHE_OPERATOR_DELETE.equals(operator)) {
            return cacheComponent.evictTransactional(cacheName, key)
                    .doOnSuccess(r -> log.info("[CacheSync] 删除缓存成功（本地+Redis）| key: {}", key));
        }

        // 新增或修改缓存（同时更新本地和 Redis）
        if (GatewayConstant.CACHE_OPERATOR_ADD.equals(operator) || GatewayConstant.CACHE_OPERATOR_MODIFY.equals(operator)) {
            // 版本号检查：如果消息带有版本号，检查是否为更新的版本
            if (incomingVersion != null && incomingVersion > 0) {
                return checkVersionAndUpdate(cacheName, key, cacheMsg.getValue(), incomingVersion);
            }
            // 无版本号，同时更新本地和 Redis 缓存
            return cacheComponent.setLocalAndRedisCache(cacheName, key, cacheMsg.getValue())
                    .doOnSuccess(r -> log.info("[CacheSync] 更新缓存成功（本地+Redis）| key: {}, operator: {}", key, operator));
        }

        log.warn("[CacheSync] 未知的操作类型 | key: {}, operator: {}", key, operator);
        return Mono.just(true);
    }

    /**
     * 检查版本号并更新缓存
     * 使用 Redis 存储版本号，防止消息乱序
     * 同时更新本地缓存和 Redis 缓存
     *
     * @param cacheName        缓存名称
     * @param key              缓存 key
     * @param value            缓存值
     * @param incomingVersion  消息版本号
     * @return 是否更新成功
     */
    private Mono<Boolean> checkVersionAndUpdate(String cacheName, String key, Object value, Integer incomingVersion) {
        String versionKey = key + ":version";

        return redisClient.get(versionKey)
                .mapNotNull(currentVersion -> {
                    try {
                        return Integer.parseInt(currentVersion.toString());
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .defaultIfEmpty(0)
                .flatMap(currentVersion -> {
                    // 版本号检查：如果消息版本号小于等于当前版本，忽略
                    if (incomingVersion <= currentVersion) {
                        log.warn("[CacheSync] 忽略过期消息 | key: {}, currentVersion: {}, incomingVersion: {}",
                                key, currentVersion, incomingVersion);
                        return Mono.just(true);
                    }

                    // 同时更新本地缓存和 Redis 缓存
                    return cacheComponent.setLocalAndRedisCache(cacheName, key, value)
                            .flatMap(success -> {
                                if (success) {
                                    return redisClient.set(versionKey, incomingVersion)
                                            .map(v -> {
                                                log.info("[CacheSync] 更新缓存成功（本地+Redis）| key: {}, version: {} -> {}",
                                                        key, currentVersion, incomingVersion);
                                                return true;
                                            });
                                }
                                return Mono.just(false);
                            });
                });
    }

    // TODO 定期扫描 PEL 重新投递或者放入死信队列


}
