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
     * 上线时间
     */
    private LocalDateTime onlineTime;

    /**
     * 下线时间
     */
    private LocalDateTime offlineTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}