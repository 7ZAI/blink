package com.blink.gateway.admin.controller;

import com.blink.base.dto.req.QuerySimpleUserReq;
import com.blink.base.dto.req.UserIdReq;
import com.blink.base.dto.rsp.QuerySimpleUserRsp;
import com.blink.base.dto.rsp.UserPermissionDetailRsp;
import com.blink.base.dubbo.service.BaseDubboService;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
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
 * ChannelUserController 单元测试类
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChannelUserController 单元测试")
class ChannelUserControllerTest {

    @Mock
    private BaseDubboService baseDubboService;

    @InjectMocks
    private ChannelUserController channelUserController;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    @Nested
    @DisplayName("getSimpleUserList 测试")
    class GetSimpleUserListTests {

        @Test
        @DisplayName("查询简化用户列表 - 正常场景")
        void testGetSimpleUserList_Success() {
            QuerySimpleUserReq req = new QuerySimpleUserReq();
            req.setKeyword("test");

            RequestDTO<QuerySimpleUserReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            QuerySimpleUserRsp rsp = new QuerySimpleUserRsp();
            ResponseDTO<QuerySimpleUserRsp> mockResponse = ResponseDTO.newSuccessInstance(rsp);
            when(baseDubboService.getSimpleUserList(any(RequestDTO.class))).thenReturn(mockResponse);

            ResponseDTO<QuerySimpleUserRsp> response = channelUserController.getSimpleUserList(requestDTO);

            assertNotNull(response);
            assertNotNull(response.getBody());
            verify(baseDubboService, times(1)).getSimpleUserList(any(RequestDTO.class));
        }

        @Test
        @DisplayName("查询简化用户列表 - 无参数场景")
        void testGetSimpleUserList_NoBody() {
            RequestDTO<QuerySimpleUserReq> requestDTO = new RequestDTO<>();

            QuerySimpleUserRsp rsp = new QuerySimpleUserRsp();
            ResponseDTO<QuerySimpleUserRsp> mockResponse = ResponseDTO.newSuccessInstance(rsp);
            when(baseDubboService.getSimpleUserList(any(RequestDTO.class))).thenReturn(mockResponse);

            ResponseDTO<QuerySimpleUserRsp> response = channelUserController.getSimpleUserList(requestDTO);

            assertNotNull(response);
            assertNotNull(response.getBody());
            verify(baseDubboService, times(1)).getSimpleUserList(any(RequestDTO.class));
        }
    }

    @Nested
    @DisplayName("getUserPermissionDetail 测试")
    class GetUserPermissionDetailTests {

        @Test
        @DisplayName("查询用户权限详情 - 正常场景")
        void testGetUserPermissionDetail_Success() {
            UserIdReq req = new UserIdReq();
            req.setUserId(1);

            RequestDTO<UserIdReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            UserPermissionDetailRsp rsp = new UserPermissionDetailRsp();
            ResponseDTO<UserPermissionDetailRsp> mockResponse = ResponseDTO.newSuccessInstance(rsp);
            when(baseDubboService.getUserPermissionDetail(any(RequestDTO.class))).thenReturn(mockResponse);

            ResponseDTO<UserPermissionDetailRsp> response = channelUserController.getUserPermissionDetail(requestDTO);

            assertNotNull(response);
            assertNotNull(response.getBody());
            verify(baseDubboService, times(1)).getUserPermissionDetail(any(RequestDTO.class));
        }

        @Test
        @DisplayName("查询用户权限详情 - 异常场景")
        void testGetUserPermissionDetail_Exception() {
            UserIdReq req = new UserIdReq();
            RequestDTO<UserIdReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(baseDubboService.getUserPermissionDetail(any(RequestDTO.class)))
                    .thenThrow(new RuntimeException("Dubbo调用失败"));

            assertThrows(RuntimeException.class, () -> channelUserController.getUserPermissionDetail(requestDTO));
            verify(baseDubboService, times(1)).getUserPermissionDetail(any(RequestDTO.class));
        }
    }
}