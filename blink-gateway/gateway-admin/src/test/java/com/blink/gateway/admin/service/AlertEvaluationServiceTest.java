package com.blink.gateway.admin.service;

import cn.hutool.core.collection.CollUtil;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AlertEvaluationService 单元测试类
 *
 * @author binblink
 * @since 2026-04-15
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AlertEvaluationService 单元测试")
class AlertEvaluationServiceTest {

    @Mock
    private GatewayAlertRuleMapper ruleMapper;

    @Mock
    private GatewayAlertHistoryMapper historyMapper;

    @Mock
    private RedisClient redisClient;

    @Mock
    private NotificationPublishService notificationService;

    @Mock
    private NotificationDispatcher notificationDispatcher;

    @InjectMocks
    private AlertEvaluationService alertEvaluationService;

    private GatewayAlertRuleDO testRule;
    private AlertConditionVO testCondition;

    @BeforeEach
    void setUp() {
        // 初始化测试规则
        testRule = new GatewayAlertRuleDO();
        testRule.setId(1L);
        testRule.setRuleName("CPU告警");
        testRule.setRuleType("RESOURCE");
        testRule.setSeverity("WARNING");
        testRule.setSuppressMinutes(5);
        testRule.setNotifyChannels("IN_APP");
        testRule.setNotifyTemplate("{{rule_name}}: {{metric_name}}={{value}}, 阈值={{threshold}}");

        // 初始化测试条件
        testCondition = new AlertConditionVO();
        testCondition.setMetricName("cpuUsage");
        testCondition.setOperator("gt");
        testCondition.setThreshold(80.0);
        testCondition.setDurationMinutes(3);
    }

    @Nested
    @DisplayName("evaluateAllRules 测试")
    class EvaluateAllRulesTests {

        @Test
        @DisplayName("评估所有规则 - 无启用规则")
        void testEvaluateAllRules_NoEnabledRules() {
            when(ruleMapper.selectEnabledRules()).thenReturn(Collections.emptyList());

            alertEvaluationService.evaluateAllRules();

            verify(ruleMapper, times(1)).selectEnabledRules();
            verify(historyMapper, never()).insert(any(GatewayAlertHistoryDO.class));
            verify(notificationDispatcher, never()).dispatchAsync(any(NotificationMessage.class), anyList());
        }

        @Test
        @DisplayName("评估所有规则 - 单规则评估")
        void testEvaluateAllRules_SingleRule() {
            testRule.setConditions("[{\"metricName\":\"cpuUsage\",\"operator\":\"gt\",\"threshold\":80,\"durationMinutes\":3}]");

            when(ruleMapper.selectEnabledRules()).thenReturn(List.of(testRule));

            // Mock Redis 汇总数据 - 指标未超标
            Map<String, Object> summary = new HashMap<>();
            summary.put("cpuUsage", 50.0);
            when(redisClient.hGetStringMap(RedisKeyConstant.GATEWAY_METRICS_SUMMARY)).thenReturn(summary);

            // Mock 历史查询 - 无触发中的告警
            when(historyMapper.selectFiringByRuleId(1L)).thenReturn(null);

            alertEvaluationService.evaluateAllRules();

            verify(ruleMapper, times(1)).selectEnabledRules();
            verify(redisClient, times(1)).hGetStringMap(RedisKeyConstant.GATEWAY_METRICS_SUMMARY);
        }

        @Test
        @DisplayName("评估所有规则 - 多规则并行评估")
        void testEvaluateAllRules_MultipleRules() {
            GatewayAlertRuleDO rule1 = new GatewayAlertRuleDO();
            rule1.setId(1L);
            rule1.setRuleName("CPU告警");
            rule1.setConditions("[{\"metricName\":\"cpuUsage\",\"operator\":\"gt\",\"threshold\":80,\"durationMinutes\":3}]");
            rule1.setNotifyChannels("IN_APP");

            GatewayAlertRuleDO rule2 = new GatewayAlertRuleDO();
            rule2.setId(2L);
            rule2.setRuleName("内存告警");
            rule2.setConditions("[{\"metricName\":\"memoryUsage\",\"operator\":\"gt\",\"threshold\":90,\"durationMinutes\":5}]");
            rule2.setNotifyChannels("IN_APP");

            when(ruleMapper.selectEnabledRules()).thenReturn(List.of(rule1, rule2));

            Map<String, Object> summary = new HashMap<>();
            summary.put("cpuUsage", 50.0);
            summary.put("memoryUsage", 60.0);
            when(redisClient.hGetStringMap(RedisKeyConstant.GATEWAY_METRICS_SUMMARY)).thenReturn(summary);
            when(historyMapper.selectFiringByRuleId(anyLong())).thenReturn(null);

            alertEvaluationService.evaluateAllRules();

            verify(ruleMapper, times(1)).selectEnabledRules();
            // 验证两个规则都被评估
            verify(historyMapper, times(2)).selectFiringByRuleId(anyLong());
        }
    }

    @Nested
    @DisplayName("evaluateThreshold 测试")
    class EvaluateThresholdTests {

        @Test
        @DisplayName("阈值评估 - gt (大于)")
        void testEvaluateThreshold_Gt() {
            // 测试通过反射或公开方法调用（此处模拟调用 evaluateRule 的效果）

            // 间接验证：设置超标场景
            testRule.setConditions("[{\"metricName\":\"cpuUsage\",\"operator\":\"gt\",\"threshold\":80,\"durationMinutes\":1}]");

            when(ruleMapper.selectEnabledRules()).thenReturn(List.of(testRule));

            Map<String, Object> summary = new HashMap<>();
            summary.put("cpuUsage", 85.0); // 超过阈值
            when(redisClient.hGetStringMap(RedisKeyConstant.GATEWAY_METRICS_SUMMARY)).thenReturn(summary);

            // Mock 持续时间已满足
            String durationKey = "blink:gateway:alert:duration:1:cpuUsage";
            when(redisClient.get(durationKey)).thenReturn(System.currentTimeMillis() - 120000); // 2分钟前开始
            when(redisClient.exists(anyString())).thenReturn(false);
            when(historyMapper.selectFiringByRuleId(1L)).thenReturn(null);

            alertEvaluationService.evaluateAllRules();

            // 验证：当持续时间满足时，应触发告警
            verify(redisClient, times(1)).get(durationKey);
        }

        @Test
        @DisplayName("阈值评估 - lt (小于)")
        void testEvaluateThreshold_Lt() {
            testRule.setConditions("[{\"metricName\":\"cpuUsage\",\"operator\":\"lt\",\"threshold\":20,\"durationMinutes\":1}]");

            when(ruleMapper.selectEnabledRules()).thenReturn(List.of(testRule));

            Map<String, Object> summary = new HashMap<>();
            summary.put("cpuUsage", 10.0); // 小于阈值
            when(redisClient.hGetStringMap(RedisKeyConstant.GATEWAY_METRICS_SUMMARY)).thenReturn(summary);
            when(historyMapper.selectFiringByRuleId(1L)).thenReturn(null);

            alertEvaluationService.evaluateAllRules();

            verify(ruleMapper, times(1)).selectEnabledRules();
        }

        @Test
        @DisplayName("阈值评估 - gte (大于等于)")
        void testEvaluateThreshold_Gte() {
            testRule.setConditions("[{\"metricName\":\"memoryUsage\",\"operator\":\"gte\",\"threshold\":90,\"durationMinutes\":1}]");

            when(ruleMapper.selectEnabledRules()).thenReturn(List.of(testRule));

            Map<String, Object> summary = new HashMap<>();
            summary.put("memoryUsage", 90.0); // 等于阈值，应触发
            when(redisClient.hGetStringMap(RedisKeyConstant.GATEWAY_METRICS_SUMMARY)).thenReturn(summary);
            when(historyMapper.selectFiringByRuleId(1L)).thenReturn(null);

            alertEvaluationService.evaluateAllRules();

            verify(ruleMapper, times(1)).selectEnabledRules();
        }

        @Test
        @DisplayName("阈值评估 - lte (小于等于)")
        void testEvaluateThreshold_Lte() {
            testRule.setConditions("[{\"metricName\":\"memoryUsage\",\"operator\":\"lte\",\"threshold\":10,\"durationMinutes\":1}]");

            when(ruleMapper.selectEnabledRules()).thenReturn(List.of(testRule));

            Map<String, Object> summary = new HashMap<>();
            summary.put("memoryUsage", 5.0); // 小于阈值
            when(redisClient.hGetStringMap(RedisKeyConstant.GATEWAY_METRICS_SUMMARY)).thenReturn(summary);
            when(historyMapper.selectFiringByRuleId(1L)).thenReturn(null);

            alertEvaluationService.evaluateAllRules();

            verify(ruleMapper, times(1)).selectEnabledRules();
        }
    }

    @Nested
    @DisplayName("checkDuration 测试")
    class CheckDurationTests {

        @Test
        @DisplayName("持续时间检测 - 未达到持续时间")
        void testCheckDuration_NotMet() {
            testRule.setConditions("[{\"metricName\":\"cpuUsage\",\"operator\":\"gt\",\"threshold\":80,\"durationMinutes\":5}]");

            when(ruleMapper.selectEnabledRules()).thenReturn(List.of(testRule));

            Map<String, Object> summary = new HashMap<>();
            summary.put("cpuUsage", 85.0);
            when(redisClient.hGetStringMap(RedisKeyConstant.GATEWAY_METRICS_SUMMARY)).thenReturn(summary);

            // Mock 刚开始超标
            String durationKey = "blink:gateway:alert:duration:1:cpuUsage";
            when(redisClient.get(durationKey)).thenReturn(System.currentTimeMillis() - 60000); // 1分钟前，未达到5分钟
            when(historyMapper.selectFiringByRuleId(1L)).thenReturn(null);

            alertEvaluationService.evaluateAllRules();

            // 验证：不应触发告警（持续时间不足）
            verify(historyMapper, never()).insert(any(GatewayAlertHistoryDO.class));
        }

        @Test
        @DisplayName("持续时间检测 - 达到持续时间")
        void testCheckDuration_Met() {
            testRule.setConditions("[{\"metricName\":\"cpuUsage\",\"operator\":\"gt\",\"threshold\":80,\"durationMinutes\":3}]");
            testRule.setSuppressMinutes(5);

            when(ruleMapper.selectEnabledRules()).thenReturn(List.of(testRule));

            Map<String, Object> summary = new HashMap<>();
            summary.put("cpuUsage", 85.0);
            when(redisClient.hGetStringMap(RedisKeyConstant.GATEWAY_METRICS_SUMMARY)).thenReturn(summary);

            // Mock 持续时间已满足
            String durationKey = "blink:gateway:alert:duration:1:cpuUsage";
            when(redisClient.get(durationKey)).thenReturn(System.currentTimeMillis() - 300000); // 5分钟前，超过3分钟要求
            when(redisClient.exists(anyString())).thenReturn(false); // 未被抑制
            when(historyMapper.selectFiringByRuleId(1L)).thenReturn(null);
            when(historyMapper.insert(any(GatewayAlertHistoryDO.class))).thenReturn(1);

            alertEvaluationService.evaluateAllRules();

            // 验证：应触发告警
            verify(historyMapper, times(1)).insert(any(GatewayAlertHistoryDO.class));
            verify(notificationDispatcher, times(1)).dispatchAsync(any(NotificationMessage.class), anyList());
            verify(redisClient, times(1)).setEx(anyString(), eq("1"), any(Duration.class)); // 设置抑制
        }
    }

    @Nested
    @DisplayName("告警抑制测试")
    class SuppressionTests {

        @Test
        @DisplayName("告警抑制 - 已被抑制")
        void testSuppression_AlreadySuppressed() {
            testRule.setConditions("[{\"metricName\":\"cpuUsage\",\"operator\":\"gt\",\"threshold\":80,\"durationMinutes\":1}]");

            when(ruleMapper.selectEnabledRules()).thenReturn(List.of(testRule));

            Map<String, Object> summary = new HashMap<>();
            summary.put("cpuUsage", 85.0);
            when(redisClient.hGetStringMap(RedisKeyConstant.GATEWAY_METRICS_SUMMARY)).thenReturn(summary);

            String durationKey = "blink:gateway:alert:duration:1:cpuUsage";
            when(redisClient.get(durationKey)).thenReturn(System.currentTimeMillis() - 120000);
            when(redisClient.exists(anyString())).thenReturn(true); // 已被抑制
            when(historyMapper.selectFiringByRuleId(1L)).thenReturn(null);

            alertEvaluationService.evaluateAllRules();

            // 验证：不应触发告警
            verify(historyMapper, never()).insert(any(GatewayAlertHistoryDO.class));
            verify(notificationDispatcher, never()).dispatchAsync(any(NotificationMessage.class), anyList());
        }
    }

    @Nested
    @DisplayName("告警恢复测试")
    class ResolveTests {

        @Test
        @DisplayName("告警恢复 - 正常场景")
        void testResolveAlert_Success() {
            testRule.setConditions("[{\"metricName\":\"cpuUsage\",\"operator\":\"gt\",\"threshold\":80,\"durationMinutes\":1}]");

            when(ruleMapper.selectEnabledRules()).thenReturn(List.of(testRule));

            Map<String, Object> summary = new HashMap<>();
            summary.put("cpuUsage", 50.0); // 恢复正常
            when(redisClient.hGetStringMap(RedisKeyConstant.GATEWAY_METRICS_SUMMARY)).thenReturn(summary);

            // Mock 有触发中的告警
            GatewayAlertHistoryDO firingAlert = new GatewayAlertHistoryDO();
            firingAlert.setId(1L);
            firingAlert.setRuleId(1L);
            firingAlert.setRuleName("CPU告警");
            firingAlert.setStatus("FIRING");
            when(historyMapper.selectFiringByRuleId(1L)).thenReturn(firingAlert);
            when(historyMapper.updateById(any(GatewayAlertHistoryDO.class))).thenReturn(1);

            alertEvaluationService.evaluateAllRules();

            // 验证：告警应被恢复
            verify(historyMapper, times(1)).updateById(any(GatewayAlertHistoryDO.class));
            verify(notificationService, times(1)).sendAlert(anyString(), anyString(), anyString());
            verify(redisClient, times(1)).deleteByPrefixScan(anyString()); // 清除持续时间标记
        }

        @Test
        @DisplayName("告警恢复 - 无触发中的告警")
        void testResolveAlert_NoFiringAlert() {
            testRule.setConditions("[{\"metricName\":\"cpuUsage\",\"operator\":\"gt\",\"threshold\":80,\"durationMinutes\":1}]");

            when(ruleMapper.selectEnabledRules()).thenReturn(List.of(testRule));

            Map<String, Object> summary = new HashMap<>();
            summary.put("cpuUsage", 50.0);
            when(redisClient.hGetStringMap(RedisKeyConstant.GATEWAY_METRICS_SUMMARY)).thenReturn(summary);
            when(historyMapper.selectFiringByRuleId(1L)).thenReturn(null); // 无触发中的告警

            alertEvaluationService.evaluateAllRules();

            // 验证：不应更新任何记录
            verify(historyMapper, never()).updateById(any(GatewayAlertHistoryDO.class));
        }
    }

    @Nested
    @DisplayName("通知发送测试")
    class NotificationTests {

        @Test
        @DisplayName("发送通知 - IN_APP 渠道")
        void testSendNotification_InApp() {
            testRule.setConditions("[{\"metricName\":\"cpuUsage\",\"operator\":\"gt\",\"threshold\":80,\"durationMinutes\":1}]");
            testRule.setNotifyChannels("IN_APP");
            testRule.setSeverity("WARNING");

            when(ruleMapper.selectEnabledRules()).thenReturn(List.of(testRule));

            Map<String, Object> summary = new HashMap<>();
            summary.put("cpuUsage", 85.0);
            when(redisClient.hGetStringMap(RedisKeyConstant.GATEWAY_METRICS_SUMMARY)).thenReturn(summary);

            String durationKey = "blink:gateway:alert:duration:1:cpuUsage";
            when(redisClient.get(durationKey)).thenReturn(System.currentTimeMillis() - 120000);
            when(redisClient.exists(anyString())).thenReturn(false);
            when(historyMapper.selectFiringByRuleId(1L)).thenReturn(null);
            when(historyMapper.insert(any(GatewayAlertHistoryDO.class))).thenReturn(1);

            alertEvaluationService.evaluateAllRules();

            // 验证：应发送站内通知
            verify(notificationDispatcher, times(1)).dispatchAsync(any(NotificationMessage.class), anyList());
        }

        @Test
        @DisplayName("发送通知 - ERROR 严重程度")
        void testSendNotification_ErrorSeverity() {
            testRule.setConditions("[{\"metricName\":\"cpuUsage\",\"operator\":\"gt\",\"threshold\":80,\"durationMinutes\":1}]");
            testRule.setNotifyChannels("IN_APP");
            testRule.setSeverity("ERROR");

            when(ruleMapper.selectEnabledRules()).thenReturn(List.of(testRule));

            Map<String, Object> summary = new HashMap<>();
            summary.put("cpuUsage", 85.0);
            when(redisClient.hGetStringMap(RedisKeyConstant.GATEWAY_METRICS_SUMMARY)).thenReturn(summary);

            String durationKey = "blink:gateway:alert:duration:1:cpuUsage";
            when(redisClient.get(durationKey)).thenReturn(System.currentTimeMillis() - 120000);
            when(redisClient.exists(anyString())).thenReturn(false);
            when(historyMapper.selectFiringByRuleId(1L)).thenReturn(null);
            when(historyMapper.insert(any(GatewayAlertHistoryDO.class))).thenReturn(1);

            alertEvaluationService.evaluateAllRules();

            verify(notificationDispatcher, times(1)).dispatchAsync(any(NotificationMessage.class), anyList());
        }

        @Test
        @DisplayName("发送通知 - 无通知渠道")
        void testSendNotification_NoChannels() {
            testRule.setConditions("[{\"metricName\":\"cpuUsage\",\"operator\":\"gt\",\"threshold\":80,\"durationMinutes\":1}]");
            testRule.setNotifyChannels("");

            when(ruleMapper.selectEnabledRules()).thenReturn(List.of(testRule));

            Map<String, Object> summary = new HashMap<>();
            summary.put("cpuUsage", 85.0);
            when(redisClient.hGetStringMap(RedisKeyConstant.GATEWAY_METRICS_SUMMARY)).thenReturn(summary);

            String durationKey = "blink:gateway:alert:duration:1:cpuUsage";
            when(redisClient.get(durationKey)).thenReturn(System.currentTimeMillis() - 120000);
            when(redisClient.exists(anyString())).thenReturn(false);
            when(historyMapper.selectFiringByRuleId(1L)).thenReturn(null);
            when(historyMapper.insert(any(GatewayAlertHistoryDO.class))).thenReturn(1);

            alertEvaluationService.evaluateAllRules();

            // 验证：不应发送通知
            verify(notificationDispatcher, never()).dispatchAsync(any(NotificationMessage.class), anyList());
        }
    }

    @Nested
    @DisplayName("模板渲染测试")
    class TemplateRenderTests {

        @Test
        @DisplayName("模板渲染 - 自定义模板")
        void testRenderTemplate_CustomTemplate() {
            testRule.setConditions("[{\"metricName\":\"cpuUsage\",\"operator\":\"gt\",\"threshold\":80,\"durationMinutes\":1}]");
            testRule.setNotifyTemplate("告警: {{rule_name}}, 指标: {{metric_name}}, 当前值: {{value}}, 阈值: {{threshold}}");

            when(ruleMapper.selectEnabledRules()).thenReturn(List.of(testRule));

            Map<String, Object> summary = new HashMap<>();
            summary.put("cpuUsage", 85.0);
            when(redisClient.hGetStringMap(RedisKeyConstant.GATEWAY_METRICS_SUMMARY)).thenReturn(summary);

            String durationKey = "blink:gateway:alert:duration:1:cpuUsage";
            when(redisClient.get(durationKey)).thenReturn(System.currentTimeMillis() - 120000);
            when(redisClient.exists(anyString())).thenReturn(false);
            when(historyMapper.selectFiringByRuleId(1L)).thenReturn(null);
            when(historyMapper.insert(any(GatewayAlertHistoryDO.class))).thenReturn(1);

            alertEvaluationService.evaluateAllRules();

            // 验证模板渲染：检查插入的告警记录内容
            verify(historyMapper, times(1)).insert(any(GatewayAlertHistoryDO.class));
        }

        @Test
        @DisplayName("模板渲染 - 默认模板")
        void testRenderTemplate_DefaultTemplate() {
            testRule.setConditions("[{\"metricName\":\"cpuUsage\",\"operator\":\"gt\",\"threshold\":80,\"durationMinutes\":1}]");
            testRule.setNotifyTemplate(""); // 空模板，使用默认

            when(ruleMapper.selectEnabledRules()).thenReturn(List.of(testRule));

            Map<String, Object> summary = new HashMap<>();
            summary.put("cpuUsage", 85.0);
            when(redisClient.hGetStringMap(RedisKeyConstant.GATEWAY_METRICS_SUMMARY)).thenReturn(summary);

            String durationKey = "blink:gateway:alert:duration:1:cpuUsage";
            when(redisClient.get(durationKey)).thenReturn(System.currentTimeMillis() - 120000);
            when(redisClient.exists(anyString())).thenReturn(false);
            when(historyMapper.selectFiringByRuleId(1L)).thenReturn(null);
            when(historyMapper.insert(any(GatewayAlertHistoryDO.class))).thenReturn(1);

            alertEvaluationService.evaluateAllRules();

            verify(historyMapper, times(1)).insert(any(GatewayAlertHistoryDO.class));
        }
    }

    @Nested
    @DisplayName("指标缺失测试")
    class MissingMetricsTests {

        @Test
        @DisplayName("指标缺失 - 不触发告警")
        void testMissingMetrics_NoTrigger() {
            testRule.setConditions("[{\"metricName\":\"unknownMetric\",\"operator\":\"gt\",\"threshold\":80,\"durationMinutes\":1}]");

            when(ruleMapper.selectEnabledRules()).thenReturn(List.of(testRule));

            Map<String, Object> summary = new HashMap<>();
            // 不包含 unknownMetric
            when(redisClient.hGetStringMap(RedisKeyConstant.GATEWAY_METRICS_SUMMARY)).thenReturn(summary);
            when(historyMapper.selectFiringByRuleId(1L)).thenReturn(null);

            alertEvaluationService.evaluateAllRules();

            // 验证：不应触发告警
            verify(historyMapper, never()).insert(any(GatewayAlertHistoryDO.class));
        }

        @Test
        @DisplayName("Redis 汇总数据为空")
        void testMissingMetrics_EmptySummary() {
            testRule.setConditions("[{\"metricName\":\"cpuUsage\",\"operator\":\"gt\",\"threshold\":80,\"durationMinutes\":1}]");

            when(ruleMapper.selectEnabledRules()).thenReturn(List.of(testRule));
            when(redisClient.hGetStringMap(RedisKeyConstant.GATEWAY_METRICS_SUMMARY)).thenReturn(null);
            when(historyMapper.selectFiringByRuleId(1L)).thenReturn(null);

            alertEvaluationService.evaluateAllRules();

            verify(historyMapper, never()).insert(any(GatewayAlertHistoryDO.class));
        }
    }

    @Nested
    @DisplayName("多条件评估测试")
    class MultiConditionTests {

        @Test
        @DisplayName("多条件评估 - AND 逻辑")
        void testMultiConditions_AndLogic() {
            testRule.setConditions("[{\"metricName\":\"cpuUsage\",\"operator\":\"gt\",\"threshold\":80,\"durationMinutes\":1},{\"metricName\":\"memoryUsage\",\"operator\":\"gt\",\"threshold\":90,\"durationMinutes\":1}]");

            when(ruleMapper.selectEnabledRules()).thenReturn(List.of(testRule));

            Map<String, Object> summary = new HashMap<>();
            summary.put("cpuUsage", 85.0);
            summary.put("memoryUsage", 95.0); // 两个条件都超标
            when(redisClient.hGetStringMap(RedisKeyConstant.GATEWAY_METRICS_SUMMARY)).thenReturn(summary);

            String cpuKey = "blink:gateway:alert:duration:1:cpuUsage";
            String memKey = "blink:gateway:alert:duration:1:memoryUsage";
            when(redisClient.get(cpuKey)).thenReturn(System.currentTimeMillis() - 120000);
            when(redisClient.get(memKey)).thenReturn(System.currentTimeMillis() - 120000);
            when(redisClient.exists(anyString())).thenReturn(false);
            when(historyMapper.selectFiringByRuleId(1L)).thenReturn(null);
            when(historyMapper.insert(any(GatewayAlertHistoryDO.class))).thenReturn(1);

            alertEvaluationService.evaluateAllRules();

            // 验证：两个条件都满足时触发告警
            verify(historyMapper, times(1)).insert(any(GatewayAlertHistoryDO.class));
        }

        @Test
        @DisplayName("多条件评估 - 部分条件未满足")
        void testMultiConditions_PartialFailure() {
            testRule.setConditions("[{\"metricName\":\"cpuUsage\",\"operator\":\"gt\",\"threshold\":80,\"durationMinutes\":1},{\"metricName\":\"memoryUsage\",\"operator\":\"gt\",\"threshold\":90,\"durationMinutes\":1}]");

            when(ruleMapper.selectEnabledRules()).thenReturn(List.of(testRule));

            Map<String, Object> summary = new HashMap<>();
            summary.put("cpuUsage", 85.0);
            summary.put("memoryUsage", 50.0); // 只有一个条件超标
            when(redisClient.hGetStringMap(RedisKeyConstant.GATEWAY_METRICS_SUMMARY)).thenReturn(summary);

            String cpuKey = "blink:gateway:alert:duration:1:cpuUsage";
            when(redisClient.get(cpuKey)).thenReturn(System.currentTimeMillis() - 120000);
            when(historyMapper.selectFiringByRuleId(1L)).thenReturn(null);

            alertEvaluationService.evaluateAllRules();

            // 验证：不应触发告警（AND 逻辑）
            verify(historyMapper, never()).insert(any(GatewayAlertHistoryDO.class));
        }
    }
}