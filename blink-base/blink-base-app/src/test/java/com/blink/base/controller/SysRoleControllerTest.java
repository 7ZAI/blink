package com.blink.base.controller;

import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.QuerySysRoleRsp;
import com.blink.base.dto.rsp.QueryUserRolesRsp;
import com.blink.base.dto.rsp.RoleDetailRsp;
import com.blink.base.dto.vo.SysRoleVO;
import com.blink.base.service.SysRoleService;
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
 * SysRoleController 单元测试类
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SysRoleController 单元测试")
class SysRoleControllerTest {

    @Mock
    private SysRoleService sysRoleService;

    @InjectMocks
    private SysRoleController sysRoleController;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    @Nested
    @DisplayName("saveSysRole 测试")
    class SaveSysRoleTests {

        @Test
        @DisplayName("新增系统角色 - 正常场景")
        void testSaveSysRole_Success() throws Exception {
            AddSysRoleReq req = new AddSysRoleReq();
            req.setRoleName("测试角色");
            req.setRoleCode("test_role");

            RequestDTO<AddSysRoleReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            SysRoleVO roleVO = new SysRoleVO();
            roleVO.setRoleId(1);
            roleVO.setRoleName("测试角色");
            when(sysRoleService.saveSysRole(any(AddSysRoleReq.class))).thenReturn(roleVO);

            ResponseDTO<SysRoleVO> response = sysRoleController.saveSysRole(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            assertEquals("测试角色", response.getBody().getRoleName());
            verify(sysRoleService, times(1)).saveSysRole(any(AddSysRoleReq.class));
        }

        @Test
        @DisplayName("新增系统角色 - 异常场景")
        void testSaveSysRole_Exception() throws Exception {
            AddSysRoleReq req = new AddSysRoleReq();
            RequestDTO<AddSysRoleReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(sysRoleService.saveSysRole(any(AddSysRoleReq.class)))
                    .thenThrow(new RuntimeException("角色已存在"));

            assertThrows(RuntimeException.class, () -> sysRoleController.saveSysRole(requestDTO));
            verify(sysRoleService, times(1)).saveSysRole(any(AddSysRoleReq.class));
        }
    }

    @Nested
    @DisplayName("deleteSysRole 测试")
    class DeleteSysRoleTests {

        @Test
        @DisplayName("删除系统角色 - 正常场景")
        void testDeleteSysRole_Success() throws Exception {
            DeleteSysRoleReq req = new DeleteSysRoleReq();
            req.setDeleteId(1); req.setBatchDelete(false);

            RequestDTO<DeleteSysRoleReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(sysRoleService).deleteSysRole(any(DeleteSysRoleReq.class));

            ResponseDTO<EmptyBody> response = sysRoleController.deleteSysRole(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(sysRoleService, times(1)).deleteSysRole(any(DeleteSysRoleReq.class));
        }
    }

    @Nested
    @DisplayName("modifySysRole 测试")
    class ModifySysRoleTests {

        @Test
        @DisplayName("更新系统角色 - 正常场景")
        void testModifySysRole_Success() throws Exception {
            UpdateSysRoleReq req = new UpdateSysRoleReq();
            req.setRoleName("新角色名");

            RequestDTO<UpdateSysRoleReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            SysRoleVO roleVO = new SysRoleVO();
            roleVO.setRoleId(1);
            roleVO.setRoleName("新角色名");
            when(sysRoleService.modifySysRole(any(UpdateSysRoleReq.class))).thenReturn(roleVO);

            ResponseDTO<SysRoleVO> response = sysRoleController.modifySysRole(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysRoleService, times(1)).modifySysRole(any(UpdateSysRoleReq.class));
        }
    }

    @Nested
    @DisplayName("getSysRoleList 测试")
    class GetSysRoleListTests {

        @Test
        @DisplayName("查询系统角色列表 - 正常场景")
        void testGetSysRoleList_Success() throws Exception {
            QuerySysRoleReq req = new QuerySysRoleReq();
            req.setPageNum(1);
            req.setPageSize(10);

            RequestDTO<QuerySysRoleReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            QuerySysRoleRsp rsp = new QuerySysRoleRsp();
            when(sysRoleService.getSysRoleList(any(QuerySysRoleReq.class))).thenReturn(rsp);

            ResponseDTO<QuerySysRoleRsp> response = sysRoleController.getSysRoleList(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysRoleService, times(1)).getSysRoleList(any(QuerySysRoleReq.class));
        }
    }

    @Nested
    @DisplayName("getSysRolesByUser 测试")
    class GetSysRolesByUserTests {

        @Test
        @DisplayName("根据用户查询角色 - 正常场景")
        void testGetSysRolesByUser_Success() throws Exception {
            QueryUserRolesReq req = new QueryUserRolesReq();
            req.setUserId(1);

            RequestDTO<QueryUserRolesReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            QueryUserRolesRsp rsp = new QueryUserRolesRsp();
            when(sysRoleService.getSysRolesByUser(any(QueryUserRolesReq.class))).thenReturn(rsp);

            ResponseDTO<QueryUserRolesRsp> response = sysRoleController.getSysRolesByUser(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysRoleService, times(1)).getSysRolesByUser(any(QueryUserRolesReq.class));
        }
    }

    @Nested
    @DisplayName("assignPermissions 测试")
    class AssignPermissionsTests {

        @Test
        @DisplayName("为角色分配权限 - 正常场景")
        void testAssignPermissions_Success() throws Exception {
            AssignPermissionReq req = new AssignPermissionReq();
            req.setRoleId(1);

            RequestDTO<AssignPermissionReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(sysRoleService).assignPermissions(any(AssignPermissionReq.class));

            ResponseDTO<EmptyBody> response = sysRoleController.assignPermissions(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(sysRoleService, times(1)).assignPermissions(any(AssignPermissionReq.class));
        }
    }

    @Nested
    @DisplayName("assignMenus 测试")
    class AssignMenusTests {

        @Test
        @DisplayName("为角色分配菜单 - 正常场景")
        void testAssignMenus_Success() throws Exception {
            AssignMenuReq req = new AssignMenuReq();
            req.setRoleId(1);

            RequestDTO<AssignMenuReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(sysRoleService).assignMenus(any(AssignMenuReq.class));

            ResponseDTO<EmptyBody> response = sysRoleController.assignMenus(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(sysRoleService, times(1)).assignMenus(any(AssignMenuReq.class));
        }
    }

    @Nested
    @DisplayName("getRoleDetail 测试")
    class GetRoleDetailTests {

        @Test
        @DisplayName("查询角色详情 - 正常场景")
        void testGetRoleDetail_Success() throws Exception {
            QueryRoleDetailReq req = new QueryRoleDetailReq();
            req.setRoleId(1);

            RequestDTO<QueryRoleDetailReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            RoleDetailRsp rsp = new RoleDetailRsp();
            when(sysRoleService.getRoleDetail(any(QueryRoleDetailReq.class))).thenReturn(rsp);

            ResponseDTO<RoleDetailRsp> response = sysRoleController.getRoleDetail(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysRoleService, times(1)).getRoleDetail(any(QueryRoleDetailReq.class));
        }
    }

    @Nested
    @DisplayName("assignRoleToUsers 测试")
    class AssignRoleToUsersTests {

        @Test
        @DisplayName("为用户分配角色 - 正常场景")
        void testAssignRoleToUsers_Success() throws Exception {
            AssignRoleToUsersReq req = new AssignRoleToUsersReq();

            RequestDTO<AssignRoleToUsersReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(sysRoleService).assignRoleToUsers(any(AssignRoleToUsersReq.class));

            ResponseDTO<EmptyBody> response = sysRoleController.assignRoleToUsers(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(sysRoleService, times(1)).assignRoleToUsers(any(AssignRoleToUsersReq.class));
        }
    }
}