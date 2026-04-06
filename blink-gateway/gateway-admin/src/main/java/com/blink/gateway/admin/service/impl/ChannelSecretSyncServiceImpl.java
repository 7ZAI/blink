package com.blink.gateway.admin.service.impl;

import com.blink.framework.common.exception.BlinkException;
import com.blink.gateway.admin.component.SecretConfigComponent;
import com.blink.gateway.admin.entity.GaChannelDO;
import com.blink.gateway.admin.service.ChannelSecretSyncService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import static com.blink.gateway.admin.constants.ErrCodeConstant.CONFIG_PUSH_FAILED;

/**
 * 渠道密钥配置异步同步服务实现类
 * 使用 IO 线程池异步执行 Nacos 密钥配置推送
 *
 * @author binblink
 */
@Service
@Slf4j
public class ChannelSecretSyncServiceImpl implements ChannelSecretSyncService {

    @Resource
    private SecretConfigComponent secretConfigComponent;

    @Async("ioIntensiveThreadPool")
    @Override
    public void addChannelSecretConfigAsync(GaChannelDO channelDO) {
        try {
            secretConfigComponent.addChannelSecretConfig(channelDO);
            log.info("[ChannelSecretSync] 添加渠道密钥配置成功 | appKey: {}", channelDO.getAppKey());
        } catch (Exception e) {
            log.error("[ChannelSecretSync] 添加渠道密钥配置失败 | appKey: {}, error: {}", channelDO.getAppKey(), e.getMessage(), e);
            // 注意：异步方法中抛出异常不会回滚主事务，仅记录日志
        }
    }

    @Async("ioIntensiveThreadPool")
    @Override
    public void deleteChannelSecretConfigAsync(String appKey) {
        try {
            secretConfigComponent.deleteChannelSecretConfig(appKey);
            log.info("[ChannelSecretSync] 删除渠道密钥配置成功 | appKey: {}", appKey);
        } catch (Exception e) {
            log.error("[ChannelSecretSync] 删除渠道密钥配置失败 | appKey: {}, error: {}", appKey, e.getMessage(), e);
        }
    }
}