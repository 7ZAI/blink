package com.blink.gateway.admin.controller;

import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.GetGatewayMetricsReq;
import com.blink.gateway.admin.dto.req.QueryGatewayInstanceReq;
import com.blink.gateway.admin.dto.req.QueryHealthStatusReq;
import com.blink.gateway.admin.dto.req.QueryStatisticsReq;
import com.blink.gateway.admin.dto.rsp.GatewayHealthStatusRsp;
import com.blink.gateway.admin.dto.rsp.GatewayInstanceListRsp;
import com.blink.gateway.admin.dto.rsp.GatewayMetricsRsp;
import com.blink.gateway.admin.dto.rsp.GatewayStatisticsRsp;
import com.blink.gateway.admin.service.MonitorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * MonitorController 单元测试类
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MonitorController 单元测试")
class MonitorControllerTest {

    @Mock
    private MonitorService monitorService;

    @InjectMocks
    private MonitorController monitorController;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    @Nested
    @DisplayName("getGatewayInstances 测试")
    class GetGatewayInstancesTests {

        @Test
        @DisplayName("获取网关实例列表 - 正常场景")
        void testGetGatewayInstances_Success() {
            QueryGatewayInstanceReq req = new QueryGatewayInstanceReq();

            RequestDTO<QueryGatewayInstanceReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            GatewayInstanceListRsp rsp = new GatewayInstanceListRsp();
            when(monitorService.getGatewayInstances(any(QueryGatewayInstanceReq.class))).thenReturn(ResponseDTO.newSuccessInstance(rsp));

            ResponseDTO<GatewayInstanceListRsp> response = monitorController.getGatewayInstances(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(monitorService, times(1)).getGatewayInstances(any(QueryGatewayInstanceReq.class));
        }
    }

    @Nested
    @DisplayName("getStatistics 测试")
    class GetStatisticsTests {

        @Test
        @DisplayName("获取网关统计数据 - 正常场景")
        void testGetStatistics_Success() {
            QueryStatisticsReq req = new QueryStatisticsReq();

            RequestDTO<QueryStatisticsReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            GatewayStatisticsRsp rsp = new GatewayStatisticsRsp();
            when(monitorService.getStatistics(any(QueryStatisticsReq.class))).thenReturn(ResponseDTO.newSuccessInstance(rsp));

            ResponseDTO<GatewayStatisticsRsp> response = monitorController.getStatistics(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(monitorService, times(1)).getStatistics(any(QueryStatisticsReq.class));
        }
    }

    @Nested
    @DisplayName("getHealthStatus 测试")
    class GetHealthStatusTests {

        @Test
        @DisplayName("获取网关健康状态 - 正常场景")
        void testGetHealthStatus_Success() {
            QueryHealthStatusReq req = new QueryHealthStatusReq();

            RequestDTO<QueryHealthStatusReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            GatewayHealthStatusRsp rsp = new GatewayHealthStatusRsp();
            when(monitorService.getHealthStatus(any(QueryHealthStatusReq.class))).thenReturn(ResponseDTO.newSuccessInstance(rsp));

            ResponseDTO<GatewayHealthStatusRsp> response = monitorController.getHealthStatus(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(monitorService, times(1)).getHealthStatus(any(QueryHealthStatusReq.class));
        }
    }

    @Nested
    @DisplayName("getGatewayMetrics 测试")
    class GetGatewayMetricsTests {

        @Test
        @DisplayName("获取网关指标数据 - 正常场景")
        void testGetGatewayMetrics_Success() {
            GetGatewayMetricsReq req = new GetGatewayMetricsReq();

            RequestDTO<GetGatewayMetricsReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            GatewayMetricsRsp rsp = new GatewayMetricsRsp();
            when(monitorService.getGatewayMetrics(any(GetGatewayMetricsReq.class))).thenReturn(ResponseDTO.newSuccessInstance(rsp));

            ResponseDTO<GatewayMetricsRsp> response = monitorController.getGatewayMetrics(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(monitorService, times(1)).getGatewayMetrics(any(GetGatewayMetricsReq.class));
        }

        @Test
        @DisplayName("获取网关指标数据 - 异常场景")
        void testGetGatewayMetrics_Exception() {
            GetGatewayMetricsReq req = new GetGatewayMetricsReq();
            RequestDTO<GetGatewayMetricsReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(monitorService.getGatewayMetrics(any(GetGatewayMetricsReq.class)))
                    .thenThrow(new RuntimeException("指标获取失败"));

            assertThrows(RuntimeException.class, () -> monitorController.getGatewayMetrics(requestDTO));
            verify(monitorService, times(1)).getGatewayMetrics(any(GetGatewayMetricsReq.class));
        }
    }
}