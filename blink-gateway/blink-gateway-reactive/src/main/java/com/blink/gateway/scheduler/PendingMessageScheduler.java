package com.blink.gateway.scheduler;

import com.blink.framework.common.utils.JacksonUtil;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.framework.redis.mq.StreamMessage;
import com.blink.gateway.config.prop.BlinkGatewayProperties;
import com.blink.gateway.constant.RedisConstans;
import com.blink.gateway.dto.DeadLetterMessageDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
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

        // 先获取消息内容，再 ACK 移除
        return redisClient.xRange(streamKey, messageId, messageId)
                .next()
                .flatMap(record -> {
                    // 构建死信消息
                    DeadLetterMessageDTO deadLetterMsg = new DeadLetterMessageDTO();
                    deadLetterMsg.setOriginalStreamKey(streamKey);
                    deadLetterMsg.setGroupName(groupName);
                    deadLetterMsg.setMessageId(messageId);
                    deadLetterMsg.setFailedTime(LocalDateTime.now());
                    deadLetterMsg.setRetryCount(0);

                    // 提取消息体信息
                    Object value = record.getValue();
                    if (value instanceof StreamMessage<?> streamMessage) {
                        deadLetterMsg.setPayloadClass(streamMessage.getPayloadClass());
                        deadLetterMsg.setPayloadJson(JacksonUtil.toJson(streamMessage.getPayload()));
                    } else {
                        deadLetterMsg.setPayloadJson(JacksonUtil.toJson(value));
                    }

                    // 保存到死信队列（Redis Hash）
                    String deadLetterKey = RedisConstans.STREAM_DEAD_LETTER_QUEUE;
                    String deadLetterJson = JacksonUtil.toJson(deadLetterMsg);

                    return redisClient.hPut(deadLetterKey, messageId, deadLetterJson)
                            .doOnSuccess(success -> log.info("[DeadLetter] 消息已保存到死信队列 | messageId: {}", messageId))
                            .then(redisClient.xAck(streamKey, groupName, messageId))
                            .doOnSuccess(acked -> log.info("[DeadLetter] 死信消息已 ACK | messageId: {}", messageId))
                            .then();
                })
                .onErrorResume(e -> {
                    log.error("[DeadLetter] 保存死信消息失败 | messageId: {}, error: {}", messageId, e.getMessage());
                    // 即使保存失败，也要 ACK 移除，避免重复处理
                    return redisClient.xAck(streamKey, groupName, messageId).then();
                });
    }

    /**
     * 将消费失败的消息移入死信队列（公共方法，供外部调用）
     *
     * @param streamKey      Stream 键名
     * @param groupName      消费者组名称
     * @param messageId      消息ID
     * @param streamMessage  原始消息
     * @param errorMsg       错误信息
     * @param failedInstance 失败实例标识
     * @return Mono<Void>
     */
    public Mono<Void> moveToDeadLetterQueue(String streamKey, String groupName, String messageId,
                                            StreamMessage<?> streamMessage, String errorMsg, String failedInstance) {
        log.error("[DeadLetter] 消费失败，移入死信队列 | messageId: {}, error: {}", messageId, errorMsg);

        // 构建死信消息
        DeadLetterMessageDTO deadLetterMsg = new DeadLetterMessageDTO();
        deadLetterMsg.setOriginalStreamKey(streamKey);
        deadLetterMsg.setGroupName(groupName);
        deadLetterMsg.setMessageId(messageId);
        deadLetterMsg.setErrorMsg(errorMsg);
        deadLetterMsg.setFailedTime(LocalDateTime.now());
        deadLetterMsg.setFailedInstance(failedInstance);
        deadLetterMsg.setRetryCount(0);

        // 提取消息体信息
        if (streamMessage != null) {
            deadLetterMsg.setPayloadClass(streamMessage.getPayloadClass());
            deadLetterMsg.setPayloadJson(JacksonUtil.toJson(streamMessage.getPayload()));
        }

        // 保存到死信队列（Redis Hash）
        String deadLetterKey = RedisConstans.STREAM_DEAD_LETTER_QUEUE;
        String deadLetterJson = JacksonUtil.toJson(deadLetterMsg);

        return redisClient.hPut(deadLetterKey, messageId, deadLetterJson)
                .doOnSuccess(success -> log.info("[DeadLetter] 消息已保存到死信队列 | messageId: {}, payloadClass: {}",
                        messageId, deadLetterMsg.getPayloadClass()))
                .then(redisClient.xAck(streamKey, groupName, messageId))
                .doOnSuccess(acked -> log.info("[DeadLetter] 死信消息已 ACK | messageId: {}", messageId))
                .onErrorResume(e -> {
                    log.error("[DeadLetter] 保存死信消息失败 | messageId: {}, error: {}", messageId, e.getMessage());
                    return redisClient.xAck(streamKey, groupName, messageId).flatMap(l -> Mono.empty());
                })
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