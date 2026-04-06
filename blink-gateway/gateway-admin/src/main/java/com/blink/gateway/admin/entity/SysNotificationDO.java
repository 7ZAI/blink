package com.blink.gateway.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统消息通知实体
 *
 * @author binblink
 * @since 2026-04-06
 */
@Data
@TableName("sys_notification")
public class SysNotificationDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long notificationId;

    private String title;

    private String content;

    private String type;

    private String severity;

    private String targetType;

    private Integer targetUserId;

    private String sourceRef;

    private Integer createdBy;

    private LocalDateTime createdTime;

    private LocalDateTime expireTime;
}