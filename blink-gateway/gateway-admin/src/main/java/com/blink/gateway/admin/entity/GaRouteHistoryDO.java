package com.blink.gateway.admin.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 网关路由历史审计实体类
 * 对应数据库表 ga_route_history
 * 用于记录路由配置的变更历史，支持回滚和审计
 *
 * @author binblink
 * @since 2026-04-11
 */
@Getter
@Setter
@TableName(value = "ga_route_history", autoResultMap = true)
public class GaRouteHistoryDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 历史记录ID
     */
    @TableId("history_id")
    private Long historyId;

    /**
     * 路由ID
     */
    @TableField("route_id")
    private String routeId;

    /**
     * 路由名称（变更时的值）
     */
    @TableField("route_name")
    private String routeName;

    /**
     * 操作类型
     * A: 新增
     * M: 修改
     * D: 删除
     */
    @TableField("operation_type")
    private String operationType;

    /**
     * 变更前数据快照
     * 修改/删除时记录
     */
    @TableField(value = "before_data", typeHandler = JacksonTypeHandler.class)
    private GaRouteDO beforeData;

    /**
     * 变更后数据快照
     * 新增/修改时记录
     */
    @TableField(value = "after_data", typeHandler = JacksonTypeHandler.class)
    private GaRouteDO afterData;

    /**
     * 操作人ID
     */
    @TableField("operator_id")
    private Integer operatorId;

    /**
     * 操作人名称
     */
    @TableField("operator_name")
    private String operatorName;

    /**
     * 操作时间
     */
    @TableField(value = "operate_time", fill = FieldFill.INSERT)
    private LocalDateTime operateTime;

    /**
     * 备注说明
     */
    @TableField("remark")
    private String remark;
}