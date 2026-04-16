package com.blink.gateway.admin.controller;

import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.data.EmptyBody;
import com.blink.gateway.admin.dto.req.*;
import com.blink.gateway.admin.dto.rsp.*;
import com.blink.gateway.admin.dto.vo.AlertConditionVO;
import com.blink.gateway.admin.mapper.GatewayAlertHistoryMapper;
import com.blink.gateway.admin.mapper.GatewayAlertRuleMapper;
import com.blink.gateway.admin.entity.GatewayAlertRuleDO;
import com.blink.gateway.admin.entity.GatewayAlertHistoryDO;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * AlertController 单元测试类
 *
 * @author binblink
 * @since 2026-04-15
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AlertController 单元测试")
class AlertControllerTest {

    @Mock
    private GatewayAlertRuleMapper ruleMapper;

    @Mock
    private GatewayAlertHistoryMapper historyMapper;

    @InjectMocks
    private AlertController alertController;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    // ========== 告警规则管理测试 ==========

    @Nested
    @DisplayName("getRules 测试")
    class GetRulesTests {

        @Test
        @DisplayName("查询告警规则列表 - 正常场景")
        void testGetRules_Success() {
            QueryAlertRuleReq req = new QueryAlertRuleReq();
            req.setPageNum(1);
            req.setPageSize(10);

            RequestDTO<QueryAlertRuleReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            // Mock 分页查询结果
            GatewayAlertRuleDO ruleDO = new GatewayAlertRuleDO();
            ruleDO.setId(1L);
            ruleDO.setRuleName("CPU告警");
            ruleDO.setRuleType("RESOURCE");
            ruleDO.setSeverity("WARNING");
            ruleDO.setConditions("[{\"metricName\":\"cpuUsage\",\"operator\":\"gt\",\"threshold\":80,\"durationMinutes\":3}]");
            ruleDO.setNotifyChannels("IN_APP");
            ruleDO.setEnabled((byte) 1);

            when(ruleMapper.selectPage(any(), any())).thenReturn(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<GatewayAlertRuleDO>(1, 10, 1).setRecords(List.of(ruleDO))
            );

            ResponseDTO<AlertRuleListRsp> response = alertController.getRules(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().getTotal());
            assertEquals(1, response.getBody().getRules().size());

            verify(ruleMapper, times(1)).selectPage(any(), any());
        }

        @Test
        @DisplayName("查询告警规则列表 - 按类型筛选")
        void testGetRules_WithFilter() {
            QueryAlertRuleReq req = new QueryAlertRuleReq();
            req.setPageNum(1);
            req.setPageSize(10);
            req.setRuleType("RESOURCE");

            RequestDTO<QueryAlertRuleReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(ruleMapper.selectPage(any(), any())).thenReturn(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<GatewayAlertRuleDO>(1, 10, 0).setRecords(List.of())
            );

            ResponseDTO<AlertRuleListRsp> response = alertController.getRules(requestDTO);

            assertNotNull(response);
            assertEquals(0, response.getBody().getTotal());
            assertTrue(response.getBody().getRules().isEmpty());

            verify(ruleMapper, times(1)).selectPage(any(), any());
        }

        @Test
        @DisplayName("查询告警规则列表 - 空结果")
        void testGetRules_EmptyResult() {
            QueryAlertRuleReq req = new QueryAlertRuleReq();
            req.setPageNum(1);
            req.setPageSize(10);

            RequestDTO<QueryAlertRuleReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(ruleMapper.selectPage(any(), any())).thenReturn(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<GatewayAlertRuleDO>(1, 10, 0).setRecords(List.of())
            );

            ResponseDTO<AlertRuleListRsp> response = alertController.getRules(requestDTO);

            assertNotNull(response);
            assertEquals(0, response.getBody().getTotal());
            assertTrue(response.getBody().getRules().isEmpty());
        }
    }

    @Nested
    @DisplayName("addRule 测试")
    class AddRuleTests {

        @Test
        @DisplayName("新增告警规则 - 正常场景")
        void testAddRule_Success() {
            AddAlertRuleReq req = new AddAlertRuleReq();
            req.setRuleName("内存告警");
            req.setRuleType("RESOURCE");
            req.setSeverity("WARNING");
            AddAlertRuleReq.AlertConditionReq condition = new AddAlertRuleReq.AlertConditionReq();
            condition.setMetricName("memoryUsage");
            condition.setOperator("gt");
            condition.setThreshold(80.0);
            condition.setDurationMinutes(3);
            req.setConditions(List.of(condition));
            req.setNotifyChannels(List.of("IN_APP"));

            RequestDTO<AddAlertRuleReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(ruleMapper.insert(any(GatewayAlertRuleDO.class))).thenReturn(1);

            ResponseDTO<EmptyBody> response = alertController.addRule(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());

            verify(ruleMapper, times(1)).insert(any(GatewayAlertRuleDO.class));
        }

        @Test
        @DisplayName("新增告警规则 - 规则名称为空")
        void testAddRule_EmptyRuleName() {
            AddAlertRuleReq req = new AddAlertRuleReq();
            req.setRuleName("");
            req.setRuleType("RESOURCE");

            RequestDTO<AddAlertRuleReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            assertThrows(com.blink.framework.common.exception.BlinkException.class,
                () -> alertController.addRule(requestDTO));

            verify(ruleMapper, never()).insert(any(GatewayAlertRuleDO.class));
        }

        @Test
        @DisplayName("新增告警规则 - 条件为空")
        void testAddRule_EmptyConditions() {
            AddAlertRuleReq req = new AddAlertRuleReq();
            req.setRuleName("测试规则");
            req.setRuleType("RESOURCE");
            req.setSeverity("WARNING");
            req.setConditions(List.of());

            RequestDTO<AddAlertRuleReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            assertThrows(com.blink.framework.common.exception.BlinkException.class,
                () -> alertController.addRule(requestDTO));

            verify(ruleMapper, never()).insert(any(GatewayAlertRuleDO.class));
        }
    }

    @Nested
    @DisplayName("updateRule 测试")
    class UpdateRuleTests {

        @Test
        @DisplayName("更新告警规则 - 正常场景")
        void testUpdateRule_Success() {
            AddAlertRuleReq req = new AddAlertRuleReq();
            req.setId(1L);
            req.setRuleName("更新后的规则");
            req.setRuleType("PERFORMANCE");
            req.setSeverity("ERROR");
            AddAlertRuleReq.AlertConditionReq condition2 = new AddAlertRuleReq.AlertConditionReq();
            condition2.setMetricName("p99ResponseTime");
            condition2.setOperator("gt");
            condition2.setThreshold(500.0);
            condition2.setDurationMinutes(5);
            req.setConditions(List.of(condition2));
            req.setNotifyChannels(List.of("IN_APP", "EMAIL"));

            RequestDTO<AddAlertRuleReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            // Mock 查询原规则
            GatewayAlertRuleDO existingRule = new GatewayAlertRuleDO();
            existingRule.setId(1L);
            existingRule.setRuleName("原规则");
            when(ruleMapper.selectById(1L)).thenReturn(existingRule);
            when(ruleMapper.updateById(any(GatewayAlertRuleDO.class))).thenReturn(1);

            ResponseDTO<EmptyBody> response = alertController.updateRule(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());

            verify(ruleMapper, times(1)).selectById(1L);
            verify(ruleMapper, times(1)).updateById(any(GatewayAlertRuleDO.class));
        }

        @Test
        @DisplayName("更新告警规则 - ID为空")
        void testUpdateRule_NullId() {
            AddAlertRuleReq req = new AddAlertRuleReq();
            req.setRuleName("更新规则");
            req.setRuleType("RESOURCE");

            RequestDTO<AddAlertRuleReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            assertThrows(com.blink.framework.common.exception.BlinkException.class,
                () -> alertController.updateRule(requestDTO));

            verify(ruleMapper, never()).updateById(any(GatewayAlertRuleDO.class));
        }

        @Test
        @DisplayName("更新告警规则 - 规则不存在")
        void testUpdateRule_RuleNotExist() {
            AddAlertRuleReq req = new AddAlertRuleReq();
            req.setId(999L);
            req.setRuleName("更新规则");
            req.setRuleType("RESOURCE");
            req.setSeverity("WARNING");
            AddAlertRuleReq.AlertConditionReq condition3 = new AddAlertRuleReq.AlertConditionReq();
            condition3.setMetricName("cpuUsage");
            condition3.setOperator("gt");
            condition3.setThreshold(80.0);
            condition3.setDurationMinutes(3);
            req.setConditions(List.of(condition3));

            RequestDTO<AddAlertRuleReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(ruleMapper.selectById(999L)).thenReturn(null);

            assertThrows(com.blink.framework.common.exception.BlinkException.class,
                () -> alertController.updateRule(requestDTO));

            verify(ruleMapper, times(1)).selectById(999L);
            verify(ruleMapper, never()).updateById(any(GatewayAlertRuleDO.class));
        }
    }

    @Nested
    @DisplayName("deleteRule 测试")
    class DeleteRuleTests {

        @Test
        @DisplayName("删除告警规则 - 正常场景")
        void testDeleteRule_Success() {
            AlertRuleIdReq req = new AlertRuleIdReq();
            req.setId(1L);

            RequestDTO<AlertRuleIdReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(ruleMapper.deleteById(1L)).thenReturn(1);

            ResponseDTO<EmptyBody> response = alertController.deleteRule(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());

            verify(ruleMapper, times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("删除告警规则 - ID为空")
        void testDeleteRule_NullId() {
            AlertRuleIdReq req = new AlertRuleIdReq();

            RequestDTO<AlertRuleIdReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            assertThrows(com.blink.framework.common.exception.BlinkException.class,
                () -> alertController.deleteRule(requestDTO));

            verify(ruleMapper, never()).deleteById(anyInt());
        }
    }

    @Nested
    @DisplayName("toggleRule 测试")
    class ToggleRuleTests {

        @Test
        @DisplayName("切换告警规则状态 - 启用")
        void testToggleRule_Enable() {
            ToggleAlertRuleReq req = new ToggleAlertRuleReq();
            req.setId(1L);
            req.setEnabled((byte) 1);

            RequestDTO<ToggleAlertRuleReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            GatewayAlertRuleDO rule = new GatewayAlertRuleDO();
            rule.setId(1L);
            rule.setEnabled((byte) 0);
            when(ruleMapper.selectById(1L)).thenReturn(rule);
            when(ruleMapper.updateById(any(GatewayAlertRuleDO.class))).thenReturn(1);

            ResponseDTO<EmptyBody> response = alertController.toggleRule(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());

            verify(ruleMapper, times(1)).selectById(1L);
            verify(ruleMapper, times(1)).updateById(any(GatewayAlertRuleDO.class));
        }

        @Test
        @DisplayName("切换告警规则状态 - 禁用")
        void testToggleRule_Disable() {
            ToggleAlertRuleReq req = new ToggleAlertRuleReq();
            req.setId(1L);
            req.setEnabled((byte) 0);

            RequestDTO<ToggleAlertRuleReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            GatewayAlertRuleDO rule = new GatewayAlertRuleDO();
            rule.setId(1L);
            rule.setEnabled((byte) 1);
            when(ruleMapper.selectById(1L)).thenReturn(rule);
            when(ruleMapper.updateById(any(GatewayAlertRuleDO.class))).thenReturn(1);

            ResponseDTO<EmptyBody> response = alertController.toggleRule(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());

            verify(ruleMapper, times(1)).selectById(1L);
            verify(ruleMapper, times(1)).updateById(any(GatewayAlertRuleDO.class));
        }

        @Test
        @DisplayName("切换告警规则状态 - 规则不存在")
        void testToggleRule_RuleNotExist() {
            ToggleAlertRuleReq req = new ToggleAlertRuleReq();
            req.setId(999L);
            req.setEnabled((byte) 1);

            RequestDTO<ToggleAlertRuleReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(ruleMapper.selectById(999L)).thenReturn(null);

            assertThrows(com.blink.framework.common.exception.BlinkException.class,
                () -> alertController.toggleRule(requestDTO));

            verify(ruleMapper, times(1)).selectById(999L);
            verify(ruleMapper, never()).updateById(any(GatewayAlertRuleDO.class));
        }
    }

    // ========== 告警历史管理测试 ==========

    @Nested
    @DisplayName("getHistory 测试")
    class GetHistoryTests {

        @Test
        @DisplayName("查询告警历史 - 正常场景")
        void testGetHistory_Success() {
            QueryAlertHistoryReq req = new QueryAlertHistoryReq();
            req.setPageNum(1);
            req.setPageSize(10);

            RequestDTO<QueryAlertHistoryReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            GatewayAlertHistoryDO historyDO = new GatewayAlertHistoryDO();
            historyDO.setId(1L);
            historyDO.setRuleId(1L);
            historyDO.setRuleName("CPU告警");
            historyDO.setInstanceId("instance-001");
            historyDO.setAlertTitle("CPU使用率超过阈值");
            historyDO.setSeverity("WARNING");
            historyDO.setStatus("FIRING");

            when(historyMapper.selectPage(any(), any())).thenReturn(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<GatewayAlertHistoryDO>(1, 10, 1).setRecords(List.of(historyDO))
            );

            ResponseDTO<AlertHistoryListRsp> response = alertController.getHistory(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().getTotal());

            verify(historyMapper, times(1)).selectPage(any(), any());
        }

        @Test
        @DisplayName("查询告警历史 - 按状态筛选")
        void testGetHistory_WithStatusFilter() {
            QueryAlertHistoryReq req = new QueryAlertHistoryReq();
            req.setPageNum(1);
            req.setPageSize(10);
            req.setStatus("FIRING");

            RequestDTO<QueryAlertHistoryReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(historyMapper.selectPage(any(), any())).thenReturn(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<GatewayAlertHistoryDO>(1, 10, 0).setRecords(List.of())
            );

            ResponseDTO<AlertHistoryListRsp> response = alertController.getHistory(requestDTO);

            assertNotNull(response);
            assertEquals(0, response.getBody().getTotal());

            verify(historyMapper, times(1)).selectPage(any(), any());
        }
    }

    @Nested
    @DisplayName("getFiring 测试")
    class GetFiringTests {

        @Test
        @DisplayName("获取触发中告警 - 正常场景")
        void testGetFiring_Success() {
            RequestDTO<Void> requestDTO = new RequestDTO<>();

            GatewayAlertHistoryDO firingAlert = new GatewayAlertHistoryDO();
            firingAlert.setId(1L);
            firingAlert.setRuleName("CPU告警");
            firingAlert.setStatus("FIRING");

            when(historyMapper.selectFiringAlerts()).thenReturn(List.of(firingAlert));

            ResponseDTO<List<AlertHistoryRsp>> response = alertController.getFiring(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().size());

            verify(historyMapper, times(1)).selectFiringAlerts();
        }

        @Test
        @DisplayName("获取触发中告警 - 无告警")
        void testGetFiring_NoAlerts() {
            RequestDTO<Void> requestDTO = new RequestDTO<>();

            when(historyMapper.selectFiringAlerts()).thenReturn(List.of());

            ResponseDTO<List<AlertHistoryRsp>> response = alertController.getFiring(requestDTO);

            assertNotNull(response);
            assertTrue(response.getBody().isEmpty());

            verify(historyMapper, times(1)).selectFiringAlerts();
        }
    }

    @Nested
    @DisplayName("acknowledge 测试")
    class AcknowledgeTests {

        @Test
        @DisplayName("确认告警 - 正常场景")
        void testAcknowledge_Success() {
            AcknowledgeAlertReq req = new AcknowledgeAlertReq();
            req.setId(1L);

            RequestDTO<AcknowledgeAlertReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);
            requestDTO.setUserId("1");

            when(historyMapper.acknowledge(1L, 1)).thenReturn(1);

            ResponseDTO<EmptyBody> response = alertController.acknowledge(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());

            verify(historyMapper, times(1)).acknowledge(1L, 1);
        }

        @Test
        @DisplayName("确认告警 - ID为空")
        void testAcknowledge_NullId() {
            AcknowledgeAlertReq req = new AcknowledgeAlertReq();

            RequestDTO<AcknowledgeAlertReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            assertThrows(com.blink.framework.common.exception.BlinkException.class,
                () -> alertController.acknowledge(requestDTO));

            verify(historyMapper, never()).acknowledge(anyLong(), anyInt());
        }

        @Test
        @DisplayName("确认告警 - 历史不存在")
        void testAcknowledge_HistoryNotExist() {
            AcknowledgeAlertReq req = new AcknowledgeAlertReq();
            req.setId(999L);

            RequestDTO<AcknowledgeAlertReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);
            requestDTO.setUserId("1");

            when(historyMapper.acknowledge(999L, 1)).thenReturn(0);

            assertThrows(com.blink.framework.common.exception.BlinkException.class,
                () -> alertController.acknowledge(requestDTO));

            verify(historyMapper, times(1)).acknowledge(999L, 1);
        }
    }
}