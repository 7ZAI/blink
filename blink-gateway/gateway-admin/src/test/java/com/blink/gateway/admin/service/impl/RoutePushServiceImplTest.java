package com.blink.gateway.admin.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.gateway.admin.dto.req.ConfirmPushReq;
import com.blink.gateway.admin.dto.req.GetInstanceRoutesFromActuatorReq;
import com.blink.gateway.admin.dto.req.QueryInstanceReq;
import com.blink.gateway.admin.dto.req.QueryRouteInstancePushStatusReq;
import com.blink.gateway.admin.dto.rsp.InstanceRoutesRsp;
import com.blink.gateway.admin.dto.rsp.RouteInstancePushStatusRsp;
import com.blink.gateway.admin.dto.rsp.QueryInstanceListRsp;
import com.blink.gateway.admin.dto.vo.InstanceInfoVO;
import com.blink.gateway.admin.entity.GaRouteInstanceRelaDO;
import com.blink.gateway.admin.entity.GaRoutePushLogDO;
import com.blink.gateway.admin.mapper.GaRouteInstanceRelaMapper;
import com.blink.gateway.admin.mapper.GaRoutePushLogMapper;
import com.blink.gateway.admin.service.GatewayInstanceService;
import com.blink.gateway.admin.constants.RouteConstant;
import com.blink.gateway.admin.constants.ConfigValueConstant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * RoutePushServiceImpl 单元测试类
 * 测试新增的推送状态跟踪和确认功能
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RoutePushServiceImpl 单元测试")
class RoutePushServiceImplTest {

    @Mock
    private GaRoutePushLogMapper gaRoutePushLogMapper;

    @Mock
    private GaRouteInstanceRelaMapper gaRouteInstanceRelaMapper;

    @Mock
    private GatewayInstanceService gatewayInstanceService;

    @InjectMocks
    private RoutePushServiceImpl routePushService;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    // ========== getRouteInstancePushStatus 测试 ==========

    @Nested
    @DisplayName("getRouteInstancePushStatus 测试")
    class GetRouteInstancePushStatusTests {

        @Test
        @DisplayName("查询路由实例推送状态 - 正常场景")
        void testGetRouteInstancePushStatus_Success() {
            // 准备请求
            QueryRouteInstancePushStatusReq req = new QueryRouteInstancePushStatusReq();
            req.setRouteIds(List.of("route-001"));

            // Mock 网关实例列表 - 使用 queryInstanceList
            InstanceInfoVO instance1 = createInstanceInfoVO("gateway-app:192.168.1.10:8080", ConfigValueConstant.INSTANCE_STATUS_ONLINE);
            InstanceInfoVO instance2 = createInstanceInfoVO("gateway-app:192.168.1.11:8080", ConfigValueConstant.INSTANCE_STATUS_ONLINE);

            QueryInstanceListRsp instanceListRsp = new QueryInstanceListRsp();
            instanceListRsp.setRows(List.of(instance1, instance2));
            instanceListRsp.setTotal(2);

            ResponseDTO<QueryInstanceListRsp> instancesResponse = ResponseDTO.newSuccessInstance(instanceListRsp);
            when(gatewayInstanceService.queryInstanceList(any(QueryInstanceReq.class))).thenReturn(instancesResponse);

            // Mock 路由实例关联记录
            GaRouteInstanceRelaDO rela1 = new GaRouteInstanceRelaDO();
            rela1.setRouteId("route-001");
            rela1.setInstanceId("gateway-app:192.168.1.10:8080");
            rela1.setPushStatus(RouteConstant.PUSH_STATUS_PUSHED);

            GaRouteInstanceRelaDO rela2 = new GaRouteInstanceRelaDO();
            rela2.setRouteId("route-001");
            rela2.setInstanceId("gateway-app:192.168.1.11:8080");
            rela2.setPushStatus(RouteConstant.PUSH_STATUS_PUSHED);

            when(gaRouteInstanceRelaMapper.selectByRouteId("route-001")).thenReturn(List.of(rela1, rela2));

            // 执行测试
            ResponseDTO<List<RouteInstancePushStatusRsp>> response = routePushService.getRouteInstancePushStatus(req);

            // 验证结果
            assertNotNull(response);
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().size());

            RouteInstancePushStatusRsp statusRsp = response.getBody().get(0);
            assertEquals("route-001", statusRsp.getRouteId());
            assertEquals(2, statusRsp.getTotalInstances());
            assertEquals(2, statusRsp.getPushedInstances());
            assertEquals(0, statusRsp.getFailedInstances());

            verify(gatewayInstanceService, times(1)).queryInstanceList(any(QueryInstanceReq.class));
            verify(gaRouteInstanceRelaMapper, times(1)).selectByRouteId("route-001");
        }

        @Test
        @DisplayName("查询路由实例推送状态 - 部分推送失败")
        void testGetRouteInstancePushStatus_PartialFailure() {
            QueryRouteInstancePushStatusReq req = new QueryRouteInstancePushStatusReq();
            req.setRouteIds(List.of("route-001"));

            // Mock 网关实例列表（3个实例）- 使用 queryInstanceList
            InstanceInfoVO instance1 = createInstanceInfoVO("gateway-app:192.168.1.10:8080", ConfigValueConstant.INSTANCE_STATUS_ONLINE);
            InstanceInfoVO instance2 = createInstanceInfoVO("gateway-app:192.168.1.11:8080", ConfigValueConstant.INSTANCE_STATUS_ONLINE);
            InstanceInfoVO instance3 = createInstanceInfoVO("gateway-app:192.168.1.12:8080", ConfigValueConstant.INSTANCE_STATUS_ONLINE);

            QueryInstanceListRsp instanceListRsp = new QueryInstanceListRsp();
            instanceListRsp.setRows(List.of(instance1, instance2, instance3));
            instanceListRsp.setTotal(3);

            when(gatewayInstanceService.queryInstanceList(any(QueryInstanceReq.class)))
                    .thenReturn(ResponseDTO.newSuccessInstance(instanceListRsp));

            // Mock 路由实例关联（2个已推送，1个失败）
            GaRouteInstanceRelaDO rela1 = createRela("route-001", "gateway-app:192.168.1.10:8080", RouteConstant.PUSH_STATUS_PUSHED);
            GaRouteInstanceRelaDO rela2 = createRela("route-001", "gateway-app:192.168.1.11:8080", RouteConstant.PUSH_STATUS_PUSHED);
            GaRouteInstanceRelaDO rela3 = createRela("route-001", "gateway-app:192.168.1.12:8080", RouteConstant.PUSH_STATUS_PUSH_FAILED);

            when(gaRouteInstanceRelaMapper.selectByRouteId("route-001")).thenReturn(List.of(rela1, rela2, rela3));

            ResponseDTO<List<RouteInstancePushStatusRsp>> response = routePushService.getRouteInstancePushStatus(req);

            assertNotNull(response.getBody());
            RouteInstancePushStatusRsp statusRsp = response.getBody().get(0);
            assertEquals(3, statusRsp.getTotalInstances());
            assertEquals(2, statusRsp.getPushedInstances());
            assertEquals(1, statusRsp.getFailedInstances());
        }

        @Test
        @DisplayName("查询路由实例推送状态 - 空路由ID列表")
        void testGetRouteInstancePushStatus_EmptyRouteIds() {
            QueryRouteInstancePushStatusReq req = new QueryRouteInstancePushStatusReq();

            ResponseDTO<List<RouteInstancePushStatusRsp>> response = routePushService.getRouteInstancePushStatus(req);

            assertNotNull(response);
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isEmpty());

            // 不应该调用任何 mapper
            verify(gatewayInstanceService, never()).queryInstanceList(any());
            verify(gaRouteInstanceRelaMapper, never()).selectByRouteId(any());
        }

        @Test
        @DisplayName("查询路由实例推送状态 - 单个路由ID")
        void testGetRouteInstancePushStatus_SingleRouteId() {
            QueryRouteInstancePushStatusReq req = new QueryRouteInstancePushStatusReq();
            req.setRouteId("route-002");

            InstanceInfoVO instance = createInstanceInfoVO("gateway-app:192.168.1.10:8080", ConfigValueConstant.INSTANCE_STATUS_ONLINE);
            QueryInstanceListRsp instanceListRsp = new QueryInstanceListRsp();
            instanceListRsp.setRows(List.of(instance));
            instanceListRsp.setTotal(1);

            when(gatewayInstanceService.queryInstanceList(any(QueryInstanceReq.class)))
                    .thenReturn(ResponseDTO.newSuccessInstance(instanceListRsp));
            when(gaRouteInstanceRelaMapper.selectByRouteId("route-002")).thenReturn(new ArrayList<>());

            ResponseDTO<List<RouteInstancePushStatusRsp>> response = routePushService.getRouteInstancePushStatus(req);

            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().size());
            assertEquals("route-002", response.getBody().get(0).getRouteId());
            assertEquals(1, response.getBody().get(0).getNotPushedInstances());
        }
    }

    // ========== confirmPush 测试 ==========

    @Nested
    @DisplayName("confirmPush 测试")
    class ConfirmPushTests {

        @Test
        @DisplayName("确认推送 - 正常场景")
        @Disabled("需要集成测试环境支持 MyBatis-Plus LambdaUpdateWrapper 的实体元数据")
        void testConfirmPush_Success() {
            ConfirmPushReq req = new ConfirmPushReq();
            req.setPushId(1L);

            // Mock 推送记录
            GaRoutePushLogDO pushLog = new GaRoutePushLogDO();
            pushLog.setPushId(1L);
            pushLog.setConfirmStatus(RouteConstant.CONFIRM_STATUS_PENDING);
            when(gaRoutePushLogMapper.selectById(1L)).thenReturn(pushLog);

            // Mock 更新操作 - 使用 doReturn 避免 MyBatis-Plus wrapper 问题
            doReturn(1).when(gaRoutePushLogMapper).update(any(), any());

            // Mock StpUtil
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
                stpUtilMock.when(StpUtil::getLoginIdAsString).thenReturn("admin");

                ResponseDTO<EmptyBody> response = routePushService.confirmPush(req);

                assertNotNull(response);
                assertEquals("BLINK0000", response.getMsgCode());
                verify(gaRoutePushLogMapper, times(1)).selectById(1L);
                verify(gaRoutePushLogMapper, times(1)).update(any(), any());
            }
        }

        @Test
        @DisplayName("确认推送 - pushId为空")
        void testConfirmPush_NullPushId() {
            ConfirmPushReq req = new ConfirmPushReq();

            assertThrows(BlinkException.class, () -> routePushService.confirmPush(req));
            verify(gaRoutePushLogMapper, never()).selectById(any());
        }

        @Test
        @DisplayName("确认推送 - 推送记录不存在")
        void testConfirmPush_PushLogNotExist() {
            ConfirmPushReq req = new ConfirmPushReq();
            req.setPushId(999L);

            when(gaRoutePushLogMapper.selectById(999L)).thenReturn(null);

            assertThrows(BlinkException.class, () -> routePushService.confirmPush(req));
            verify(gaRoutePushLogMapper, times(1)).selectById(999L);
        }

        @Test
        @DisplayName("确认推送 - 已确认的记录不重复更新")
        void testConfirmPush_AlreadyConfirmed() {
            ConfirmPushReq req = new ConfirmPushReq();
            req.setPushId(1L);

            GaRoutePushLogDO pushLog = new GaRoutePushLogDO();
            pushLog.setPushId(1L);
            pushLog.setConfirmStatus(RouteConstant.CONFIRM_STATUS_CONFIRMED);
            when(gaRoutePushLogMapper.selectById(1L)).thenReturn(pushLog);

            ResponseDTO<EmptyBody> response = routePushService.confirmPush(req);

            assertNotNull(response);
            // 已确认的情况下不应该执行更新
            verify(gaRoutePushLogMapper, never()).update(any(), any());
        }
    }

    // ========== getInstanceRoutesFromActuator 测试 ==========

    @Nested
    @DisplayName("getInstanceRoutesFromActuator 测试")
    class GetInstanceRoutesFromActuatorTests {

        @Test
        @DisplayName("从实例获取路由 - 参数校验失败")
        void testGetInstanceRoutesFromActuator_NullInstanceId() {
            GetInstanceRoutesFromActuatorReq req = new GetInstanceRoutesFromActuatorReq();

            assertThrows(BlinkException.class, () -> routePushService.getInstanceRoutesFromActuator(req));
        }

        @Test
        @DisplayName("从实例获取路由 - 实例ID格式错误")
        void testGetInstanceRoutesFromActuator_InvalidInstanceIdFormat() {
            GetInstanceRoutesFromActuatorReq req = new GetInstanceRoutesFromActuatorReq();
            req.setInstanceId("invalid-format");

            ResponseDTO<InstanceRoutesRsp> response = routePushService.getInstanceRoutesFromActuator(req);

            assertNotNull(response);
            assertNotNull(response.getBody());
            assertNotNull(response.getBody().getError());
            assertEquals(0, response.getBody().getTotal());
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 创建实例信息 VO 对象
     *
     * @param instanceId 实例ID
     * @param status 实例状态
     * @return 实例信息 VO
     */
    private InstanceInfoVO createInstanceInfoVO(String instanceId, Byte status) {
        InstanceInfoVO instance = new InstanceInfoVO();
        instance.setInstanceId(instanceId);
        instance.setStatus(status);
        return instance;
    }

    /**
     * 创建路由实例关联 DO 对象
     *
     * @param routeId 路由ID
     * @param instanceId 实例ID
     * @param pushStatus 推送状态
     * @return 路由实例关联 DO
     */
    private GaRouteInstanceRelaDO createRela(String routeId, String instanceId, Byte pushStatus) {
        GaRouteInstanceRelaDO rela = new GaRouteInstanceRelaDO();
        rela.setRouteId(routeId);
        rela.setInstanceId(instanceId);
        rela.setPushStatus(pushStatus);
        rela.setPushTime(LocalDateTime.now());
        return rela;
    }
}