package com.blink.gateway.admin.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 部署配置属性
 *
 * @author binblink
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "gateway-admin.deploy")
public class DeployProperties {

    /**
     * 部署模式: single(单实例) / cluster(多实例集群)
     */
    private String mode = "single";

    /**
     * 权限缓存开关（多实例必须开启）
     */
    private Boolean cacheEnabled = false;
}