package com.blink.gateway.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息读取状态实体
 *
 * @author binblink
 * @since 2026-04-06
 */
@Data
@TableName("sys_notification_read")
public class SysNotificationReadDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long readId;

    private Long notificationId;

    private Integer userId;

    private LocalDateTime readTime;
}