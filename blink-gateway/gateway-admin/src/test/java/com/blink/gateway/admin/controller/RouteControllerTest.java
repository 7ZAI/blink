package com.blink.gateway.admin.controller;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.*;
import com.blink.gateway.admin.dto.rsp.*;
import com.blink.gateway.admin.dto.vo.GatewayInstanceVO;
import com.blink.gateway.admin.dto.vo.StorageModeVO;
import com.blink.gateway.admin.entity.GaRouteDO;
import com.blink.gateway.admin.service.NacosRouteService;
import com.blink.gateway.admin.service.RoutePushService;
import com.blink.gateway.admin.service.RouteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RouteController 单元测试类
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RouteController 单元测试")
class RouteControllerTest {

    @Mock
    private RouteService routeService;

    @Mock
    private NacosRouteService nacosRouteService;

    @Mock
    private RoutePushService routePushService;

    @InjectMocks
    private RouteController routeController;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    // ========== Redis/数据库 路由管理 ==========

    @Nested
    @DisplayName("getRouteList 测试")
    class GetRouteListTests {

        @Test
        @DisplayName("查询路由列表 - 正常场景")
        void testGetRouteList_Success() {
            QueryRouteReq req = new QueryRouteReq();
            req.setPageNum(1);
            req.setPageSize(10);

            RequestDTO<QueryRouteReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            QueryRouteRsp rsp = new QueryRouteRsp();
            when(routeService.getRouteList(any(QueryRouteReq.class))).thenReturn(ResponseDTO.newSuccessInstance(rsp));

            ResponseDTO<QueryRouteRsp> response = routeController.getRouteList(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(routeService, times(1)).getRouteList(any(QueryRouteReq.class));
        }
    }

    @Nested
    @DisplayName("getRouteDetail 测试")
    class GetRouteDetailTests {

        @Test
        @DisplayName("获取路由详情 - 正常场景")
        void testGetRouteDetail_Success() {
            RequestDTO<String> requestDTO = new RequestDTO<>();
            requestDTO.setBody("route-001");

            GaRouteDO routeDO = new GaRouteDO();
            routeDO.setRouteId("route-001");
            when(routeService.getRouteDetail(any(String.class))).thenReturn(ResponseDTO.newSuccessInstance(routeDO));

            ResponseDTO<GaRouteDO> response = routeController.getRouteDetail(requestDTO);

            assertNotNull(response);
            assertNotNull(response.getBody());
            verify(routeService, times(1)).getRouteDetail("route-001");
        }
    }

    @Nested
    @DisplayName("saveRoute 测试")
    class SaveRouteTests {

        @Test
        @DisplayName("保存路由 - 正常场景")
        void testSaveRoute_Success() {
            SaveRouteReq req = new SaveRouteReq();
            req.setRouteId("route-001");

            RequestDTO<SaveRouteReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(routeService.saveRoute(any(SaveRouteReq.class))).thenReturn(ResponseDTO.newSuccessInstance());

            ResponseDTO<EmptyBody> response = routeController.saveRoute(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(routeService, times(1)).saveRoute(any(SaveRouteReq.class));
        }

        @Test
        @DisplayName("保存路由 - 异常场景")
        void testSaveRoute_Exception() {
            SaveRouteReq req = new SaveRouteReq();
            RequestDTO<SaveRouteReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(routeService.saveRoute(any(SaveRouteReq.class)))
                    .thenThrow(new RuntimeException("路由已存在"));

            assertThrows(RuntimeException.class, () -> routeController.saveRoute(requestDTO));
            verify(routeService, times(1)).saveRoute(any(SaveRouteReq.class));
        }
    }

    @Nested
    @DisplayName("updateRoute 测试")
    class UpdateRouteTests {

        @Test
        @DisplayName("更新路由 - 正常场景")
        void testUpdateRoute_Success() {
            UpdateRouteReq req = new UpdateRouteReq();
            req.setRouteId("route-001");

            RequestDTO<UpdateRouteReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(routeService.updateRoute(any(UpdateRouteReq.class))).thenReturn(ResponseDTO.newSuccessInstance());

            ResponseDTO<EmptyBody> response = routeController.updateRoute(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(routeService, times(1)).updateRoute(any(UpdateRouteReq.class));
        }
    }

    @Nested
    @DisplayName("deleteRoute 测试")
    class DeleteRouteTests {

        @Test
        @DisplayName("删除路由 - 正常场景")
        void testDeleteRoute_Success() {
            DeleteRouteReq req = new DeleteRouteReq();
            req.setRouteIds(List.of("route-001"));

            RequestDTO<DeleteRouteReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(routeService.deleteRoute(any(DeleteRouteReq.class))).thenReturn(ResponseDTO.newSuccessInstance());

            ResponseDTO<EmptyBody> response = routeController.deleteRoute(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(routeService, times(1)).deleteRoute(any(DeleteRouteReq.class));
        }
    }

    @Nested
    @DisplayName("getRouteHistory 测试")
    class GetRouteHistoryTests {

        @Test
        @DisplayName("查询路由变更历史 - 正常场景")
        void testGetRouteHistory_Success() {
            QueryRouteHistoryReq req = new QueryRouteHistoryReq();
            req.setPageNum(1);
            req.setPageSize(10);

            RequestDTO<QueryRouteHistoryReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            QueryRouteHistoryRsp rsp = new QueryRouteHistoryRsp();
            when(routeService.getRouteHistory(any(QueryRouteHistoryReq.class))).thenReturn(ResponseDTO.newSuccessInstance(rsp));

            ResponseDTO<QueryRouteHistoryRsp> response = routeController.getRouteHistory(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(routeService, times(1)).getRouteHistory(any(QueryRouteHistoryReq.class));
        }
    }

    @Nested
    @DisplayName("rollbackRoute 测试")
    class RollbackRouteTests {

        @Test
        @DisplayName("回滚路由 - 正常场景")
        void testRollbackRoute_Success() {
            RollbackRouteReq req = new RollbackRouteReq();
            req.setHistoryId(1L);

            RequestDTO<RollbackRouteReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(routeService.rollbackRoute(any(RollbackRouteReq.class))).thenReturn(ResponseDTO.newSuccessInstance());

            ResponseDTO<EmptyBody> response = routeController.rollbackRoute(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(routeService, times(1)).rollbackRoute(any(RollbackRouteReq.class));
        }
    }

    @Nested
    @DisplayName("refreshRoutes 测试")
    class RefreshRoutesTests {

        @Test
        @DisplayName("刷新路由缓存 - 正常场景")
        void testRefreshRoutes_Success() {
            RequestDTO<EmptyBody> requestDTO = new RequestDTO<>();

            when(routeService.refreshRoutes()).thenReturn(ResponseDTO.newSuccessInstance());

            ResponseDTO<EmptyBody> response = routeController.refreshRoutes(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(routeService, times(1)).refreshRoutes();
        }
    }

    // ========== 存储方式和实例同步 ==========

    @Nested
    @DisplayName("getStorageModes 测试")
    class GetStorageModesTests {

        @Test
        @DisplayName("获取存储方式列表 - 正常场景")
        void testGetStorageModes_Success() {
            RequestDTO<EmptyBody> requestDTO = new RequestDTO<>();

            List<StorageModeVO> modes = List.of(new StorageModeVO());
            when(routeService.getStorageModes()).thenReturn(ResponseDTO.newSuccessInstance(modes));

            ResponseDTO<List<StorageModeVO>> response = routeController.getStorageModes(requestDTO);

            assertNotNull(response);
            assertNotNull(response.getBody());
            verify(routeService, times(1)).getStorageModes();
        }
    }

    @Nested
    @DisplayName("getOnlineGatewayInstances 测试")
    class GetOnlineGatewayInstancesTests {

        @Test
        @DisplayName("获取在线网关实例列表 - 正常场景")
        void testGetOnlineGatewayInstances_Success() {
            RequestDTO<EmptyBody> requestDTO = new RequestDTO<>();

            List<GatewayInstanceVO> instances = List.of(new GatewayInstanceVO());
            when(routeService.getOnlineGatewayInstances()).thenReturn(ResponseDTO.newSuccessInstance(instances));

            ResponseDTO<List<GatewayInstanceVO>> response = routeController.getOnlineGatewayInstances(requestDTO);

            assertNotNull(response);
            assertNotNull(response.getBody());
            verify(routeService, times(1)).getOnlineGatewayInstances();
        }
    }

    @Nested
    @DisplayName("syncRoutesToInstances 测试")
    class SyncRoutesToInstancesTests {

        @Test
        @DisplayName("同步路由到实例 - 正常场景")
        void testSyncRoutesToInstances_Success() {
            SyncRoutesReq req = new SyncRoutesReq();
            req.setTargetInstanceIds(List.of("instance-001"));

            RequestDTO<SyncRoutesReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(routeService.syncRoutesToInstances(any(SyncRoutesReq.class))).thenReturn(ResponseDTO.newSuccessInstance());

            ResponseDTO<EmptyBody> response = routeController.syncRoutesToInstances(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(routeService, times(1)).syncRoutesToInstances(any(SyncRoutesReq.class));
        }
    }

    // ========== Nacos 路由管理 ==========

    @Nested
    @DisplayName("getNacosRouteList 测试")
    class GetNacosRouteListTests {

        @Test
        @DisplayName("查询 Nacos 路由列表 - 正常场景")
        void testGetNacosRouteList_Success() {
            QueryNacosRouteReq req = new QueryNacosRouteReq();

            RequestDTO<QueryNacosRouteReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            QueryGateWayRoutesRsp rsp = new QueryGateWayRoutesRsp();
            when(nacosRouteService.getNacosRouteList(any(QueryNacosRouteReq.class))).thenReturn(ResponseDTO.newSuccessInstance(rsp));

            ResponseDTO<QueryGateWayRoutesRsp> response = routeController.getNacosRouteList(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(nacosRouteService, times(1)).getNacosRouteList(any(QueryNacosRouteReq.class));
        }
    }

    @Nested
    @DisplayName("saveNacosRoute 测试")
    class SaveNacosRouteTests {

        @Test
        @DisplayName("保存 Nacos 路由 - 正常场景")
        void testSaveNacosRoute_Success() {
            SaveNacosRouteReq req = new SaveNacosRouteReq();

            RequestDTO<SaveNacosRouteReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(nacosRouteService.saveNacosRoute(any(SaveNacosRouteReq.class))).thenReturn(ResponseDTO.newSuccessInstance());

            ResponseDTO<EmptyBody> response = routeController.saveNacosRoute(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(nacosRouteService, times(1)).saveNacosRoute(any(SaveNacosRouteReq.class));
        }
    }

    @Nested
    @DisplayName("deleteNacosRoute 测试")
    class DeleteNacosRouteTests {

        @Test
        @DisplayName("删除 Nacos 路由 - 正常场景")
        void testDeleteNacosRoute_Success() {
            DeleteNacosRouteReq req = new DeleteNacosRouteReq();

            RequestDTO<DeleteNacosRouteReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(nacosRouteService.deleteNacosRoute(any(DeleteNacosRouteReq.class))).thenReturn(ResponseDTO.newSuccessInstance());

            ResponseDTO<EmptyBody> response = routeController.deleteNacosRoute(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(nacosRouteService, times(1)).deleteNacosRoute(any(DeleteNacosRouteReq.class));
        }
    }

    // ========== 路由推送管理 ==========

    @Nested
    @DisplayName("pushRoutes 测试")
    class PushRoutesTests {

        @Test
        @DisplayName("推送路由到实例 - 正常场景")
        void testPushRoutes_Success() {
            PushRoutesReq req = new PushRoutesReq();

            RequestDTO<PushRoutesReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(routePushService.pushRoutes(any(PushRoutesReq.class))).thenReturn(ResponseDTO.newSuccessInstance());

            ResponseDTO<EmptyBody> response = routeController.pushRoutes(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(routePushService, times(1)).pushRoutes(any(PushRoutesReq.class));
        }
    }

    @Nested
    @DisplayName("getPushHistory 测试")
    class GetPushHistoryTests {

        @Test
        @DisplayName("查询推送历史 - 正常场景")
        void testGetPushHistory_Success() {
            QueryPushLogReq req = new QueryPushLogReq();

            RequestDTO<QueryPushLogReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            QueryPushLogRsp rsp = new QueryPushLogRsp();
            when(routePushService.getPushHistory(any(QueryPushLogReq.class))).thenReturn(ResponseDTO.newSuccessInstance(rsp));

            ResponseDTO<QueryPushLogRsp> response = routeController.getPushHistory(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(routePushService, times(1)).getPushHistory(any(QueryPushLogReq.class));
        }
    }

    @Nested
    @DisplayName("getInstanceRoutes 测试")
    class GetInstanceRoutesTests {

        @Test
        @DisplayName("查询实例当前路由 - 正常场景")
        void testGetInstanceRoutes_Success() {
            QueryInstanceRoutesReq req = new QueryInstanceRoutesReq();

            RequestDTO<QueryInstanceRoutesReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            QueryInstanceRoutesRsp rsp = new QueryInstanceRoutesRsp();
            when(routePushService.getInstanceRoutes(any(QueryInstanceRoutesReq.class))).thenReturn(ResponseDTO.newSuccessInstance(rsp));

            ResponseDTO<QueryInstanceRoutesRsp> response = routeController.getInstanceRoutes(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(routePushService, times(1)).getInstanceRoutes(any(QueryInstanceRoutesReq.class));
        }
    }

    @Nested
    @DisplayName("rollbackPush 测试")
    class RollbackPushTests {

        @Test
        @DisplayName("回滚推送 - 正常场景")
        void testRollbackPush_Success() {
            RollbackPushReq req = new RollbackPushReq();

            RequestDTO<RollbackPushReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(routePushService.rollbackPush(any(RollbackPushReq.class))).thenReturn(ResponseDTO.newSuccessInstance());

            ResponseDTO<EmptyBody> response = routeController.rollbackPush(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(routePushService, times(1)).rollbackPush(any(RollbackPushReq.class));
        }
    }

    // ========== 新增接口测试 (P0-1.1, P0-1.2, P1-2.1) ==========

    @Nested
    @DisplayName("getInstanceRoutesFromActuator 测试")
    class GetInstanceRoutesFromActuatorTests {

        @Test
        @DisplayName("从实例获取路由 - 正常场景")
        void testGetInstanceRoutesFromActuator_Success() {
            GetInstanceRoutesFromActuatorReq req = new GetInstanceRoutesFromActuatorReq();
            req.setInstanceId("gateway-app:192.168.1.10:8080");

            RequestDTO<GetInstanceRoutesFromActuatorReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            InstanceRoutesRsp rsp = new InstanceRoutesRsp();
            rsp.setInstanceId("gateway-app:192.168.1.10:8080");
            rsp.setTotal(5);
            rsp.setFromActuator(true);
            when(routePushService.getInstanceRoutesFromActuator(any(GetInstanceRoutesFromActuatorReq.class)))
                    .thenReturn(ResponseDTO.newSuccessInstance(rsp));

            ResponseDTO<InstanceRoutesRsp> response = routeController.getInstanceRoutesFromActuator(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            assertEquals("gateway-app:192.168.1.10:8080", response.getBody().getInstanceId());
            assertTrue(response.getBody().getFromActuator());
            verify(routePushService, times(1)).getInstanceRoutesFromActuator(any(GetInstanceRoutesFromActuatorReq.class));
        }

        @Test
        @DisplayName("从实例获取路由 - 实例ID为空")
        void testGetInstanceRoutesFromActuator_EmptyInstanceId() {
            GetInstanceRoutesFromActuatorReq req = new GetInstanceRoutesFromActuatorReq();

            RequestDTO<GetInstanceRoutesFromActuatorReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            InstanceRoutesRsp rsp = new InstanceRoutesRsp();
            rsp.setError("实例ID格式错误");
            when(routePushService.getInstanceRoutesFromActuator(any(GetInstanceRoutesFromActuatorReq.class)))
                    .thenReturn(ResponseDTO.newSuccessInstance(rsp));

            ResponseDTO<InstanceRoutesRsp> response = routeController.getInstanceRoutesFromActuator(requestDTO);

            assertNotNull(response);
            assertNotNull(response.getBody().getError());
            verify(routePushService, times(1)).getInstanceRoutesFromActuator(any(GetInstanceRoutesFromActuatorReq.class));
        }
    }

    @Nested
    @DisplayName("getRouteInstancePushStatus 测试")
    class GetRouteInstancePushStatusTests {

        @Test
        @DisplayName("查询路由实例推送状态 - 正常场景")
        void testGetRouteInstancePushStatus_Success() {
            QueryRouteInstancePushStatusReq req = new QueryRouteInstancePushStatusReq();
            req.setRouteIds(List.of("route-001", "route-002"));

            RequestDTO<QueryRouteInstancePushStatusReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            RouteInstancePushStatusRsp statusRsp = new RouteInstancePushStatusRsp();
            statusRsp.setRouteId("route-001");
            statusRsp.setTotalInstances(3);
            statusRsp.setPushedInstances(2);
            statusRsp.setFailedInstances(1);

            List<RouteInstancePushStatusRsp> rspList = List.of(statusRsp);
            when(routePushService.getRouteInstancePushStatus(any(QueryRouteInstancePushStatusReq.class)))
                    .thenReturn(ResponseDTO.newSuccessInstance(rspList));

            ResponseDTO<List<RouteInstancePushStatusRsp>> response = routeController.getRouteInstancePushStatus(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().size());
            assertEquals("route-001", response.getBody().get(0).getRouteId());
            assertEquals(3, response.getBody().get(0).getTotalInstances());
            verify(routePushService, times(1)).getRouteInstancePushStatus(any(QueryRouteInstancePushStatusReq.class));
        }

        @Test
        @DisplayName("查询路由实例推送状态 - 单个路由ID")
        void testGetRouteInstancePushStatus_SingleRouteId() {
            QueryRouteInstancePushStatusReq req = new QueryRouteInstancePushStatusReq();
            req.setRouteId("route-001");

            RequestDTO<QueryRouteInstancePushStatusReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            RouteInstancePushStatusRsp statusRsp = new RouteInstancePushStatusRsp();
            statusRsp.setRouteId("route-001");
            statusRsp.setTotalInstances(5);
            statusRsp.setPushedInstances(5);

            List<RouteInstancePushStatusRsp> rspList = List.of(statusRsp);
            when(routePushService.getRouteInstancePushStatus(any(QueryRouteInstancePushStatusReq.class)))
                    .thenReturn(ResponseDTO.newSuccessInstance(rspList));

            ResponseDTO<List<RouteInstancePushStatusRsp>> response = routeController.getRouteInstancePushStatus(requestDTO);

            assertNotNull(response);
            assertEquals(1, response.getBody().size());
            assertEquals(5, response.getBody().get(0).getPushedInstances());
            verify(routePushService, times(1)).getRouteInstancePushStatus(any(QueryRouteInstancePushStatusReq.class));
        }
    }

    @Nested
    @DisplayName("confirmPush 测试")
    class ConfirmPushTests {

        @Test
        @DisplayName("确认推送 - 正常场景")
        void testConfirmPush_Success() {
            ConfirmPushReq req = new ConfirmPushReq();
            req.setPushId(1L);

            RequestDTO<ConfirmPushReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(routePushService.confirmPush(any(ConfirmPushReq.class))).thenReturn(ResponseDTO.newSuccessInstance());

            ResponseDTO<EmptyBody> response = routeController.confirmPush(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(routePushService, times(1)).confirmPush(any(ConfirmPushReq.class));
        }

        @Test
        @DisplayName("确认推送 - pushId为空")
        void testConfirmPush_NullPushId() {
            ConfirmPushReq req = new ConfirmPushReq();

            RequestDTO<ConfirmPushReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(routePushService.confirmPush(any(ConfirmPushReq.class)))
                    .thenThrow(new RuntimeException("参数不能为空"));

            assertThrows(RuntimeException.class, () -> routeController.confirmPush(requestDTO));
            verify(routePushService, times(1)).confirmPush(any(ConfirmPushReq.class));
        }
    }
}