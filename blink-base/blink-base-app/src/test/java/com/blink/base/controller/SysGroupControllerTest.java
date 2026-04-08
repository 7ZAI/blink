package com.blink.base.controller;

import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.QuerySysGroupRsp;
import com.blink.base.dto.rsp.SysGroupRsp;
import com.blink.base.service.SysGroupService;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.record.RequestRecord;
import com.blink.framework.common.record.ResponseRecord;
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
 * SysGroupController 单元测试类
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SysGroupController 单元测试")
class SysGroupControllerTest {

    @Mock
    private SysGroupService sysGroupService;

    @InjectMocks
    private SysGroupController sysGroupController;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    @Nested
    @DisplayName("saveSysGroup 测试")
    class SaveSysGroupTests {

        @Test
        @DisplayName("新增组 - 正常场景")
        void testSaveSysGroup_Success() throws Exception {
            AddSysGroupReq req = new AddSysGroupReq();
            req.setGroupName("测试组");

            RequestRecord<AddSysGroupReq> requestRecord = RequestRecord.of(req);

            SysGroupRsp groupRsp = new SysGroupRsp();
            groupRsp.setGroupId(1);
            groupRsp.setGroupName("测试组");
            when(sysGroupService.saveSysGroup(any(AddSysGroupReq.class))).thenReturn(groupRsp);

            ResponseRecord<SysGroupRsp> response = sysGroupController.saveSysGroup(requestRecord);

            assertNotNull(response);
            verify(sysGroupService, times(1)).saveSysGroup(any(AddSysGroupReq.class));
        }
    }

    @Nested
    @DisplayName("deleteSysGroup 测试")
    class DeleteSysGroupTests {

        @Test
        @DisplayName("删除组 - 正常场景")
        void testDeleteSysGroup_Success() throws Exception {
            DeleteSysGroupReq req = new DeleteSysGroupReq();
            req.setDeleteId(1);
            req.setBatchDelete(false);

            RequestDTO<DeleteSysGroupReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(sysGroupService).deleteSysGroup(any(DeleteSysGroupReq.class));

            ResponseDTO<EmptyBody> response = sysGroupController.deleteSysGroup(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(sysGroupService, times(1)).deleteSysGroup(any(DeleteSysGroupReq.class));
        }
    }

    @Nested
    @DisplayName("modifySysGroup 测试")
    class ModifySysGroupTests {

        @Test
        @DisplayName("更新组 - 正常场景")
        void testModifySysGroup_Success() throws Exception {
            UpdateSysGroupReq req = new UpdateSysGroupReq();
            req.setGroupId(1);
            req.setGroupName("新组名");

            RequestDTO<UpdateSysGroupReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            SysGroupRsp groupRsp = new SysGroupRsp();
            groupRsp.setGroupId(1);
            groupRsp.setGroupName("新组名");
            when(sysGroupService.modifySysGroup(any(UpdateSysGroupReq.class))).thenReturn(groupRsp);

            ResponseDTO<SysGroupRsp> response = sysGroupController.modifySysGroup(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysGroupService, times(1)).modifySysGroup(any(UpdateSysGroupReq.class));
        }
    }

    @Nested
    @DisplayName("getSysGroupList 测试")
    class GetSysGroupListTests {

        @Test
        @DisplayName("查询组列表 - 正常场景")
        void testGetSysGroupList_Success() throws Exception {
            QuerySysGroupReq req = new QuerySysGroupReq();

            RequestDTO<QuerySysGroupReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            QuerySysGroupRsp rsp = new QuerySysGroupRsp();
            when(sysGroupService.getSysGroupList(any(QuerySysGroupReq.class))).thenReturn(rsp);

            ResponseDTO<QuerySysGroupRsp> response = sysGroupController.getSysGroupList(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysGroupService, times(1)).getSysGroupList(any(QuerySysGroupReq.class));
        }
    }
}