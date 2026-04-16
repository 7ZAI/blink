package com.blink.gateway.admin.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 路由实例关联实体
 * 记录每个路由在每个实例上的推送状态
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
@TableName("ga_route_instance_rela")
public class GaRouteInstanceRelaDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId("rela_id")
    private Long relaId;

    /**
     * 路由ID
     */
    @TableField("route_id")
    private String routeId;

    /**
     * 实例ID，格式：gateway-app:host:port
     */
    @TableField("instance_id")
    private String instanceId;

    /**
     * 推送记录ID
     */
    @TableField("push_id")
    private Long pushId;

    /**
     * 推送状态
     * 0 - 未推送
     * 1 - 已推送
     * 2 - 推送失败
     */
    @TableField("push_status")
    private Byte pushStatus;

    /**
     * 推送时间
     */
    @TableField("push_time")
    private LocalDateTime pushTime;

    /**
     * 加载状态
     * 0 - 未知
     * 1 - 已加载
     * 2 - 加载失败
     */
    @TableField("load_status")
    private Byte loadStatus;

    /**
     * 加载确认时间
     */
    @TableField("load_time")
    private LocalDateTime loadTime;

    /**
     * 错误信息
     */
    @TableField("error_msg")
    private String errorMsg;

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
