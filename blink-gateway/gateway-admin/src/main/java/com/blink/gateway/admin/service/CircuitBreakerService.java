package com.blink.gateway.admin.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.constants.CircuitBreakerConstant;
import com.blink.gateway.admin.dto.req.GetCircuitBreakerDetailReq;
import com.blink.gateway.admin.dto.req.GetCircuitBreakerHistoryReq;
import com.blink.gateway.admin.dto.req.GetCircuitBreakerOverviewReq;
import com.blink.gateway.admin.dto.rsp.CircuitBreakerConfigRsp;
import com.blink.gateway.admin.dto.rsp.CircuitBreakerDetailRsp;
import com.blink.gateway.admin.dto.rsp.CircuitBreakerInstanceRsp;
import com.blink.gateway.admin.dto.rsp.CircuitBreakerOverviewRsp;
import com.blink.gateway.admin.dto.rsp.CircuitBreakerSummaryRsp;
import com.blink.gateway.admin.dto.rsp.InstanceSummaryRsp;
import com.blink.gateway.admin.dto.rsp.StateTransitionHistoryRsp;
import com.blink.gateway.admin.dto.rsp.TrendDataRsp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 熔断器服务
 *
 * 负责从 Redis 读取熔断器指标、聚合计算、健康度评分、状态转换历史查询
 *
 * @author binblink
 * @since 2026-04-16
 */
@Service
@Slf4j
public class CircuitBreakerService {

    private final RedisClient redisClient;
    private final ObjectMapper objectMapper;

    public CircuitBreakerService(RedisClient redisClient) {
        this.redisClient = redisClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 获取实例列表及熔断器汇总
     *
     * @return 实例列表
     */
    public List<InstanceSummaryRsp> getInstanceList() {
        // 获取所有在线实例
        Map<String, Object> instanceList = redisClient.hGetStringMap(CircuitBreakerConstant.INSTANCE_LIST_KEY);

        if (CollUtil.isEmpty(instanceList)) {
            return Collections.emptyList();
        }

        List<InstanceSummaryRsp> result = new ArrayList<>();

        for (String instanceId : instanceList.keySet()) {
            InstanceSummaryRsp instanceRsp = buildInstanceSummary(instanceId);
            if (instanceRsp != null) {
                result.add(instanceRsp);
            }
        }

        log.debug("[CircuitBreakerService] 获取实例列表 | count: {}", result.size());
        return result;
    }

    /**
     * 构建实例摘要
     *
     * @param instanceId 实例ID
     * @return 实例摘要
     */
    private InstanceSummaryRsp buildInstanceSummary(String instanceId) {
        try {
            // 解析实例ID格式: serviceId:host:port 或 其他格式
            String host = null;
            Integer port = null;

            // 尝试解析格式: serviceId:host:port
            String[] parts = instanceId.split(":");
            if (parts.length >= 3) {
                // 最后两部分是 host 和 port
                try {
                    port = Integer.parseInt(parts[parts.length - 1]);
                    host = parts[parts.length - 2];
                } catch (NumberFormatException e) {
                    // port 解析失败，尝试其他格式
                }
            }

            // 如果上面解析失败，尝试解析格式: host#port##group@@serviceId
            if (host == null || port == null) {
                String[] hashParts = instanceId.split("#");
                if (hashParts.length >= 2) {
                    host = hashParts[0];
                    try {
                        port = Integer.parseInt(hashParts[1]);
                    } catch (NumberFormatException e) {
                        log.warn("[CircuitBreakerService] 无法解析端口 | instanceId: {}", instanceId);
                        return null;
                    }
                }
            }

            if (host == null || port == null) {
                log.warn("[CircuitBreakerService] 实例ID格式不正确 | instanceId: {}", instanceId);
                return null;
            }

            InstanceSummaryRsp rsp = new InstanceSummaryRsp();
            rsp.setInstanceId(instanceId);
            rsp.setHost(host);
            rsp.setPort(port);
            rsp.setStatus("ONLINE");
            rsp.setHealthStatus("UP");

            // 获取熔断器汇总
            InstanceSummaryRsp.CircuitBreakerSummary summary = getCircuitBreakerSummary(instanceId);
            rsp.setSummary(summary);

            return rsp;
        } catch (Exception e) {
            log.warn("[CircuitBreakerService] 构建实例摘要失败 | instanceId: {}, error: {}", instanceId, e.getMessage());
            return null;
        }
    }

    /**
     * 获取实例的熔断器汇总
     *
     * @param instanceId 实例ID
     * @return 熔断器汇总
     */
    private InstanceSummaryRsp.CircuitBreakerSummary getCircuitBreakerSummary(String instanceId) {
        String cbKey = CircuitBreakerConstant.CB_KEY_PREFIX + instanceId;
        Map<String, Object> cbData = redisClient.hGetStringMap(cbKey);

        InstanceSummaryRsp.CircuitBreakerSummary summary = new InstanceSummaryRsp.CircuitBreakerSummary();
        summary.setTotal(0);
        summary.setOpen(0);
        summary.setClosed(0);
        summary.setHalfOpen(0);

        if (CollUtil.isEmpty(cbData)) {
            return summary;
        }

        int total = 0;
        int open = 0;
        int closed = 0;
        int halfOpen = 0;

        for (Map.Entry<String, Object> entry : cbData.entrySet()) {
            if ("timestamp".equals(entry.getKey())) {
                continue;
            }

            try {
                String json = (String) entry.getValue();
                if (StrUtil.isBlank(json)) {
                    continue;
                }

                Map<String, Object> metric = objectMapper.readValue(json,
                        new TypeReference<Map<String, Object>>() {});

                total++;
                String state = (String) metric.get("state");
                if (CircuitBreakerConstant.STATE_OPEN.equals(state)) {
                    open++;
                } else if (CircuitBreakerConstant.STATE_CLOSED.equals(state)) {
                    closed++;
                } else if (CircuitBreakerConstant.STATE_HALF_OPEN.equals(state)) {
                    halfOpen++;
                }
            } catch (Exception e) {
                log.debug("[CircuitBreakerService] 解析熔断器指标失败 | key: {}, error: {}", entry.getKey(), e.getMessage());
            }
        }

        summary.setTotal(total);
        summary.setOpen(open);
        summary.setClosed(closed);
        summary.setHalfOpen(halfOpen);

        return summary;
    }

    /**
     * 获取熔断器总览
     *
     * @param req 请求参数
     * @return 熔断器总览
     */
    public CircuitBreakerOverviewRsp getOverview(GetCircuitBreakerOverviewReq req) {
        String instanceId = req.getInstanceId();

        if (StrUtil.isNotBlank(instanceId)) {
            // 单实例视图
            return getOverviewByInstance(instanceId);
        } else {
            // 聚合视图
            return getOverviewAggregated();
        }
    }

    /**
     * 获取单实例的熔断器总览
     *
     * @param instanceId 实例ID
     * @return 熔断器总览
     */
    private CircuitBreakerOverviewRsp getOverviewByInstance(String instanceId) {
        CircuitBreakerOverviewRsp rsp = new CircuitBreakerOverviewRsp();
        rsp.setTotalInstances(1);

        String cbKey = CircuitBreakerConstant.CB_KEY_PREFIX + instanceId;
        Map<String, Object> cbData = redisClient.hGetStringMap(cbKey);

        if (CollUtil.isEmpty(cbData)) {
            rsp.setCircuitBreakers(Collections.emptyList());
            rsp.setTotalCircuitBreakers(0);
            rsp.setOpenCount(0);
            rsp.setClosedCount(0);
            rsp.setHalfOpenCount(0);
            rsp.setHealthScore(100.0);
            return rsp;
        }

        List<CircuitBreakerSummaryRsp> circuitBreakers = new ArrayList<>();
        int openCount = 0;
        int closedCount = 0;
        int halfOpenCount = 0;

        for (Map.Entry<String, Object> entry : cbData.entrySet()) {
            if ("timestamp".equals(entry.getKey())) {
                continue;
            }

            try {
                String json = (String) entry.getValue();
                if (StrUtil.isBlank(json)) {
                    continue;
                }

                Map<String, Object> metric = objectMapper.readValue(json,
                        new TypeReference<Map<String, Object>>() {});

                CircuitBreakerSummaryRsp summary = buildCircuitBreakerSummary(entry.getKey(), metric, instanceId);
                circuitBreakers.add(summary);

                // 统计状态
                String state = (String) metric.get("state");
                if (CircuitBreakerConstant.STATE_OPEN.equals(state)) {
                    openCount++;
                } else if (CircuitBreakerConstant.STATE_CLOSED.equals(state)) {
                    closedCount++;
                } else if (CircuitBreakerConstant.STATE_HALF_OPEN.equals(state)) {
                    halfOpenCount++;
                }
            } catch (Exception e) {
                log.debug("[CircuitBreakerService] 解析熔断器指标失败 | key: {}, error: {}", entry.getKey(), e.getMessage());
            }
        }

        rsp.setCircuitBreakers(circuitBreakers);
        rsp.setTotalCircuitBreakers(circuitBreakers.size());
        rsp.setOpenCount(openCount);
        rsp.setClosedCount(closedCount);
        rsp.setHalfOpenCount(halfOpenCount);
        rsp.setHealthScore(calculateHealthScore(openCount, closedCount, halfOpenCount));

        return rsp;
    }

    /**
     * 获取聚合视图的熔断器总览
     *
     * @return 熔断器总览
     */
    private CircuitBreakerOverviewRsp getOverviewAggregated() {
        CircuitBreakerOverviewRsp rsp = new CircuitBreakerOverviewRsp();

        // 获取所有在线实例
        Map<String, Object> instanceList = redisClient.hGetStringMap(CircuitBreakerConstant.INSTANCE_LIST_KEY);

        if (CollUtil.isEmpty(instanceList)) {
            rsp.setCircuitBreakers(Collections.emptyList());
            rsp.setTotalCircuitBreakers(0);
            rsp.setTotalInstances(0);
            rsp.setOpenCount(0);
            rsp.setClosedCount(0);
            rsp.setHalfOpenCount(0);
            rsp.setHealthScore(100.0);
            return rsp;
        }

        // 按熔断器名称聚合数据
        Map<String, List<CircuitBreakerInstanceRsp>> cbInstanceMap = new HashMap<>();
        int totalOpen = 0;
        int totalClosed = 0;
        int totalHalfOpen = 0;

        for (String instanceId : instanceList.keySet()) {
            String cbKey = CircuitBreakerConstant.CB_KEY_PREFIX + instanceId;
            Map<String, Object> cbData = redisClient.hGetStringMap(cbKey);

            if (CollUtil.isEmpty(cbData)) {
                continue;
            }

            for (Map.Entry<String, Object> entry : cbData.entrySet()) {
                if ("timestamp".equals(entry.getKey())) {
                    continue;
                }

                try {
                    String json = (String) entry.getValue();
                    if (StrUtil.isBlank(json)) {
                        continue;
                    }

                    Map<String, Object> metric = objectMapper.readValue(json,
                            new TypeReference<Map<String, Object>>() {});

                    String cbName = entry.getKey();
                    CircuitBreakerInstanceRsp instanceRsp = buildCircuitBreakerInstanceRsp(instanceId, metric);

                    cbInstanceMap.computeIfAbsent(cbName, k -> new ArrayList<>()).add(instanceRsp);

                    // 统计状态
                    String state = (String) metric.get("state");
                    if (CircuitBreakerConstant.STATE_OPEN.equals(state)) {
                        totalOpen++;
                    } else if (CircuitBreakerConstant.STATE_CLOSED.equals(state)) {
                        totalClosed++;
                    } else if (CircuitBreakerConstant.STATE_HALF_OPEN.equals(state)) {
                        totalHalfOpen++;
                    }
                } catch (Exception e) {
                    log.debug("[CircuitBreakerService] 解析熔断器指标失败 | error: {}", e.getMessage());
                }
            }
        }

        // 构建聚合结果
        List<CircuitBreakerSummaryRsp> circuitBreakers = new ArrayList<>();
        for (Map.Entry<String, List<CircuitBreakerInstanceRsp>> entry : cbInstanceMap.entrySet()) {
            CircuitBreakerSummaryRsp summary = buildCircuitBreakerSummaryAggregated(entry.getKey(), entry.getValue());
            circuitBreakers.add(summary);
        }

        rsp.setCircuitBreakers(circuitBreakers);
        rsp.setTotalCircuitBreakers(circuitBreakers.size());
        rsp.setTotalInstances(instanceList.size());
        rsp.setOpenCount(totalOpen);
        rsp.setClosedCount(totalClosed);
        rsp.setHalfOpenCount(totalHalfOpen);
        rsp.setHealthScore(calculateHealthScore(totalOpen, totalClosed, totalHalfOpen));

        return rsp;
    }

    /**
     * 构建熔断器汇总（单实例）
     *
     * @param cbName   熔断器名称
     * @param metric   指标数据
     * @param instanceId 实例ID
     * @return 熔断器汇总
     */
    private CircuitBreakerSummaryRsp buildCircuitBreakerSummary(String cbName, Map<String, Object> metric, String instanceId) {
        CircuitBreakerSummaryRsp summary = new CircuitBreakerSummaryRsp();
        summary.setName(cbName);

        // 设置配置信息（从预定义配置获取或使用默认值）
        setDefaultConfig(summary);

        // 设置状态统计
        String state = (String) metric.get("state");
        summary.setClosedCount(CircuitBreakerConstant.STATE_CLOSED.equals(state) ? 1 : 0);
        summary.setOpenCount(CircuitBreakerConstant.STATE_OPEN.equals(state) ? 1 : 0);
        summary.setHalfOpenCount(CircuitBreakerConstant.STATE_HALF_OPEN.equals(state) ? 1 : 0);

        // 设置实例详情
        CircuitBreakerInstanceRsp instanceRsp = buildCircuitBreakerInstanceRsp(instanceId, metric);
        summary.setInstances(Collections.singletonList(instanceRsp));

        return summary;
    }

    /**
     * 构建熔断器汇总（聚合视图）
     *
     * @param cbName    熔断器名称
     * @param instances 实例列表
     * @return 熔断器汇总
     */
    private CircuitBreakerSummaryRsp buildCircuitBreakerSummaryAggregated(String cbName, List<CircuitBreakerInstanceRsp> instances) {
        CircuitBreakerSummaryRsp summary = new CircuitBreakerSummaryRsp();
        summary.setName(cbName);

        // 设置配置信息
        setDefaultConfig(summary);

        // 统计各状态数量
        int closedCount = 0;
        int openCount = 0;
        int halfOpenCount = 0;

        for (CircuitBreakerInstanceRsp instance : instances) {
            if (CircuitBreakerConstant.STATE_CLOSED.equals(instance.getState())) {
                closedCount++;
            } else if (CircuitBreakerConstant.STATE_OPEN.equals(instance.getState())) {
                openCount++;
            } else if (CircuitBreakerConstant.STATE_HALF_OPEN.equals(instance.getState())) {
                halfOpenCount++;
            }
        }

        summary.setClosedCount(closedCount);
        summary.setOpenCount(openCount);
        summary.setHalfOpenCount(halfOpenCount);
        summary.setInstances(instances);

        return summary;
    }

    /**
     * 构建熔断器实例状态
     *
     * @param instanceId 实例ID
     * @param metric     指标数据
     * @return 熔断器实例状态
     */
    private CircuitBreakerInstanceRsp buildCircuitBreakerInstanceRsp(String instanceId, Map<String, Object> metric) {
        CircuitBreakerInstanceRsp rsp = new CircuitBreakerInstanceRsp();
        rsp.setInstanceId(instanceId);
        rsp.setState((String) metric.get("state"));
        rsp.setFailureRate(parseDouble(metric.get("failureRate")));
        rsp.setSlowCallRate(parseDouble(metric.get("slowCallRate")));
        rsp.setNumberOfCalls(parseInt(metric.get("numberOfCalls")));
        rsp.setNumberOfFailedCalls(parseInt(metric.get("numberOfFailedCalls")));
        rsp.setNumberOfSuccessfulCalls(parseInt(metric.get("numberOfSuccessfulCalls")));
        rsp.setTimestamp(parseLong(metric.get("timestamp")));
        return rsp;
    }

    /**
     * 设置默认配置
     *
     * @param summary 熔断器汇总
     */
    private void setDefaultConfig(CircuitBreakerSummaryRsp summary) {
        // 默认配置值（实际应从配置中心或 Redis 配置中获取）
        summary.setBaseConfig("default");
        summary.setFailureRateThreshold(50.0);
        summary.setSlidingWindowSize(100);
        summary.setMinimumNumberOfCalls(10);
        summary.setWaitDurationInOpenState(60L);
    }

    /**
     * 计算健康度评分
     *
     * @param openCount     OPEN 状态数量
     * @param closedCount   CLOSED 状态数量
     * @param halfOpenCount HALF_OPEN 状态数量
     * @return 健康度评分（0-100）
     */
    private Double calculateHealthScore(int openCount, int closedCount, int halfOpenCount) {
        int total = openCount + closedCount + halfOpenCount;
        if (total == 0) {
            return 100.0;
        }

        // CLOSED 状态得满分，HALF_OPEN 状态得 50 分，OPEN 状态得 0 分
        double score = (closedCount * 100.0 + halfOpenCount * 50.0) / total;
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 获取熔断器详情
     *
     * @param req 请求参数
     * @return 熔断器详情
     */
    public CircuitBreakerDetailRsp getDetail(GetCircuitBreakerDetailReq req) {
        String name = req.getName();
        String instanceId = req.getInstanceId();

        if (StrUtil.isBlank(name)) {
            return null;
        }

        CircuitBreakerDetailRsp detail = new CircuitBreakerDetailRsp();

        // 获取配置信息
        detail.setConfig(getCircuitBreakerConfig(name));

        // 获取实例状态列表
        List<CircuitBreakerInstanceRsp> instances;
        if (StrUtil.isNotBlank(instanceId)) {
            // 单实例
            instances = getInstancesByName(instanceId, name);
        } else {
            // 所有实例
            instances = getAllInstancesByName(name);
        }
        detail.setInstances(instances);

        // 获取状态转换历史
        if (StrUtil.isNotBlank(instanceId)) {
            List<StateTransitionHistoryRsp> history = getHistory(instanceId, name, CircuitBreakerConstant.DEFAULT_HISTORY_LIMIT);
            detail.setHistory(history);
        } else {
            detail.setHistory(Collections.emptyList());
        }

        // 趋势数据暂不实现
        detail.setTrend(Collections.emptyList());

        return detail;
    }

    /**
     * 获取熔断器配置
     *
     * @param name 熔断器名称
     * @return 配置信息
     */
    private CircuitBreakerConfigRsp getCircuitBreakerConfig(String name) {
        CircuitBreakerConfigRsp config = new CircuitBreakerConfigRsp();
        config.setName(name);
        config.setBaseConfig("default");
        config.setSlidingWindowType("COUNT_BASED");
        config.setSlidingWindowSize(100);
        config.setMinimumNumberOfCalls(10);
        config.setFailureRateThreshold(50.0);
        config.setSlowCallRateThreshold(100.0);
        config.setSlowCallDurationThreshold(60000L);
        config.setWaitDurationInOpenState(60L);
        config.setPermittedNumberOfCallsInHalfOpenState(10);
        config.setAutomaticTransitionFromOpenToHalfOpenEnabled(true);
        return config;
    }

    /**
     * 获取指定实例的熔断器状态
     *
     * @param instanceId 实例ID
     * @param name       熔断器名称
     * @return 实例状态列表
     */
    private List<CircuitBreakerInstanceRsp> getInstancesByName(String instanceId, String name) {
        String cbKey = CircuitBreakerConstant.CB_KEY_PREFIX + instanceId;
        String json = (String) redisClient.hGetField(cbKey, name);

        if (StrUtil.isBlank(json)) {
            return Collections.emptyList();
        }

        try {
            Map<String, Object> metric = objectMapper.readValue(json,
                    new TypeReference<Map<String, Object>>() {});

            CircuitBreakerInstanceRsp rsp = buildCircuitBreakerInstanceRsp(instanceId, metric);
            return Collections.singletonList(rsp);
        } catch (Exception e) {
            log.warn("[CircuitBreakerService] 解析熔断器指标失败 | instanceId: {}, name: {}, error: {}",
                    instanceId, name, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 获取所有实例中指定熔断器的状态
     *
     * @param name 熔断器名称
     * @return 实例状态列表
     */
    private List<CircuitBreakerInstanceRsp> getAllInstancesByName(String name) {
        Map<String, Object> instanceList = redisClient.hGetStringMap(CircuitBreakerConstant.INSTANCE_LIST_KEY);

        if (CollUtil.isEmpty(instanceList)) {
            return Collections.emptyList();
        }

        List<CircuitBreakerInstanceRsp> result = new ArrayList<>();

        for (String instanceId : instanceList.keySet()) {
            List<CircuitBreakerInstanceRsp> instances = getInstancesByName(instanceId, name);
            result.addAll(instances);
        }

        return result;
    }

    /**
     * 获取状态转换历史
     *
     * @param req 请求参数
     * @return 状态转换历史列表
     */
    public List<StateTransitionHistoryRsp> getHistory(GetCircuitBreakerHistoryReq req) {
        return getHistory(req.getInstanceId(), req.getName(), req.getLimit());
    }

    /**
     * 获取状态转换历史
     *
     * @param instanceId 实例ID
     * @param name       熔断器名称
     * @param limit      查询数量限制
     * @return 状态转换历史列表
     */
    private List<StateTransitionHistoryRsp> getHistory(String instanceId, String name, Integer limit) {
        if (StrUtil.isBlank(instanceId) || StrUtil.isBlank(name)) {
            return Collections.emptyList();
        }

        String historyKey = CircuitBreakerConstant.CB_KEY_PREFIX + "history:" + instanceId + ":" + name;
        int queryLimit = limit != null && limit > 0 ? limit : CircuitBreakerConstant.DEFAULT_HISTORY_LIMIT;

        List<Object> historyList = redisClient.lRange(historyKey, 0, queryLimit - 1);

        if (CollUtil.isEmpty(historyList)) {
            return Collections.emptyList();
        }

        List<StateTransitionHistoryRsp> result = new ArrayList<>();

        for (Object item : historyList) {
            String json = item != null ? item.toString() : null;
            if (StrUtil.isBlank(json)) {
                continue;
            }
            try {
                Map<String, Object> transition = objectMapper.readValue(json,
                        new TypeReference<Map<String, Object>>() {});

                StateTransitionHistoryRsp rsp = new StateTransitionHistoryRsp();
                rsp.setFromState((String) transition.get("from"));
                rsp.setToState((String) transition.get("to"));
                rsp.setTimestamp(parseLong(transition.get("time")));
                rsp.setReason((String) transition.get("reason"));
                rsp.setFailureRate(parseDouble(transition.get("failureRate")));
                rsp.setNumberOfCalls(parseInt(transition.get("numberOfCalls")));

                result.add(rsp);
            } catch (Exception e) {
                log.debug("[CircuitBreakerService] 解析状态转换历史失败 | error: {}", e.getMessage());
            }
        }

        return result;
    }

    // ==================== 类型转换工具方法 ====================

    private Double parseDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private Integer parseInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
