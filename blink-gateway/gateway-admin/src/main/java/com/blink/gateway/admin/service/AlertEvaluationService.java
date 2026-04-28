package com.blink.gateway.admin.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.constants.RedisKeyConstant;
import com.blink.gateway.admin.dto.vo.AlertConditionVO;
import com.blink.gateway.admin.entity.GatewayAlertHistoryDO;
import com.blink.gateway.admin.entity.GatewayAlertRuleDO;
import com.blink.gateway.admin.mapper.GatewayAlertHistoryMapper;
import com.blink.gateway.admin.mapper.GatewayAlertRuleMapper;
import com.blink.gateway.admin.notification.dispatcher.NotificationDispatcher;
import com.blink.gateway.admin.notification.model.ChannelType;
import com.blink.gateway.admin.notification.model.NotificationMessage;
import com.blink.gateway.admin.service.NotificationPublishService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.blink.gateway.admin.constants.NotificationSeverityConstant.*;
import static com.blink.gateway.admin.constants.NotificationTypeConstant.ALERT;

/**
 * 告警评估服务
 *
 * 定时评估所有启用的告警规则，触发告警通知
 *
 * @author binblink
 * @since 2026-04-15
 */
@Service
@Slf4j
public class AlertEvaluationService {

    /**
     * 抑制标记 Redis Key 前缀
     */
    private static final String SUPPRESS_KEY_PREFIX = "blink:gateway:alert:suppress:";

    /**
     * 超标开始时间 Redis Key 前缀
     */
    private static final String DURATION_KEY_PREFIX = "blink:gateway:alert:duration:";

    @Resource
    private GatewayAlertRuleMapper ruleMapper;

    @Resource
    private GatewayAlertHistoryMapper historyMapper;

    @Resource
    private RedisClient redisClient;

    @Resource
    private NotificationPublishService notificationService;

    @Resource
    private NotificationDispatcher notificationDispatcher;

    /**
     * 定时评估所有规则 (每分钟执行)
     * Cron: 每分钟的第 0 秒执行
     */
    @Scheduled(cron = "0 * * * * ?")
    public void evaluateAllRules() {
        List<GatewayAlertRuleDO> rules = ruleMapper.selectEnabledRules();

        if (CollUtil.isEmpty(rules)) {
            log.debug("[AlertEvaluation] 无启用的告警规则");
            return;
        }

        log.info("[AlertEvaluation] 开始评估告警规则 | 规则数: {}", rules.size());

        for (GatewayAlertRuleDO rule : rules) {
            try {
                evaluateRule(rule);
            } catch (Exception e) {
                log.error("[AlertEvaluation] 规则评估失败 | ruleId: {}, ruleName: {}, error: {}",
                        rule.getId(), rule.getRuleName(), e.getMessage(), e);
            }
        }
    }

    /**
     * 评估单个规则
     */
    private void evaluateRule(GatewayAlertRuleDO rule) {
        // 1. 解析条件列表
        List<AlertConditionVO> conditions = parseConditions(rule.getConditions());

        if (CollUtil.isEmpty(conditions)) {
            log.warn("[AlertEvaluation] 规则无有效条件 | ruleId: {}", rule.getId());
            return;
        }

        // 2. 获取指标数据 (从 Redis 汇总)
        Map<String, Double> metrics = getAggregatedMetrics();

        // 3. 评估所有条件 (AND 逻辑)
        List<AlertConditionVO> triggeredConditions = new ArrayList<>();
        boolean allTriggered = true;

        for (AlertConditionVO cond : conditions) {
            Double value = metrics.get(cond.getMetricName());
            if (value == null) {
                log.debug("[AlertEvaluation] 指标数据缺失 | metric: {}", cond.getMetricName());
                allTriggered = false;
                continue;
            }

            // 阈值判断
            boolean exceeded = evaluateThreshold(value, cond.getOperator(), cond.getThreshold());

            // 持续时间检测
            boolean durationMet = checkDuration(rule.getId(), cond, exceeded);

            if (exceeded && durationMet) {
                triggeredConditions.add(cond);
                log.debug("[AlertEvaluation] 条件满足 | metric: {}, value: {}, threshold: {}",
                        cond.getMetricName(), value, cond.getThreshold());
            } else {
                allTriggered = false;
            }
        }

        // 4. 处理结果
        if (allTriggered && !triggeredConditions.isEmpty()) {
            // 检查抑制
            if (!isSuppressed(rule.getId())) {
                triggerAlert(rule, metrics, triggeredConditions);
                setSuppressed(rule.getId(), rule.getSuppressMinutes());
            } else {
                log.debug("[AlertEvaluation] 告警抑制中 | ruleId: {}", rule.getId());
            }
        } else {
            // 检查恢复
            resolveAlert(rule.getId());
        }
    }

    /**
     * 评估阈值
     */
    private boolean evaluateThreshold(Double value, String operator, Double threshold) {
        if (value == null) {
            return false;
        }

        switch (operator) {
            case "gt":
                return value > threshold;
            case "lt":
                return value < threshold;
            case "gte":
                return value >= threshold;
            case "lte":
                return value <= threshold;
            default:
                return false;
        }
    }

    /**
     * 检查持续时间
     *
     * 从 Redis 获取超标开始时间，判断是否达到配置的持续时间
     */
    private boolean checkDuration(Long ruleId, AlertConditionVO cond, boolean exceeded) {
        String key = DURATION_KEY_PREFIX + ruleId + ":" + cond.getMetricName();

        if (exceeded) {
            // 获取超标开始时间
            Object startTimeObj = redisClient.get(key);
            if (startTimeObj == null) {
                // 记录开始时间
                redisClient.set(key, System.currentTimeMillis());
                log.debug("[AlertEvaluation] 记录超标开始时间 | key: {}", key);
                return false;
            }

            // 计算持续时间 (分钟)
            long startTime = startTimeObj instanceof Long ? (Long) startTimeObj : Long.parseLong(startTimeObj.toString());
            long durationMinutes = (System.currentTimeMillis() - startTime) / 60000;
            boolean met = durationMinutes >= cond.getDurationMinutes();

            log.debug("[AlertEvaluation] 持续时间检测 | key: {}, duration: {}分钟, required: {}分钟, met: {}",
                    key, durationMinutes, cond.getDurationMinutes(), met);

            return met;
        } else {
            // 清除超标标记
            redisClient.delete(key);
            return false;
        }
    }

    /**
     * 检查抑制
     */
    private boolean isSuppressed(Long ruleId) {
        String key = SUPPRESS_KEY_PREFIX + ruleId + ":alert";
        return redisClient.exists(key);
    }

    /**
     * 设置抑制
     */
    private void setSuppressed(Long ruleId, Integer minutes) {
        String key = SUPPRESS_KEY_PREFIX + ruleId + ":alert";
        redisClient.setEx(key, "1", Duration.ofMinutes(minutes));
        log.debug("[AlertEvaluation] 设置告警抑制 | ruleId: {}, minutes: {}", ruleId, minutes);
    }

    /**
     * 触发告警
     */
    private void triggerAlert(GatewayAlertRuleDO rule,
                              Map<String, Double> metrics,
                              List<AlertConditionVO> triggeredConditions) {
        // 1. 创建告警记录
        GatewayAlertHistoryDO alert = new GatewayAlertHistoryDO();
        alert.setRuleId(rule.getId());
        alert.setRuleName(rule.getRuleName());
        alert.setAlertTitle(rule.getRuleName() + " 触发");

        // 2. 渲染通知模板
        String content = renderTemplate(rule.getNotifyTemplate(), rule, metrics, triggeredConditions);
        alert.setAlertContent(content);
        alert.setTriggeredConditions(JacksonUtil.toJson(triggeredConditions));
        alert.setSeverity(rule.getSeverity());
        alert.setStatus("FIRING");
        alert.setFiredTime(LocalDateTime.now());

        historyMapper.insert(alert);

        // 3. 发送通知
        sendNotifications(rule, alert);

        log.info("[AlertEvaluation] 告警触发 | ruleId: {}, ruleName: {}, alertId: {}",
                rule.getId(), rule.getRuleName(), alert.getId());
    }

    /**
     * 渲染通知模板
     */
    private String renderTemplate(String template,
                                   GatewayAlertRuleDO rule,
                                   Map<String, Double> metrics,
                                   List<AlertConditionVO> triggeredConditions) {
        if (StrUtil.isBlank(template)) {
            return "告警规则 " + rule.getRuleName() + " 已触发";
        }

        // 获取第一个触发条件的值
        Double value = 0.0;
        Double threshold = 0.0;
        String metricName = "";

        if (CollUtil.isNotEmpty(triggeredConditions)) {
            AlertConditionVO firstCond = triggeredConditions.get(0);
            metricName = firstCond.getMetricName();
            threshold = firstCond.getThreshold();
            value = metrics.getOrDefault(metricName, 0.0);
        }

        // 替换变量
        return template
                .replace("{{rule_name}}", rule.getRuleName())
                .replace("{{instance_id}}", "gateway-all")
                .replace("{{metric_name}}", metricName)
                .replace("{{value}}", String.format("%.2f", value))
                .replace("{{threshold}}", String.format("%.2f", threshold));
    }

    /**
     * 发送通知
     */
    private void sendNotifications(GatewayAlertRuleDO rule, GatewayAlertHistoryDO alert) {
        if (StrUtil.isBlank(rule.getNotifyChannels())) {
            return;
        }

        // 构建通知消息
        NotificationMessage message = NotificationMessage.builder()
                .title(alert.getAlertTitle())
                .content(alert.getAlertContent())
                .notificationType(ALERT)
                .severity(convertSeverity(alert.getSeverity()))
                .businessId(String.valueOf(alert.getId()))
                .extra(buildAlertExtra(alert))
                .build();

        // 解析渠道类型
        List<ChannelType> channelTypes = Arrays.stream(rule.getNotifyChannels().split(","))
                .map(String::trim)
                .map(ChannelType::fromName)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (CollUtil.isEmpty(channelTypes)) {
            log.warn("[AlertEvaluation] 无有效通知渠道 | channels={}", rule.getNotifyChannels());
            return;
        }

        // 异步发送通知
        notificationDispatcher.dispatchAsync(message, channelTypes);
        log.info("[AlertEvaluation] 通知已分发 | channels={}", channelTypes);
    }

    /**
     * 构建告警扩展参数
     */
    private Map<String, Object> buildAlertExtra(GatewayAlertHistoryDO alert) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("alertId", alert.getId());
        extra.put("ruleId", alert.getRuleId());
        extra.put("ruleName", alert.getRuleName());
        extra.put("severity", alert.getSeverity());
        extra.put("triggeredConditions", alert.getTriggeredConditions());
        return extra;
    }

    /**
     * 转换严重程度为通知严重程度
     */
    private String convertSeverity(String alertSeverity) {
        switch (alertSeverity) {
            case "INFO":
                return INFO;
            case "WARNING":
                return WARNING;
            case "ERROR":
                return ERROR;
            default:
                return WARNING;
        }
    }

    /**
     * 恢复告警
     */
    private void resolveAlert(Long ruleId) {
        // 查询当前触发中的告警
        GatewayAlertHistoryDO firingAlert = historyMapper.selectFiringByRuleId(ruleId);

        if (firingAlert != null) {
            firingAlert.setStatus("RESOLVED");
            firingAlert.setResolvedTime(LocalDateTime.now());
            historyMapper.updateById(firingAlert);

            // 发送恢复通知
            notificationService.sendAlert(
                    firingAlert.getRuleName() + " 已恢复",
                    "告警规则 " + firingAlert.getRuleName() + " 已恢复正常",
                    INFO);

            log.info("[AlertEvaluation] 告警恢复 | ruleId: {}, ruleName: {}, alertId: {}",
                    ruleId, firingAlert.getRuleName(), firingAlert.getId());
        }

        // 清除持续时间标记
        String pattern = DURATION_KEY_PREFIX + ruleId + ":*";
        redisClient.deleteByPrefixScan(pattern);
    }

    /**
     * 解析条件 JSON
     */
    private List<AlertConditionVO> parseConditions(String conditionsJson) {
        if (StrUtil.isBlank(conditionsJson)) {
            return Collections.emptyList();
        }

        try {
            return JacksonUtil.fromJsonToList(conditionsJson, AlertConditionVO.class);
        } catch (Exception e) {
            log.error("[AlertEvaluation] 条件解析失败 | json: {}", conditionsJson, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取聚合指标
     *
     * 从 Redis 汇总数据获取指标值
     */
    private Map<String, Double> getAggregatedMetrics() {
        Map<String, Double> metrics = new HashMap<>();

        // 从 Redis 汇总数据获取
        Map<String, Object> summary = redisClient.hGetStringMap(RedisKeyConstant.GATEWAY_METRICS_SUMMARY);

        if (summary != null) {
            metrics.put("cpuUsage", getDoubleValue(summary, "cpuUsage"));
            metrics.put("memoryUsage", getDoubleValue(summary, "memoryUsage"));
            metrics.put("errorRate", getDoubleValue(summary, "errorRate"));

            // P95/P99 响应时间 (需要扩展 MetricsReporterImpl 后才有)
            metrics.put("p95ResponseTime", getDoubleValue(summary, "p95ResponseTime"));
            metrics.put("p99ResponseTime", getDoubleValue(summary, "p99ResponseTime"));
        }

        log.debug("[AlertEvaluation] 获取聚合指标 | cpuUsage: {}, memoryUsage: {}, errorRate: {}",
                metrics.get("cpuUsage"), metrics.get("memoryUsage"), metrics.get("errorRate"));

        return metrics;
    }

    /**
     * 从 Map 中获取 Double 值
     */
    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
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
}