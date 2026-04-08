package com.blink.base.controller;

import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.*;
import com.blink.base.dto.vo.DataFilterVO;
import com.blink.base.service.SysDataFilterService;
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
 * SysDataFilterController 单元测试类
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SysDataFilterController 单元测试")
class SysDataFilterControllerTest {

    @Mock
    private SysDataFilterService sysDataFilterService;

    @InjectMocks
    private SysDataFilterController sysDataFilterController;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    @Nested
    @DisplayName("queryDataFilterList 测试")
    class QueryDataFilterListTests {

        @Test
        @DisplayName("查询数据过滤规则列表 - 正常场景")
        void testQueryDataFilterList_Success() {
            QueryDataFilterReq req = new QueryDataFilterReq();

            RequestDTO<QueryDataFilterReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            ResponseDTO<QueryDataFilterRsp> mockResponse = ResponseDTO.newSuccessInstance(new QueryDataFilterRsp());
            when(sysDataFilterService.queryDataFilterList(any())).thenReturn(mockResponse);

            ResponseDTO<QueryDataFilterRsp> response = sysDataFilterController.queryDataFilterList(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(sysDataFilterService, times(1)).queryDataFilterList(any());
        }
    }

    @Nested
    @DisplayName("addDataFilter 测试")
    class AddDataFilterTests {

        @Test
        @DisplayName("新增数据过滤规则 - 正常场景")
        void testAddDataFilter_Success() {
            AddDataFilterReq req = new AddDataFilterReq();

            RequestDTO<AddDataFilterReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            ResponseDTO<EmptyBody> mockResponse = ResponseDTO.newSuccessInstance();
            when(sysDataFilterService.addDataFilter(any())).thenReturn(mockResponse);

            ResponseDTO<EmptyBody> response = sysDataFilterController.addDataFilter(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(sysDataFilterService, times(1)).addDataFilter(any());
        }
    }

    @Nested
    @DisplayName("updateDataFilter 测试")
    class UpdateDataFilterTests {

        @Test
        @DisplayName("更新数据过滤规则 - 正常场景")
        void testUpdateDataFilter_Success() {
            UpdateDataFilterReq req = new UpdateDataFilterReq();

            RequestDTO<UpdateDataFilterReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            ResponseDTO<EmptyBody> mockResponse = ResponseDTO.newSuccessInstance();
            when(sysDataFilterService.updateDataFilter(any())).thenReturn(mockResponse);

            ResponseDTO<EmptyBody> response = sysDataFilterController.updateDataFilter(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(sysDataFilterService, times(1)).updateDataFilter(any());
        }
    }

    @Nested
    @DisplayName("deleteDataFilter 测试")
    class DeleteDataFilterTests {

        @Test
        @DisplayName("删除数据过滤规则 - 正常场景")
        void testDeleteDataFilter_Success() {
            DataFilterIdReq req = new DataFilterIdReq();

            RequestDTO<DataFilterIdReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            ResponseDTO<EmptyBody> mockResponse = ResponseDTO.newSuccessInstance();
            when(sysDataFilterService.deleteDataFilter(any())).thenReturn(mockResponse);

            ResponseDTO<EmptyBody> response = sysDataFilterController.deleteDataFilter(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(sysDataFilterService, times(1)).deleteDataFilter(any());
        }
    }

    @Nested
    @DisplayName("getDataFilterDetail 测试")
    class GetDataFilterDetailTests {

        @Test
        @DisplayName("获取数据过滤规则详情 - 正常场景")
        void testGetDataFilterDetail_Success() {
            DataFilterIdReq req = new DataFilterIdReq();

            RequestDTO<DataFilterIdReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            ResponseDTO<DataFilterVO> mockResponse = ResponseDTO.newSuccessInstance(new DataFilterVO());
            when(sysDataFilterService.getDataFilterDetail(any())).thenReturn(mockResponse);

            ResponseDTO<DataFilterVO> response = sysDataFilterController.getDataFilterDetail(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(sysDataFilterService, times(1)).getDataFilterDetail(any());
        }
    }

    @Nested
    @DisplayName("getEntityFields 测试")
    class GetEntityFieldsTests {

        @Test
        @DisplayName("获取实体类字段列表 - 正常场景")
        void testGetEntityFields_Success() {
            GetEntityFieldsReq req = new GetEntityFieldsReq();
            req.setEntityClass("com.blink.base.entity.SysUserDO");

            RequestDTO<GetEntityFieldsReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            EntityFieldsRsp rsp = new EntityFieldsRsp();
            when(sysDataFilterService.getEntityFields(anyString())).thenReturn(rsp);

            ResponseDTO<EntityFieldsRsp> response = sysDataFilterController.getEntityFields(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysDataFilterService, times(1)).getEntityFields(anyString());
        }
    }

    @Nested
    @DisplayName("getEntityList 测试")
    class GetEntityListTests {

        @Test
        @DisplayName("获取已注册实体列表 - 正常场景")
        void testGetEntityList_Success() {
            RequestDTO<EmptyBody> requestDTO = new RequestDTO<>();

            EntityListRsp rsp = new EntityListRsp();
            when(sysDataFilterService.getEntityList()).thenReturn(rsp);

            ResponseDTO<EntityListRsp> response = sysDataFilterController.getEntityList(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysDataFilterService, times(1)).getEntityList();
        }
    }

    @Nested
    @DisplayName("getMatchTypes 测试")
    class GetMatchTypesTests {

        @Test
        @DisplayName("获取匹配类型选项 - 正常场景")
        void testGetMatchTypes_Success() {
            GetMatchTypesReq req = new GetMatchTypesReq();
            req.setTableName("sys_user");

            RequestDTO<GetMatchTypesReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            MatchTypesRsp rsp = new MatchTypesRsp();
            when(sysDataFilterService.getMatchTypes(anyString(), any())).thenReturn(rsp);

            ResponseDTO<MatchTypesRsp> response = sysDataFilterController.getMatchTypes(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysDataFilterService, times(1)).getMatchTypes(anyString(), any());
        }
    }

    @Nested
    @DisplayName("refreshCache 测试")
    class RefreshCacheTests {

        @Test
        @DisplayName("刷新数据权限缓存 - 正常场景")
        void testRefreshCache_Success() {
            RequestDTO<EmptyBody> requestDTO = new RequestDTO<>();

            ResponseDTO<EmptyBody> mockResponse = ResponseDTO.newSuccessInstance();
            when(sysDataFilterService.refreshCache()).thenReturn(mockResponse);

            ResponseDTO<EmptyBody> response = sysDataFilterController.refreshCache(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(sysDataFilterService, times(1)).refreshCache();
        }
    }
}