package com.blink.gateway.admin.service.impl;

import cn.hutool.core.util.StrUtil;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.entity.GatewayMetricsHistoryDO;
import com.blink.gateway.admin.mapper.GatewayMetricsHistoryMapper;
import com.blink.gateway.admin.service.MetricsCollectorService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

import static com.blink.gateway.admin.constants.ScheduleConstant.METRICS_HISTORY_CLEAN_CRON;

/**
 * 网关指标历史数据管理服务实现
 *
 * 负责历史数据的存储和清理
 *
 * 架构变更说明：
 * - 旧架构：admin 定时 HTTP 轮询 gateway-reactive 的 actuator 端点
 * - 新架构：gateway-reactive 通过 Redis Stream 主动上报指标，admin 消费并存储
 *
 * 相关类：
 * - MetricsReporter (gateway-reactive): 指标上报
 * - MetricsStreamConsumer (gateway-admin): 指标消费和 Redis 存储
 * - 本类: MySQL 历史数据存储和清理
 *
 * @author binblink
 */
@Service
@Slf4j
public class MetricsCollectorServiceImpl implements MetricsCollectorService {

    @Resource
    private RedisClient redisClient;

    @Resource
    private GatewayMetricsHistoryMapper metricsHistoryMapper;

    @Value("${blink.gateway.monitor.history-retention-days:7}")
    private int historyRetentionDays;

    /**
     * Redis 指标 Key 前缀
     */
    private static final String METRICS_KEY_PREFIX = "blink:gateway:metrics:";

    @Override
    @Scheduled(cron = METRICS_HISTORY_CLEAN_CRON)
    public void cleanHistoryMetrics() {
        try {
            log.info("[MetricsCollector] 开始清理历史数据...");
            LocalDateTime beforeTime = LocalDateTime.now().minusDays(historyRetentionDays);
            int deleted = metricsHistoryMapper.deleteBeforeTime(beforeTime);
            log.info("[MetricsCollector] 清理历史数据完成 | 删除记录数: {}", deleted);
        } catch (Exception e) {
            log.error("[MetricsCollector] 清理历史数据失败", e);
        }
    }
}
