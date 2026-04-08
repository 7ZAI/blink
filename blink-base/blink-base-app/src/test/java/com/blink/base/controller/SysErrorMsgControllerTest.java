package com.blink.base.controller;

import com.blink.base.dto.req.QueryErrMsgReq;
import com.blink.base.dto.rsp.QueryErrMsgRsp;
import com.blink.base.service.SysErrorMsgService;
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
 * SysErrorMsgController 单元测试类
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SysErrorMsgController 单元测试")
class SysErrorMsgControllerTest {

    @Mock
    private SysErrorMsgService sysErrorMsgService;

    @InjectMocks
    private SysErrorMsgController sysErrorMsgController;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    @Nested
    @DisplayName("getMsg 测试")
    class GetMsgTests {

        @Test
        @DisplayName("根据错误码和语言查询错误消息 - 正常场景")
        void testGetMsg_Success() throws Exception {
            QueryErrMsgReq req = new QueryErrMsgReq();
            req.setCode("PARAM0001");
            req.setLocal("zh_CN");

            RequestDTO<QueryErrMsgReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            QueryErrMsgRsp rsp = new QueryErrMsgRsp();
            rsp.setMsgInfo("参数不能为空");
            when(sysErrorMsgService.getErrorMsg(any(QueryErrMsgReq.class))).thenReturn(rsp);

            ResponseDTO<QueryErrMsgRsp> response = sysErrorMsgController.getMsg(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            assertEquals("参数不能为空", response.getBody().getMsgInfo());
            verify(sysErrorMsgService, times(1)).getErrorMsg(any(QueryErrMsgReq.class));
        }

        @Test
        @DisplayName("根据错误码和语言查询错误消息 - 英文场景")
        void testGetMsg_English() throws Exception {
            QueryErrMsgReq req = new QueryErrMsgReq();
            req.setCode("PARAM0001");
            req.setLocal("en_US");

            RequestDTO<QueryErrMsgReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            QueryErrMsgRsp rsp = new QueryErrMsgRsp();
            rsp.setMsgInfo("Parameter cannot be empty");
            when(sysErrorMsgService.getErrorMsg(any(QueryErrMsgReq.class))).thenReturn(rsp);

            ResponseDTO<QueryErrMsgRsp> response = sysErrorMsgController.getMsg(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            assertEquals("Parameter cannot be empty", response.getBody().getMsgInfo());
            verify(sysErrorMsgService, times(1)).getErrorMsg(any(QueryErrMsgReq.class));
        }

        @Test
        @DisplayName("根据错误码和语言查询错误消息 - 错误码不存在")
        void testGetMsg_NotFound() throws Exception {
            QueryErrMsgReq req = new QueryErrMsgReq();
            req.setCode("NOT_EXIST");
            req.setLocal("zh_CN");

            RequestDTO<QueryErrMsgReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(sysErrorMsgService.getErrorMsg(any(QueryErrMsgReq.class))).thenReturn(null);

            ResponseDTO<QueryErrMsgRsp> response = sysErrorMsgController.getMsg(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNull(response.getBody());
            verify(sysErrorMsgService, times(1)).getErrorMsg(any(QueryErrMsgReq.class));
        }

        @Test
        @DisplayName("根据错误码和语言查询错误消息 - 异常场景")
        void testGetMsg_Exception() throws Exception {
            QueryErrMsgReq req = new QueryErrMsgReq();
            req.setCode("PARAM0001");

            RequestDTO<QueryErrMsgReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(sysErrorMsgService.getErrorMsg(any(QueryErrMsgReq.class)))
                    .thenThrow(new RuntimeException("服务异常"));

            assertThrows(RuntimeException.class, () -> sysErrorMsgController.getMsg(requestDTO));
            verify(sysErrorMsgService, times(1)).getErrorMsg(any(QueryErrMsgReq.class));
        }
    }
}