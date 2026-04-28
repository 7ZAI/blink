package com.blink.gateway.listener;

import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.framework.redis.mq.StreamMessage;
import com.blink.gateway.component.GateWayCacheComponent;
import com.blink.gateway.component.GatewayInstanceStateManager;
import com.blink.gateway.component.MultiLevelCacheComponent;
import com.blink.gateway.component.ChannelConfigCache;
import com.blink.gateway.config.prop.BlinkGatewayProperties;
import com.blink.gateway.constant.GatewayConstant;
import com.blink.gateway.dto.CacheMsg;
import com.blink.gateway.dto.ChannelNacosRefreshMsg;
import com.blink.gateway.dto.InstanceOfflineMsg;
import com.blink.gateway.dto.InstanceOnlineMsg;
import com.blink.gateway.dto.MonitorConfigMsg;
import com.blink.gateway.dto.req.MessageAckReq;
import com.blink.gateway.dto.RouteSyncMsg;
import com.blink.gateway.dubbo.service.GatewayAdminDubboService;
import com.blink.gateway.event.EnableStreamEvent;
import com.blink.gateway.monitor.MonitorConfigHolder;
import com.blink.gateway.monitor.MetricsReportScheduler;
import com.blink.gateway.scheduler.PendingMessageScheduler;
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

    @Resource
    private MonitorConfigHolder monitorConfigHolder;

    @Resource
    private MetricsReportScheduler metricsReportScheduler;

    @Resource
    private GatewayInstanceStateManager instanceStateManager;

    @Resource
    private ChannelConfigCache channelConfigCache;

    @Resource
    private PendingMessageScheduler pendingMessageScheduler;

    @Value("${blink.gateway.instance-id:01}")
    private String instanceId;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${server.port}")
    private String serverPort;

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
                                            // 消费失败，移入死信队列
                                            log.warn("[Stream] 消费失败，移入死信队列 | messageId: {}", rid);
                                            return pendingMessageScheduler.moveToDeadLetterQueue(
                                                    streamKey, groupName, rid, message,
                                                    "消费处理失败", appName + ":" + instanceId)
                                                    .then(Mono.just(streamMsgRecord));
                                        }
                                        return ack(result).map(r -> streamMsgRecord);
                                    });

                                }).onErrorContinue((e, r) -> {
                                    //捕获异常，移入死信队列，不停止监听
                                    log.error("[Stream] 处理消费同步缓存消息出错！", e);
                                    if (r instanceof StreamMsgRecord smr) {
                                        String failedMsgId = smr.getId();
                                        StreamMessage<?> failedMsg = smr.getStreamMessage();
                                        pendingMessageScheduler.moveToDeadLetterQueue(
                                                smr.getStreamKey(), smr.getGroupName(), failedMsgId, failedMsg,
                                                e.getMessage(), appName + ":" + instanceId)
                                                .subscribe();
                                    }
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
                    List<String> targetInstanceIds = routeEvent.getTargetInstanceIds();
                    if (targetInstanceIds == null || targetInstanceIds.isEmpty()) {
                        log.warn("[RouteSync] 指定实例推送模式，但目标实例列表为空");
                        smr.setHandledResult(true);
                        return Mono.just(smr);
                    }

                    // 构造当前实例的多种标识格式进行匹配
                    // 格式1: appName:instanceId (例如 gateway-app:00)
                    // 格式2: host:port (例如 192.168.1.100:8002)
                    String currentAppInstanceId = appName + ":" + instanceId;
                    // 获取本机 IP 地址
                    String localHost = GateWayUtil.getLocalIp();
                    String currentHostPort = localHost + ":" + serverPort;

                    boolean matched = targetInstanceIds.contains(currentAppInstanceId)
                            || targetInstanceIds.contains(currentHostPort);

                    if (!matched) {
                        log.info("[RouteSync] 跳过同步，当前实例不在目标列表中 | appInstanceId: {}, hostPort: {}, targetIds: {}",
                                currentAppInstanceId, currentHostPort, targetInstanceIds);
                        smr.setHandledResult(true);
                        return Mono.just(smr);
                    }
                    log.info("[RouteSync] 指定实例推送，当前实例在目标列表中 | appInstanceId: {}, hostPort: {}",
                            currentAppInstanceId, currentHostPort);
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
        // 监控配置同步
        if (message.getPayload() instanceof MonitorConfigMsg configMsg) {
            try {
                monitorConfigHandler(configMsg);
                smr.setHandledResult(true);
                log.info("[MonitorConfigSync] 监控配置同步成功 | configKey: {}, configValue: {}",
                        configMsg.getConfigKey(), configMsg.getConfigValue());
                return Mono.just(smr);
            } catch (Exception e) {
                log.error("[MonitorConfigSync] 监控配置同步失败 | error: {}", e.getMessage(), e);
                smr.setHandledResult(false);
                return Mono.just(smr);
            }
        }
        // 实例下线指令
        if (message.getPayload() instanceof InstanceOfflineMsg offlineMsg) {
            try {
                return instanceOfflineHandler(offlineMsg)
                        .doOnSuccess(result -> {
                            smr.setHandledResult(result);
                            if (result) {
                                log.info("[InstanceOffline] 实例下线指令处理成功 | target: {}, type: {}",
                                        offlineMsg.getTargetInstance(), offlineMsg.getOfflineType());
                            }
                        })
                        .onErrorResume(e -> {
                            log.error("[InstanceOffline] 实例下线指令处理失败 | error: {}", e.getMessage(), e);
                            smr.setHandledResult(false);
                            return Mono.just(false);
                        })
                        .map(r -> smr);
            } catch (Exception e) {
                log.error("[InstanceOffline] 实例下线指令处理异常 | error: {}", e.getMessage(), e);
                smr.setHandledResult(false);
                return Mono.just(smr);
            }
        }
        // 实例上线指令
        if (message.getPayload() instanceof InstanceOnlineMsg onlineMsg) {
            try {
                boolean result = instanceOnlineHandler(onlineMsg);
                smr.setHandledResult(result);
                if (result) {
                    log.info("[InstanceOnline] 实例上线指令处理成功 | target: {}", onlineMsg.getTargetInstance());
                }
                return Mono.just(smr);
            } catch (Exception e) {
                log.error("[InstanceOnline] 实例上线指令处理失败 | error: {}", e.getMessage(), e);
                smr.setHandledResult(false);
                return Mono.just(smr);
            }
        }
        // 渠道配置刷新
        if (message.getPayload() instanceof ChannelNacosRefreshMsg refreshMsg) {
            try {
                channelNacosRefreshHandler(refreshMsg);
                smr.setHandledResult(true);
                log.info("[ChannelConfigSync] 渠道配置刷新成功 | type: {}, appKey: {}",
                        refreshMsg.getRefreshType(), refreshMsg.getAppKey());
                return Mono.just(smr);
            } catch (Exception e) {
                log.error("[ChannelConfigSync] 渠道配置刷新失败 | error: {}", e.getMessage(), e);
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

    /**
     * 监控配置同步处理
     * 处理 gateway-admin 推送的监控配置变更
     *
     * @param configMsg 监控配置消息
     */
    private void monitorConfigHandler(MonitorConfigMsg configMsg) {
        String configKey = configMsg.getConfigKey();
        String configValue = configMsg.getConfigValue();

        log.info("[MonitorConfigSync] 收到配置同步 | key: {}, value: {}", configKey, configValue);

        // 监控开关配置
        if ("monitor.enabled".equals(configKey)) {
            boolean enabled = Boolean.parseBoolean(configValue);
            boolean oldEnabled = monitorConfigHolder.isEnabled();
            monitorConfigHolder.setEnabled(enabled);

            // 如果开关状态变化，需要重启调度
            if (oldEnabled != enabled) {
                if (enabled) {
                    metricsReportScheduler.restart();
                    log.info("[MonitorConfigSync] 监控已启用，重启调度");
                } else {
                    metricsReportScheduler.stop();
                    log.info("[MonitorConfigSync] 监控已禁用，停止调度");
                }
            }
            return;
        }

        // 推送间隔配置
        if ("monitor.interval-ms".equals(configKey)) {
            try {
                long intervalMs = Long.parseLong(configValue);
                monitorConfigHolder.setIntervalMs(intervalMs);
                // 间隔变化需要重启调度
                metricsReportScheduler.restart();
                log.info("[MonitorConfigSync] 推送间隔已更新: {}ms", intervalMs);
            } catch (NumberFormatException e) {
                log.warn("[MonitorConfigSync] 无效的间隔配置值: {}", configValue);
            }
            return;
        }

        // 首次延迟配置
        if ("monitor.initial-delay-ms".equals(configKey)) {
            try {
                long initialDelayMs = Long.parseLong(configValue);
                monitorConfigHolder.setInitialDelayMs(initialDelayMs);
                log.info("[MonitorConfigSync] 首次延迟已更新: {}ms", initialDelayMs);
            } catch (NumberFormatException e) {
                log.warn("[MonitorConfigSync] 无效的延迟配置值: {}", configValue);
            }
            return;
        }

        log.warn("[MonitorConfigSync] 未知的配置项: {}", configKey);
    }

    /**
     * 实例下线指令处理
     * 只处理目标实例匹配的消息，其他实例忽略
     *
     * @param offlineMsg 下线消息
     * @return 是否处理成功
     */
    private Mono<Boolean> instanceOfflineHandler(InstanceOfflineMsg offlineMsg) {
        String targetInstance = offlineMsg.getTargetInstance();
        String currentInstance = getCurrentInstanceIdentifier();

        // 实例过滤：只处理目标实例匹配的消息
        if (!currentInstance.equals(targetInstance)) {
            log.debug("[InstanceOffline] 跳过非目标实例 | current: {}, target: {}", currentInstance, targetInstance);
            return Mono.just(true); // 非目标实例，返回成功但不处理
        }

        log.info("[InstanceOffline] 收到下线指令 | instance: {}, type: {}, waitSeconds: {}s, reason: {}",
                currentInstance, offlineMsg.getOfflineType(), offlineMsg.getDrainWaitSeconds(), offlineMsg.getReason());

        // 强制下线
        if ("FORCE".equals(offlineMsg.getOfflineType())) {
            instanceStateManager.startForceOffline(offlineMsg.getReason());
            return Mono.just(true);
        }

        // 优雅下线
        int waitSeconds = offlineMsg.getDrainWaitSeconds() != null ? offlineMsg.getDrainWaitSeconds() : 30;
        boolean started = instanceStateManager.startGracefulOffline(waitSeconds, offlineMsg.getReason());

        if (!started) {
            log.warn("[InstanceOffline] 实例已在下线中，忽略重复指令");
            return Mono.just(true);
        }

        // 异步等待排空完成后上报状态
        return Mono.fromRunnable(() -> {
            // 启动异步排空等待
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    log.info("[InstanceOffline] 开始等待流量排空 | waitSeconds: {}s", waitSeconds);
                    Thread.sleep(waitSeconds * 1000L);
                    log.info("[InstanceOffline] 流量排空完成，实例已完全下线 | instance: {}", currentInstance);
                } catch (InterruptedException e) {
                    log.warn("[InstanceOffline] 排空等待被中断 | instance: {}", currentInstance);
                    Thread.currentThread().interrupt();
                }
            });
        }).then(Mono.just(true));
    }

    /**
     * 实例上线指令处理
     * 只处理目标实例匹配的消息，其他实例忽略
     *
     * @param onlineMsg 上线消息
     * @return 是否处理成功
     */
    private boolean instanceOnlineHandler(InstanceOnlineMsg onlineMsg) {
        String targetInstance = onlineMsg.getTargetInstance();
        String currentInstance = getCurrentInstanceIdentifier();

        // 实例过滤：只处理目标实例匹配的消息
        if (!currentInstance.equals(targetInstance)) {
            log.debug("[InstanceOnline] 跳过非目标实例 | current: {}, target: {}", currentInstance, targetInstance);
            return true; // 非目标实例，返回成功但不处理
        }

        log.info("[InstanceOnline] 收到上线指令 | instance: {}", currentInstance);

        boolean result = instanceStateManager.online();
        if (result) {
            log.info("[InstanceOnline] 实例已恢复接收请求 | instance: {}", currentInstance);
        } else {
            log.info("[InstanceOnline] 实例已在在线状态 | instance: {}", currentInstance);
        }

        return true;
    }

    /**
     * 获取当前实例标识
     *
     * @return 实例标识，格式：host:port
     */
    private String getCurrentInstanceIdentifier() {
        String localHost = GateWayUtil.getLocalIp();
        return localHost + ":" + serverPort;
    }

    /**
     * 渠道配置刷新处理
     * 处理 gateway-admin 推送的渠道配置变更
     *
     * @param refreshMsg 刷新消息
     */
    private void channelNacosRefreshHandler(ChannelNacosRefreshMsg refreshMsg) {
        String refreshType = refreshMsg.getRefreshType();
        String appKey = refreshMsg.getAppKey();

        log.info("[ChannelConfigSync] 收到刷新消息 | type: {}, appKey: {}", refreshType, appKey);

        switch (refreshType) {
            case ChannelNacosRefreshMsg.REFRESH_TYPE_ALL:
                // 全量刷新
                channelConfigCache.refreshAll();
                break;
            case ChannelNacosRefreshMsg.REFRESH_TYPE_SINGLE:
                // 单个刷新
                channelConfigCache.refreshSingle(appKey);
                break;
            case ChannelNacosRefreshMsg.REFRESH_TYPE_DELETE:
                // 删除缓存
                channelConfigCache.evict(appKey);
                break;
            default:
                log.warn("[ChannelConfigSync] 未知的刷新类型 | type: {}", refreshType);
        }
    }

    // TODO 定期扫描 PEL 重新投递或者放入死信队列


}
