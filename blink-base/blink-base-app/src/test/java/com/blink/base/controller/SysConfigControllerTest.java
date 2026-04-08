package com.blink.base.controller;

import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.ConfigGroupRsp;
import com.blink.base.dto.rsp.QuerySysConfigRsp;
import com.blink.base.dto.vo.SysConfigVO;
import com.blink.base.service.SysConfigService;
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
 * SysConfigController 单元测试类
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SysConfigController 单元测试")
class SysConfigControllerTest {

    @Mock
    private SysConfigService sysConfigService;

    @InjectMocks
    private SysConfigController sysConfigController;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    @Nested
    @DisplayName("deleteSysConfig 测试")
    class DeleteSysConfigTests {

        @Test
        @DisplayName("删除配置 - 正常场景")
        void testDeleteSysConfig_Success() throws Exception {
            DeleteSysConfigReq req = new DeleteSysConfigReq();
            req.setDeleteId(1); req.setBatchDelete(false);

            RequestDTO<DeleteSysConfigReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(sysConfigService).deleteSysConfig(any(DeleteSysConfigReq.class));

            ResponseDTO<EmptyBody> response = sysConfigController.deleteSysConfig(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(sysConfigService, times(1)).deleteSysConfig(any(DeleteSysConfigReq.class));
        }
    }

    @Nested
    @DisplayName("modifySysConfig 测试")
    class ModifySysConfigTests {

        @Test
        @DisplayName("更新配置 - 正常场景")
        void testModifySysConfig_Success() throws Exception {
            UpdateSysConfigReq req = new UpdateSysConfigReq();
            req.setId(1);
            req.setConfigValue("new_value");

            RequestDTO<UpdateSysConfigReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(sysConfigService).modifySysConfig(any(UpdateSysConfigReq.class));

            ResponseDTO<EmptyBody> response = sysConfigController.modifySysConfig(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(sysConfigService, times(1)).modifySysConfig(any(UpdateSysConfigReq.class));
        }
    }

    @Nested
    @DisplayName("getSysConfigList 测试")
    class GetSysConfigListTests {

        @Test
        @DisplayName("查询配置列表 - 正常场景")
        void testGetSysConfigList_Success() throws Exception {
            QuerySysConfigReq req = new QuerySysConfigReq();

            RequestDTO<QuerySysConfigReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            QuerySysConfigRsp rsp = new QuerySysConfigRsp();
            when(sysConfigService.getSysConfigList(any(QuerySysConfigReq.class))).thenReturn(rsp);

            ResponseDTO<QuerySysConfigRsp> response = sysConfigController.getSysConfigList(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysConfigService, times(1)).getSysConfigList(any(QuerySysConfigReq.class));
        }
    }

    @Nested
    @DisplayName("getOneConfigFromDataBase 测试")
    class GetOneConfigFromDataBaseTests {

        @Test
        @DisplayName("从数据库获取单个配置 - 正常场景")
        void testGetOneConfigFromDataBase_Success() throws Exception {
            QueryOneSysConfigReq req = new QueryOneSysConfigReq();
            req.setConfigKey("test_key");

            RequestDTO<QueryOneSysConfigReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            SysConfigVO configVO = new SysConfigVO();
            configVO.setConfigKey("test_key");
            configVO.setConfigValue("test_value");
            when(sysConfigService.getOneConfigFromDataBase(any(QueryOneSysConfigReq.class))).thenReturn(configVO);

            ResponseDTO<SysConfigVO> response = sysConfigController.getOneConfigFromDataBase(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysConfigService, times(1)).getOneConfigFromDataBase(any(QueryOneSysConfigReq.class));
        }
    }

    @Nested
    @DisplayName("getOneConfigFromCacheOrDataBase 测试")
    class GetOneConfigFromCacheOrDataBaseTests {

        @Test
        @DisplayName("从缓存或数据库获取单个配置 - 正常场景")
        void testGetOneConfigFromCacheOrDataBase_Success() throws Exception {
            QueryOneSysConfigReq req = new QueryOneSysConfigReq();
            req.setConfigKey("test_key");

            RequestDTO<QueryOneSysConfigReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            SysConfigVO configVO = new SysConfigVO();
            configVO.setConfigKey("test_key");
            configVO.setConfigValue("test_value");
            when(sysConfigService.getOneConfigFromCacheOrDataBase(any(QueryOneSysConfigReq.class))).thenReturn(configVO);

            ResponseDTO<SysConfigVO> response = sysConfigController.getOneConfigFromCacheOrDataBase(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysConfigService, times(1)).getOneConfigFromCacheOrDataBase(any(QueryOneSysConfigReq.class));
        }
    }

    @Nested
    @DisplayName("getConfigsByGroupKey 测试")
    class GetConfigsByGroupKeyTests {

        @Test
        @DisplayName("根据分组键查询配置 - 正常场景")
        void testGetConfigsByGroupKey_Success() throws Exception {
            QueryConfigByGroupKeyReq req = new QueryConfigByGroupKeyReq();
            req.setGroupKey("test_group");

            RequestDTO<QueryConfigByGroupKeyReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            ConfigGroupRsp rsp = new ConfigGroupRsp();
            when(sysConfigService.getConfigsByGroupKey(anyString())).thenReturn(rsp);

            ResponseDTO<ConfigGroupRsp> response = sysConfigController.getConfigsByGroupKey(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysConfigService, times(1)).getConfigsByGroupKey(anyString());
        }
    }

    @Nested
    @DisplayName("batchUpdateConfigs 测试")
    class BatchUpdateConfigsTests {

        @Test
        @DisplayName("批量更新配置 - 正常场景")
        void testBatchUpdateConfigs_Success() throws Exception {
            BatchUpdateSysConfigReq req = new BatchUpdateSysConfigReq();
            List<UpdateSysConfigReq> configs = new ArrayList<>();
            req.setConfigs(configs);

            RequestDTO<BatchUpdateSysConfigReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(sysConfigService).batchUpdateConfigs(anyList());

            ResponseDTO<EmptyBody> response = sysConfigController.batchUpdateConfigs(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(sysConfigService, times(1)).batchUpdateConfigs(anyList());
        }
    }
}