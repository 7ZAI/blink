package com.blink.gateway.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 操作日志实体
 * </p>
 *
 * @author binblink
 * @since 2024-03-11
 */
@Getter
@Setter
@TableName("sys_operation_log")
public class SysOperationLogDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日志ID
     */
    @TableId(value = "log_id", type = IdType.AUTO)
    private Long logId;

    /**
     * 操作用户ID
     */
    @TableField("user_id")
    private Integer userId;

    /**
     * 登录名
     */
    @TableField("login_name")
    private String loginName;

    /**
     * 日志类型
     * <p>
     * LOGIN-登入日志, SYSTEM-系统日志, OPERATION-操作日志
     */
    @TableField("log_type")
    private String logType;

    /**
     * 操作描述
     */
    @TableField("description")
    private String description;

    /**
     * 请求URL
     */
    @TableField("request_url")
    private String requestUrl;

    /**
     * 请求方法
     */
    @TableField("request_method")
    private String requestMethod;

    /**
     * 请求参数（JSON格式，已脱敏）
     */
    @TableField("request_params")
    private String requestParams;

    /**
     * 响应数据（JSON格式，已脱敏）
     */
    @TableField("response_data")
    private String responseData;

    /**
     * 执行状态 0成功 1失败
     */
    @TableField("execute_status")
    private Integer executeStatus;

    /**
     * 错误信息
     */
    @TableField("error_msg")
    private String errorMsg;

    /**
     * 执行时长(毫秒)
     */
    @TableField("execute_time_ms")
    private Integer executeTimeMs;

    /**
     * IP地址
     */
    @TableField("ip_address")
    private String ipAddress;

    /**
     * 浏览器UA
     */
    @TableField("user_agent")
    private String userAgent;

    /**
     * 操作时间
     */
    @TableField("operation_time")
    private LocalDateTime operationTime;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
