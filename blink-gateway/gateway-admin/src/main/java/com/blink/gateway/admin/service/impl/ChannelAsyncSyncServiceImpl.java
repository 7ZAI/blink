package com.blink.gateway.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.blink.framework.common.constrant.RedisCacheKeyConstant;
import com.blink.framework.common.data.ChannelInfoRedisDO;
import com.blink.gateway.admin.producer.GateWayStreamMessageProducer;
import com.blink.gateway.admin.service.CacheSyncFailureService;
import com.blink.gateway.admin.service.ChannelAsyncSyncService;
import com.blink.gateway.dto.CacheMsg;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 渠道异步同步服务实现类
 * 使用 IO 线程池异步执行渠道数据同步到网关
 * 通过 CacheMsg 的 operator 字段区分操作类型
 *
 * @author binblink
 */
@Service
@Slf4j
public class ChannelAsyncSyncServiceImpl implements ChannelAsyncSyncService {

    @Resource
    private GateWayStreamMessageProducer gateWayStreamMessageProducer;

    @Resource
    private CacheSyncFailureService cacheSyncFailureService;

    /**
     * 同步操作符常量
     */
    private static final String OPERATOR_ADD = "A";
    private static final String OPERATOR_MODIFY = "M";
    private static final String OPERATOR_DELETE = "D";

    /**
     * 最大重试次数
     */
    private static final int MAX_RETRY_TIMES = 3;

    @Async("ioIntensiveThreadPool")
    @Override
    public void syncAddChannel(String appKey, ChannelInfoRedisDO channelInfo) {
        syncChannelWithRetry(appKey, channelInfo, OPERATOR_ADD);
    }

    @Async("ioIntensiveThreadPool")
    @Override
    public void syncModifyChannel(String appKey, ChannelInfoRedisDO channelInfo) {
        syncChannelWithRetry(appKey, channelInfo, OPERATOR_MODIFY);
    }

    @Async("ioIntensiveThreadPool")
    @Override
    public void syncDeleteChannel(String appKey) {
        syncChannelWithRetry(appKey, null, OPERATOR_DELETE);
    }

    /**
     * 带重试机制的同步方法
     *
     * @param appKey       渠道 appKey
     * @param channelInfo  渠道信息（删除时为 null）
     * @param operator     操作类型：A/M/D
     */
    private void syncChannelWithRetry(String appKey, ChannelInfoRedisDO channelInfo, String operator) {
        // 构建缓存 key
        String cacheKey = RedisCacheKeyConstant.CHANNEL_CACHE_PREFIX + appKey;

        // 构建 CacheMsg 消息
        CacheMsg cacheMsg = new CacheMsg();
        cacheMsg.setKey(cacheKey);
        cacheMsg.setValue(channelInfo);
        cacheMsg.setOperator(operator);
        // 使用时间戳作为版本号，防止消息乱序
        cacheMsg.setVersion((int) (System.currentTimeMillis() / 1000));

        // 重试发送
        for (int retryCount = 0; retryCount < MAX_RETRY_TIMES; retryCount++) {
            try {
                gateWayStreamMessageProducer.sendCacheSyncMsg(cacheMsg);
                log.info("[ChannelAsyncSync] 渠道数据同步成功 | appKey: {}, operator: {}, version: {}",
                        appKey, operator, cacheMsg.getVersion());
                return;
            } catch (Exception e) {
                log.warn("[ChannelAsyncSync] 同步失败，第{}次重试 | appKey: {}, operator: {}, error: {}",
                        retryCount + 1, appKey, operator, e.getMessage());

                if (retryCount == MAX_RETRY_TIMES - 1) {
                    // 达到最大重试次数，记录失败
                    log.error("[ChannelAsyncSync] 同步失败，已达最大重试次数 | appKey: {}, operator: {}", appKey, operator);
                    cacheSyncFailureService.recordFailure(cacheMsg, e);
                } else {
                    // 等待后重试
                    try {
                        Thread.sleep(100 * (retryCount + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("[ChannelAsyncSync] 线程被中断");
                        return;
                    }
                }
            }
        }
    }
}