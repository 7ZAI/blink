package com.blink.gateway.admin.controller;

import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.data.EmptyBody;
import com.blink.gateway.admin.dto.req.MarkReadReq;
import com.blink.gateway.admin.dto.req.QueryHistoryReq;
import com.blink.gateway.admin.dto.req.QueryNotificationReq;
import com.blink.gateway.admin.dto.rsp.NotificationHistoryRsp;
import com.blink.gateway.admin.dto.rsp.NotificationListRsp;
import com.blink.gateway.admin.dto.rsp.UnreadCountRsp;
import com.blink.gateway.admin.sse.SseConnectionPool;
import com.blink.gateway.admin.service.NotificationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 消息通知控制器
 *
 * @author binblink
 * @since 2026-04-06
 */
@Slf4j
@RestController
@RequestMapping("/notification")
public class NotificationController {

    @Resource
    private SseConnectionPool sseConnectionPool;

    @Resource
    private NotificationService notificationService;

    /**
     * SSE连接端点（POST请求，支持在header中传递token）
     */
    @PostMapping("/sse/connect")
    public SseEmitter connect() {
        log.info("[SSE] 收到连接请求");
        return sseConnectionPool.createConnection();
    }

    /**
     * 获取消息列表
     */
    @PostMapping("/list")
    public ResponseDTO<NotificationListRsp> getNotificationList(
        @RequestBody @Validated RequestDTO<QueryNotificationReq> reqDto) {
        NotificationListRsp rsp = notificationService.getNotificationList(reqDto.getBody());
        return ResponseDTO.newSuccessInstance(rsp);
    }

    /**
     * 获取未读消息数量
     */
    @PostMapping("/unreadCount")
    public ResponseDTO<UnreadCountRsp> getUnreadCount(
        @RequestBody @Validated RequestDTO<EmptyBody> reqDto) {
        UnreadCountRsp rsp = notificationService.getUnreadCount();
        return ResponseDTO.newSuccessInstance(rsp);
    }

    /**
     * 标记已读
     */
    @PostMapping("/markRead")
    public ResponseDTO<EmptyBody> markRead(
        @RequestBody @Validated RequestDTO<MarkReadReq> reqDto) {
        notificationService.markRead(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 标记全部已读
     */
    @PostMapping("/markAllRead")
    public ResponseDTO<EmptyBody> markAllRead(
        @RequestBody @Validated RequestDTO<EmptyBody> reqDto) {
        MarkReadReq req = new MarkReadReq();
        req.setMarkAll(true);
        notificationService.markRead(req);
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 查询历史消息
     */
    @PostMapping("/history")
    public ResponseDTO<NotificationHistoryRsp> getHistory(
        @RequestBody @Validated RequestDTO<QueryHistoryReq> reqDto) {
        NotificationHistoryRsp rsp = notificationService.getHistory(reqDto.getBody());
        return ResponseDTO.newSuccessInstance(rsp);
    }
}