package com.blink.gateway.admin.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知发送失败记录实体
 *
 * @author binblink
 * @since 2026-04-28
 */
@Data
@TableName("sys_notification_failure_log")
public class NotificationFailureLogDO {

    /**
     * 日志ID
     */
    @TableId(type = IdType.AUTO)
    private Long logId;

    /**
     * 渠道类型
     */
    private String channelType;

    /**
     * 通知类型
     */
    private String notificationType;

    /**
     * 业务ID
     */
    private String businessId;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 接收人（JSON数组）
     */
    private String recipients;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 状态：0-待重试，1-已成功，2-已放弃
     */
    private Byte status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
