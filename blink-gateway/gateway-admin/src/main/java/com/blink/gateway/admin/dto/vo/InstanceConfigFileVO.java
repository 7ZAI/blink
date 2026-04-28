package com.blink.gateway.admin.dto.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实例配置文件 VO
 * 包含配置文件元信息和配置内容
 *
 * @author binblink
 * @since 2026-04-26
 */
@Data
public class InstanceConfigFileVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例ID
     */
    private String instanceId;

    /**
     * 配置文件 DataId
     */
    private String dataId;

    /**
     * 配置文件 Group
     */
    private String group;

    /**
     * 配置文件是否存在
     */
    private Boolean exists;

    /**
     * 配置内容（原始 YAML）
     */
    private String content;

    /**
     * 解析后的配置对象
     */
    private GatewayInstanceConfigVO config;

    /**
     * 最后修改时间
     */
    private LocalDateTime lastModifiedTime;

    /**
     * 配置来源描述
     */
    private String sourceDesc;
}
