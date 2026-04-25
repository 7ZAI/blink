package com.blink.gateway.admin.constants;

/**
 * Nacos 配置常量
 *
 * @author binblink
 */
public interface NacosConfigConstant {

    /**
     * 密钥文件nacos上的dataId
     */
    String SECRET_CONFIG_DATA_ID = "secretConfig.json";

    /**
     * 密钥文件nacos上的group
     */
    String SECRET_CONFIG_GROUP = "DEFAULT_GROUP";

    /**
     * 渠道配置文件nacos上的dataId
     */
    String CHANNEL_CONFIG_DATA_ID = "channel-config.json";

    /**
     * 渠道配置文件nacos上的group
     */
    String CHANNEL_CONFIG_GROUP = "DEFAULT_GROUP";
}