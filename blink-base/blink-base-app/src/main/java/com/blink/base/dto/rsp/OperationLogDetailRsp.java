package com.blink.base.dto.rsp;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志详情响应DTO
 *
 * @author binblink
 * @since 2024-03-11
 */
@Data
public class OperationLogDetailRsp implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日志ID
     */
    private Long logId;

    /**
     * 操作用户ID
     */
    private Integer userId;

    /**
     * 登录名
     */
    private String loginName;

    /**
     * 日志类型
     * <p>
     * LOGIN-登入日志, SYSTEM-系统日志, OPERATION-操作日志
     */
    private String logType;

    /**
     * 日志类型描述
     */
    private String logTypeDesc;

    /**
     * 操作描述
     */
    private String description;

    /**
     * 请求URL
     */
    private String requestUrl;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 请求参数（JSON格式）
     */
    private String requestParams;

    /**
     * 响应数据（JSON格式）
     */
    private String responseData;

    /**
     * 执行状态 0成功 1失败
     */
    private Integer executeStatus;

    /**
     * 执行状态描述
     */
    private String executeStatusDesc;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 执行时长(毫秒)
     */
    private Integer executeTimeMs;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 浏览器UA
     */
    private String userAgent;

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;

}