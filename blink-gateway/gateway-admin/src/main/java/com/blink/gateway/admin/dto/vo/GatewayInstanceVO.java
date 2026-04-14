package com.blink.gateway.admin.dto.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 网关实例视图对象
 *
 * @author binblink
 */
@Data
public class GatewayInstanceVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
     * 实例状态：0-在线，1-离线，2-下线
     */
    private Byte status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 健康状态：true-健康，false-不健康
     */
    private Boolean healthy;

    /**
     * 上线时间
     */
    private LocalDateTime onlineTime;

    /**
     * 下线时间
     */
    private LocalDateTime offlineTime;

    /**
     * 最后心跳时间
     */
    private LocalDateTime lastHeartbeat;
}
