package com.blink.gateway.admin.controller;

import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.GetCircuitBreakerDetailReq;
import com.blink.gateway.admin.dto.req.GetCircuitBreakerHistoryReq;
import com.blink.gateway.admin.dto.req.GetCircuitBreakerOverviewReq;
import com.blink.gateway.admin.dto.rsp.CircuitBreakerDetailRsp;
import com.blink.gateway.admin.dto.rsp.CircuitBreakerOverviewRsp;
import com.blink.gateway.admin.dto.rsp.InstanceSummaryRsp;
import com.blink.gateway.admin.dto.rsp.StateTransitionHistoryRsp;
import com.blink.gateway.admin.service.CircuitBreakerService;
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

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CircuitBreakerController 单元测试类
 *
 * @author binblink
 * @since 2026-04-15
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CircuitBreakerController 单元测试")
class CircuitBreakerControllerTest {

    @Mock
    private CircuitBreakerService circuitBreakerService;

    @InjectMocks
    private CircuitBreakerController circuitBreakerController;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    @Nested
    @DisplayName("getInstanceList 测试")
    class GetInstanceListTests {

        @Test
        @DisplayName("获取实例列表 - 正常场景")
        void testGetInstanceList_Success() {
            RequestDTO<Void> requestDTO = new RequestDTO<>();

            InstanceSummaryRsp instance = new InstanceSummaryRsp();
            instance.setInstanceId("instance-001");
            instance.setHost("192.168.1.1");
            instance.setPort(8002);
            instance.setStatus("ONLINE");

            when(circuitBreakerService.getInstanceList()).thenReturn(List.of(instance));

            ResponseDTO<List<InstanceSummaryRsp>> response = circuitBreakerController.getInstanceList(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().size());
            assertEquals("instance-001", response.getBody().get(0).getInstanceId());

            verify(circuitBreakerService, times(1)).getInstanceList();
        }

        @Test
        @DisplayName("获取实例列表 - 空列表")
        void testGetInstanceList_EmptyList() {
            RequestDTO<Void> requestDTO = new RequestDTO<>();

            when(circuitBreakerService.getInstanceList()).thenReturn(Collections.emptyList());

            ResponseDTO<List<InstanceSummaryRsp>> response = circuitBreakerController.getInstanceList(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            assertEquals(0, response.getBody().size());

            verify(circuitBreakerService, times(1)).getInstanceList();
        }

        @Test
        @DisplayName("获取实例列表 - 异常处理")
        void testGetInstanceList_Exception() {
            RequestDTO<Void> requestDTO = new RequestDTO<>();

            when(circuitBreakerService.getInstanceList())
                .thenThrow(new RuntimeException("Service error"));

            ResponseDTO<List<InstanceSummaryRsp>> response = circuitBreakerController.getInstanceList(requestDTO);

            assertNotNull(response);
            assertNotNull(response.getBody());
            assertEquals(0, response.getBody().size());

            verify(circuitBreakerService, times(1)).getInstanceList();
        }
    }

    @Nested
    @DisplayName("getOverview 测试")
    class GetOverviewTests {

        @Test
        @DisplayName("获取熔断器总览 - 正常场景")
        void testGetOverview_Success() {
            RequestDTO<GetCircuitBreakerOverviewReq> requestDTO = new RequestDTO<>();
            GetCircuitBreakerOverviewReq req = new GetCircuitBreakerOverviewReq();
            requestDTO.setBody(req);

            CircuitBreakerOverviewRsp overview = new CircuitBreakerOverviewRsp();
            overview.setTotalCircuitBreakers(7);
            overview.setTotalInstances(1);
            overview.setOpenCount(0);
            overview.setClosedCount(7);
            overview.setHalfOpenCount(0);
            overview.setHealthScore(100.0);

            when(circuitBreakerService.getOverview(any(GetCircuitBreakerOverviewReq.class))).thenReturn(overview);

            ResponseDTO<CircuitBreakerOverviewRsp> response = circuitBreakerController.getOverview(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            assertEquals(7, response.getBody().getTotalCircuitBreakers());
            assertEquals(1, response.getBody().getTotalInstances());

            verify(circuitBreakerService, times(1)).getOverview(any(GetCircuitBreakerOverviewReq.class));
        }

        @Test
        @DisplayName("获取熔断器总览 - 指定实例ID")
        void testGetOverview_WithInstanceId() {
            RequestDTO<GetCircuitBreakerOverviewReq> requestDTO = new RequestDTO<>();
            GetCircuitBreakerOverviewReq req = new GetCircuitBreakerOverviewReq();
            req.setInstanceId("instance-001");
            requestDTO.setBody(req);

            CircuitBreakerOverviewRsp overview = new CircuitBreakerOverviewRsp();
            overview.setTotalCircuitBreakers(5);
            overview.setTotalInstances(1);

            when(circuitBreakerService.getOverview(any(GetCircuitBreakerOverviewReq.class))).thenReturn(overview);

            ResponseDTO<CircuitBreakerOverviewRsp> response = circuitBreakerController.getOverview(requestDTO);

            assertNotNull(response);
            assertEquals(5, response.getBody().getTotalCircuitBreakers());

            verify(circuitBreakerService, times(1)).getOverview(any(GetCircuitBreakerOverviewReq.class));
        }

        @Test
        @DisplayName("获取熔断器总览 - body为空")
        void testGetOverview_NullBody() {
            RequestDTO<GetCircuitBreakerOverviewReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(null);

            CircuitBreakerOverviewRsp overview = new CircuitBreakerOverviewRsp();
            overview.setTotalCircuitBreakers(0);
            overview.setTotalInstances(0);

            when(circuitBreakerService.getOverview(any(GetCircuitBreakerOverviewReq.class))).thenReturn(overview);

            ResponseDTO<CircuitBreakerOverviewRsp> response = circuitBreakerController.getOverview(requestDTO);

            assertNotNull(response);
            assertNotNull(response.getBody());

            verify(circuitBreakerService, times(1)).getOverview(any(GetCircuitBreakerOverviewReq.class));
        }

        @Test
        @DisplayName("获取熔断器总览 - 异常处理")
        void testGetOverview_Exception() {
            RequestDTO<GetCircuitBreakerOverviewReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(new GetCircuitBreakerOverviewReq());

            when(circuitBreakerService.getOverview(any(GetCircuitBreakerOverviewReq.class)))
                .thenThrow(new RuntimeException("Service error"));

            ResponseDTO<CircuitBreakerOverviewRsp> response = circuitBreakerController.getOverview(requestDTO);

            assertNotNull(response);
            assertNotNull(response.getBody());

            verify(circuitBreakerService, times(1)).getOverview(any(GetCircuitBreakerOverviewReq.class));
        }
    }

    @Nested
    @DisplayName("getDetail 测试")
    class GetDetailTests {

        @Test
        @DisplayName("获取熔断器详情 - 正常场景")
        void testGetDetail_Success() {
            RequestDTO<GetCircuitBreakerDetailReq> requestDTO = new RequestDTO<>();
            GetCircuitBreakerDetailReq req = new GetCircuitBreakerDetailReq();
            req.setName("myCircuitBreaker");
            requestDTO.setBody(req);

            CircuitBreakerDetailRsp detail = new CircuitBreakerDetailRsp();
            detail.setInstances(Collections.emptyList());

            when(circuitBreakerService.getDetail(any(GetCircuitBreakerDetailReq.class))).thenReturn(detail);

            ResponseDTO<CircuitBreakerDetailRsp> response = circuitBreakerController.getDetail(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());

            verify(circuitBreakerService, times(1)).getDetail(any(GetCircuitBreakerDetailReq.class));
        }

        @Test
        @DisplayName("获取熔断器详情 - name为空")
        void testGetDetail_EmptyName() {
            RequestDTO<GetCircuitBreakerDetailReq> requestDTO = new RequestDTO<>();
            GetCircuitBreakerDetailReq req = new GetCircuitBreakerDetailReq();
            req.setName(null);
            requestDTO.setBody(req);

            ResponseDTO<CircuitBreakerDetailRsp> response = circuitBreakerController.getDetail(requestDTO);

            assertNotNull(response);
            assertNotNull(response.getBody());

            verify(circuitBreakerService, never()).getDetail(any());
        }

        @Test
        @DisplayName("获取熔断器详情 - body为空")
        void testGetDetail_NullBody() {
            RequestDTO<GetCircuitBreakerDetailReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(null);

            ResponseDTO<CircuitBreakerDetailRsp> response = circuitBreakerController.getDetail(requestDTO);

            assertNotNull(response);
            assertNotNull(response.getBody());

            verify(circuitBreakerService, never()).getDetail(any());
        }

        @Test
        @DisplayName("获取熔断器详情 - 异常处理")
        void testGetDetail_Exception() {
            RequestDTO<GetCircuitBreakerDetailReq> requestDTO = new RequestDTO<>();
            GetCircuitBreakerDetailReq req = new GetCircuitBreakerDetailReq();
            req.setName("myCircuitBreaker");
            requestDTO.setBody(req);

            when(circuitBreakerService.getDetail(any(GetCircuitBreakerDetailReq.class)))
                .thenThrow(new RuntimeException("Service error"));

            ResponseDTO<CircuitBreakerDetailRsp> response = circuitBreakerController.getDetail(requestDTO);

            assertNotNull(response);
            assertNotNull(response.getBody());

            verify(circuitBreakerService, times(1)).getDetail(any(GetCircuitBreakerDetailReq.class));
        }
    }

    @Nested
    @DisplayName("getHistory 测试")
    class GetHistoryTests {

        @Test
        @DisplayName("获取状态转换历史 - 正常场景")
        void testGetHistory_Success() {
            RequestDTO<GetCircuitBreakerHistoryReq> requestDTO = new RequestDTO<>();
            GetCircuitBreakerHistoryReq req = new GetCircuitBreakerHistoryReq();
            req.setInstanceId("instance-001");
            req.setName("myCircuitBreaker");
            requestDTO.setBody(req);

            StateTransitionHistoryRsp history = new StateTransitionHistoryRsp();
            history.setFromState("CLOSED");
            history.setToState("OPEN");

            when(circuitBreakerService.getHistory(any(GetCircuitBreakerHistoryReq.class)))
                .thenReturn(List.of(history));

            ResponseDTO<List<StateTransitionHistoryRsp>> response = circuitBreakerController.getHistory(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().size());
            assertEquals("CLOSED", response.getBody().get(0).getFromState());
            assertEquals("OPEN", response.getBody().get(0).getToState());

            verify(circuitBreakerService, times(1)).getHistory(any(GetCircuitBreakerHistoryReq.class));
        }

        @Test
        @DisplayName("获取状态转换历史 - 缺少参数")
        void testGetHistory_MissingParams() {
            RequestDTO<GetCircuitBreakerHistoryReq> requestDTO = new RequestDTO<>();
            GetCircuitBreakerHistoryReq req = new GetCircuitBreakerHistoryReq();
            req.setInstanceId(null);
            req.setName("myCircuitBreaker");
            requestDTO.setBody(req);

            ResponseDTO<List<StateTransitionHistoryRsp>> response = circuitBreakerController.getHistory(requestDTO);

            assertNotNull(response);
            assertNotNull(response.getBody());
            assertEquals(0, response.getBody().size());

            verify(circuitBreakerService, never()).getHistory(any());
        }

        @Test
        @DisplayName("获取状态转换历史 - body为空")
        void testGetHistory_NullBody() {
            RequestDTO<GetCircuitBreakerHistoryReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(null);

            ResponseDTO<List<StateTransitionHistoryRsp>> response = circuitBreakerController.getHistory(requestDTO);

            assertNotNull(response);
            assertNotNull(response.getBody());
            assertEquals(0, response.getBody().size());

            verify(circuitBreakerService, never()).getHistory(any());
        }

        @Test
        @DisplayName("获取状态转换历史 - 异常处理")
        void testGetHistory_Exception() {
            RequestDTO<GetCircuitBreakerHistoryReq> requestDTO = new RequestDTO<>();
            GetCircuitBreakerHistoryReq req = new GetCircuitBreakerHistoryReq();
            req.setInstanceId("instance-001");
            req.setName("myCircuitBreaker");
            requestDTO.setBody(req);

            when(circuitBreakerService.getHistory(any(GetCircuitBreakerHistoryReq.class)))
                .thenThrow(new RuntimeException("Service error"));

            ResponseDTO<List<StateTransitionHistoryRsp>> response = circuitBreakerController.getHistory(requestDTO);

            assertNotNull(response);
            assertNotNull(response.getBody());
            assertEquals(0, response.getBody().size());

            verify(circuitBreakerService, times(1)).getHistory(any(GetCircuitBreakerHistoryReq.class));
        }
    }
}