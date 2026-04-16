package com.blink.gateway.monitor;

import cn.hutool.core.util.StrUtil;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 熔断器状态转换事件监听器
 *
 * 监听 Resilience4j 熔断器状态转换事件，记录到 Redis 并触发告警
 *
 * @author binblink
 * @since 2026-04-16
 */
@Component
@Slf4j
public class CircuitBreakerStateTransitionListener {

    private static final String HISTORY_KEY_PREFIX = "blink:gateway:circuitbreaker:history:";
    private static final int HISTORY_TTL_SECONDS = 7 * 24 * 60 * 60; // 7 天

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final ReactiveRedisClient redisClient;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name:gateway-reactive}")
    private String serviceId;

    @Value("${server.port:8080}")
    private Integer port;

    @Value("${blink.gateway.instance.ip:}")
    private String configuredIp;

    private final AtomicReference<String> instanceId = new AtomicReference<>();

    public CircuitBreakerStateTransitionListener(CircuitBreakerRegistry circuitBreakerRegistry,
                                                  ReactiveRedisClient redisClient) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.redisClient = redisClient;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        if (circuitBreakerRegistry == null) {
            log.warn("[CircuitBreakerListener] CircuitBreakerRegistry 未注入，跳过状态监听");
            return;
        }

        // 为所有熔断器注册状态转换监听
        circuitBreakerRegistry.getAllCircuitBreakers()
                .forEach(this::registerListener);

        log.info("[CircuitBreakerListener] 状态转换监听器初始化完成");
    }

    /**
     * 为单个熔断器注册监听器
     */
    private void registerListener(CircuitBreaker circuitBreaker) {
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> handleStateTransition(circuitBreaker.getName(), event));

        log.debug("[CircuitBreakerListener] 注册监听器 | name: {}", circuitBreaker.getName());
    }

    /**
     * 处理状态转换事件
     */
    private void handleStateTransition(String cbName, CircuitBreakerOnStateTransitionEvent event) {
        try {
            String id = getInstanceId();

            // 构建状态转换记录
            Map<String, Object> transition = new HashMap<>();
            transition.put("from", event.getStateTransition().getFromState().name());
            transition.put("to", event.getStateTransition().getToState().name());
            transition.put("time", System.currentTimeMillis());
            transition.put("reason", buildReason(event));

            // 获取当前指标
            CircuitBreaker.Metrics metrics = circuitBreakerRegistry.circuitBreaker(cbName).getMetrics();
            transition.put("failureRate", metrics.getFailureRate());
            transition.put("numberOfCalls", metrics.getNumberOfBufferedCalls());

            // 存储到 Redis List（LPUSH 新记录）- 使用响应式客户端
            String historyKey = HISTORY_KEY_PREFIX + id + ":" + cbName;
            String json = objectMapper.writeValueAsString(transition);
            redisClient.lPush(historyKey, json)
                    .flatMap(success -> redisClient.expire(historyKey, Duration.ofSeconds(HISTORY_TTL_SECONDS)))
                    .subscribe(
                            success -> log.debug("[CircuitBreakerListener] 状态记录已保存 | key: {}", historyKey),
                            error -> log.error("[CircuitBreakerListener] 保存状态记录失败 | key: {}, error: {}", historyKey, error.getMessage())
                    );

            log.info("[CircuitBreakerListener] 状态转换记录 | instance: {}, cb: {}, {} -> {}",
                    id, cbName,
                    event.getStateTransition().getFromState().name(),
                    event.getStateTransition().getToState().name());

        } catch (JsonProcessingException e) {
            log.error("[CircuitBreakerListener] 序列化状态转换记录失败", e);
        }
    }

    /**
     * 构建状态转换原因
     */
    private String buildReason(CircuitBreakerOnStateTransitionEvent event) {
        String transition = event.getStateTransition().name();
        return switch (transition) {
            case "CLOSED_TO_OPEN" -> "failureRate_exceeded";
            case "OPEN_TO_HALF_OPEN" -> "waitDurationElapsed";
            case "HALF_OPEN_TO_OPEN" -> "probe_failed";
            case "HALF_OPEN_TO_CLOSED" -> "probe_succeeded";
            default -> transition.toLowerCase();
        };
    }

    /**
     * 获取实例 ID
     */
    private String getInstanceId() {
        if (instanceId.get() == null) {
            try {
                String host = StrUtil.isNotBlank(configuredIp)
                        ? configuredIp
                        : InetAddress.getLocalHost().getHostAddress();
                instanceId.set(serviceId + ":" + host + ":" + port);
            } catch (Exception e) {
                instanceId.set(serviceId + ":unknown:" + port);
            }
        }
        return instanceId.get();
    }
}
