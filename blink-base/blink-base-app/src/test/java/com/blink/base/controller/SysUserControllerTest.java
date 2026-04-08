package com.blink.base.controller;

import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.SysUserRsp;
import com.blink.base.dto.rsp.UserPermissionRsp;
import com.blink.base.dto.vo.SysUserVO;
import com.blink.base.dto.vo.UserPreferenceVO;
import com.blink.base.service.SysUserPreferenceService;
import com.blink.base.service.SysUserService;
import com.blink.framework.common.context.BlinkRequestContextHolder;
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
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SysUserController 单元测试类
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SysUserController 单元测试")
class SysUserControllerTest {

    @Mock
    private SysUserService sysUserService;

    @Mock
    private SysUserPreferenceService sysUserPreferenceService;

    @InjectMocks
    private SysUserController sysUserController;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    @Nested
    @DisplayName("saveSysUser 测试")
    class SaveSysUserTests {

        @Test
        @DisplayName("新增系统用户 - 正常场景")
        void testSaveSysUser_Success() throws Exception {
            AddSysUserReq req = new AddSysUserReq();
            req.setLoginName("testuser");
            req.setUsername("测试用户");

            RequestDTO<AddSysUserReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(sysUserService).saveSysUser(any(AddSysUserReq.class));

            ResponseDTO<EmptyBody> response = sysUserController.saveSysUser(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(sysUserService, times(1)).saveSysUser(any(AddSysUserReq.class));
        }

        @Test
        @DisplayName("新增系统用户 - 异常场景")
        void testSaveSysUser_Exception() throws Exception {
            AddSysUserReq req = new AddSysUserReq();
            RequestDTO<AddSysUserReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doThrow(new RuntimeException("保存失败")).when(sysUserService).saveSysUser(any(AddSysUserReq.class));

            assertThrows(RuntimeException.class, () -> sysUserController.saveSysUser(requestDTO));
            verify(sysUserService, times(1)).saveSysUser(any(AddSysUserReq.class));
        }
    }

    @Nested
    @DisplayName("deleteSysUser 测试")
    class DeleteSysUserTests {

        @Test
        @DisplayName("删除系统用户 - 正常场景")
        void testDeleteSysUser_Success() throws Exception {
            DeleteSysUserReq req = new DeleteSysUserReq();
            req.setUserId(1);

            RequestDTO<DeleteSysUserReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(sysUserService).deleteSysUser(any(DeleteSysUserReq.class));

            ResponseDTO<EmptyBody> response = sysUserController.deleteSysUser(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(sysUserService, times(1)).deleteSysUser(any(DeleteSysUserReq.class));
        }
    }

    @Nested
    @DisplayName("modifySysUser 测试")
    class ModifySysUserTests {

        @Test
        @DisplayName("更新系统用户 - 正常场景")
        void testModifySysUser_Success() throws Exception {
            UpdateSysUserReq req = new UpdateSysUserReq();
            req.setUserId(1);
            req.setUsername("新昵称");

            RequestDTO<UpdateSysUserReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(sysUserService).modifySysUser(any(UpdateSysUserReq.class));

            ResponseDTO<EmptyBody> response = sysUserController.modifySysUser(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(sysUserService, times(1)).modifySysUser(any(UpdateSysUserReq.class));
        }
    }

    @Nested
    @DisplayName("getSysUserList 测试")
    class GetSysUserListTests {

        @Test
        @DisplayName("查询系统用户列表 - 正常场景")
        void testGetSysUserList_Success() throws Exception {
            QuerySysUserReq req = new QuerySysUserReq();
            req.setPageNum(1);
            req.setPageSize(10);

            RequestDTO<QuerySysUserReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            SysUserRsp rsp = new SysUserRsp();
            when(sysUserService.getSysUserList(any(QuerySysUserReq.class))).thenReturn(rsp);

            ResponseDTO<SysUserRsp> response = sysUserController.getSysUserList(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysUserService, times(1)).getSysUserList(any(QuerySysUserReq.class));
        }
    }

    @Nested
    @DisplayName("getSysUserDetail 测试")
    class GetSysUserDetailTests {

        @Test
        @DisplayName("查询系统用户详情 - 正常场景")
        void testGetSysUserDetail_Success() throws Exception {
            QuerySysUserReq req = new QuerySysUserReq();
            req.setLoginName("testuser");

            RequestDTO<QuerySysUserReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            SysUserVO userVO = new SysUserVO();
            userVO.setUserId(1);
            userVO.setLoginName("testuser");
            when(sysUserService.getSysUserDetail(any(QuerySysUserReq.class))).thenReturn(userVO);

            ResponseDTO<SysUserVO> response = sysUserController.getSysUserDetail(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            assertEquals("testuser", response.getBody().getLoginName());
            verify(sysUserService, times(1)).getSysUserDetail(any(QuerySysUserReq.class));
        }
    }

    @Nested
    @DisplayName("lockUser 测试")
    class LockUserTests {

        @Test
        @DisplayName("锁定用户 - 正常场景")
        void testLockUser_Success() throws Exception {
            LockUserReq req = new LockUserReq();
            req.setUserId(1);
            req.setLocked(1);

            RequestDTO<LockUserReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(sysUserService).lockUser(anyInt(), anyInt());

            ResponseDTO<EmptyBody> response = sysUserController.lockUser(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(sysUserService, times(1)).lockUser(1, 1);
        }
    }

    @Nested
    @DisplayName("assignUserRoles 测试")
    class AssignUserRolesTests {

        @Test
        @DisplayName("分配用户角色 - 正常场景")
        void testAssignUserRoles_Success() throws Exception {
            AssignUserRoleReq req = new AssignUserRoleReq();
            req.setUserIdList(java.util.List.of(1));
            req.setRoleIdList(java.util.List.of(1));

            RequestDTO<AssignUserRoleReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(sysUserService).assignUserRoles(any(AssignUserRoleReq.class));

            ResponseDTO<EmptyBody> response = sysUserController.assignUserRoles(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(sysUserService, times(1)).assignUserRoles(any(AssignUserRoleReq.class));
        }
    }

    @Nested
    @DisplayName("modifyPassword 测试")
    class ModifyPasswordTests {

        @Test
        @DisplayName("修改密码 - 正常场景")
        void testModifyPassword_Success() throws Exception {
            ModifyPasswordReq req = new ModifyPasswordReq();
            req.setOldPassword("oldPass");
            req.setNewPassword("newPass");

            RequestDTO<ModifyPasswordReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(sysUserService).modifyPassword(any(ModifyPasswordReq.class));

            ResponseDTO<EmptyBody> response = sysUserController.modifyPassword(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(sysUserService, times(1)).modifyPassword(any(ModifyPasswordReq.class));
        }
    }

    @Nested
    @DisplayName("resetPassword 测试")
    class ResetPasswordTests {

        @Test
        @DisplayName("重置密码 - 正常场景")
        void testResetPassword_Success() throws Exception {
            ResetPasswordReq req = new ResetPasswordReq();
            req.setUserId(1);

            RequestDTO<ResetPasswordReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(sysUserService).resetPassword(any(ResetPasswordReq.class));

            ResponseDTO<EmptyBody> response = sysUserController.resetPassword(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(sysUserService, times(1)).resetPassword(any(ResetPasswordReq.class));
        }
    }

    @Nested
    @DisplayName("saveUserPreference 测试")
    class SaveUserPreferenceTests {

        @Test
        @DisplayName("保存用户偏好设置 - 正常场景")
        void testSaveUserPreference_Success() throws Exception {
            SaveUserPreferenceReq req = new SaveUserPreferenceReq();

            RequestDTO<SaveUserPreferenceReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            try (MockedStatic<BlinkRequestContextHolder> mockedStatic = mockStatic(BlinkRequestContextHolder.class)) {
                mockedStatic.when(BlinkRequestContextHolder::getUserId).thenReturn("1");

                doNothing().when(sysUserPreferenceService).saveOrUpdatePreference(anyInt(), any(SaveUserPreferenceReq.class));

                ResponseDTO<EmptyBody> response = sysUserController.saveUserPreference(requestDTO);

                assertNotNull(response);
                assertEquals("BLINK0000", response.getMsgCode());
                verify(sysUserPreferenceService, times(1)).saveOrUpdatePreference(anyInt(), any(SaveUserPreferenceReq.class));
            }
        }
    }

    @Nested
    @DisplayName("getUserPreference 测试")
    class GetUserPreferenceTests {

        @Test
        @DisplayName("获取用户偏好设置 - 正常场景")
        void testGetUserPreference_Success() throws Exception {
            RequestDTO<EmptyBody> requestDTO = new RequestDTO<>();

            UserPreferenceVO preferenceVO = new UserPreferenceVO();

            try (MockedStatic<BlinkRequestContextHolder> mockedStatic = mockStatic(BlinkRequestContextHolder.class)) {
                mockedStatic.when(BlinkRequestContextHolder::getUserId).thenReturn("1");

                when(sysUserPreferenceService.getPreferenceByUserId(anyInt())).thenReturn(preferenceVO);

                ResponseDTO<UserPreferenceVO> response = sysUserController.getUserPreference(requestDTO);

                assertNotNull(response);
                assertEquals("BLINK0000", response.getMsgCode());
                assertNotNull(response.getBody());
                verify(sysUserPreferenceService, times(1)).getPreferenceByUserId(anyInt());
            }
        }
    }

    @Nested
    @DisplayName("getUserPermissions 测试")
    class GetUserPermissionsTests {

        @Test
        @DisplayName("获取用户权限信息 - 正常场景")
        void testGetUserPermissions_Success() throws Exception {
            UserIdReq req = new UserIdReq();
            req.setUserId(1);

            RequestDTO<UserIdReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            UserPermissionRsp permissionRsp = new UserPermissionRsp();
            when(sysUserService.getUserPermissions(any(UserIdReq.class))).thenReturn(permissionRsp);

            ResponseDTO<UserPermissionRsp> response = sysUserController.getUserPermissions(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysUserService, times(1)).getUserPermissions(any(UserIdReq.class));
        }
    }
}