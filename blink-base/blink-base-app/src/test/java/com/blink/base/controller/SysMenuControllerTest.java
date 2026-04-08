package com.blink.base.controller;

import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.CheckMenuRoleRsp;
import com.blink.base.dto.rsp.QueryShowMenuRsp;
import com.blink.base.dto.rsp.QuerySysMenuRsp;
import com.blink.base.dto.vo.SysMenuVO;
import com.blink.base.service.SysMenuService;
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
 * SysMenuController 单元测试类
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SysMenuController 单元测试")
class SysMenuControllerTest {

    @Mock
    private SysMenuService sysMenuService;

    @InjectMocks
    private SysMenuController sysMenuController;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    @Nested
    @DisplayName("saveSysMenu 测试")
    class SaveSysMenuTests {

        @Test
        @DisplayName("新增系统菜单 - 正常场景")
        void testSaveSysMenu_Success() throws Exception {
            AddSysMenuReq req = new AddSysMenuReq();
            req.setMenuName("测试菜单");
            req.setType(1);

            RequestDTO<AddSysMenuReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            SysMenuVO menuVO = new SysMenuVO();
            menuVO.setMenuId(1);
            menuVO.setMenuName("测试菜单");
            when(sysMenuService.saveSysMenu(any(AddSysMenuReq.class))).thenReturn(menuVO);

            ResponseDTO<SysMenuVO> response = sysMenuController.saveSysMenu(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            assertEquals("测试菜单", response.getBody().getMenuName());
            verify(sysMenuService, times(1)).saveSysMenu(any(AddSysMenuReq.class));
        }

        @Test
        @DisplayName("新增系统菜单 - 异常场景")
        void testSaveSysMenu_Exception() throws Exception {
            AddSysMenuReq req = new AddSysMenuReq();
            RequestDTO<AddSysMenuReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(sysMenuService.saveSysMenu(any(AddSysMenuReq.class)))
                    .thenThrow(new RuntimeException("菜单已存在"));

            assertThrows(RuntimeException.class, () -> sysMenuController.saveSysMenu(requestDTO));
            verify(sysMenuService, times(1)).saveSysMenu(any(AddSysMenuReq.class));
        }
    }

    @Nested
    @DisplayName("deleteSysMenu 测试")
    class DeleteSysMenuTests {

        @Test
        @DisplayName("删除系统菜单 - 正常场景")
        void testDeleteSysMenu_Success() throws Exception {
            DeleteSysMenuReq req = new DeleteSysMenuReq();
            req.setDeleteId(1);
            req.setBatchDelete(false);

            RequestDTO<DeleteSysMenuReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(sysMenuService).deleteSysMenu(any(DeleteSysMenuReq.class));

            ResponseDTO<EmptyBody> response = sysMenuController.deleteSysMenu(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(sysMenuService, times(1)).deleteSysMenu(any(DeleteSysMenuReq.class));
        }
    }

    @Nested
    @DisplayName("modifySysMenu 测试")
    class ModifySysMenuTests {

        @Test
        @DisplayName("更新系统菜单 - 正常场景")
        void testModifySysMenu_Success() throws Exception {
            UpdateSysMenuReq req = new UpdateSysMenuReq();
            req.setMenuId(1);
            req.setMenuName("新菜单名");

            RequestDTO<UpdateSysMenuReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            SysMenuVO menuVO = new SysMenuVO();
            menuVO.setMenuId(1);
            menuVO.setMenuName("新菜单名");
            when(sysMenuService.modifySysMenu(any(UpdateSysMenuReq.class))).thenReturn(menuVO);

            ResponseDTO<SysMenuVO> response = sysMenuController.modifySysMenu(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysMenuService, times(1)).modifySysMenu(any(UpdateSysMenuReq.class));
        }
    }

    @Nested
    @DisplayName("getSysMenuList 测试")
    class GetSysMenuListTests {

        @Test
        @DisplayName("查询系统菜单列表 - 正常场景")
        void testGetSysMenuList_Success() throws Exception {
            QuerySysMenuReq req = new QuerySysMenuReq();

            RequestDTO<QuerySysMenuReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            QuerySysMenuRsp rsp = new QuerySysMenuRsp();
            when(sysMenuService.getSysMenuList(any(QuerySysMenuReq.class))).thenReturn(rsp);

            ResponseDTO<QuerySysMenuRsp> response = sysMenuController.getSysMenuList(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysMenuService, times(1)).getSysMenuList(any(QuerySysMenuReq.class));
        }
    }

    @Nested
    @DisplayName("getSysMenusByRoles 测试")
    class GetSysMenusByRolesTests {

        @Test
        @DisplayName("根据角色查询菜单 - 正常场景")
        void testGetSysMenusByRoles_Success() throws Exception {
            QueryShowMenuReq req = new QueryShowMenuReq();

            RequestDTO<QueryShowMenuReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            QueryShowMenuRsp rsp = new QueryShowMenuRsp();
            when(sysMenuService.getSysMenusByRoles(any(QueryShowMenuReq.class))).thenReturn(rsp);

            ResponseDTO<QueryShowMenuRsp> response = sysMenuController.getSysMenusByRoles(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysMenuService, times(1)).getSysMenusByRoles(any(QueryShowMenuReq.class));
        }
    }

    @Nested
    @DisplayName("checkMenuRoleAssignment 测试")
    class CheckMenuRoleAssignmentTests {

        @Test
        @DisplayName("检查菜单角色分配 - 正常场景")
        void testCheckMenuRoleAssignment_Success() throws Exception {
            CheckMenuRoleReq req = new CheckMenuRoleReq();
            req.setMenuId(1);

            RequestDTO<CheckMenuRoleReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            CheckMenuRoleRsp rsp = new CheckMenuRoleRsp();
            when(sysMenuService.checkMenuRoleAssignment(any(CheckMenuRoleReq.class))).thenReturn(rsp);

            ResponseDTO<CheckMenuRoleRsp> response = sysMenuController.checkMenuRoleAssignment(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysMenuService, times(1)).checkMenuRoleAssignment(any(CheckMenuRoleReq.class));
        }
    }
}