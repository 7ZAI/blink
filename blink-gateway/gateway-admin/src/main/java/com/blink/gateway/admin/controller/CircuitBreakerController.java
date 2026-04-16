package com.blink.gateway.admin.controller;

import cn.hutool.core.collection.CollUtil;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.rsp.CircuitBreakerConfigRsp;
import com.blink.gateway.admin.dto.rsp.CircuitBreakerInstanceRsp;
import com.blink.gateway.admin.dto.rsp.CircuitBreakerOverviewRsp;
import com.blink.gateway.admin.dto.rsp.CircuitBreakerStatusRsp;
import com.blink.gateway.admin.dto.rsp.CircuitBreakerSummaryRsp;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 熔断器监控控制器
 * 提供熔断器配置和状态监控
 *
 * @author binblink
 * @since 2026-04-15
 */
@RestController
@RequestMapping("/circuitBreaker")
@Slf4j
public class CircuitBreakerController {

    private static final String GATEWAY_SERVICE_NAME = "gateway-app";

    @Resource
    private DiscoveryClient discoveryClient;

    /**
     * 预定义的熔断器配置（与 application.yml 中配置一致）
     */
    private static final Map<String, CircuitBreakerConfigRsp> PREDEFINED_CONFIGS = new HashMap<>();

    static {
        // 默认配置
        CircuitBreakerConfigRsp defaultConfig = new CircuitBreakerConfigRsp();
        defaultConfig.setName("default");
        defaultConfig.setBaseConfig(null);
        defaultConfig.setSlidingWindowType("COUNT_BASED");
        defaultConfig.setSlidingWindowSize(10);
        defaultConfig.setMinimumNumberOfCalls(5);
        defaultConfig.setFailureRateThreshold(50.0);
        defaultConfig.setSlowCallRateThreshold(100.0);
        defaultConfig.setSlowCallDurationThreshold(8000L);
        defaultConfig.setWaitDurationInOpenState(60L);
        defaultConfig.setPermittedNumberOfCallsInHalfOpenState(3);
        defaultConfig.setAutomaticTransitionFromOpenToHalfOpenEnabled(true);
        PREDEFINED_CONFIGS.put("default", defaultConfig);

        // 严格配置
        CircuitBreakerConfigRsp strictConfig = new CircuitBreakerConfigRsp();
        strictConfig.setName("strict");
        strictConfig.setSlidingWindowType("COUNT_BASED");
        strictConfig.setSlidingWindowSize(10);
        strictConfig.setMinimumNumberOfCalls(5);
        strictConfig.setFailureRateThreshold(30.0);
        strictConfig.setSlowCallRateThreshold(100.0);
        strictConfig.setSlowCallDurationThreshold(8000L);
        strictConfig.setWaitDurationInOpenState(120L);
        strictConfig.setPermittedNumberOfCallsInHalfOpenState(2);
        strictConfig.setAutomaticTransitionFromOpenToHalfOpenEnabled(true);
        PREDEFINED_CONFIGS.put("strict", strictConfig);

        // 宽松配置
        CircuitBreakerConfigRsp lenientConfig = new CircuitBreakerConfigRsp();
        lenientConfig.setName("lenient");
        lenientConfig.setSlidingWindowType("COUNT_BASED");
        lenientConfig.setSlidingWindowSize(20);
        lenientConfig.setMinimumNumberOfCalls(10);
        lenientConfig.setFailureRateThreshold(70.0);
        lenientConfig.setSlowCallRateThreshold(100.0);
        lenientConfig.setSlowCallDurationThreshold(8000L);
        lenientConfig.setWaitDurationInOpenState(30L);
        lenientConfig.setPermittedNumberOfCallsInHalfOpenState(5);
        lenientConfig.setAutomaticTransitionFromOpenToHalfOpenEnabled(true);
        PREDEFINED_CONFIGS.put("lenient", lenientConfig);

        // 实例配置
        String[] instanceNames = {
            "myCircuitBreaker", "resilientCircuitBreaker", "protectedCircuitBreaker",
            "strictCircuitBreaker", "lenientCircuitBreaker", "userCircuitBreaker", "imageCircuitBreaker"
        };

        Map<String, String> instanceBaseConfigs = new HashMap<>();
        instanceBaseConfigs.put("myCircuitBreaker", "default");
        instanceBaseConfigs.put("resilientCircuitBreaker", "default");
        instanceBaseConfigs.put("protectedCircuitBreaker", "default");
        instanceBaseConfigs.put("strictCircuitBreaker", "strict");
        instanceBaseConfigs.put("lenientCircuitBreaker", "lenient");
        instanceBaseConfigs.put("userCircuitBreaker", "default");
        instanceBaseConfigs.put("imageCircuitBreaker", "lenient");

        Map<String, Double> instanceThresholdOverrides = new HashMap<>();
        instanceThresholdOverrides.put("resilientCircuitBreaker", 60.0);
        instanceThresholdOverrides.put("protectedCircuitBreaker", 55.0);
        instanceThresholdOverrides.put("imageCircuitBreaker", 80.0);

        for (String name : instanceNames) {
            String baseConfigName = instanceBaseConfigs.get(name);
            CircuitBreakerConfigRsp baseConfig = PREDEFINED_CONFIGS.get(baseConfigName);

            CircuitBreakerConfigRsp instanceConfig = new CircuitBreakerConfigRsp();
            instanceConfig.setName(name);
            instanceConfig.setBaseConfig(baseConfigName);
            instanceConfig.setSlidingWindowType(baseConfig.getSlidingWindowType());
            instanceConfig.setSlidingWindowSize(baseConfig.getSlidingWindowSize());
            instanceConfig.setMinimumNumberOfCalls(baseConfig.getMinimumNumberOfCalls());
            instanceConfig.setFailureRateThreshold(
                instanceThresholdOverrides.getOrDefault(name, baseConfig.getFailureRateThreshold())
            );
            instanceConfig.setSlowCallRateThreshold(baseConfig.getSlowCallRateThreshold());
            instanceConfig.setSlowCallDurationThreshold(baseConfig.getSlowCallDurationThreshold());
            instanceConfig.setWaitDurationInOpenState(baseConfig.getWaitDurationInOpenState());
            instanceConfig.setPermittedNumberOfCallsInHalfOpenState(baseConfig.getPermittedNumberOfCallsInHalfOpenState());
            instanceConfig.setAutomaticTransitionFromOpenToHalfOpenEnabled(baseConfig.getAutomaticTransitionFromOpenToHalfOpenEnabled());
            PREDEFINED_CONFIGS.put(name, instanceConfig);
        }
    }

    /**
     * 获取熔断器监控总览
     *
     * @param reqDto 请求参数
     * @return 熔断器总览
     */
    @PostMapping("/getOverview")
    public ResponseDTO<CircuitBreakerOverviewRsp> getOverview(@RequestBody RequestDTO<Void> reqDto) {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(GATEWAY_SERVICE_NAME);

            CircuitBreakerOverviewRsp overview = new CircuitBreakerOverviewRsp();

            // 构建熔断器配置列表（实例级别的配置）
            List<CircuitBreakerSummaryRsp> circuitBreakers = new ArrayList<>();

            // 添加所有预定义的熔断器实例配置
            String[] instanceNames = {
                "myCircuitBreaker", "resilientCircuitBreaker", "protectedCircuitBreaker",
                "strictCircuitBreaker", "lenientCircuitBreaker", "userCircuitBreaker", "imageCircuitBreaker"
            };

            for (String name : instanceNames) {
                CircuitBreakerConfigRsp config = PREDEFINED_CONFIGS.get(name);
                if (config != null) {
                    // 构建汇总响应
                    CircuitBreakerSummaryRsp summary = new CircuitBreakerSummaryRsp();
                    summary.setName(name);
                    summary.setBaseConfig(config.getBaseConfig());
                    summary.setFailureRateThreshold(config.getFailureRateThreshold());
                    summary.setSlidingWindowSize(config.getSlidingWindowSize());
                    summary.setMinimumNumberOfCalls(config.getMinimumNumberOfCalls());
                    summary.setWaitDurationInOpenState(config.getWaitDurationInOpenState());
                    summary.setClosedCount(instances.size());
                    summary.setOpenCount(0);
                    summary.setHalfOpenCount(0);

                    // 为每个实例创建状态（模拟数据，实际需要从 Redis 获取）
                    List<CircuitBreakerInstanceRsp> instanceRsps = new ArrayList<>();
                    for (ServiceInstance instance : instances) {
                        CircuitBreakerInstanceRsp instanceRsp = new CircuitBreakerInstanceRsp();
                        instanceRsp.setInstanceId(instance.getInstanceId());
                        instanceRsp.setState("CLOSED"); // 默认 CLOSED 状态
                        instanceRsp.setFailureRate(0.0);
                        instanceRsp.setNumberOfCalls(0);
                        instanceRsp.setNumberOfFailedCalls(0);
                        instanceRsp.setNumberOfSuccessfulCalls(0);
                        instanceRsp.setTimestamp(System.currentTimeMillis());
                        instanceRsps.add(instanceRsp);
                    }
                    summary.setInstances(instanceRsps);
                    circuitBreakers.add(summary);
                }
            }

            overview.setCircuitBreakers(circuitBreakers);
            overview.setTotalCircuitBreakers(circuitBreakers.size());
            overview.setOpenCount(0); // 统计实际 OPEN 状态数量
            overview.setClosedCount(instances.size() * circuitBreakers.size()); // 默认都是 CLOSED
            overview.setHalfOpenCount(0);
            overview.setTotalInstances(instances.size());
            overview.setHealthScore(100.0); // 默认健康度满分

            log.info("[CircuitBreaker] 获取熔断器总览成功 | total: {}, instances: {}",
                    overview.getTotalCircuitBreakers(), overview.getTotalInstances());

            return ResponseDTO.newSuccessInstance(overview);
        } catch (Exception e) {
            log.error("[CircuitBreaker] 获取熔断器总览失败 | error: {}", e.getMessage(), e);
            return ResponseDTO.newSuccessInstance(new CircuitBreakerOverviewRsp());
        }
    }

    /**
     * 获取指定熔断器配置详情
     *
     * @param reqDto 请求参数（name）
     * @return 熔断器配置详情
     */
    @PostMapping("/getConfig")
    public ResponseDTO<CircuitBreakerConfigRsp> getConfig(@RequestBody RequestDTO<Map<String, String>> reqDto) {
        try {
            String name = reqDto.getBody().get("name");

            if (name == null || name.isEmpty()) {
                return ResponseDTO.newSuccessInstance(PREDEFINED_CONFIGS.get("default"));
            }

            CircuitBreakerConfigRsp config = PREDEFINED_CONFIGS.get(name);
            if (config == null) {
                config = PREDEFINED_CONFIGS.get("default");
            }

            // 获取实例状态
            List<ServiceInstance> instances = discoveryClient.getInstances(GATEWAY_SERVICE_NAME);
            List<CircuitBreakerStatusRsp> instanceStatuses = new ArrayList<>();

            for (ServiceInstance instance : instances) {
                CircuitBreakerStatusRsp status = new CircuitBreakerStatusRsp();
                status.setName(name);
                status.setInstanceId(instance.getInstanceId());
                status.setState("CLOSED");
                status.setFailureRate(0.0);
                status.setNumberOfCalls(0);
                status.setNumberOfFailedCalls(0);
                status.setNumberOfSlowCalls(0);
                status.setNumberOfSuccessfulCalls(0);
                status.setTimestamp(System.currentTimeMillis());
                instanceStatuses.add(status);
            }

            config.setInstanceStatuses(instanceStatuses);

            log.info("[CircuitBreaker] 获取熔断器配置成功 | name: {}", name);

            return ResponseDTO.newSuccessInstance(config);
        } catch (Exception e) {
            log.error("[CircuitBreaker] 获取熔断器配置失败 | error: {}", e.getMessage(), e);
            return ResponseDTO.newSuccessInstance(new CircuitBreakerConfigRsp());
        }
    }
}