package com.blink.gateway.admin.dto.rsp;

import com.blink.gateway.admin.entity.GaRouteDO;
import lombok.Data;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 实例路由响应
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class InstanceRoutesRsp {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例ID
     */
    private String instanceId;

    /**
     * 获取时间
     */
    private LocalDateTime timestamp;

    /**
     * 路由列表
     */
    private List<GaRouteDO> rows;

    /**
     * 路由数量
     */
    private Integer total;

    /**
     * 是否从 Actuator 获取
     * true - 从实例获取
     * false - 从配置中心获取
     */
    private Boolean fromActuator;

    /**
     * 错误信息（获取失败时）
     */
    private String error;
}
