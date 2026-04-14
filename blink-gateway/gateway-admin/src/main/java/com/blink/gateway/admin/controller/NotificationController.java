package com.blink.gateway.admin.controller;

import cn.dev33.satoken.stp.StpUtil;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 消息通知控制器
 *
 * SSE 连接管理策略：
 * - 连接标识 = userId + token（支持多设备登录）
 * - 同一 userId+token 组合只允许一个 SSE 连接
 * - 页面刷新/重连时，相同组合会替换旧连接
 * - 多设备登录：同一 userId 不同 token = 不同连接
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
     * SSE连接端点（POST请求）
     */
    @PostMapping("/sse/connect")
    public SseEmitter connect() {
        Integer userId = StpUtil.getLoginIdAsInt();
        String tokenValue = StpUtil.getTokenValue();

        if (tokenValue == null) {
            log.warn("[SSE] Token 不存在，拒绝连接 | userId: {}", userId);
            throw new IllegalStateException("Token 无效，请重新登录");
        }

        // 连接标识 = userId + token，支持多设备登录
        String connectionKey = userId + ":" + tokenValue;

        log.info("[SSE] 收到连接请求 | userId: {}, connectionKey: {}", userId, maskConnectionKey(connectionKey));
        return sseConnectionPool.createConnection(connectionKey, userId);
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

    /**
     * 连接标识脱敏显示
     */
    private String maskConnectionKey(String connectionKey) {
        if (connectionKey == null) {
            return null;
        }
        // userId:token -> userId:前8位...
        int colonIndex = connectionKey.indexOf(':');
        if (colonIndex > 0 && connectionKey.length() > colonIndex + 8) {
            return connectionKey.substring(0, colonIndex + 8) + "...";
        }
        return connectionKey;
    }
}