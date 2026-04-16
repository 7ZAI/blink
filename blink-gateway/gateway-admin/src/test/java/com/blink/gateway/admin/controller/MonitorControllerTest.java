package com.blink.gateway.admin.controller;

import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.GetGatewayInstanceDetailReq;
import com.blink.gateway.admin.dto.req.GetGatewayMetricsReq;
import com.blink.gateway.admin.dto.req.QueryGatewayInstanceReq;
import com.blink.gateway.admin.dto.req.QueryHealthStatusReq;
import com.blink.gateway.admin.dto.req.QueryStatisticsReq;
import com.blink.gateway.admin.dto.rsp.GatewayHealthStatusRsp;
import com.blink.gateway.admin.dto.rsp.GatewayInstanceDetailRsp;
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

    @Nested
    @DisplayName("getInstanceDetail 测试")
    class GetInstanceDetailTests {

        @Test
        @DisplayName("获取实例详情 - 正常场景")
        void testGetInstanceDetail_Success() {
            GetGatewayInstanceDetailReq req = new GetGatewayInstanceDetailReq();
            req.setInstanceId("instance-001");

            RequestDTO<GetGatewayInstanceDetailReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            GatewayInstanceDetailRsp rsp = new GatewayInstanceDetailRsp();
            rsp.setInstanceId("instance-001");
            rsp.setHost("192.168.1.1");
            rsp.setPort(8002);
            rsp.setCpuUsage(25.5);
            rsp.setHeapUsagePercent(60.0);
            rsp.setTotalRequests(1000L);
            rsp.setSuccessRequests(990L);
            rsp.setFailedRequests(10L);

            when(monitorService.getInstanceDetail(any(GetGatewayInstanceDetailReq.class)))
                    .thenReturn(ResponseDTO.newSuccessInstance(rsp));

            ResponseDTO<GatewayInstanceDetailRsp> response = monitorController.getInstanceDetail(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            assertEquals("instance-001", response.getBody().getInstanceId());
            assertEquals(25.5, response.getBody().getCpuUsage());
            assertEquals(60.0, response.getBody().getHeapUsagePercent());

            verify(monitorService, times(1)).getInstanceDetail(any(GetGatewayInstanceDetailReq.class));
        }

        @Test
        @DisplayName("获取实例详情 - 包含完整指标")
        void testGetInstanceDetail_FullMetrics() {
            GetGatewayInstanceDetailReq req = new GetGatewayInstanceDetailReq();
            req.setInstanceId("instance-001");

            RequestDTO<GetGatewayInstanceDetailReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            GatewayInstanceDetailRsp rsp = new GatewayInstanceDetailRsp();
            rsp.setInstanceId("instance-001");
            rsp.setServiceId("gateway-app");
            rsp.setHost("192.168.1.1");
            rsp.setPort(8002);
            rsp.setHealthStatus("UP");
            rsp.setCpuUsage(30.0);
            rsp.setMemoryUsage(65.0);
            rsp.setHeapUsed(524288000L);
            rsp.setHeapMax(1073741824L);
            rsp.setHeapUsagePercent(49.0);
            rsp.setYoungGcCount(100L);
            rsp.setYoungGcTime(500L);
            rsp.setOldGcCount(5L);
            rsp.setOldGcTime(2000L);
            rsp.setTotalGcCount(105L);
            rsp.setTotalGcTime(2500L);
            rsp.setLiveThreads(150);
            rsp.setPeakThreads(200);
            rsp.setDaemonThreads(25);
            rsp.setTotalRequests(50000L);
            rsp.setSuccessRequests(49500L);
            rsp.setFailedRequests(500L);
            rsp.setSuccessRate(99.0);
            rsp.setAvgResponseTime(150L);

            when(monitorService.getInstanceDetail(any(GetGatewayInstanceDetailReq.class)))
                    .thenReturn(ResponseDTO.newSuccessInstance(rsp));

            ResponseDTO<GatewayInstanceDetailRsp> response = monitorController.getInstanceDetail(requestDTO);

            assertNotNull(response);
            assertNotNull(response.getBody());

            // 验证 JVM 指标
            assertEquals(30.0, response.getBody().getCpuUsage());
            assertEquals(65.0, response.getBody().getMemoryUsage());
            assertEquals(524288000L, response.getBody().getHeapUsed());

            // 验证 GC 指标
            assertEquals(100L, response.getBody().getYoungGcCount());
            assertEquals(105L, response.getBody().getTotalGcCount());
            assertEquals(2500L, response.getBody().getTotalGcTime());

            // 验证线程指标
            assertEquals(150, response.getBody().getLiveThreads());
            assertEquals(200, response.getBody().getPeakThreads());

            // 验证 HTTP 指标
            assertEquals(50000L, response.getBody().getTotalRequests());
            assertEquals(99.0, response.getBody().getSuccessRate());
            assertEquals(150L, response.getBody().getAvgResponseTime());

            verify(monitorService, times(1)).getInstanceDetail(any(GetGatewayInstanceDetailReq.class));
        }

        @Test
        @DisplayName("获取实例详情 - 实例不存在")
        void testGetInstanceDetail_InstanceNotExist() {
            GetGatewayInstanceDetailReq req = new GetGatewayInstanceDetailReq();
            req.setInstanceId("instance-999");

            RequestDTO<GetGatewayInstanceDetailReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(monitorService.getInstanceDetail(any(GetGatewayInstanceDetailReq.class)))
                    .thenThrow(new com.blink.framework.common.exception.BlinkException(
                        "实例不存在", com.blink.gateway.admin.constants.ErrCodeConstant.GATEWAY_INSTANCE_NOT_EXIST
                    ));

            assertThrows(com.blink.framework.common.exception.BlinkException.class,
                () -> monitorController.getInstanceDetail(requestDTO));

            verify(monitorService, times(1)).getInstanceDetail(any(GetGatewayInstanceDetailReq.class));
        }

        @Test
        @DisplayName("获取实例详情 - instanceId 为空")
        void testGetInstanceDetail_EmptyInstanceId() {
            GetGatewayInstanceDetailReq req = new GetGatewayInstanceDetailReq();
            req.setInstanceId("");

            RequestDTO<GetGatewayInstanceDetailReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(monitorService.getInstanceDetail(any(GetGatewayInstanceDetailReq.class)))
                    .thenThrow(new com.blink.framework.common.exception.BlinkException(
                        "参数不能为空", com.blink.gateway.admin.constants.ErrCodeConstant.PARAMETER_NOT_NULL
                    ));

            assertThrows(com.blink.framework.common.exception.BlinkException.class,
                () -> monitorController.getInstanceDetail(requestDTO));

            verify(monitorService, times(1)).getInstanceDetail(any(GetGatewayInstanceDetailReq.class));
        }

        @Test
        @DisplayName("获取实例详情 - 异常处理")
        void testGetInstanceDetail_Exception() {
            GetGatewayInstanceDetailReq req = new GetGatewayInstanceDetailReq();
            req.setInstanceId("instance-001");

            RequestDTO<GetGatewayInstanceDetailReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(monitorService.getInstanceDetail(any(GetGatewayInstanceDetailReq.class)))
                    .thenThrow(new RuntimeException("Redis connection error"));

            assertThrows(RuntimeException.class,
                () -> monitorController.getInstanceDetail(requestDTO));

            verify(monitorService, times(1)).getInstanceDetail(any(GetGatewayInstanceDetailReq.class));
        }
    }
}