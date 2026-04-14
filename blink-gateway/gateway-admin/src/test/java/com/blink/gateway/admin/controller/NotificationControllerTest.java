package com.blink.gateway.admin.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.MarkReadReq;
import com.blink.gateway.admin.dto.req.QueryHistoryReq;
import com.blink.gateway.admin.dto.req.QueryNotificationReq;
import com.blink.gateway.admin.dto.rsp.NotificationHistoryRsp;
import com.blink.gateway.admin.dto.rsp.NotificationListRsp;
import com.blink.gateway.admin.dto.rsp.UnreadCountRsp;
import com.blink.gateway.admin.sse.SseConnectionPool;
import com.blink.gateway.admin.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * NotificationController 单元测试类
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationController 单元测试")
class NotificationControllerTest {

    @Mock
    private SseConnectionPool sseConnectionPool;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    @Nested
    @DisplayName("connect 测试")
    class ConnectTests {

        @Test
        @DisplayName("SSE连接 - 正常场景")
        void testConnect_Success() {
            // Mock Sa-Token 静态方法
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(StpUtil::getLoginIdAsInt).thenReturn(1001);
                stpUtilMock.when(StpUtil::getTokenValue).thenReturn("test-token-abc123");

                SseEmitter emitter = new SseEmitter();
                when(sseConnectionPool.createConnection(any(String.class), any(Integer.class))).thenReturn(emitter);

                SseEmitter result = notificationController.connect();

                assertNotNull(result);
                verify(sseConnectionPool, times(1)).createConnection(any(String.class), any(Integer.class));
            }
        }
    }

    @Nested
    @DisplayName("getNotificationList 测试")
    class GetNotificationListTests {

        @Test
        @DisplayName("获取消息列表 - 正常场景")
        void testGetNotificationList_Success() {
            QueryNotificationReq req = new QueryNotificationReq();
            req.setLimit(20);

            RequestDTO<QueryNotificationReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            NotificationListRsp rsp = new NotificationListRsp();
            when(notificationService.getNotificationList(any(QueryNotificationReq.class))).thenReturn(rsp);

            ResponseDTO<NotificationListRsp> response = notificationController.getNotificationList(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(notificationService, times(1)).getNotificationList(any(QueryNotificationReq.class));
        }
    }

    @Nested
    @DisplayName("getUnreadCount 测试")
    class GetUnreadCountTests {

        @Test
        @DisplayName("获取未读消息数量 - 正常场景")
        void testGetUnreadCount_Success() {
            RequestDTO<EmptyBody> requestDTO = new RequestDTO<>();

            UnreadCountRsp rsp = new UnreadCountRsp();
            rsp.setUnreadCount(5);
            when(notificationService.getUnreadCount()).thenReturn(rsp);

            ResponseDTO<UnreadCountRsp> response = notificationController.getUnreadCount(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            assertEquals(5, response.getBody().getUnreadCount());
            verify(notificationService, times(1)).getUnreadCount();
        }
    }

    @Nested
    @DisplayName("markRead 测试")
    class MarkReadTests {

        @Test
        @DisplayName("标记已读 - 正常场景")
        void testMarkRead_Success() {
            MarkReadReq req = new MarkReadReq();
            req.setNotificationId(1L);

            RequestDTO<MarkReadReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(notificationService).markRead(any(MarkReadReq.class));

            ResponseDTO<EmptyBody> response = notificationController.markRead(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(notificationService, times(1)).markRead(any(MarkReadReq.class));
        }
    }

    @Nested
    @DisplayName("markAllRead 测试")
    class MarkAllReadTests {

        @Test
        @DisplayName("标记全部已读 - 正常场景")
        void testMarkAllRead_Success() {
            RequestDTO<EmptyBody> requestDTO = new RequestDTO<>();

            doNothing().when(notificationService).markRead(any(MarkReadReq.class));

            ResponseDTO<EmptyBody> response = notificationController.markAllRead(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(notificationService, times(1)).markRead(any(MarkReadReq.class));
        }
    }

    @Nested
    @DisplayName("getHistory 测试")
    class GetHistoryTests {

        @Test
        @DisplayName("查询历史消息 - 正常场景")
        void testGetHistory_Success() {
            QueryHistoryReq req = new QueryHistoryReq();
            req.setPageNum(1);
            req.setPageSize(10);

            RequestDTO<QueryHistoryReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            NotificationHistoryRsp rsp = new NotificationHistoryRsp();
            when(notificationService.getHistory(any(QueryHistoryReq.class))).thenReturn(rsp);

            ResponseDTO<NotificationHistoryRsp> response = notificationController.getHistory(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(notificationService, times(1)).getHistory(any(QueryHistoryReq.class));
        }
    }
}