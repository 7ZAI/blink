package com.blink.log.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志记录模型
 * <p>
 * 通用的日志数据载体，与具体存储解耦。
 * 业务模块可通过 {@link #extraFields} 扩展自定义字段。
 *
 * @author binblink
 */
@Getter
@Setter
public class OperationLogRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 日志类型
     *
     * @see com.blink.log.constant.LogType
     */
    private String logType;

    /**
     * 操作描述
     */
    private String description;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 登录名
     */
    private String loginName;

    /**
     * 请求URL
     */
    private String requestUrl;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 请求参数（已脱敏）
     */
    private String requestParams;

    /**
     * 响应数据（已脱敏）
     */
    private String responseData;

    /**
     * 执行状态 0成功 1失败
     */
    private Integer executeStatus;

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

    /**
     * 扩展字段（用于业务定制）
     * <p>
     * 业务模块可在此存放自定义字段，如：traceId、moduleId 等
     */
    private Map<String, Object> extraFields;

    /**
     * 添加扩展字段
     *
     * @param key   字段名
     * @param value 字段值
     */
    public void addExtraField(String key, Object value) {
        if (extraFields == null) {
            extraFields = new HashMap<>();
        }
        extraFields.put(key, value);
    }

    /**
     * 获取扩展字段
     *
     * @param key 字段名
     * @return 字段值
     */
    public Object getExtraField(String key) {
        return extraFields != null ? extraFields.get(key) : null;
    }
}