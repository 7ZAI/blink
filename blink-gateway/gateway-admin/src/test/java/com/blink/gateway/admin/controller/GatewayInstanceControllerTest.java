package com.blink.gateway.admin.controller;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.GetGatewayInstanceDetailReq;
import com.blink.gateway.admin.dto.req.OfflineGatewayInstanceReq;
import com.blink.gateway.admin.dto.req.OnlineGatewayInstanceReq;
import com.blink.gateway.admin.dto.rsp.GatewayInstanceListRsp;
import com.blink.gateway.admin.dto.vo.GatewayInstanceVO;
import com.blink.gateway.admin.service.GatewayInstanceService;
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
 * GatewayInstanceController 单元测试类
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GatewayInstanceController 单元测试")
class GatewayInstanceControllerTest {

    @Mock
    private GatewayInstanceService gatewayInstanceService;

    @InjectMocks
    private GatewayInstanceController gatewayInstanceController;

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
            RequestDTO<EmptyBody> requestDTO = new RequestDTO<>();

            GatewayInstanceListRsp rsp = new GatewayInstanceListRsp();
            when(gatewayInstanceService.getGatewayInstances()).thenReturn(ResponseDTO.newSuccessInstance(rsp));

            ResponseDTO<GatewayInstanceListRsp> response = gatewayInstanceController.getGatewayInstances(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(gatewayInstanceService, times(1)).getGatewayInstances();
        }
    }

    @Nested
    @DisplayName("getGatewayInstanceDetail 测试")
    class GetGatewayInstanceDetailTests {

        @Test
        @DisplayName("获取网关实例详情 - 正常场景")
        void testGetGatewayInstanceDetail_Success() {
            GetGatewayInstanceDetailReq req = new GetGatewayInstanceDetailReq();
            req.setInstanceId("instance-001");

            RequestDTO<GetGatewayInstanceDetailReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            GatewayInstanceVO instanceVO = new GatewayInstanceVO();
            instanceVO.setInstanceId("instance-001");
            when(gatewayInstanceService.getGatewayInstanceDetail(any(GetGatewayInstanceDetailReq.class)))
                    .thenReturn(ResponseDTO.newSuccessInstance(instanceVO));

            ResponseDTO<GatewayInstanceVO> response = gatewayInstanceController.getGatewayInstanceDetail(requestDTO);

            assertNotNull(response);
            assertNotNull(response.getBody());
            assertEquals("instance-001", response.getBody().getInstanceId());
            verify(gatewayInstanceService, times(1)).getGatewayInstanceDetail(any(GetGatewayInstanceDetailReq.class));
        }

        @Test
        @DisplayName("获取网关实例详情 - 异常场景")
        void testGetGatewayInstanceDetail_Exception() {
            GetGatewayInstanceDetailReq req = new GetGatewayInstanceDetailReq();
            RequestDTO<GetGatewayInstanceDetailReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(gatewayInstanceService.getGatewayInstanceDetail(any(GetGatewayInstanceDetailReq.class)))
                    .thenThrow(new RuntimeException("实例不存在"));

            assertThrows(RuntimeException.class, () -> gatewayInstanceController.getGatewayInstanceDetail(requestDTO));
            verify(gatewayInstanceService, times(1)).getGatewayInstanceDetail(any(GetGatewayInstanceDetailReq.class));
        }
    }

    @Nested
    @DisplayName("offlineInstance 测试")
    class OfflineInstanceTests {

        @Test
        @DisplayName("下线网关实例 - 正常场景")
        void testOfflineInstance_Success() {
            OfflineGatewayInstanceReq req = new OfflineGatewayInstanceReq();
            req.setInstanceId("instance-001");

            RequestDTO<OfflineGatewayInstanceReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(gatewayInstanceService.offlineInstance(any(OfflineGatewayInstanceReq.class))).thenReturn(ResponseDTO.newSuccessInstance());

            ResponseDTO<EmptyBody> response = gatewayInstanceController.offlineInstance(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(gatewayInstanceService, times(1)).offlineInstance(any(OfflineGatewayInstanceReq.class));
        }
    }

    @Nested
    @DisplayName("onlineInstance 测试")
    class OnlineInstanceTests {

        @Test
        @DisplayName("上线网关实例 - 正常场景")
        void testOnlineInstance_Success() {
            OnlineGatewayInstanceReq req = new OnlineGatewayInstanceReq();
            req.setInstanceId("instance-001");

            RequestDTO<OnlineGatewayInstanceReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(gatewayInstanceService.onlineInstance(any(OnlineGatewayInstanceReq.class))).thenReturn(ResponseDTO.newSuccessInstance());

            ResponseDTO<EmptyBody> response = gatewayInstanceController.onlineInstance(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(gatewayInstanceService, times(1)).onlineInstance(any(OnlineGatewayInstanceReq.class));
        }
    }
}