package com.blink.gateway.admin.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知渠道配置实体
 *
 * @author binblink
 * @since 2026-04-28
 */
@Data
@TableName("sys_notification_channel_config")
public class NotificationChannelConfigDO {

    /**
     * 配置ID
     */
    @TableId(type = IdType.AUTO)
    private Integer configId;

    /**
     * 渠道类型
     */
    private String channelType;

    /**
     * 渠道名称
     */
    private String channelName;

    /**
     * 配置JSON
     */
    private String configJson;

    /**
     * 是否启用
     */
    private Byte enabled;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
