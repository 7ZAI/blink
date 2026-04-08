package com.blink.base.controller;

import com.blink.base.dto.req.DeleteSysOperationLogReq;
import com.blink.base.dto.req.QueryOperationLogReq;
import com.blink.base.dto.rsp.OperationLogDetailRsp;
import com.blink.base.dto.rsp.OperationLogRsp;
import com.blink.base.service.SysOperationLogService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * SysOperationLogController 单元测试类
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SysOperationLogController 单元测试")
class SysOperationLogControllerTest {

    @Mock
    private SysOperationLogService sysOperationLogService;

    @InjectMocks
    private SysOperationLogController sysOperationLogController;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    @Nested
    @DisplayName("getOperationLogList 测试")
    class GetOperationLogListTests {

        @Test
        @DisplayName("分页查询操作日志列表 - 正常场景")
        void testGetOperationLogList_Success() throws Exception {
            QueryOperationLogReq req = new QueryOperationLogReq();

            RequestDTO<QueryOperationLogReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            OperationLogRsp rsp = new OperationLogRsp();
            when(sysOperationLogService.getOperationLogList(any(QueryOperationLogReq.class))).thenReturn(rsp);

            ResponseDTO<OperationLogRsp> response = sysOperationLogController.getOperationLogList(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysOperationLogService, times(1)).getOperationLogList(any(QueryOperationLogReq.class));
        }

        @Test
        @DisplayName("分页查询操作日志列表 - 异常场景")
        void testGetOperationLogList_Exception() throws Exception {
            QueryOperationLogReq req = new QueryOperationLogReq();

            RequestDTO<QueryOperationLogReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(sysOperationLogService.getOperationLogList(any(QueryOperationLogReq.class)))
                    .thenThrow(new RuntimeException("查询失败"));

            assertThrows(RuntimeException.class, () -> sysOperationLogController.getOperationLogList(requestDTO));
            verify(sysOperationLogService, times(1)).getOperationLogList(any(QueryOperationLogReq.class));
        }
    }

    @Nested
    @DisplayName("getOperationLogDetail 测试")
    class GetOperationLogDetailTests {

        @Test
        @DisplayName("查询操作日志详情 - 正常场景")
        void testGetOperationLogDetail_Success() throws Exception {
            DeleteSysOperationLogReq req = new DeleteSysOperationLogReq();
            req.setLogId(1L);

            RequestDTO<DeleteSysOperationLogReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            OperationLogDetailRsp rsp = new OperationLogDetailRsp();
            when(sysOperationLogService.getOperationLogDetail(anyLong())).thenReturn(rsp);

            ResponseDTO<OperationLogDetailRsp> response = sysOperationLogController.getOperationLogDetail(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(sysOperationLogService, times(1)).getOperationLogDetail(anyLong());
        }

        @Test
        @DisplayName("查询操作日志详情 - 日志不存在")
        void testGetOperationLogDetail_NotFound() throws Exception {
            DeleteSysOperationLogReq req = new DeleteSysOperationLogReq();
            req.setLogId(999L);

            RequestDTO<DeleteSysOperationLogReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(sysOperationLogService.getOperationLogDetail(anyLong())).thenReturn(null);

            ResponseDTO<OperationLogDetailRsp> response = sysOperationLogController.getOperationLogDetail(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNull(response.getBody());
            verify(sysOperationLogService, times(1)).getOperationLogDetail(anyLong());
        }
    }
}