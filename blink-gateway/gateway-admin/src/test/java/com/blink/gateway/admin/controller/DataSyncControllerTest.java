package com.blink.gateway.admin.controller;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.SyncChannelDataReq;
import com.blink.gateway.admin.service.DataSyncService;
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
 * DataSyncController 单元测试类
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DataSyncController 单元测试")
class DataSyncControllerTest {

    @Mock
    private DataSyncService dataSyncService;

    @InjectMocks
    private DataSyncController dataSyncController;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    @Nested
    @DisplayName("syncChannelData 测试")
    class SyncChannelDataTests {

        @Test
        @DisplayName("同步渠道数据到网关 - 正常场景")
        void testSyncChannelData_Success() {
            SyncChannelDataReq req = new SyncChannelDataReq();

            RequestDTO<SyncChannelDataReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(dataSyncService.syncChannelData(any(SyncChannelDataReq.class))).thenReturn(ResponseDTO.newSuccessInstance());

            ResponseDTO<EmptyBody> response = dataSyncController.syncChannelData(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(dataSyncService, times(1)).syncChannelData(any(SyncChannelDataReq.class));
        }

        @Test
        @DisplayName("同步渠道数据到网关 - 异常场景")
        void testSyncChannelData_Exception() {
            SyncChannelDataReq req = new SyncChannelDataReq();
            RequestDTO<SyncChannelDataReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(dataSyncService.syncChannelData(any(SyncChannelDataReq.class)))
                    .thenThrow(new RuntimeException("同步失败"));

            assertThrows(RuntimeException.class, () -> dataSyncController.syncChannelData(requestDTO));
            verify(dataSyncService, times(1)).syncChannelData(any(SyncChannelDataReq.class));
        }
    }

    @Nested
    @DisplayName("syncRouteData 测试")
    class SyncRouteDataTests {

        @Test
        @DisplayName("同步路由数据到网关 - 正常场景")
        void testSyncRouteData_Success() {
            when(dataSyncService.syncRouteData()).thenReturn(ResponseDTO.newSuccessInstance());

            ResponseDTO<EmptyBody> response = dataSyncController.syncRouteData();

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(dataSyncService, times(1)).syncRouteData();
        }
    }

    @Nested
    @DisplayName("syncConfigData 测试")
    class SyncConfigDataTests {

        @Test
        @DisplayName("同步配置数据到网关 - 正常场景")
        void testSyncConfigData_Success() {
            when(dataSyncService.syncConfigData()).thenReturn(ResponseDTO.newSuccessInstance());

            ResponseDTO<EmptyBody> response = dataSyncController.syncConfigData();

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(dataSyncService, times(1)).syncConfigData();
        }
    }
}