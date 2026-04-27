package com.blink.gateway.admin.dto.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实例基本信息 VO
 *
 * @author binblink
 */
@Data
public class InstanceInfoVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    private Integer id;

    /**
     * 实例 ID
     */
    private String instanceId;

    /**
     * 服务 ID
     */
    private String serviceId;

    /**
     * 主机地址
     */
    private String host;

    /**
     * 端口
     */
    private Integer port;

    /**
     * URI
     */
    private String uri;

    /**
     * 元数据
     */
    private String metadata;

    /**
     * 实例状态
     * 0-在线，1-离线，2-下线
     */
    private Byte status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 分组标识
     */
    private String groupKey;

    /**
     * 存储方式：nacos/redis
     */
    private String storageMode;

    /**
     * 上线时间
     */
    private LocalDateTime onlineTime;

    /**
     * 下线时间
     */
    private LocalDateTime offlineTime;

    /**
     * 下线原因
     */
    private String offlineReason;

    /**
     * 下线类型: MANUAL-主动/FAULT-被动/DRAINING-排空中
     */
    private String offlineType;

    /**
     * 是否在注册中心（Nacos 实时状态）
     */
    private Boolean inRegistry;

    /**
     * 状态冲突提示（数据库状态与实际状态不一致时）
     */
    private String statusConflict;

    /**
     * 健康状态（UP/DOWN/UNKNOWN/OFFLINE）
     * UP: 实例健康且在注册中心
     * DOWN: 实例不健康或不在注册中心
     * UNKNOWN: 无法确定健康状态
     * OFFLINE: 实例已离线/下线
     */
    private String healthStatus;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}