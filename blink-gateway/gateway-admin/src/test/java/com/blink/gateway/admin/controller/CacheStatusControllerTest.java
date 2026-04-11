package com.blink.gateway.admin.controller;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.CacheCheckReq;
import com.blink.gateway.admin.dto.req.CacheSyncReq;
import com.blink.gateway.admin.dto.rsp.CacheCheckRsp;
import com.blink.gateway.admin.dto.rsp.SyncLogRsp;
import com.blink.gateway.admin.service.CacheStatusService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * CacheStatusController 单元测试类
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CacheStatusController 单元测试")
class CacheStatusControllerTest {

    @Mock
    private CacheStatusService cacheStatusService;

    @InjectMocks
    private CacheStatusController cacheStatusController;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    @Nested
    @DisplayName("getInstances 测试")
    class GetInstancesTests {

        @Test
        @DisplayName("获取网关实例列表 - 正常场景")
        void testGetInstances_Success() {
            // 使用 doReturn 来处理通配符类型问题
            doReturn(ResponseDTO.newSuccessInstance()).when(cacheStatusService).getGatewayInstances();

            ResponseDTO<?> response = cacheStatusController.getInstances();

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(cacheStatusService, times(1)).getGatewayInstances();
        }
    }

    @Nested
    @DisplayName("check 测试")
    class CheckTests {

        @Test
        @DisplayName("执行一致性检查 - 正常场景")
        void testCheck_Success() {
            CacheCheckReq req = new CacheCheckReq();

            RequestDTO<CacheCheckReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            CacheCheckRsp rsp = new CacheCheckRsp();
            when(cacheStatusService.checkConsistency(any(CacheCheckReq.class))).thenReturn(ResponseDTO.newSuccessInstance(rsp));

            ResponseDTO<CacheCheckRsp> response = cacheStatusController.check(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(cacheStatusService, times(1)).checkConsistency(any(CacheCheckReq.class));
        }
    }

    @Nested
    @DisplayName("sync 测试")
    class SyncTests {

        @Test
        @DisplayName("同步数据到网关 - 正常场景")
        void testSync_Success() {
            CacheSyncReq req = new CacheSyncReq();

            RequestDTO<CacheSyncReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(cacheStatusService.syncData(any(CacheSyncReq.class))).thenReturn(ResponseDTO.newSuccessInstance());

            ResponseDTO<EmptyBody> response = cacheStatusController.sync(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(cacheStatusService, times(1)).syncData(any(CacheSyncReq.class));
        }

        @Test
        @DisplayName("同步数据到网关 - 异常场景")
        void testSync_Exception() {
            CacheSyncReq req = new CacheSyncReq();
            RequestDTO<CacheSyncReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(cacheStatusService.syncData(any(CacheSyncReq.class)))
                    .thenThrow(new RuntimeException("同步失败"));

            assertThrows(RuntimeException.class, () -> cacheStatusController.sync(requestDTO));
            verify(cacheStatusService, times(1)).syncData(any(CacheSyncReq.class));
        }
    }

    @Nested
    @DisplayName("getLogs 测试")
    class GetLogsTests {

        @Test
        @DisplayName("获取同步日志列表 - 正常场景")
        void testGetLogs_Success() {
            SyncLogRsp rsp = new SyncLogRsp();
            when(cacheStatusService.getSyncLogs(anyInt(), anyInt())).thenReturn(ResponseDTO.newSuccessInstance(rsp));

            ResponseDTO<SyncLogRsp> response = cacheStatusController.getLogs(1, 10);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(cacheStatusService, times(1)).getSyncLogs(1, 10);
        }

        @Test
        @DisplayName("获取同步日志列表 - 默认参数")
        void testGetLogs_DefaultParams() {
            SyncLogRsp rsp = new SyncLogRsp();
            when(cacheStatusService.getSyncLogs(eq(1), eq(10))).thenReturn(ResponseDTO.newSuccessInstance(rsp));

            ResponseDTO<SyncLogRsp> response = cacheStatusController.getLogs(1, 10);

            assertNotNull(response);
            verify(cacheStatusService, times(1)).getSyncLogs(1, 10);
        }
    }
}