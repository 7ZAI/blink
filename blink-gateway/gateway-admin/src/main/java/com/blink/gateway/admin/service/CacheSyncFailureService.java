package com.blink.gateway.admin.service;

import com.blink.gateway.dto.CacheMsg;

/**
 * 缓存同步失败补偿服务接口
 * 用于记录和处理缓存同步失败的消息
 *
 * @author binblink
 */
public interface CacheSyncFailureService {

    /**
     * 记录同步失败的消息
     *
     * @param cacheMsg 缓存消息
     * @param e        异常信息
     */
    void recordFailure(CacheMsg cacheMsg, Exception e);

    /**
     * 重试发送失败的消息
     *
     * @param msgId 消息ID
     * @return 是否重试成功
     */
    boolean retryFailedMessage(String msgId);

    /**
     * 批量重试所有失败的消息
     *
     * @return 重试成功的消息数量
     */
    int retryAllFailedMessages();
}