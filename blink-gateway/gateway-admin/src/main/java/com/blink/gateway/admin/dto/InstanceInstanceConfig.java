package com.blink.gateway.admin.dto;

import lombok.Data;

/**
 * 实例配置信息
 * 从配置文件或 metadata 获取
 *
 * @author binblink
 * @since 2026-04-26
 */
@Data
public class InstanceInstanceConfig {

    /**
     * 分组标识
     */
    private String groupKey;

    /**
     * 配置来源：config_file / metadata / default
     */
    private String source;

    /**
     * 创建默认配置
     *
     * @param defaultGroupKey 默认分组
     * @return 默认配置
     */
    public static InstanceInstanceConfig createDefault(String defaultGroupKey) {
        InstanceInstanceConfig config = new InstanceInstanceConfig();
        config.setGroupKey(defaultGroupKey);
        config.setSource("default");
        return config;
    }
}
