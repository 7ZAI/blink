package com.blink.base.controller;

import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.QuerySysConfigGroupRsp;
import com.blink.base.service.SysConfigGroupService;
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
 * SysConfigGroupController 单元测试类
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SysConfigGroupController 单元测试")
class SysConfigGroupControllerTest {

    @Mock
    private SysConfigGroupService sysConfigGroupService;

    @InjectMocks
    private SysConfigGroupController sysConfigGroupController;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    @Nested
    @DisplayName("saveSysConfigGroup 测试")
    class SaveSysConfigGroupTests {

        @Test
        @DisplayName("新增参数分组 - 正常场景")
        void testSaveSysConfigGroup_Success() throws Exception {
            AddSysConfigGroupReq req = new AddSysConfigGroupReq();
            req.setGroupName("测试分组");
            req.setGroupKey("test_group");

            RequestDTO<AddSysConfigGroupReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(sysConfigGroupService).saveSysConfigGroup(any(AddSysConfigGroupReq.class));

            ResponseDTO<EmptyBody> response = sysConfigGroupController.saveSysConfigGroup(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(sysConfigGroupService, times(1)).saveSysConfigGroup(any(AddSysConfigGroupReq.class));
        }
    }

    @Nested
    @DisplayName("deleteSysConfigGroup 测试")
    class DeleteSysConfigGroupTests {

        @Test
        @DisplayName("删除参数分组 - 正常场景")
        void testDeleteSysConfigGroup_Success() throws Exception {
            DeleteSysConfigGroupReq req = new DeleteSysConfigGroupReq();
            req.setDeleteId(1); req.setBatchDelete(false);

            RequestDTO<DeleteSysConfigGroupReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(sysConfigGroupService).deleteSysConfigGroup(any(DeleteSysConfigGroupReq.class));

            ResponseDTO<EmptyBody> response = sysConfigGroupController.deleteSysConfigGroup(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(sysConfigGroupService, times(1)).deleteSysConfigGroup(any(DeleteSysConfigGroupReq.class));
        }
    }

    @Nested
    @DisplayName("modifySysConfigGroup 测试")
    class ModifySysConfigGroupTests {

        @Test
        @DisplayName("更新参数分组 - 正常场景")
        void testModifySysConfigGroup_Success() throws Exception {
            UpdateSysConfigGroupReq req = new UpdateSysConfigGroupReq();
            req.setId(1);
            req.setGroupName("新分组名");

            RequestDTO<UpdateSysConfigGroupReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(sysConfigGroupService).modifySysConfigGroup(any(UpdateSysConfigGroupReq.class));

            ResponseDTO<EmptyBody> response = sysConfigGroupController.modifySysConfigGroup(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(sysConfigGroupService, times(1)).modifySysConfigGroup(any(UpdateSysConfigGroupReq.class));
        }
    }

    @Nested
    @DisplayName("getSysConfigGroupList 测试")
    class GetSysConfigGroupListTests {

        @Test
        @DisplayName("查询参数分组列表 - 正常场景")
        void testGetSysConfigGroupList_Success() throws Exception {
            QuerySysConfigGroupReq req = new QuerySysConfigGroupReq();

            RequestDTO<QuerySysConfigGroupReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            QuerySysConfigGroupRsp rsp = new QuerySysConfigGroupRsp();
            when(sysConfigGroupService.getSysConfigGroupList(any(QuerySysConfigGroupReq.class))).thenReturn(rsp);

            ResponseDTO<QuerySysConfigGroupRsp> response = sysConfigGroupController.getSysConfigGroupList(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysConfigGroupService, times(1)).getSysConfigGroupList(any(QuerySysConfigGroupReq.class));
        }
    }
}