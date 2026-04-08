package com.blink.base.controller;

import com.blink.base.dto.req.FirstTimeResetPasswordReq;
import com.blink.base.dto.req.SysLoginReq;
import com.blink.base.dto.req.SysLogoutReq;
import com.blink.base.dto.rsp.LoginConfigRsp;
import com.blink.base.dto.rsp.SysLoginRsp;
import com.blink.base.dto.vo.SysUserVO;
import com.blink.base.service.UserAuthService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * UserAuthController 单元测试类
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserAuthController 单元测试")
class UserAuthControllerTest {

    @Mock
    private UserAuthService userAuthService;

    @InjectMocks
    private UserAuthController userAuthController;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    @Nested
    @DisplayName("login 测试")
    class LoginTests {

        @Test
        @DisplayName("用户登录 - 正常场景")
        void testLogin_Success() throws Exception {
            SysLoginReq req = new SysLoginReq();
            req.setLoginName("admin");
            req.setPassword("123456");

            RequestDTO<SysLoginReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            SysLoginRsp rsp = new SysLoginRsp();
            rsp.setToken("test_token");
            when(userAuthService.login(any(SysLoginReq.class))).thenReturn(rsp);

            ResponseDTO<SysLoginRsp> response = userAuthController.login(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            assertEquals("test_token", response.getBody().getToken());
            verify(userAuthService, times(1)).login(any(SysLoginReq.class));
        }

        @Test
        @DisplayName("用户登录 - 密码错误")
        void testLogin_WrongPassword() throws Exception {
            SysLoginReq req = new SysLoginReq();
            req.setLoginName("admin");
            req.setPassword("wrong_password");

            RequestDTO<SysLoginReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(userAuthService.login(any(SysLoginReq.class)))
                    .thenThrow(new RuntimeException("密码错误"));

            assertThrows(RuntimeException.class, () -> userAuthController.login(requestDTO));
            verify(userAuthService, times(1)).login(any(SysLoginReq.class));
        }
    }

    @Nested
    @DisplayName("logout 测试")
    class LogoutTests {

        @Test
        @DisplayName("用户登出 - 正常场景")
        void testLogout_Success() throws Exception {
            SysLogoutReq req = new SysLogoutReq();
            req.setToken("test_token");

            RequestDTO<SysLogoutReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(userAuthService).logout(any(SysLogoutReq.class));

            ResponseDTO<EmptyBody> response = userAuthController.logout(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(userAuthService, times(1)).logout(any(SysLogoutReq.class));
        }
    }

    @Nested
    @DisplayName("getUserInfo 测试")
    class GetUserInfoTests {

        @Test
        @DisplayName("获取当前登录用户信息 - 正常场景")
        void testGetUserInfo_Success() throws Exception {
            RequestDTO<EmptyBody> requestDTO = new RequestDTO<>();
            requestDTO.setToken("test_token");

            SysLoginRsp rsp = new SysLoginRsp();
            rsp.setToken("test_token");
            SysUserVO userVO = new SysUserVO();
            userVO.setLoginName("admin");
            rsp.setUserInfo(userVO);
            when(userAuthService.getLoginUserInfo(anyString())).thenReturn(rsp);

            ResponseDTO<SysLoginRsp> response = userAuthController.getUserInfo(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            assertEquals("admin", response.getBody().getUserInfo().getLoginName());
            verify(userAuthService, times(1)).getLoginUserInfo(anyString());
        }

        @Test
        @DisplayName("获取当前登录用户信息 - Token无效")
        void testGetUserInfo_InvalidToken() throws Exception {
            RequestDTO<EmptyBody> requestDTO = new RequestDTO<>();
            requestDTO.setToken("invalid_token");

            when(userAuthService.getLoginUserInfo(anyString()))
                    .thenThrow(new RuntimeException("Token无效"));

            assertThrows(RuntimeException.class, () -> userAuthController.getUserInfo(requestDTO));
            verify(userAuthService, times(1)).getLoginUserInfo(anyString());
        }
    }

    @Nested
    @DisplayName("getLoginConfig 测试")
    class GetLoginConfigTests {

        @Test
        @DisplayName("获取登录配置 - 正常场景")
        void testGetLoginConfig_Success() throws Exception {
            RequestDTO<EmptyBody> requestDTO = new RequestDTO<>();

            LoginConfigRsp rsp = new LoginConfigRsp();
            when(userAuthService.getLoginConfig()).thenReturn(rsp);

            ResponseDTO<LoginConfigRsp> response = userAuthController.getLoginConfig(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(userAuthService, times(1)).getLoginConfig();
        }
    }

    @Nested
    @DisplayName("firstTimeResetPassword 测试")
    class FirstTimeResetPasswordTests {

        @Test
        @DisplayName("首次登录重置密码 - 正常场景")
        void testFirstTimeResetPassword_Success() throws Exception {
            FirstTimeResetPasswordReq req = new FirstTimeResetPasswordReq();
            req.setNewPassword("newPassword123");

            RequestDTO<FirstTimeResetPasswordReq> requestDTO = new RequestDTO<>();
            requestDTO.setToken("test_token");
            requestDTO.setBody(req);

            doNothing().when(userAuthService).firstTimeResetPassword(anyString(), any(FirstTimeResetPasswordReq.class));

            ResponseDTO<EmptyBody> response = userAuthController.firstTimeResetPassword(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(userAuthService, times(1)).firstTimeResetPassword(anyString(), any(FirstTimeResetPasswordReq.class));
        }

        @Test
        @DisplayName("首次登录重置密码 - 密码格式不正确")
        void testFirstTimeResetPassword_InvalidPassword() throws Exception {
            FirstTimeResetPasswordReq req = new FirstTimeResetPasswordReq();
            req.setNewPassword("123");

            RequestDTO<FirstTimeResetPasswordReq> requestDTO = new RequestDTO<>();
            requestDTO.setToken("test_token");
            requestDTO.setBody(req);

            doThrow(new RuntimeException("密码格式不正确")).when(userAuthService)
                    .firstTimeResetPassword(anyString(), any(FirstTimeResetPasswordReq.class));

            assertThrows(RuntimeException.class, () -> userAuthController.firstTimeResetPassword(requestDTO));
            verify(userAuthService, times(1)).firstTimeResetPassword(anyString(), any(FirstTimeResetPasswordReq.class));
        }
    }
}