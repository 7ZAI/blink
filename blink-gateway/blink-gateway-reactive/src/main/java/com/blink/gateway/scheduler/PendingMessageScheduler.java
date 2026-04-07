package com.blink.gateway.scheduler;

import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.gateway.config.prop.BlinkGatewayProperties;
import com.blink.gateway.constant.RedisConstans;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * PEL（Pending Entry List）清理调度器
 * 处理长时间未 ACK 的消息
 *
 * @author binblink
 * @since 2026-04-08
 */
@Slf4j
@Component
public class PendingMessageScheduler {

    private static final long PEL_IDLE_THRESHOLD_MS = 60_000L;

    private static final int MAX_RETRY_COUNT = 3;

    @Resource
    private ReactiveRedisClient redisClient;

    @Resource
    private BlinkGatewayProperties properties;

    /**
     * 定时处理 PEL 中的超时消息
     * 每2分钟执行一次
     */
    @Scheduled(fixedRate = 120_000)
    public void processPendingMessages() {
        if (!properties.getEventStreamEnable()) {
            return;
        }

        String streamKey = RedisConstans.GATEWAY_STREAM_EVENT;
        String groupName = properties.getGroupName();

        log.debug("[PEL] 开始扫描待处理消息 | stream: {}, group: {}", streamKey, groupName);

        redisClient.xPending(streamKey, groupName)
                .flatMap(summary -> {
                    long totalPending = summary.getTotalPendingMessages();
                    if (totalPending == 0) {
                        log.debug("[PEL] 无待处理消息");
                        return Mono.empty();
                    }

                    log.info("[PEL] 发现 {} 条待处理消息", totalPending);
                    return processPendingMessagesInternal(streamKey, groupName);
                })
                .onErrorResume(e -> {
                    log.error("[PEL] 扫描待处理消息失败", e);
                    return Mono.empty();
                })
                .subscribe();
    }

    /**
     * 内部处理逻辑
     *
     * @param streamKey Stream 键名
     * @param groupName 消费者组名称
     * @return Mono<Void>
     */
    private Mono<Void> processPendingMessagesInternal(String streamKey, String groupName) {
        return redisClient.xPending(streamKey, groupName, "event-consumer")
                .flatMapMany(pendingMessages -> {
                    List<PendingMessage> messages = pendingMessages.stream().toList();
                    if (messages.isEmpty()) {
                        return Flux.empty();
                    }

                    return Flux.fromIterable(messages)
                            .filter(this::isMessageTimeout)
                            .flatMap(msg -> handleTimeoutMessage(streamKey, groupName, msg));
                })
                .then();
    }

    /**
     * 判断消息是否超时
     *
     * @param msg 待处理消息
     * @return 是否超时
     */
    private boolean isMessageTimeout(PendingMessage msg) {
        Duration idleTime = msg.getElapsedTimeSinceLastDelivery();
        return idleTime.toMillis() > PEL_IDLE_THRESHOLD_MS;
    }

    /**
     * 处理超时消息
     *
     * @param streamKey Stream 键名
     * @param groupName 消费者组名称
     * @param msg       待处理消息
     * @return Mono<Void>
     */
    private Mono<Void> handleTimeoutMessage(String streamKey, String groupName, PendingMessage msg) {
        String messageId = msg.getIdAsString();
        long deliveryCount = msg.getTotalDeliveryCount();

        log.warn("[PEL] 消息超时 | messageId: {}, deliveryCount: {}", messageId, deliveryCount);

        // 超过最大重试次数，移入死信队列
        if (deliveryCount >= MAX_RETRY_COUNT) {
            return moveToDeadLetterQueue(streamKey, groupName, messageId);
        }

        // 重新投递
        return reclaimAndRetry(streamKey, groupName, messageId);
    }

    /**
     * 移入死信队列
     *
     * @param streamKey Stream 键名
     * @param groupName 消费者组名称
     * @param messageId 消息ID
     * @return Mono<Void>
     */
    private Mono<Void> moveToDeadLetterQueue(String streamKey, String groupName, String messageId) {
        log.error("[PEL] 消息移入死信队列 | messageId: {}", messageId);

        // 先 ACK 移除，避免重复处理
        return redisClient.xAck(streamKey, groupName, messageId)
                .doOnSuccess(acked -> log.info("[PEL] 死信消息已 ACK | messageId: {}", messageId))
                .then();
    }

    /**
     * 重新认领并重试
     *
     * @param streamKey Stream 键名
     * @param groupName 消费者组名称
     * @param messageId 消息ID
     * @return Mono<Void>
     */
    private Mono<Void> reclaimAndRetry(String streamKey, String groupName, String messageId) {
        log.info("[PEL] 重新认领消息 | messageId: {}", messageId);

        // ACK 后让消息重新可用（下次消费时会重新投递）
        return redisClient.xAck(streamKey, groupName, messageId)
                .doOnSuccess(acked -> log.info("[PEL] 消息已重新投递 | messageId: {}", messageId))
                .then();
    }
}