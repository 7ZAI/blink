package com.blink.gateway.config;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * 网关实例 ID 环境后置处理器
 * 在 Spring Boot 启动早期自动生成实例 ID，用于动态命名配置文件
 *
 * <p>实例ID格式：{spring.application.name}:{ip}:{server.port}</p>
 * <p>例如：gateway-app:10.186.131.120:8002</p>
 *
 * <p>配置文件命名规则：</p>
 * <ul>
 *   <li>共享配置：blink-gateway.yaml</li>
 *   <li>实例配置：gateway-instance-{instanceId}.yaml</li>
 * </ul>
 *
 * @author binblink
 * @since 2026-04-27
 */
@Slf4j
public class GatewayInstanceIdEnvironmentPostProcessor implements EnvironmentPostProcessor {

    /**
     * 实例ID属性名
     */
    public static final String INSTANCE_ID_PROPERTY = "blink.gateway.instance-id";

    /**
     * 应用名属性名
     */
    public static final String APP_NAME_PROPERTY = "spring.application.name";

    /**
     * 服务端口属性名
     */
    public static final String SERVER_PORT_PROPERTY = "server.port";

    /**
     * Nacos 发现 IP 属性名
     */
    public static final String NACOS_DISCOVERY_IP_PROPERTY = "spring.cloud.nacos.discovery.ip";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // 检查是否已手动设置实例ID
        String existingInstanceId = environment.getProperty(INSTANCE_ID_PROPERTY);
        if (StrUtil.isNotBlank(existingInstanceId)) {
            log.info("[GatewayInstance] 使用手动配置的实例ID: {}", existingInstanceId);
            return;
        }

        // 自动生成实例ID
        String instanceId = generateInstanceId(environment);

        if (StrUtil.isNotBlank(instanceId)) {
            // 添加到环境变量
            Map<String, Object> properties = new HashMap<>();
            properties.put(INSTANCE_ID_PROPERTY, instanceId);

            MapPropertySource propertySource = new MapPropertySource("gatewayInstanceId", properties);
            environment.getPropertySources().addFirst(propertySource);

            log.info("[GatewayInstance] 自动生成实例ID: {}", instanceId);
        }
    }

    /**
     * 生成实例ID
     * 格式：{appName}:{ip}:{port}
     *
     * @param environment 环境变量
     * @return 实例ID
     */
    private String generateInstanceId(ConfigurableEnvironment environment) {
        // 获取应用名
        String appName = environment.getProperty(APP_NAME_PROPERTY, "gateway-app");

        // 获取端口
        String port = environment.getProperty(SERVER_PORT_PROPERTY, "8080");

        // 获取 IP（优先使用 Nacos 发现配置的 IP）
        String ip = environment.getProperty(NACOS_DISCOVERY_IP_PROPERTY);
        if (StrUtil.isBlank(ip)) {
            ip = getLocalIp();
        }

        if (StrUtil.isBlank(ip)) {
            log.warn("[GatewayInstance] 无法获取本机IP，使用默认值");
            ip = "unknown";
        }

        return appName + ":" + ip + ":" + port;
    }

    /**
     * 获取本机 IP 地址
     *
     * @return IP 地址
     */
    private String getLocalIp() {
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            return inetAddress.getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("[GatewayInstance] 获取本机IP失败: {}", e.getMessage());
            return null;
        }
    }
}
