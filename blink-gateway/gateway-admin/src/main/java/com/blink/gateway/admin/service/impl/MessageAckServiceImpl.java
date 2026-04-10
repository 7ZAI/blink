package com.blink.gateway.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.blink.gateway.admin.constants.MessageStatusConstant;
import com.blink.gateway.admin.entity.RedisMqDO;
import com.blink.gateway.admin.mapper.RedisMqMapper;
import com.blink.gateway.admin.service.MessageAckService;
import com.blink.gateway.admin.sse.NotificationMsg;
import com.blink.gateway.admin.sse.SseConnectionPool;
import com.blink.gateway.dto.req.MessageAckReq;
import com.blink.gateway.dto.rsp.MessageAckRsp;
import com.blink.framework.redis.component.RedisClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * 消息 ACK 确认服务实现类
 *
 * @author binblink
 * @since 2026-04-10
 */
@Slf4j
@Service
public class MessageAckServiceImpl implements MessageAckService {

    @Resource
    private RedisMqMapper redisMqMapper;

    @Resource
    private SseConnectionPool sseConnectionPool;

    @Resource
    private RedisClient redisClient;

    /**
     * ACK 通知去重 key 前缀（防止同一实例重复通知）
     */
    private static final String ACK_NOTIFY_KEY_PREFIX = "blink:ack:notify:";

    /**
     * ACK 结果收集 key 前缀（用于收集多实例 ACK 结果）
     */
    private static final String ACK_RESULT_KEY_PREFIX = "blink:ack:result:";

    /**
     * ACK 汇总通知标记 key 前缀（防止重复发送汇总通知）
     */
    private static final String ACK_SUMMARY_KEY_PREFIX = "blink:ack:summary:";

    /**
     * ACK 结果收集超时时间（秒）- 等待所有实例 ACK
     */
    private static final long ACK_RESULT_TTL = 30L;

    /**
     * 汇总通知等待时间（毫秒）- 等待 3 秒后发送汇总
     */
    private static final long SUMMARY_DELAY_MS = 3000L;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageAckRsp ackMessage(MessageAckReq req) {
        MessageAckRsp rsp = new MessageAckRsp();

        String streamId = req.getStreamId();
        String msgId = req.getMsgId();
        Boolean success = req.getSuccess();
        String errorMsg = req.getErrorMsg();

        log.info("[MessageAck] 收到 ACK 确认 | streamId: {}, msgId: {}, success: {}, consumer: {}",
                streamId, msgId, success, req.getConsumer());

        LambdaUpdateWrapper<RedisMqDO> updateWrapper = new LambdaUpdateWrapper<>();

        // 优先使用 streamId 查找，其次使用 msgId
        if (streamId != null && !streamId.isEmpty()) {
            updateWrapper.eq(RedisMqDO::getStreamId, streamId);
        } else if (msgId != null && !msgId.isEmpty()) {
            updateWrapper.eq(RedisMqDO::getMsgId, msgId);
        } else {
            log.warn("[MessageAck] 缺少有效标识 | streamId 和 msgId 均为空");
            rsp.setAcked(false);
            rsp.setMessage("缺少有效的消息标识");
            return rsp;
        }

        // 根据消费结果更新状态
        if (Boolean.TRUE.equals(success)) {
            updateWrapper.set(RedisMqDO::getMsgStatus, MessageStatusConstant.REDIS_MSG_STATUS_ACK);
            updateWrapper.set(RedisMqDO::getExtra, "{\"message\": \"消费确认成功\", \"consumer\": \"" + req.getConsumer() + "\"}");
        } else {
            updateWrapper.set(RedisMqDO::getMsgStatus, MessageStatusConstant.REDIS_MSG_STATUS_CONSUME_FAILED);
            updateWrapper.set(RedisMqDO::getFailTimes, 1);
            updateWrapper.set(RedisMqDO::getExtra, "{\"message\": \"消费失败: " + (errorMsg != null ? errorMsg : "") + "\", \"consumer\": \"" + req.getConsumer() + "\"}");
        }

        int updated = redisMqMapper.update(null, updateWrapper);

        if (updated > 0) {
            log.info("[MessageAck] 状态更新成功 | streamId: {}, msgId: {}, newStatus: {}",
                    streamId, msgId, Boolean.TRUE.equals(success) ? "3(已确认)" : "4(消费失败)");
            rsp.setAcked(true);
            rsp.setMessage("消息状态已更新");

            // 通过 SSE 推送通知给操作人
            notifyOperator(req, success, errorMsg);
        } else {
            log.warn("[MessageAck] 未找到对应消息记录 | streamId: {}, msgId: {}", streamId, msgId);
            rsp.setAcked(false);
            rsp.setMessage("未找到对应消息记录");
        }

        return rsp;
    }

    /**
     * 通过 SSE 推送通知给操作人
     * 1. 每个实例 ACK 发送单独的进度通知（Toast 弹窗）
     * 2. 收集所有实例结果到 Redis
     * 3. 延迟后发送汇总通知
     *
     * @param req      ACK 请求
     * @param success  是否成功
     * @param errorMsg 错误信息
     */
    private void notifyOperator(MessageAckReq req, Boolean success, String errorMsg) {
        Integer operatorUser = req.getOperatorUser();
        String operatorName = req.getOperatorName();
        String consumer = req.getConsumer();

        // 如果没有操作人信息，不推送通知
        if (operatorUser == null || operatorUser <= 0) {
            log.debug("[MessageAck] 无操作人信息，跳过 SSE 通知 | operatorUser: {}", operatorUser);
            return;
        }

        // 使用 msgId 作为标识
        String msgId = req.getMsgId();
        if (msgId == null || msgId.isEmpty()) {
            msgId = req.getStreamId();
        }

        // 1. 去重检查：同一实例只发送一次进度通知
        String notifyKey = ACK_NOTIFY_KEY_PREFIX + msgId + ":" + consumer;
        Boolean isNew = redisClient.setIfAbsentWithExpire(notifyKey, "1", Duration.ofSeconds(ACK_RESULT_TTL));
        if (!Boolean.TRUE.equals(isNew)) {
            log.debug("[MessageAck] 实例已通知，跳过 | consumer: {}, msgId: {}", consumer, msgId);
            return;
        }

        // 2. 发送单个实例的进度通知（Toast 弹窗）
        sendProgressNotification(operatorUser, operatorName, consumer, success, errorMsg, msgId);

        // 3. 收集实例结果到 Redis Set
        String resultKey = ACK_RESULT_KEY_PREFIX + msgId;
        String instanceResult = Boolean.TRUE.equals(success)
                ? consumer + ":success"
                : consumer + ":failed:" + (errorMsg != null ? errorMsg : "");
        redisClient.sAdd(resultKey, instanceResult);
        redisClient.expire(resultKey, ACK_RESULT_TTL);

        // 4. 异步延迟发送汇总通知
        sendSummaryNotificationAsync(operatorUser, operatorName, msgId);
    }

    /**
     * 发送单个实例的进度通知
     */
    private void sendProgressNotification(Integer operatorUser, String operatorName,
                                          String consumer, Boolean success, String errorMsg, String msgId) {
        try {
            NotificationMsg notification = new NotificationMsg();
            // 进度通知使用固定 ID（基于 msgId + consumer）
            notification.setNotificationId((long) Math.abs((msgId + ":" + consumer).hashCode()));
            notification.setTargetUserId(operatorUser);
            notification.setCreatedTime(LocalDateTime.now());
            notification.setSourceRef(msgId);

            if (Boolean.TRUE.equals(success)) {
                notification.setTitle("网关实例同步成功");
                notification.setContent(String.format("%s，实例 %s 缓存更新成功",
                        operatorName != null ? operatorName : "", consumer));
                notification.setType("instance_sync_success");
                notification.setSeverity("info");
            } else {
                notification.setTitle("网关实例同步失败");
                notification.setContent(String.format("%s，实例 %s 缓存更新失败：%s",
                        operatorName != null ? operatorName : "", consumer, errorMsg != null ? errorMsg : ""));
                notification.setType("instance_sync_failed");
                notification.setSeverity("error");
            }

            notification.setTargetType("user");

            sseConnectionPool.sendToUser(operatorUser, notification);
            log.info("[MessageAck] 进度通知已推送 | userId: {}, consumer: {}, success: {}", operatorUser, consumer, success);
        } catch (Exception e) {
            log.warn("[MessageAck] 进度通知推送失败 | userId: {}, error: {}", operatorUser, e.getMessage());
        }
    }

    /**
     * 异步延迟发送汇总通知
     * 等待 SUMMARY_DELAY_MS 后发送，避免重复发送
     */
    @Async("ioIntensiveThreadPool")
    private void sendSummaryNotificationAsync(Integer operatorUser, String operatorName, String msgId) {
        try {
            // 延迟等待其他实例 ACK
            Thread.sleep(SUMMARY_DELAY_MS);

            // 检查是否已发送过汇总通知
            String summaryKey = ACK_SUMMARY_KEY_PREFIX + msgId;
            Boolean canSend = redisClient.setIfAbsentWithExpire(summaryKey, "1", Duration.ofSeconds(ACK_RESULT_TTL));
            if (!Boolean.TRUE.equals(canSend)) {
                log.debug("[MessageAck] 汇总通知已发送，跳过 | msgId: {}", msgId);
                return;
            }

            // 获取收集的实例结果
            String resultKey = ACK_RESULT_KEY_PREFIX + msgId;
            Set<Object> syncedInstances = redisClient.sMembers(resultKey);

            // 统计成功和失败的实例数
            int successCount = 0;
            int failCount = 0;
            StringBuilder successInstances = new StringBuilder();
            StringBuilder failInstances = new StringBuilder();

            for (Object obj : syncedInstances) {
                String result = obj.toString();
                if (result.contains(":success")) {
                    successCount++;
                    successInstances.append(result.replace(":success", "")).append(", ");
                } else if (result.contains(":failed")) {
                    failCount++;
                    String[] parts = result.split(":failed:");
                    failInstances.append(parts[0]).append("(").append(parts.length > 1 ? parts[1] : "").append("), ");
                }
            }

            // 发送汇总通知
            NotificationMsg notification = new NotificationMsg();
            notification.setNotificationId((long) Math.abs((msgId + ":summary").hashCode()));
            notification.setTargetUserId(operatorUser);
            notification.setCreatedTime(LocalDateTime.now());
            notification.setSourceRef(msgId);
            notification.setTargetType("user");

            if (failCount > 0) {
                notification.setTitle("缓存同步完成（部分失败）");
                notification.setContent(String.format("%s，缓存同步完成：成功 %d 个实例[%s]，失败 %d 个实例[%s]",
                        operatorName != null ? operatorName : "",
                        successCount, successInstances.toString().replaceAll(", $", ""),
                        failCount, failInstances.toString().replaceAll(", $", "")));
                notification.setType("cache_sync_summary_partial");
                notification.setSeverity("warning");
            } else {
                notification.setTitle("缓存同步完成");
                notification.setContent(String.format("%s，缓存已同步到所有 %d 个网关实例：%s",
                        operatorName != null ? operatorName : "",
                        successCount, successInstances.toString().replaceAll(", $", "")));
                notification.setType("cache_sync_summary_success");
                notification.setSeverity("info");
            }

            sseConnectionPool.sendToUser(operatorUser, notification);
            log.info("[MessageAck] 汇总通知已推送 | userId: {}, msgId: {}, successCount: {}, failCount: {}",
                    operatorUser, msgId, successCount, failCount);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[MessageAck] 汇总通知延迟被中断 | msgId: {}", msgId);
        } catch (Exception e) {
            log.warn("[MessageAck] 汇总通知发送失败 | userId: {}, msgId: {}, error: {}", operatorUser, msgId, e.getMessage());
        }
    }
}