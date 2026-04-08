package com.blink.base.controller;

import com.blink.base.dto.req.KickoutUserReq;
import com.blink.base.dto.req.QueryOnlineUserReq;
import com.blink.base.dto.rsp.OnlineUserRsp;
import com.blink.base.service.OnlineUserService;
import com.blink.framework.common.data.EmptyBody;
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
 * OnlineUserController 单元测试类
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OnlineUserController 单元测试")
class OnlineUserControllerTest {

    @Mock
    private OnlineUserService onlineUserService;

    @InjectMocks
    private OnlineUserController onlineUserController;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    @Nested
    @DisplayName("getOnlineUserList 测试")
    class GetOnlineUserListTests {

        @Test
        @DisplayName("查询在线用户列表 - 正常场景")
        void testGetOnlineUserList_Success() throws Exception {
            QueryOnlineUserReq req = new QueryOnlineUserReq();

            RequestDTO<QueryOnlineUserReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            OnlineUserRsp rsp = new OnlineUserRsp();
            when(onlineUserService.getOnlineUserList(any(QueryOnlineUserReq.class))).thenReturn(rsp);

            ResponseDTO<OnlineUserRsp> response = onlineUserController.getOnlineUserList(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(onlineUserService, times(1)).getOnlineUserList(any(QueryOnlineUserReq.class));
        }

        @Test
        @DisplayName("查询在线用户列表 - 带搜索条件")
        void testGetOnlineUserList_WithSearch() throws Exception {
            QueryOnlineUserReq req = new QueryOnlineUserReq();

            RequestDTO<QueryOnlineUserReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            OnlineUserRsp rsp = new OnlineUserRsp();
            when(onlineUserService.getOnlineUserList(any(QueryOnlineUserReq.class))).thenReturn(rsp);

            ResponseDTO<OnlineUserRsp> response = onlineUserController.getOnlineUserList(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(onlineUserService, times(1)).getOnlineUserList(any(QueryOnlineUserReq.class));
        }

        @Test
        @DisplayName("查询在线用户列表 - 异常场景")
        void testGetOnlineUserList_Exception() throws Exception {
            QueryOnlineUserReq req = new QueryOnlineUserReq();

            RequestDTO<QueryOnlineUserReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(onlineUserService.getOnlineUserList(any(QueryOnlineUserReq.class)))
                    .thenThrow(new RuntimeException("Redis连接失败"));

            assertThrows(RuntimeException.class, () -> onlineUserController.getOnlineUserList(requestDTO));
            verify(onlineUserService, times(1)).getOnlineUserList(any(QueryOnlineUserReq.class));
        }
    }

    @Nested
    @DisplayName("kickoutUser 测试")
    class KickoutUserTests {

        @Test
        @DisplayName("强制用户下线 - 正常场景")
        void testKickoutUser_Success() throws Exception {
            KickoutUserReq req = new KickoutUserReq();
            req.setToken("test_token_123");

            RequestDTO<KickoutUserReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(onlineUserService).kickoutUser(any(KickoutUserReq.class));

            ResponseDTO<EmptyBody> response = onlineUserController.kickoutUser(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(onlineUserService, times(1)).kickoutUser(any(KickoutUserReq.class));
        }

        @Test
        @DisplayName("强制用户下线 - 异常场景")
        void testKickoutUser_Exception() throws Exception {
            KickoutUserReq req = new KickoutUserReq();
            req.setToken("invalid_token");

            RequestDTO<KickoutUserReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doThrow(new RuntimeException("Token不存在")).when(onlineUserService).kickoutUser(any(KickoutUserReq.class));

            assertThrows(RuntimeException.class, () -> onlineUserController.kickoutUser(requestDTO));
            verify(onlineUserService, times(1)).kickoutUser(any(KickoutUserReq.class));
        }
    }
}