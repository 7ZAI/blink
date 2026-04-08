package com.blink.base.controller;

import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.GetAllApiPermissionsRsp;
import com.blink.base.dto.rsp.QueryPermissionIdentityRsp;
import com.blink.base.dto.rsp.QuerySysPermissionRsp;
import com.blink.base.dto.rsp.QueryUserPermissionRsp;
import com.blink.base.dto.vo.SysPermissionVO;
import com.blink.base.service.SysPermissionService;
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
 * SysPermissionController 单元测试类
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SysPermissionController 单元测试")
class SysPermissionControllerTest {

    @Mock
    private SysPermissionService sysPermissionService;

    @InjectMocks
    private SysPermissionController sysPermissionController;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    @Nested
    @DisplayName("saveSysPermission 测试")
    class SaveSysPermissionTests {

        @Test
        @DisplayName("新增权限 - 正常场景")
        void testSaveSysPermission_Success() throws Exception {
            AddSysPermissionReq req = new AddSysPermissionReq();
            req.setAcName("测试权限");
            req.setAcIdentity("test_perm");

            RequestDTO<AddSysPermissionReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            SysPermissionVO permVO = new SysPermissionVO();
            permVO.setAcId(1);
            permVO.setAcName("测试权限");
            when(sysPermissionService.saveSysPermission(any(AddSysPermissionReq.class))).thenReturn(permVO);

            ResponseDTO<SysPermissionVO> response = sysPermissionController.saveSysPermission(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysPermissionService, times(1)).saveSysPermission(any(AddSysPermissionReq.class));
        }
    }

    @Nested
    @DisplayName("deleteSysPermission 测试")
    class DeleteSysPermissionTests {

        @Test
        @DisplayName("删除权限 - 正常场景")
        void testDeleteSysPermission_Success() throws Exception {
            DeleteSysPermissionReq req = new DeleteSysPermissionReq();
            req.setDeleteId(1);
            req.setBatchDelete(false);

            RequestDTO<DeleteSysPermissionReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(sysPermissionService).deleteSysPermission(any(DeleteSysPermissionReq.class));

            ResponseDTO<EmptyBody> response = sysPermissionController.deleteSysPermission(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(sysPermissionService, times(1)).deleteSysPermission(any(DeleteSysPermissionReq.class));
        }
    }

    @Nested
    @DisplayName("modifySysPermission 测试")
    class ModifySysPermissionTests {

        @Test
        @DisplayName("更新权限 - 正常场景")
        void testModifySysPermission_Success() throws Exception {
            UpdateSysPermissionReq req = new UpdateSysPermissionReq();
            req.setAcId(1);

            RequestDTO<UpdateSysPermissionReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(sysPermissionService).modifySysPermission(any(UpdateSysPermissionReq.class));

            ResponseDTO<EmptyBody> response = sysPermissionController.modifySysPermission(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(sysPermissionService, times(1)).modifySysPermission(any(UpdateSysPermissionReq.class));
        }
    }

    @Nested
    @DisplayName("getSysPermissionList 测试")
    class GetSysPermissionListTests {

        @Test
        @DisplayName("查询权限列表 - 正常场景")
        void testGetSysPermissionList_Success() throws Exception {
            QuerySysPermissionReq req = new QuerySysPermissionReq();

            RequestDTO<QuerySysPermissionReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            QuerySysPermissionRsp<SysPermissionVO> rsp = new QuerySysPermissionRsp<>();
            when(sysPermissionService.getSysPermissionList(any(QuerySysPermissionReq.class))).thenReturn(rsp);

            ResponseDTO<QuerySysPermissionRsp<SysPermissionVO>> response = sysPermissionController.getSysPermissionList(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysPermissionService, times(1)).getSysPermissionList(any(QuerySysPermissionReq.class));
        }
    }

    @Nested
    @DisplayName("getPermissionByUrl 测试")
    class GetPermissionByUrlTests {

        @Test
        @DisplayName("根据URL查询权限 - 正常场景")
        void testGetPermissionByUrl_Success() throws Exception {
            QueryPermissionIdentityReq req = new QueryPermissionIdentityReq();
            req.setUrl("/api/test");

            RequestDTO<QueryPermissionIdentityReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            QueryPermissionIdentityRsp rsp = new QueryPermissionIdentityRsp();
            when(sysPermissionService.getPermissionByUrl(any(QueryPermissionIdentityReq.class))).thenReturn(rsp);

            ResponseDTO<QueryPermissionIdentityRsp> response = sysPermissionController.getPermissionByUrl(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysPermissionService, times(1)).getPermissionByUrl(any(QueryPermissionIdentityReq.class));
        }
    }

    @Nested
    @DisplayName("getAllApiPermission 测试")
    class GetAllApiPermissionTests {

        @Test
        @DisplayName("获取所有接口权限 - 正常场景")
        void testGetAllApiPermission_Success() throws Exception {
            GetAllApiPermissionsReq req = new GetAllApiPermissionsReq();

            RequestDTO<GetAllApiPermissionsReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            GetAllApiPermissionsRsp rsp = new GetAllApiPermissionsRsp();
            when(sysPermissionService.getAllApiPermission(any(GetAllApiPermissionsReq.class))).thenReturn(rsp);

            ResponseDTO<GetAllApiPermissionsRsp> response = sysPermissionController.getAllApiPermission(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysPermissionService, times(1)).getAllApiPermission(any(GetAllApiPermissionsReq.class));
        }
    }

    @Nested
    @DisplayName("getPermissions 测试")
    class GetPermissionsTests {

        @Test
        @DisplayName("根据用户ID或URL查询权限 - 正常场景")
        void testGetPermissions_Success() throws Exception {
            QueryUserPermissionReq req = new QueryUserPermissionReq();
            req.setUserId(1);

            RequestDTO<QueryUserPermissionReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            QueryUserPermissionRsp rsp = new QueryUserPermissionRsp();
            when(sysPermissionService.getPermissions(any(QueryUserPermissionReq.class))).thenReturn(rsp);

            ResponseDTO<QueryUserPermissionRsp> response = sysPermissionController.getPermissions(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysPermissionService, times(1)).getPermissions(any(QueryUserPermissionReq.class));
        }

        @Test
        @DisplayName("根据URL查询权限 - 正常场景")
        void testGetPermissionsByUrl_Success() throws Exception {
            QueryUserPermissionReq req = new QueryUserPermissionReq();
            req.setUrl("/api/test");

            RequestDTO<QueryUserPermissionReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            QueryUserPermissionRsp rsp = new QueryUserPermissionRsp();
            when(sysPermissionService.getPermissions(any(QueryUserPermissionReq.class))).thenReturn(rsp);

            ResponseDTO<QueryUserPermissionRsp> response = sysPermissionController.getPermissions(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysPermissionService, times(1)).getPermissions(any(QueryUserPermissionReq.class));
        }
    }
}