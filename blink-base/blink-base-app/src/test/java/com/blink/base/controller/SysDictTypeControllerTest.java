package com.blink.base.controller;

import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.QuerySysDictTypeRsp;
import com.blink.base.dto.vo.SysDictTypeVO;
import com.blink.base.service.SysDictTypeService;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * SysDictTypeController 单元测试类
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SysDictTypeController 单元测试")
class SysDictTypeControllerTest {

    @Mock
    private SysDictTypeService sysDictTypeService;

    @InjectMocks
    private SysDictTypeController sysDictTypeController;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    @Nested
    @DisplayName("saveSysDictType 测试")
    class SaveSysDictTypeTests {

        @Test
        @DisplayName("新增字典类型 - 正常场景")
        void testSaveSysDictType_Success() throws Exception {
            AddSysDictTypeReq req = new AddSysDictTypeReq();
            req.setDictName("测试字典");
            req.setDictType("test_dict");

            RequestDTO<AddSysDictTypeReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            SysDictTypeVO dictVO = new SysDictTypeVO();
            dictVO.setDictId(1);
            dictVO.setDictName("测试字典");
            when(sysDictTypeService.saveSysDictType(any(AddSysDictTypeReq.class))).thenReturn(dictVO);

            ResponseDTO<SysDictTypeVO> response = sysDictTypeController.saveSysDictType(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysDictTypeService, times(1)).saveSysDictType(any(AddSysDictTypeReq.class));
        }
    }

    @Nested
    @DisplayName("deleteSysDictType 测试")
    class DeleteSysDictTypeTests {

        @Test
        @DisplayName("删除字典类型 - 正常场景")
        void testDeleteSysDictType_Success() throws Exception {
            DeleteSysDictTypeReq req = new DeleteSysDictTypeReq();
            req.setDeleteId(1); req.setBatchDelete(false);

            RequestDTO<DeleteSysDictTypeReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(sysDictTypeService).deleteSysDictType(any(DeleteSysDictTypeReq.class));

            ResponseDTO<EmptyBody> response = sysDictTypeController.deleteSysDictType(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(sysDictTypeService, times(1)).deleteSysDictType(any(DeleteSysDictTypeReq.class));
        }
    }

    @Nested
    @DisplayName("modifySysDictType 测试")
    class ModifySysDictTypeTests {

        @Test
        @DisplayName("更新字典类型 - 正常场景")
        void testModifySysDictType_Success() throws Exception {
            UpdateSysDictTypeReq req = new UpdateSysDictTypeReq();
            req.setDictName("新字典名");

            RequestDTO<UpdateSysDictTypeReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            SysDictTypeVO dictVO = new SysDictTypeVO();
            dictVO.setDictId(1);
            dictVO.setDictName("新字典名");
            when(sysDictTypeService.modifySysDictType(any(UpdateSysDictTypeReq.class))).thenReturn(dictVO);

            ResponseDTO<SysDictTypeVO> response = sysDictTypeController.modifySysDictType(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysDictTypeService, times(1)).modifySysDictType(any(UpdateSysDictTypeReq.class));
        }
    }

    @Nested
    @DisplayName("getSysDictTypeList 测试")
    class GetSysDictTypeListTests {

        @Test
        @DisplayName("查询字典类型列表 - 正常场景")
        void testGetSysDictTypeList_Success() throws Exception {
            QuerySysDictTypeReq req = new QuerySysDictTypeReq();

            RequestDTO<QuerySysDictTypeReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            QuerySysDictTypeRsp rsp = new QuerySysDictTypeRsp();
            when(sysDictTypeService.getSysDictTypeList(any(QuerySysDictTypeReq.class))).thenReturn(rsp);

            ResponseDTO<QuerySysDictTypeRsp> response = sysDictTypeController.getSysDictTypeList(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysDictTypeService, times(1)).getSysDictTypeList(any(QuerySysDictTypeReq.class));
        }
    }

    @Nested
    @DisplayName("getSysDictTypeByType 测试")
    class GetSysDictTypeByTypeTests {

        @Test
        @DisplayName("根据类型编码查询字典类型 - 正常场景")
        void testGetSysDictTypeByType_Success() throws Exception {
            QuerySysDictTypeReq req = new QuerySysDictTypeReq();
            req.setDictType("test_dict");

            RequestDTO<QuerySysDictTypeReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            SysDictTypeVO dictVO = new SysDictTypeVO();
            dictVO.setDictType("test_dict");
            when(sysDictTypeService.getSysDictTypeByType(anyString())).thenReturn(dictVO);

            ResponseDTO<SysDictTypeVO> response = sysDictTypeController.getSysDictTypeByType(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysDictTypeService, times(1)).getSysDictTypeByType(anyString());
        }
    }

    @Nested
    @DisplayName("getSysDictTypeById 测试")
    class GetSysDictTypeByIdTests {

        @Test
        @DisplayName("根据ID查询字典类型 - 正常场景")
        void testGetSysDictTypeById_Success() throws Exception {
            QuerySysDictTypeReq req = new QuerySysDictTypeReq();
            req.setDictId(1);

            RequestDTO<QuerySysDictTypeReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            SysDictTypeVO dictVO = new SysDictTypeVO();
            dictVO.setDictId(1);
            when(sysDictTypeService.getSysDictTypeById(anyInt())).thenReturn(dictVO);

            ResponseDTO<SysDictTypeVO> response = sysDictTypeController.getSysDictTypeById(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysDictTypeService, times(1)).getSysDictTypeById(anyInt());
        }
    }
}