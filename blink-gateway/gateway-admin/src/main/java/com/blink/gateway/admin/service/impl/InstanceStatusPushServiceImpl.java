package com.blink.gateway.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.config.MonitorProperties;
import com.blink.gateway.admin.dto.InstanceStatusSnapshot;
import com.blink.gateway.admin.dto.InstanceStatusSnapshot.StatusChangeType;
import com.blink.gateway.admin.sse.InstanceStatusPayload;
import com.blink.gateway.admin.sse.SseConnectionPool;
import com.blink.gateway.admin.sse.SseMessage;
import com.blink.gateway.admin.sse.SseMessageType;
import com.blink.gateway.admin.service.InstanceStatusPushService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 实例状态推送服务实现
 *
 * @author binblink
 * @since 2026-04-14
 */
@Service
@Slf4j
public class InstanceStatusPushServiceImpl implements InstanceStatusPushService {

    @Resource
    private RedisClient redisClient;

    @Resource
    private SseConnectionPool sseConnectionPool;

    @Resource
    private MonitorProperties monitorProperties;

    private static final String SNAPSHOT_KEY_PREFIX = "blink:gateway:instance:snapshot:";

    @Override
    public void checkAndPush(List<InstanceStatusPayload.InstanceSummary> payloads) {
        if (CollUtil.isEmpty(payloads)) {
            return;
        }

        // 检测状态变化
        List<String> changedInstanceIds = new ArrayList<>();
        boolean hasImportantChange = false;

        for (InstanceStatusPayload.InstanceSummary current : payloads) {
            InstanceStatusSnapshot currentSnapshot = toSnapshot(current);
            InstanceStatusSnapshot previousSnapshot = getSnapshot(current.getInstanceId());

            StatusChangeType changeType = currentSnapshot.detectChange(
                    previousSnapshot,
                    monitorProperties.getCpuChangeThreshold(),
                    monitorProperties.getHeapChangeThreshold());

            if (changeType != null) {
                changedInstanceIds.add(current.getInstanceId());

                // 重要变化：新实例、状态变化、健康变化
                if (changeType != StatusChangeType.METRICS_CHANGED) {
                    hasImportantChange = true;
                    log.info("[InstanceStatus] 检测到状态变化 | instanceId: {}, changeType: {}",
                            current.getInstanceId(), changeType);
                }
            }

            // 保存当前快照
            saveSnapshot(currentSnapshot);
        }

        // 构建推送载荷
        InstanceStatusPayload payload = buildPayload(payloads, changedInstanceIds);

        // 根据变化类型决定推送策略
        if (hasImportantChange) {
            // 重要变化：立即广播
            SseMessage<InstanceStatusPayload> msg = SseMessage.instanceStatus(payload);
            sseConnectionPool.broadcast(msg);
            log.info("[InstanceStatus] 广播实例状态变化 | 变化实例数: {}", changedInstanceIds.size());
        } else if (CollUtil.isNotEmpty(changedInstanceIds)) {
            // 仅指标变化：推送，但标记为非重要
            SseMessage<InstanceStatusPayload> msg = SseMessage.instanceStatus(payload);
            sseConnectionPool.broadcast(msg);
            log.debug("[InstanceStatus] 推送指标变化 | 变化实例数: {}", changedInstanceIds.size());
        }
        // 无变化则不推送
    }

    @Override
    public void pushStatusChange(String instanceId, Integer status) {
        // 获取实例最新状态
        String metricsKey = "blink:gateway:metrics:" + instanceId;
        Map<String, Object> metrics = redisClient.hGetStringMap(metricsKey);

        InstanceStatusPayload.InstanceSummary summary = new InstanceStatusPayload.InstanceSummary();
        summary.setInstanceId(instanceId);
        summary.setStatus(status);

        if (CollUtil.isNotEmpty(metrics)) {
            Object healthStatus = metrics.get("healthStatus");
            summary.setHealthStatus(healthStatus != null ? healthStatus.toString() : "UNKNOWN");

            Object cpuUsage = metrics.get("cpuUsage");
            if (cpuUsage != null) {
                summary.setCpuUsage(toDoubleValue(cpuUsage));
            }

            Object timestamp = metrics.get("timestamp");
            if (timestamp != null) {
                summary.setTimestamp(toLongValue(timestamp));
            }
        }

        // 构建推送载荷（单个实例）
        InstanceStatusPayload payload = new InstanceStatusPayload();
        payload.setInstances(List.of(summary));
        payload.setHasChange(true);
        payload.setChangedInstanceIds(List.of(instanceId));

        // 广播
        SseMessage<InstanceStatusPayload> msg = SseMessage.instanceStatus(payload);
        sseConnectionPool.broadcast(msg);

        log.info("[InstanceStatus] 推送实例状态变化 | instanceId: {}, status: {}", instanceId, status);
    }

    @Override
    public void sendFullStatusToUser(Integer userId) {
        // 从 Redis 获取所有实例状态
        String listKey = "blink:gateway:instance:list";
        Map<String, Object> instanceIds = redisClient.hGetStringMap(listKey);

        if (CollUtil.isEmpty(instanceIds)) {
            return;
        }

        List<InstanceStatusPayload.InstanceSummary> summaries = new ArrayList<>();
        int onlineCount = 0;
        int healthyCount = 0;
        double totalCpu = 0;

        for (String instanceId : instanceIds.keySet()) {
            String metricsKey = "blink:gateway:metrics:" + instanceId;
            Map<String, Object> metrics = redisClient.hGetStringMap(metricsKey);

            if (CollUtil.isEmpty(metrics)) {
                continue;
            }

            InstanceStatusPayload.InstanceSummary summary = new InstanceStatusPayload.InstanceSummary();
            summary.setInstanceId(instanceId);

            // 解析指标数据
            Object status = metrics.get("status");
            if (status != null) {
                int statusValue = toIntValue(status);
                summary.setStatus(statusValue);
                if (statusValue == 0) {
                    onlineCount++;
                }
            }

            Object healthStatus = metrics.get("healthStatus");
            if ("UP".equals(healthStatus)) {
                summary.setHealthStatus("UP");
                healthyCount++;
            } else {
                summary.setHealthStatus(healthStatus != null ? healthStatus.toString() : "UNKNOWN");
            }

            Object cpuUsage = metrics.get("cpuUsage");
            if (cpuUsage != null) {
                double cpu = toDoubleValue(cpuUsage);
                summary.setCpuUsage(cpu);
                totalCpu += cpu;
            }

            Object timestamp = metrics.get("timestamp");
            if (timestamp != null) {
                summary.setTimestamp(toLongValue(timestamp));
            }

            summaries.add(summary);
        }

        // 构建统计信息
        InstanceStatusPayload.InstanceSummaryStats stats = new InstanceStatusPayload.InstanceSummaryStats();
        stats.setTotal(summaries.size());
        stats.setOnline(onlineCount);
        stats.setHealthy(healthyCount);
        stats.setAvgCpuUsage(summaries.size() > 0 ?
                BigDecimal.valueOf(totalCpu / summaries.size()).setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0);

        // 构建推送载荷
        InstanceStatusPayload payload = new InstanceStatusPayload();
        payload.setInstances(summaries);
        payload.setStats(stats);
        payload.setHasChange(false);  // 首次加载，非变化推送
        payload.setChangedInstanceIds(null);

        // 发送给指定用户
        SseMessage<InstanceStatusPayload> msg = SseMessage.instanceStatus(payload);
        sseConnectionPool.sendToUser(userId, msg);

        log.info("[InstanceStatus] 发送完整状态给用户 | userId: {}, 实例数: {}", userId, summaries.size());
    }

    /**
     * 获取上一次快照
     */
    private InstanceStatusSnapshot getSnapshot(String instanceId) {
        String key = SNAPSHOT_KEY_PREFIX + instanceId;
        Map<String, Object> data = redisClient.hGetStringMap(key);

        if (CollUtil.isEmpty(data)) {
            return null;
        }

        InstanceStatusSnapshot snapshot = new InstanceStatusSnapshot();
        snapshot.setInstanceId(instanceId);

        Object status = data.get("status");
        if (status != null) {
            snapshot.setStatus(toIntValue(status));
        }

        snapshot.setHealthStatus((String) data.get("healthStatus"));

        Object cpuUsageInt = data.get("cpuUsageInt");
        if (cpuUsageInt != null) {
            snapshot.setCpuUsageInt(toIntValue(cpuUsageInt));
        }

        Object heapUsageInt = data.get("heapUsageInt");
        if (heapUsageInt != null) {
            snapshot.setHeapUsageInt(toIntValue(heapUsageInt));
        }

        Object timestamp = data.get("timestamp");
        if (timestamp != null) {
            snapshot.setTimestamp(toLongValue(timestamp));
        }

        return snapshot;
    }

    /**
     * 保存快照到 Redis
     */
    private void saveSnapshot(InstanceStatusSnapshot snapshot) {
        String key = SNAPSHOT_KEY_PREFIX + snapshot.getInstanceId();

        Map<String, Object> data = new HashMap<>();
        data.put("instanceId", snapshot.getInstanceId());
        data.put("status", snapshot.getStatus());
        data.put("healthStatus", snapshot.getHealthStatus());
        data.put("cpuUsageInt", snapshot.getCpuUsageInt());
        data.put("heapUsageInt", snapshot.getHeapUsageInt());
        data.put("timestamp", snapshot.getTimestamp());

        // 快照保存 5 分钟
        redisClient.hSet(key, data);
        redisClient.expire(key, 300);
    }

    /**
     * 转换为快照
     */
    private InstanceStatusSnapshot toSnapshot(InstanceStatusPayload.InstanceSummary summary) {
        InstanceStatusSnapshot snapshot = new InstanceStatusSnapshot();
        snapshot.setInstanceId(summary.getInstanceId());
        snapshot.setStatus(summary.getStatus());
        snapshot.setHealthStatus(summary.getHealthStatus());
        snapshot.setTimestamp(summary.getTimestamp());

        if (summary.getCpuUsage() != null) {
            snapshot.setCpuUsageInt(summary.getCpuUsage().intValue());
        }

        if (summary.getHeapUsagePercent() != null) {
            snapshot.setHeapUsageInt(summary.getHeapUsagePercent().intValue());
        }

        return snapshot;
    }

    /**
     * 将 Object 转换为 Long 值（支持 String 和 Number 类型）
     */
    private Long toLongValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L;
    }

    /**
     * 将 Object 转换为 Double 值（支持 String 和 Number 类型）
     */
    private Double toDoubleValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    /**
     * 将 Object 转换为 Integer 值（支持 String 和 Number 类型）
     */
    private Integer toIntValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * 构建推送载荷
     */
    private InstanceStatusPayload buildPayload(
            List<InstanceStatusPayload.InstanceSummary> summaries,
            List<String> changedInstanceIds) {

        // 计算统计信息
        int onlineCount = 0;
        int healthyCount = 0;
        double totalCpu = 0;

        for (InstanceStatusPayload.InstanceSummary s : summaries) {
            if (s.getStatus() != null && s.getStatus() == 0) {
                onlineCount++;
            }
            if ("UP".equals(s.getHealthStatus())) {
                healthyCount++;
            }
            if (s.getCpuUsage() != null) {
                totalCpu += s.getCpuUsage();
            }
        }

        InstanceStatusPayload.InstanceSummaryStats stats = new InstanceStatusPayload.InstanceSummaryStats();
        stats.setTotal(summaries.size());
        stats.setOnline(onlineCount);
        stats.setHealthy(healthyCount);
        stats.setAvgCpuUsage(summaries.size() > 0 ?
                BigDecimal.valueOf(totalCpu / summaries.size()).setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0);

        InstanceStatusPayload payload = new InstanceStatusPayload();
        payload.setInstances(summaries);
        payload.setStats(stats);
        payload.setHasChange(CollUtil.isNotEmpty(changedInstanceIds));
        payload.setChangedInstanceIds(changedInstanceIds);

        return payload;
    }
}
