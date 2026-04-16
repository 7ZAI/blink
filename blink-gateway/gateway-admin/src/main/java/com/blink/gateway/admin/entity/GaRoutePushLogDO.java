package com.blink.gateway.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 路由推送记录实体
 * 用于记录路由推送历史，支持回滚和审计
 *
 * @author binblink
 * @since 2026-04-11
 */
@Data
@TableName(value = "ga_route_push_log", autoResultMap = true)
public class GaRoutePushLogDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 推送记录ID
     */
    @TableId
    private Long pushId;

    /**
     * 存储方式: redis/nacos
     */
    private String storageMode;

    /**
     * 路由分组（Redis模式）
     */
    private String routesGroup;

    /**
     * Nacos Data ID（Nacos模式）
     */
    private String nacosDataId;

    /**
     * Nacos Group（Nacos模式）
     */
    private String nacosGroup;

    /**
     * 推送的路由ID列表(JSON数组)
     */
    private String routeIds;

    /**
     * 路由配置快照(JSON数组)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<GaRouteDO> routeSnapshot;

    /**
     * 推送模式: broadcast/specified
     */
    private String pushMode;

    /**
     * 目标实例ID列表(JSON数组)
     */
    private String targetInstanceIds;

    /**
     * 目标实例数量
     */
    private Integer instanceCount;

    /**
     * 成功推送实例数量
     */
    private Integer successCount;

    /**
     * 推送结果: 0-成功, 1-部分失败, 2-失败
     */
    private Byte pushResult;

    /**
     * 各实例推送详情(JSON对象)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> pushDetail;

    /**
     * 操作人ID
     */
    private Integer operatorId;

    /**
     * 操作人名称
     */
    private String operatorName;

    /**
     * 推送时间
     */
    private LocalDateTime pushTime;

    /**
     * 备注说明
     */
    private String remark;

    /**
     * 失败实例ID列表(JSON数组)
     * 记录推送失败的网关实例ID
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> failedInstanceIds;

    /**
     * 各实例错误信息(JSON对象)
     * key: instanceId, value: errorMsg
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> instanceErrors;

    /**
     * 确认状态
     * 0: 待确认
     * 1: 已确认
     * 2: 超时
     */
    private Byte confirmStatus;

    /**
     * 确认时间
     */
    private LocalDateTime confirmTime;

    /**
     * 确认人
     */
    private String confirmBy;
}