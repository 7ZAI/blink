package com.blink.gateway.admin.sse;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.constants.RedisKeyConstant;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * SSE实例关闭处理器
 * 负责在实例关闭时清理Redis中的连接注册信息
 *
 * @author binblink
 * @since 2026-04-08
 */
@Slf4j
@Component
public class SseShutdownHandler {

    @Resource
    private RedisClient redisClient;

    @Resource
    private SseInstanceIdentifier instanceIdentifier;

    /**
     * 实例关闭时清理连接注册
     */
    @PreDestroy
    public void onShutdown() {
        String instanceId = instanceIdentifier.getInstanceId();
        String registryKey = RedisKeyConstant.SSE_CONNECTION_REGISTRY;

        log.info("[SSE] 实例关闭，开始清理连接注册 | instanceId: {}", instanceId);

        try {
            // 获取所有注册的用户
            Map<String, Object> registry = redisClient.hGetStringMap(registryKey);

            if (CollUtil.isEmpty(registry)) {
                log.info("[SSE] 连接注册表为空，无需清理");
                return;
            }

            // 清理本实例注册的所有用户
            int cleanedCount = 0;
            for (Map.Entry<String, Object> entry : registry.entrySet()) {
                String userId = entry.getKey();
                Object registeredInstance = entry.getValue();

                if (ObjectUtil.isNotNull(registeredInstance)
                    && instanceId.equals(registeredInstance.toString())) {
                    redisClient.hDeleteFields(registryKey, userId);
                    cleanedCount++;
                }
            }

            // 清理心跳记录
            String heartbeatKey = RedisKeyConstant.SSE_INSTANCE_HEARTBEAT + instanceId;
            redisClient.delete(heartbeatKey);

            log.info("[SSE] 连接注册清理完成 | instanceId: {}, 清理用户数: {}", instanceId, cleanedCount);
        } catch (Exception e) {
            log.error("[SSE] 连接注册清理失败 | instanceId: {}", instanceId, e);
        }
    }
}