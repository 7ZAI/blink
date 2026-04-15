package com.blink.gateway.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 网关实例持久化对象
 * 用于记录网关实例的注册、上下线历史
 *
 * @author binblink
 */
@Data
@TableName("gateway_instance")
public class GatewayInstanceDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 实例 ID
     */
    @TableField("instance_id")
    private String instanceId;

    /**
     * 服务 ID
     */
    @TableField("service_id")
    private String serviceId;

    /**
     * 主机地址
     */
    @TableField("host")
    private String host;

    /**
     * 端口
     */
    @TableField("port")
    private Integer port;

    /**
     * URI
     */
    @TableField("uri")
    private String uri;

    /**
     * 元数据
     */
    @TableField("metadata")
    private String metadata;

    /**
     * 实例状态：0-在线，1-离线，2-下线
     */
    @TableField("status")
    private Byte status;

    /**
     * 上线时间
     */
    @TableField("online_time")
    private LocalDateTime onlineTime;

    /**
     * 下线时间
     */
    @TableField("offline_time")
    private LocalDateTime offlineTime;

    /**
     * 下线原因
     */
    @TableField("offline_reason")
    private String offlineReason;

    /**
     * 下线类型: MANUAL-主动/FAULT-被动/DRAINING-排空中
     */
    @TableField("offline_type")
    private String offlineType;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
