package com.blink.framework.redis.mq;

import com.blink.framework.common.mq.BlinkProducer;
import com.blink.framework.redis.component.RedisClient;
import com.blink.framework.redis.entity.RedisException;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * stream 消息发送封装
 *
 * @Author binblink
 * @Date 2025/11/5
 */
@Slf4j
public class RedisStreamProducer implements BlinkProducer<StreamMessage, String> {


    private final RedisClient redisClient;


    public RedisStreamProducer(RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    /**
     * 发送消息到 Redis Stream中
     *
     * @param message 消息包装对象
     * @return 由redis生成的消息id
     */
    @Override
    public String sendMessage(StreamMessage message) {
        try {
            // 将消息转换为Map格式
            Map<String, Object> messageMap = StreamMessage.convertMessageToMap(message);
            String mid = redisClient.xAdd(message.getTopic(), messageMap);
            log.info("消息发送成功:  messageInfo={}", message);
            return mid;
        } catch (Exception e) {
            log.error("消息发送失败: messageInfo={}", message, e);
            throw new RuntimeException("消息发送失败", e);
        }
    }

    /**
     * 带重试机制的发送消息
     *
     * @param message 消息包装对象
     * @param retry   最大重试次数
     * @return 由redis生成的消息id 非空-设置成功，null-设置失败（包括重试后仍然失败）
     */
    public String sendMessageWithRetry(StreamMessage message, Retry retry) {

        int retries = retry.getTryTimes();
        int maxRetries = retry.getMaxRetries();
        int failTimes = retry.getFailTimes();
        String mid = null;
        while (retries < maxRetries) {
            try {

                Map<String, Object> messageMap = StreamMessage.convertMessageToMap(message);
                mid = redisClient.xAdd(message.getTopic(), messageMap);
                log.info("消息发送成功:  messageInfo={}", message);
                return mid;
            } catch (RedisException e) {

                retries++;
                failTimes++;
                retry.setTryTimes(retries);
                retry.setFailTimes(failTimes);

                log.warn("sendMessage 失败 ！, 已重试 {}/{} 次", retries, maxRetries, e);
                //超过限制
                if (retries >= maxRetries) {
                    log.error("sendMessage 失败！, 重试次数：{} retries", maxRetries, e);
                    return mid;
                }
                try {
                    // 指数延迟
                    Thread.sleep(100L * retries);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    // sleep本身报错
                    return mid;
                }
            }
        }
        return mid;
    }

    /**
     * 重试Bean统计结果
     */
    public static class Retry {

        /**
         * 尝试次数
         */
        private Integer tryTimes;
        /**
         * 失败次数
         */
        private Integer failTimes;
        /**
         * 最大尝试次数
         */
        private Integer maxRetries;

        public Retry() {

        }

        public Retry(Integer tryTimes, Integer failTimes, Integer maxRetries) {
            this.tryTimes = tryTimes;
            this.failTimes = failTimes;
            this.maxRetries = maxRetries;
        }

        public Integer getTryTimes() {
            return tryTimes;
        }

        public void setTryTimes(Integer tryTimes) {
            this.tryTimes = tryTimes;
        }

        public Integer getFailTimes() {
            return failTimes;
        }

        public void setFailTimes(Integer failTimes) {
            this.failTimes = failTimes;
        }

        public Integer getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
        }
    }


}
