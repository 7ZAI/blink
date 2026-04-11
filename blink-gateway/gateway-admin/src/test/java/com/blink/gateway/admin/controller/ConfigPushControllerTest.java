package com.blink.gateway.admin.controller;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.GetConfigHistoryReq;
import com.blink.gateway.admin.dto.req.PushConfigReq;
import com.blink.gateway.admin.dto.req.RollbackConfigReq;
import com.blink.gateway.admin.dto.rsp.ConfigHistoryRsp;
import com.blink.gateway.admin.service.ConfigPushService;
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
 * ConfigPushController 单元测试类
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConfigPushController 单元测试")
class ConfigPushControllerTest {

    @Mock
    private ConfigPushService configPushService;

    @InjectMocks
    private ConfigPushController configPushController;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    @Nested
    @DisplayName("pushConfig 测试")
    class PushConfigTests {

        @Test
        @DisplayName("推送配置到 Nacos - 正常场景")
        void testPushConfig_Success() {
            PushConfigReq req = new PushConfigReq();

            RequestDTO<PushConfigReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(configPushService.pushConfigToNacos(any(PushConfigReq.class))).thenReturn(ResponseDTO.newSuccessInstance());

            ResponseDTO<EmptyBody> response = configPushController.pushConfig(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(configPushService, times(1)).pushConfigToNacos(any(PushConfigReq.class));
        }

        @Test
        @DisplayName("推送配置到 Nacos - 异常场景")
        void testPushConfig_Exception() {
            PushConfigReq req = new PushConfigReq();
            RequestDTO<PushConfigReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(configPushService.pushConfigToNacos(any(PushConfigReq.class)))
                    .thenThrow(new RuntimeException("Nacos连接失败"));

            assertThrows(RuntimeException.class, () -> configPushController.pushConfig(requestDTO));
            verify(configPushService, times(1)).pushConfigToNacos(any(PushConfigReq.class));
        }
    }

    @Nested
    @DisplayName("getConfigHistory 测试")
    class GetConfigHistoryTests {

        @Test
        @DisplayName("获取配置历史列表 - 正常场景")
        void testGetConfigHistory_Success() {
            GetConfigHistoryReq req = new GetConfigHistoryReq();
            req.setDataId("test-dataId");
            req.setGroup("test-group");
            req.setLimit(10);

            RequestDTO<GetConfigHistoryReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            ConfigHistoryRsp rsp = new ConfigHistoryRsp();
            when(configPushService.getConfigHistory(any(GetConfigHistoryReq.class))).thenReturn(ResponseDTO.newSuccessInstance(rsp));

            ResponseDTO<ConfigHistoryRsp> response = configPushController.getConfigHistory(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(configPushService, times(1)).getConfigHistory(any(GetConfigHistoryReq.class));
        }
    }

    @Nested
    @DisplayName("rollbackConfig 测试")
    class RollbackConfigTests {

        @Test
        @DisplayName("回滚配置到指定版本 - 正常场景")
        void testRollbackConfig_Success() {
            RollbackConfigReq req = new RollbackConfigReq();
            req.setHistoryId(1);

            RequestDTO<RollbackConfigReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(configPushService.rollbackConfig(any(RollbackConfigReq.class))).thenReturn(ResponseDTO.newSuccessInstance());

            ResponseDTO<EmptyBody> response = configPushController.rollbackConfig(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(configPushService, times(1)).rollbackConfig(any(RollbackConfigReq.class));
        }
    }
}