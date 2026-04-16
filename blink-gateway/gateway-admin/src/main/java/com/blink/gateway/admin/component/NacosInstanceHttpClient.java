package com.blink.gateway.admin.component;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.blink.framework.common.exception.BlinkException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.blink.gateway.admin.constants.ErrCodeConstant.OFFLINE_INSTANCE_FAILED;
import static com.blink.gateway.admin.constants.ErrCodeConstant.ONLINE_INSTANCE_FAILED;

/**
 * Nacos 实例管理 HTTP 客户端
 * 通过 Nacos Open API 实现实例的上下线和权重更新，避免 SDK 版本兼容性问题
 *
 * @author binblink
 * @since 2026-04-16
 */
@Component
@Slf4j
public class NacosInstanceHttpClient {

    @Value("${spring.cloud.nacos.server-addr:127.0.0.1:8848}")
    private String serverAddr;

    @Value("${spring.cloud.nacos.discovery.namespace:public}")
    private String namespaceId;

    @Value("${spring.cloud.nacos.discovery.group:DEFAULT_GROUP}")
    private String groupName;

    /**
     * Nacos v2 Open API 基础 URL
     * 注意：v3 API 是 Nacos 3.x 版本，v2 API 是 Nacos 2.x 版本
     */
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        // Nacos 2.x 使用 v2 API
        baseUrl = "http://" + serverAddr + "/nacos/v2/ns/instance";
        log.info("[NacosInstanceHttpClient] 初始化完成 | baseUrl: {}, namespace: {}, group: {}",
                baseUrl, namespaceId, groupName);
    }

    /**
     * 更新实例的启用状态（上下线）
     *
     * @param serviceName 服务名称
     * @param ip          实例IP
     * @param port        实例端口
     * @param enabled     是否启用
     */
    public void updateInstanceEnabled(String serviceName, String ip, Integer port, boolean enabled) {
        Map<String, Object> params = buildBaseParams(serviceName, ip, port);
        params.put("enabled", enabled);

        executeUpdate(params, "更新实例状态", enabled ? ONLINE_INSTANCE_FAILED : OFFLINE_INSTANCE_FAILED);

        log.info("[NacosInstanceHttpClient] 实例状态更新成功 | service: {}, ip: {}, port: {}, enabled: {}",
                serviceName, ip, port, enabled);
    }

    /**
     * 更新实例的权重
     *
     * @param serviceName 服务名称
     * @param ip          实例IP
     * @param port        实例端口
     * @param weight      权重值（0-100，0表示不接收新流量）
     */
    public void updateInstanceWeight(String serviceName, String ip, Integer port, double weight) {
        Map<String, Object> params = buildBaseParams(serviceName, ip, port);
        params.put("weight", weight);

        executeUpdate(params, "更新实例权重", OFFLINE_INSTANCE_FAILED);

        log.info("[NacosInstanceHttpClient] 实例权重更新成功 | service: {}, ip: {}, port: {}, weight: {}",
                serviceName, ip, port, weight);
    }

    /**
     * 同时更新实例的启用状态和权重
     *
     * @param serviceName 服务名称
     * @param ip          实例IP
     * @param port        实例端口
     * @param enabled     是否启用
     * @param weight      权重值
     */
    public void updateInstance(String serviceName, String ip, Integer port, Boolean enabled, Double weight) {
        Map<String, Object> params = buildBaseParams(serviceName, ip, port);
        if (enabled != null) {
            params.put("enabled", enabled);
        }
        if (weight != null) {
            params.put("weight", weight);
        }

        executeUpdate(params, "更新实例", OFFLINE_INSTANCE_FAILED);

        log.info("[NacosInstanceHttpClient] 实例更新成功 | service: {}, ip: {}, port: {}, enabled: {}, weight: {}",
                serviceName, ip, port, enabled, weight);
    }

    /**
     * 构建基础请求参数
     */
    private Map<String, Object> buildBaseParams(String serviceName, String ip, Integer port) {
        Map<String, Object> params = new HashMap<>();
        params.put("namespaceId", namespaceId);
        params.put("groupName", groupName);
        params.put("serviceName", serviceName);
        params.put("ip", ip);
        params.put("port", port);
        params.put("ephemeral", true);
        return params;
    }

    /**
     * 执行更新请求
     * 使用 Nacos v2 Open API: PUT /nacos/v2/ns/instance
     */
    private void executeUpdate(Map<String, Object> params, String operation, String errorCode) {
        try {
            String paramStr = HttpUtil.toParams(params);

            log.debug("[NacosInstanceHttpClient] {} 请求 | url: {}, params: {}", operation, baseUrl, paramStr);

            // 发送 PUT 请求
            HttpResponse response = HttpRequest.put(baseUrl)
                    .body(paramStr)
                    .contentType("application/x-www-form-urlencoded")
                    .timeout(5000)
                    .execute();

            String body = response.body();
            log.debug("[NacosInstanceHttpClient] {} 响应 | status: {}, body: {}", operation, response.getStatus(), body);

            // 检查响应
            if (!response.isOk()) {
                throw new BlinkException(operation + "失败，HTTP状态码: " + response.getStatus(), errorCode);
            }

            // 解析响应 JSON
            Map<String, Object> result = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
            Integer code = (Integer) result.get("code");

            if (code == null || code != 0) {
                String message = (String) result.getOrDefault("message", "未知错误");
                throw new BlinkException(operation + "失败: " + message, errorCode);
            }

        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[NacosInstanceHttpClient] {} 异常 | params: {}, error: {}", operation, params, e.getMessage(), e);
            throw new BlinkException(operation + "异常: " + e.getMessage(), e, errorCode);
        }
    }
}
