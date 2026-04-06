package com.blink.gateway.admin.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.gateway.admin.constants.MessageStatusConstant;
import com.blink.gateway.admin.entity.RedisMqDO;
import com.blink.gateway.admin.mapper.RedisMqMapper;
import com.blink.gateway.admin.producer.GateWayStreamMessageProducer;
import com.blink.gateway.admin.service.CacheSyncFailureService;
import com.blink.gateway.dto.CacheMsg;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 缓存同步失败补偿服务实现类
 *
 * @author binblink
 */
@Service
@Slf4j
public class CacheSyncFailureServiceImpl implements CacheSyncFailureService {

    @Resource
    private RedisMqMapper redisMqMapper;

    @Resource
    private GateWayStreamMessageProducer gateWayStreamMessageProducer;

    @Override
    public void recordFailure(CacheMsg cacheMsg, Exception e) {
        try {
            // 查找是否已存在该 key 的失败记录
            LambdaQueryWrapper<RedisMqDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(RedisMqDO::getPayload, JacksonUtil.toJson(cacheMsg))
                    .eq(RedisMqDO::getMsgStatus, MessageStatusConstant.REDIS_MSG_STATUS_SEND_FAILED);

            RedisMqDO existingRecord = redisMqMapper.selectOne(queryWrapper);

            if (existingRecord != null) {
                // 更新失败次数
                LambdaUpdateWrapper<RedisMqDO> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(RedisMqDO::getMsgId, existingRecord.getMsgId())
                        .set(RedisMqDO::getFailTimes, existingRecord.getFailTimes() + 1)
                        .set(RedisMqDO::getExtra, "失败原因: " + e.getMessage());
                redisMqMapper.update(null, updateWrapper);
                log.warn("[CacheSyncFailure] 更新失败记录 | msgId: {}, failTimes: {}",
                        existingRecord.getMsgId(), existingRecord.getFailTimes() + 1);
            } else {
                // 创建新的失败记录
                RedisMqDO redisMqDO = new RedisMqDO();
                redisMqDO.setMsgId(IdUtil.simpleUUID());
                redisMqDO.setMsgStatus(MessageStatusConstant.REDIS_MSG_STATUS_SEND_FAILED);
                redisMqDO.setPayload(JacksonUtil.toJson(cacheMsg));
                redisMqDO.setPayloadClass(CacheMsg.class.getName());
                redisMqDO.setFailTimes(1);
                redisMqDO.setRetryTimes(0);
                redisMqDO.setExtra("失败原因: " + e.getMessage());
                redisMqMapper.insert(redisMqDO);
                log.warn("[CacheSyncFailure] 记录失败消息 | msgId: {}, key: {}", redisMqDO.getMsgId(), cacheMsg.getKey());
            }
        } catch (Exception recordException) {
            log.error("[CacheSyncFailure] 记录失败消息异常 | key: {}, error: {}", cacheMsg.getKey(), recordException.getMessage(), recordException);
        }
    }

    @Override
    public boolean retryFailedMessage(String msgId) {
        RedisMqDO redisMqDO = redisMqMapper.selectById(msgId);
        if (redisMqDO == null) {
            log.warn("[CacheSyncFailure] 消息不存在 | msgId: {}", msgId);
            return false;
        }

        try {
            CacheMsg cacheMsg = JacksonUtil.fromJson(redisMqDO.getPayload(), CacheMsg.class);
            if (cacheMsg == null) {
                log.error("[CacheSyncFailure] 解析消息失败 | msgId: {}", msgId);
                return false;
            }

            gateWayStreamMessageProducer.sendCacheSyncMsg(cacheMsg);

            // 重试成功，更新状态
            LambdaUpdateWrapper<RedisMqDO> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(RedisMqDO::getMsgId, msgId)
                    .set(RedisMqDO::getMsgStatus, MessageStatusConstant.REDIS_MSG_STATUS_UNREADED)
                    .set(RedisMqDO::getRetryTimes, redisMqDO.getRetryTimes() + 1);
            redisMqMapper.update(null, updateWrapper);

            log.info("[CacheSyncFailure] 重试成功 | msgId: {}, key: {}", msgId, cacheMsg.getKey());
            return true;
        } catch (Exception e) {
            // 更新重试次数
            LambdaUpdateWrapper<RedisMqDO> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(RedisMqDO::getMsgId, msgId)
                    .set(RedisMqDO::getRetryTimes, redisMqDO.getRetryTimes() + 1)
                    .set(RedisMqDO::getExtra, "重试失败: " + e.getMessage());
            redisMqMapper.update(null, updateWrapper);

            log.error("[CacheSyncFailure] 重试失败 | msgId: {}, error: {}", msgId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public int retryAllFailedMessages() {
        LambdaQueryWrapper<RedisMqDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RedisMqDO::getMsgStatus, MessageStatusConstant.REDIS_MSG_STATUS_SEND_FAILED);
        List<RedisMqDO> failedMessages = redisMqMapper.selectList(queryWrapper);

        int successCount = 0;
        for (RedisMqDO redisMqDO : failedMessages) {
            if (retryFailedMessage(redisMqDO.getMsgId())) {
                successCount++;
            }
        }

        log.info("[CacheSyncFailure] 批量重试完成 | total: {}, success: {}", failedMessages.size(), successCount);
        return successCount;
    }
}