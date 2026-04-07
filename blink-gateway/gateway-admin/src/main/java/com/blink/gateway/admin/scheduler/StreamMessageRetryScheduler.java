package com.blink.gateway.admin.scheduler;

import cn.hutool.core.collection.CollUtil;
import com.blink.gateway.admin.constants.MessageStatusConstant;
import com.blink.gateway.admin.entity.RedisMqDO;
import com.blink.gateway.admin.mapper.RedisMqMapper;
import com.blink.gateway.admin.service.CacheSyncFailureService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Stream 消息重试调度器
 * 定时扫描失败消息并自动重试
 *
 * @author binblink
 * @since 2026-04-08
 */
@Slf4j
@Component
public class StreamMessageRetryScheduler {

    @Resource
    private RedisMqMapper redisMqMapper;

    @Resource
    private CacheSyncFailureService cacheSyncFailureService;

    /**
     * 定时重试失败消息
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 300_000)
    public void retryFailedMessages() {
        log.info("[StreamRetry] 开始扫描失败消息...");

        // 查询发送失败且重试次数未超限的消息
        List<RedisMqDO> failedMessages = redisMqMapper.selectFailedMessagesForRetry(
                MessageStatusConstant.REDIS_MSG_STATUS_SEND_FAILED,
                MessageStatusConstant.MAX_RETRY_TIMES
        );

        if (CollUtil.isEmpty(failedMessages)) {
            log.debug("[StreamRetry] 无需重试的失败消息");
            return;
        }

        log.info("[StreamRetry] 发现 {} 条失败消息待重试", failedMessages.size());

        int successCount = 0;
        int deadLetterCount = 0;

        for (RedisMqDO message : failedMessages) {
            // 检查是否超过最大重试次数
            if (message.getRetryTimes() >= MessageStatusConstant.MAX_RETRY_TIMES) {
                // 移入死信队列
                moveToDeadLetterQueue(message);
                deadLetterCount++;
                continue;
            }

            // 尝试重试
            boolean success = cacheSyncFailureService.retryFailedMessage(message.getMsgId());
            if (success) {
                successCount++;
            }
        }

        log.info("[StreamRetry] 重试完成 | 成功: {}, 死信: {}, 总计: {}",
                successCount, deadLetterCount, failedMessages.size());
    }

    /**
     * 移入死信队列
     *
     * @param message 失败消息
     */
    private void moveToDeadLetterQueue(RedisMqDO message) {
        // 更新状态为消费失败（死信）
        message.setMsgStatus(MessageStatusConstant.REDIS_MSG_STATUS_CONSUME_FAILED);
        redisMqMapper.updateById(message);
        log.warn("[StreamRetry] 消息移入死信队列 | msgId: {}, payload: {}",
                message.getMsgId(), message.getPayload());
    }
}