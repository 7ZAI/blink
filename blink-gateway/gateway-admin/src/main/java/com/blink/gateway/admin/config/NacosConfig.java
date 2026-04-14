package com.blink.gateway.admin.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingMaintainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * Nacos 配置类
 * 提供 NamingMaintainService 用于实例管理（上线/下线）
 *
 * @author binblink
 */
@Configuration
@Slf4j
public class NacosConfig {

    @Value("${spring.cloud.nacos.server-addr:127.0.0.1:8848}")
    private String serverAddr;

    @Value("${spring.cloud.nacos.discovery.namespace:public}")
    private String namespace;

    /**
     * 创建 NamingMaintainService Bean
     * 用于管理 Nacos 服务实例（上线、下线、元数据更新等）
     */
    @Bean
    public NamingMaintainService namingMaintainService() throws NacosException {
        Properties properties = new Properties();
        properties.setProperty("serverAddr", serverAddr);
        properties.setProperty("namespace", namespace);

        log.info("[NacosConfig] 创建 NamingMaintainService | serverAddr: {}, namespace: {}", serverAddr, namespace);

        return NacosFactory.createMaintainService(properties);
    }
}
