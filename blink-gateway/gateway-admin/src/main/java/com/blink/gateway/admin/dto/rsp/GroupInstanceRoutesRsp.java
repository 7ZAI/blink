package com.blink.gateway.admin.dto.rsp;

import com.blink.gateway.admin.entity.GaRouteDO;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 分组实例路由响应
 * 包含从实例获取的路由列表及来源信息
 *
 * @author binblink
 * @since 2026-04-26
 */
@Data
public class GroupInstanceRoutesRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 来源实例 ID
     */
    private String instanceId;

    /**
     * 存储模式（redis/nacos）
     */
    private String storageMode;

    /**
     * 获取时间
     */
    private LocalDateTime timestamp;

    /**
     * 跨由列表
     */
    private List<GaRouteDO> rows;

    /**
     * 跨由总数
     */
    private Integer total;

    /**
     * 是否来自 Actuator
     * true - 从实例获取
     * false - 从配置中心获取
     */
    private Boolean fromActuator;

    /**
     * 错误信息（可选，获取失败时返回）
     */
    private String error;
}