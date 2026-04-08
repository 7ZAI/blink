package com.blink.base.controller;

import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.DictDataMapRsp;
import com.blink.base.dto.rsp.QuerySysDictDataRsp;
import com.blink.base.dto.vo.SysDictDataVO;
import com.blink.base.service.SysDictDataService;
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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * SysDictDataController 单元测试类
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SysDictDataController 单元测试")
class SysDictDataControllerTest {

    @Mock
    private SysDictDataService sysDictDataService;

    @InjectMocks
    private SysDictDataController sysDictDataController;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    @Nested
    @DisplayName("saveSysDictData 测试")
    class SaveSysDictDataTests {

        @Test
        @DisplayName("新增字典数据 - 正常场景")
        void testSaveSysDictData_Success() throws Exception {
            AddSysDictDataReq req = new AddSysDictDataReq();
            req.setDictType("test_dict");
            req.setDictLabel("测试数据");

            RequestDTO<AddSysDictDataReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            SysDictDataVO dictVO = new SysDictDataVO();
            dictVO.setDictCode(1);
            dictVO.setDictLabel("测试数据");
            when(sysDictDataService.saveSysDictData(any(AddSysDictDataReq.class))).thenReturn(dictVO);

            ResponseDTO<SysDictDataVO> response = sysDictDataController.saveSysDictData(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysDictDataService, times(1)).saveSysDictData(any(AddSysDictDataReq.class));
        }
    }

    @Nested
    @DisplayName("deleteSysDictData 测试")
    class DeleteSysDictDataTests {

        @Test
        @DisplayName("删除字典数据 - 正常场景")
        void testDeleteSysDictData_Success() throws Exception {
            DeleteSysDictDataReq req = new DeleteSysDictDataReq();
            req.setDeleteId(1); req.setBatchDelete(false);

            RequestDTO<DeleteSysDictDataReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(sysDictDataService).deleteSysDictData(any(DeleteSysDictDataReq.class));

            ResponseDTO<EmptyBody> response = sysDictDataController.deleteSysDictData(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(sysDictDataService, times(1)).deleteSysDictData(any(DeleteSysDictDataReq.class));
        }
    }

    @Nested
    @DisplayName("modifySysDictData 测试")
    class ModifySysDictDataTests {

        @Test
        @DisplayName("更新字典数据 - 正常场景")
        void testModifySysDictData_Success() throws Exception {
            UpdateSysDictDataReq req = new UpdateSysDictDataReq();
            req.setDictLabel("新标签");

            RequestDTO<UpdateSysDictDataReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            SysDictDataVO dictVO = new SysDictDataVO();
            dictVO.setDictCode(1);
            dictVO.setDictLabel("新标签");
            when(sysDictDataService.modifySysDictData(any(UpdateSysDictDataReq.class))).thenReturn(dictVO);

            ResponseDTO<SysDictDataVO> response = sysDictDataController.modifySysDictData(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysDictDataService, times(1)).modifySysDictData(any(UpdateSysDictDataReq.class));
        }
    }

    @Nested
    @DisplayName("getSysDictDataList 测试")
    class GetSysDictDataListTests {

        @Test
        @DisplayName("查询字典数据列表 - 正常场景")
        void testGetSysDictDataList_Success() throws Exception {
            QuerySysDictDataReq req = new QuerySysDictDataReq();

            RequestDTO<QuerySysDictDataReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            QuerySysDictDataRsp rsp = new QuerySysDictDataRsp();
            when(sysDictDataService.getSysDictDataList(any(QuerySysDictDataReq.class))).thenReturn(rsp);

            ResponseDTO<QuerySysDictDataRsp> response = sysDictDataController.getSysDictDataList(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysDictDataService, times(1)).getSysDictDataList(any(QuerySysDictDataReq.class));
        }
    }

    @Nested
    @DisplayName("getDictDataByType 测试")
    class GetDictDataByTypeTests {

        @Test
        @DisplayName("根据类型获取字典数据 - 正常场景")
        void testGetDictDataByType_Success() throws Exception {
            GetDictDataByTypeReq req = new GetDictDataByTypeReq();
            req.setDictType("test_dict");

            RequestDTO<GetDictDataByTypeReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            List<SysDictDataVO> dictList = new ArrayList<>();
            when(sysDictDataService.getDictDataByType(anyString())).thenReturn(dictList);

            ResponseDTO<List<SysDictDataVO>> response = sysDictDataController.getDictDataByType(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysDictDataService, times(1)).getDictDataByType(anyString());
        }
    }

    @Nested
    @DisplayName("getDictDataByTypes 测试")
    class GetDictDataByTypesTests {

        @Test
        @DisplayName("批量根据类型获取字典数据 - 正常场景")
        void testGetDictDataByTypes_Success() throws Exception {
            GetDictDataByTypesReq req = new GetDictDataByTypesReq();
            List<String> dictTypes = new ArrayList<>();
            dictTypes.add("test_dict");
            req.setDictTypes(dictTypes);

            RequestDTO<GetDictDataByTypesReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            DictDataMapRsp rsp = new DictDataMapRsp();
            when(sysDictDataService.getDictDataByTypes(anyList())).thenReturn(rsp);

            ResponseDTO<DictDataMapRsp> response = sysDictDataController.getDictDataByTypes(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysDictDataService, times(1)).getDictDataByTypes(anyList());
        }
    }
}