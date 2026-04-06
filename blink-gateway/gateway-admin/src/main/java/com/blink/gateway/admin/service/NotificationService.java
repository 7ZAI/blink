package com.blink.gateway.admin.service;

import com.blink.gateway.admin.dto.req.MarkReadReq;
import com.blink.gateway.admin.dto.req.QueryHistoryReq;
import com.blink.gateway.admin.dto.req.QueryNotificationReq;
import com.blink.gateway.admin.dto.rsp.NotificationHistoryRsp;
import com.blink.gateway.admin.dto.rsp.NotificationListRsp;
import com.blink.gateway.admin.dto.rsp.UnreadCountRsp;

/**
 * 消息通知服务接口
 *
 * @author binblink
 * @since 2026-04-06
 */
public interface NotificationService {

    /**
     * 获取消息列表
     *
     * @param req 查询请求
     * @return 消息列表响应
     */
    NotificationListRsp getNotificationList(QueryNotificationReq req);

    /**
     * 获取未读消息数量
     *
     * @return 未读消息数量响应
     */
    UnreadCountRsp getUnreadCount();

    /**
     * 标记已读
     *
     * @param req 标记已读请求
     */
    void markRead(MarkReadReq req);

    /**
     * 查询历史消息
     *
     * @param req 查询历史请求
     * @return 历史消息响应
     */
    NotificationHistoryRsp getHistory(QueryHistoryReq req);
}