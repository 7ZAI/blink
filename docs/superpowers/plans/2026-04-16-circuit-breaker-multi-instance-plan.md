# 熔断器监控多实例切换重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构熔断器监控页面，实现左右分栏多实例切换视图，通过 Redis Stream 采集真实熔断器指标，SSE 实时推送数据到前端 Store。

**Architecture:** 复用现有 Redis Stream 架构，gateway-reactive 采集熔断器指标并上报，gateway-admin 消费存储到 Redis，通过 SSE 推送到前端 Pinia Store，页面监听 Store 数据变化自动更新。

**Tech Stack:** Spring Boot 3.2, Resilience4j, Redis Stream, SSE, Vue 3, Pinia, Element Plus, TypeScript

---

## 文件结构概览

### 后端新增文件

```
blink-gateway/blink-gateway-reactive/src/main/java/com/blink/gateway/monitor/
├── dto/
│   └── CircuitBreakerMetric.java         # 熔断器指标 DTO
└── CircuitBreakerStateTransitionListener.java  # 状态转换监听器

blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/
├── dto/
│   ├── req/
│   │   ├── GetCircuitBreakerOverviewReq.java   # 总览请求
│   │   ├── GetCircuitBreakerDetailReq.java     # 详情请求
│   │   └── GetCircuitBreakerHistoryReq.java    # 历史请求
│   └── rsp/
│       ├── CircuitBreakerOverviewRsp.java      # 总览响应（重构）
│       ├── CircuitBreakerSummaryRsp.java       # 熔断器汇总
│       ├── CircuitBreakerInstanceRsp.java      # 实例状态
│       ├── CircuitBreakerDetailRsp.java        # 详情响应
│       ├── StateTransitionHistoryRsp.java      # 状态转换历史
│       └── InstanceSummaryRsp.java             # 实例摘要
├── service/
│   └── CircuitBreakerService.java              # 熔断器服务
└── constants/
    └── CircuitBreakerConstant.java             # 熔断器常量
```

### 后端修改文件

```
blink-gateway/blink-gateway-reactive/src/main/java/com/blink/gateway/monitor/
├── dto/MetricsMessage.java                     # 新增 circuitBreakers 字段
└── MetricsReporterImpl.java                    # 新增熔断器指标采集方法

blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/
├── service/MetricsStreamConsumer.java          # 新增熔断器指标存储逻辑
└── controller/CircuitBreakerController.java    # 重构为真实数据
```

### 前端新增文件

```
frontend/packages/gateway-admin/src/
├── stores/
│   └── circuitBreaker.ts                       # 熔断器 Pinia Store
├── views/monitor/circuitBreaker/
│   ├── index.vue                               # 重构主页面
│   └── components/
│       ├── InstancePanel.vue                   # 左侧实例列表
│       ├── SummaryCards.vue                    # 汇总统计卡片
│       ├── CircuitBreakerList.vue              # 熔断器列表
│       ├── StateHistory.vue                    # 状态转换历史
│       └── TrendChart.vue                      # 趋势图组件
└── composables/
    └── useCircuitBreaker.ts                    # 熔断器数据逻辑
```

### 前端修改文件

```
frontend/packages/gateway-admin/src/
├── api/circuitBreaker.ts                       # 扩展 API 定义
└── stores/notification.ts                      # 新增熔断器 SSE 消息类型处理
```

---

## Phase 1: 后端数据采集

### Task 1.1: 新增熔断器指标 DTO

**Files:**
- Create: `blink-gateway/blink-gateway-reactive/src/main/java/com/blink/gateway/monitor/dto/CircuitBreakerMetric.java`

- [ ] **Step 1: 创建 CircuitBreakerMetric DTO**

```java
package com.blink.gateway.monitor.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 单个熔断器指标 DTO
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class CircuitBreakerMetric implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 熔断器名称
     */
    private String name;

    /**
     * 状态：CLOSED/OPEN/HALF_OPEN
     */
    private String state;

    /**
     * 失败率（%）
     */
    private Double failureRate;

    /**
     * 慢调用率（%）
     */
    private Double slowCallRate;

    /**
     * 总调用次数
     */
    private Integer numberOfCalls;

    /**
     * 失败调用次数
     */
    private Integer numberOfFailedCalls;

    /**
     * 慢调用次数
     */
    private Integer numberOfSlowCalls;

    /**
     * 成功调用次数
     */
    private Integer numberOfSuccessfulCalls;

    /**
     * 状态转换时间戳
     */
    private Long stateTransitionTime;

    /**
     * 指标采集时间戳
     */
    private Long timestamp;
}
```

- [ ] **Step 2: 验证文件创建成功**

Run: `ls -la blink-gateway/blink-gateway-reactive/src/main/java/com/blink/gateway/monitor/dto/CircuitBreakerMetric.java`
Expected: 文件存在

---

### Task 1.2: 扩展 MetricsMessage 支持熔断器指标

**Files:**
- Modify: `blink-gateway/blink-gateway-reactive/src/main/java/com/blink/gateway/monitor/dto/MetricsMessage.java`

- [ ] **Step 1: 在 MetricsMessage 中新增 circuitBreakers 字段**

在 `MetricsMessage.java` 类中，在 `healthStatus` 字段后添加：

```java
    // ==================== 熔断器指标 ====================

    /**
     * 熔断器指标列表
     */
    private List<CircuitBreakerMetric> circuitBreakers;
```

并在文件顶部添加 import：

```java
import java.util.List;
```

- [ ] **Step 2: 验证修改**

Run: `grep -n "circuitBreakers" blink-gateway/blink-gateway-reactive/src/main/java/com/blink/gateway/monitor/dto/MetricsMessage.java`
Expected: 输出包含 `private List<CircuitBreakerMetric> circuitBreakers;`

---

### Task 1.3: 扩展 MetricsReporterImpl 采集熔断器指标

**Files:**
- Modify: `blink-gateway/blink-gateway-reactive/src/main/java/com/blink/gateway/monitor/MetricsReporterImpl.java`

- [ ] **Step 1: 添加 CircuitBreakerRegistry 依赖注入**

在 `MetricsReporterImpl` 类中，添加字段和构造函数参数：

```java
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

// 在类字段区域添加
private final CircuitBreakerRegistry circuitBreakerRegistry;

// 修改构造函数，添加 CircuitBreakerRegistry 参数
public MetricsReporterImpl(MeterRegistry meterRegistry,
                           ReactiveStringRedisTemplate redisTemplate,
                           BuildProperties buildProperties,
                           MonitorConfigHolder configHolder,
                           CircuitBreakerRegistry circuitBreakerRegistry) {
    this.meterRegistry = meterRegistry;
    this.redisTemplate = redisTemplate;
    this.objectMapper = new ObjectMapper();
    this.configHolder = configHolder;
    this.circuitBreakerRegistry = circuitBreakerRegistry;
}
```

- [ ] **Step 2: 新增 collectCircuitBreakerMetrics 方法**

在 `collectMetrics()` 方法中，在 `collectHttpMetrics(message);` 后添加调用：

```java
        // 采集熔断器指标
        collectCircuitBreakerMetrics(message);
```

并在类中添加新方法：

```java
    /**
     * 采集熔断器指标
     */
    private void collectCircuitBreakerMetrics(MetricsMessage message) {
        if (circuitBreakerRegistry == null) {
            log.debug("[MetricsReporter] CircuitBreakerRegistry 未注入，跳过熔断器指标采集");
            return;
        }

        List<CircuitBreakerMetric> metrics = new ArrayList<>();

        // 获取所有已注册的熔断器配置名称
        for (String name : circuitBreakerRegistry.getConfigurationNames()) {
            try {
                CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);
                CircuitBreaker.Metrics cbMetrics = cb.getMetrics();

                CircuitBreakerMetric metric = new CircuitBreakerMetric();
                metric.setName(name);
                metric.setState(cb.getState().name());
                metric.setFailureRate(cbMetrics.getFailureRate());
                metric.setSlowCallRate(cbMetrics.getSlowCallRate());
                metric.setNumberOfCalls(cbMetrics.getNumberOfCalls());
                metric.setNumberOfFailedCalls(cbMetrics.getNumberOfFailedCalls());
                metric.setNumberOfSlowCalls(cbMetrics.getNumberOfSlowCalls());
                metric.setNumberOfSuccessfulCalls(cbMetrics.getNumberOfSuccessfulCalls());
                metric.setTimestamp(System.currentTimeMillis());

                // 状态转换时间需要通过事件监听获取，这里暂不设置
                metric.setStateTransitionTime(null);

                metrics.add(metric);
            } catch (Exception e) {
                log.warn("[MetricsReporter] 获取熔断器指标失败 | name: {}, error: {}", name, e.getMessage());
            }
        }

        // 也获取所有已创建的熔断器实例
        for (CircuitBreaker cb : circuitBreakerRegistry.getAllCircuitBreakers()) {
            String name = cb.getName();
            // 避免重复添加
            if (metrics.stream().noneMatch(m -> m.getName().equals(name))) {
                try {
                    CircuitBreaker.Metrics cbMetrics = cb.getMetrics();

                    CircuitBreakerMetric metric = new CircuitBreakerMetric();
                    metric.setName(name);
                    metric.setState(cb.getState().name());
                    metric.setFailureRate(cbMetrics.getFailureRate());
                    metric.setSlowCallRate(cbMetrics.getSlowCallRate());
                    metric.setNumberOfCalls(cbMetrics.getNumberOfCalls());
                    metric.setNumberOfFailedCalls(cbMetrics.getNumberOfFailedCalls());
                    metric.setNumberOfSlowCalls(cbMetrics.getNumberOfSlowCalls());
                    metric.setNumberOfSuccessfulCalls(cbMetrics.getNumberOfSuccessfulCalls());
                    metric.setTimestamp(System.currentTimeMillis());
                    metric.setStateTransitionTime(null);

                    metrics.add(metric);
                } catch (Exception e) {
                    log.warn("[MetricsReporter] 获取熔断器实例指标失败 | name: {}, error: {}", name, e.getMessage());
                }
            }
        }

        message.setCircuitBreakers(metrics);

        log.debug("[MetricsReporter] 采集熔断器指标完成 | count: {}", metrics.size());
    }
```

- [ ] **Step 3: 添加必要的 import**

```java
import com.blink.gateway.monitor.dto.CircuitBreakerMetric;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.ArrayList;
import java.util.List;
```

- [ ] **Step 4: 更新 messageToMap 方法支持熔断器指标序列化**

在 `messageToMap` 方法末尾，`return data;` 之前添加：

```java
        // 熔断器指标
        if (CollUtil.isNotEmpty(message.getCircuitBreakers())) {
            try {
                String circuitBreakersJson = objectMapper.writeValueAsString(message.getCircuitBreakers());
                data.put("circuitBreakers", circuitBreakersJson);
            } catch (JsonProcessingException e) {
                log.error("[MetricsReporter] 序列化熔断器指标失败", e);
            }
        }
```

添加 import：

```java
import cn.hutool.core.collection.CollUtil;
```

- [ ] **Step 5: 验证修改**

Run: `grep -n "collectCircuitBreakerMetrics\|CircuitBreakerRegistry" blink-gateway/blink-gateway-reactive/src/main/java/com/blink/gateway/monitor/MetricsReporterImpl.java`
Expected: 输出包含相关方法签名和字段

---

### Task 1.4: 新增状态转换事件监听器

**Files:**
- Create: `blink-gateway/blink-gateway-reactive/src/main/java/com/blink/gateway/monitor/CircuitBreakerStateTransitionListener.java`

- [ ] **Step 1: 创建状态转换监听器**

```java
package com.blink.gateway.monitor;

import cn.hutool.core.util.StrUtil;
import com.blink.framework.redis.component.RedisClient;
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
    private final RedisClient redisClient;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name:gateway-reactive}")
    private String serviceId;

    @Value("${server.port:8080}")
    private Integer port;

    @Value("${blink.gateway.instance.ip:}")
    private String configuredIp;

    private final AtomicReference<String> instanceId = new AtomicReference<>();

    public CircuitBreakerStateTransitionListener(CircuitBreakerRegistry circuitBreakerRegistry,
                                                  RedisClient redisClient) {
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
            transition.put("numberOfCalls", metrics.getNumberOfCalls());

            // 存储到 Redis List（LPUSH 新记录）
            String historyKey = HISTORY_KEY_PREFIX + id + ":" + cbName;
            String json = objectMapper.writeValueAsString(transition);
            redisClient.lPush(historyKey, json);
            redisClient.expire(historyKey, HISTORY_TTL_SECONDS);

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
```

- [ ] **Step 2: 验证文件创建成功**

Run: `ls -la blink-gateway/blink-gateway-reactive/src/main/java/com/blink/gateway/monitor/CircuitBreakerStateTransitionListener.java`
Expected: 文件存在

---

### Task 1.5: 扩展 MetricsStreamConsumer 存储熔断器指标

**Files:**
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/MetricsStreamConsumer.java`

- [ ] **Step 1: 添加熔断器指标存储常量**

在类顶部常量区域添加：

```java
    private static final String CB_KEY_PREFIX = "blink:gateway:circuitbreaker:";
    private static final int CB_TTL_SECONDS = 90;
```

- [ ] **Step 2: 在 handleMetrics 方法中添加熔断器指标处理**

在 `handleMetrics` 方法的 `// 存储实例指标到 Redis Hash` 注释后，添加熔断器指标处理逻辑：

```java
        // 处理熔断器指标
        String circuitBreakersJson = message.get("circuitBreakers");
        if (StrUtil.isNotBlank(circuitBreakersJson)) {
            storeCircuitBreakerMetrics(instanceId, circuitBreakersJson);
        }
```

- [ ] **Step 3: 新增 storeCircuitBreakerMetrics 方法**

在 `MetricsStreamConsumer` 类中添加新方法：

```java
    /**
     * 存储熔断器指标到 Redis
     */
    private void storeCircuitBreakerMetrics(String instanceId, String circuitBreakersJson) {
        try {
            // 解析 JSON 数组
            List<Map<String, Object>> metrics = objectMapper.readValue(
                    circuitBreakersJson,
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            if (CollUtil.isEmpty(metrics)) {
                return;
            }

            // 存储到 Redis Hash: blink:gateway:circuitbreaker:{instanceId}
            String cbKey = CB_KEY_PREFIX + instanceId;
            Map<String, Object> cbData = new HashMap<>();
            cbData.put("timestamp", System.currentTimeMillis());

            for (Map<String, Object> metric : metrics) {
                String cbName = (String) metric.get("name");
                if (StrUtil.isNotBlank(cbName)) {
                    // 检测状态变化
                    checkStateTransition(instanceId, cbName, metric);
                    // 存储单个熔断器指标（JSON 字符串）
                    cbData.put(cbName, objectMapper.writeValueAsString(metric));
                }
            }

            redisClient.hSet(cbKey, cbData);
            redisClient.expire(cbKey, CB_TTL_SECONDS);

            log.debug("[MetricsStreamConsumer] 存储熔断器指标 | instanceId: {}, count: {}",
                    instanceId, metrics.size());

        } catch (Exception e) {
            log.error("[MetricsStreamConsumer] 解析熔断器指标失败 | error: {}", e.getMessage());
        }
    }

    /**
     * 检测状态转换
     */
    private void checkStateTransition(String instanceId, String cbName, Map<String, Object> newMetric) {
        try {
            String cbKey = CB_KEY_PREFIX + instanceId;
            String oldMetricJson = (String) redisClient.hGet(cbKey, cbName);

            if (StrUtil.isBlank(oldMetricJson)) {
                return;
            }

            Map<String, Object> oldMetric = objectMapper.readValue(oldMetricJson,
                    new TypeReference<Map<String, Object>>() {});

            String oldState = (String) oldMetric.get("state");
            String newState = (String) newMetric.get("state");

            if (!StrUtil.equals(oldState, newState)) {
                // 记录状态转换历史
                recordStateTransition(instanceId, cbName, oldState, newState, newMetric);

                // 触发告警
                triggerCircuitBreakerAlert(instanceId, cbName, oldState, newState, newMetric);
            }
        } catch (Exception e) {
            log.error("[MetricsStreamConsumer] 检测状态转换失败 | error: {}", e.getMessage());
        }
    }

    /**
     * 记录状态转换历史
     */
    private void recordStateTransition(String instanceId, String cbName,
                                        String fromState, String toState,
                                        Map<String, Object> metric) {
        try {
            String historyKey = CB_KEY_PREFIX + "history:" + instanceId + ":" + cbName;

            Map<String, Object> transition = new HashMap<>();
            transition.put("from", fromState);
            transition.put("to", toState);
            transition.put("time", System.currentTimeMillis());
            transition.put("reason", buildTransitionReason(fromState, toState));
            transition.put("failureRate", metric.get("failureRate"));
            transition.put("numberOfCalls", metric.get("numberOfCalls"));

            String json = objectMapper.writeValueAsString(transition);
            redisClient.lPush(historyKey, json);
            redisClient.expire(historyKey, 7 * 24 * 60 * 60); // 7 天

            log.info("[MetricsStreamConsumer] 状态转换记录 | instance: {}, cb: {}, {} -> {}",
                    instanceId, cbName, fromState, toState);

        } catch (Exception e) {
            log.error("[MetricsStreamConsumer] 记录状态转换历史失败", e);
        }
    }

    /**
     * 构建状态转换原因
     */
    private String buildTransitionReason(String fromState, String toState) {
        if ("CLOSED".equals(fromState) && "OPEN".equals(toState)) {
            return "failureRate_exceeded";
        } else if ("OPEN".equals(fromState) && "HALF_OPEN".equals(toState)) {
            return "waitDurationElapsed";
        } else if ("HALF_OPEN".equals(fromState) && "OPEN".equals(toState)) {
            return "probe_failed";
        } else if ("HALF_OPEN".equals(fromState) && "CLOSED".equals(toState)) {
            return "probe_succeeded";
        }
        return fromState.toLowerCase() + "_to_" + toState.toLowerCase();
    }

    /**
     * 触发熔断器告警
     */
    private void triggerCircuitBreakerAlert(String instanceId, String cbName,
                                            String fromState, String toState,
                                            Map<String, Object> metric) {
        String severity = determineAlertSeverity(fromState, toState);

        if (severity == null) {
            return;
        }

        NotificationPayload payload = new NotificationPayload();
        payload.setTitle("熔断器状态变化");
        payload.setContent(String.format("实例 %s 的 %s 从 %s 变为 %s",
                instanceId, cbName, fromState, toState));
        payload.setSeverity(severity);
        payload.setCreatedTime(LocalDateTime.now());
        payload.setTargetType("all");

        SseMessage<NotificationPayload> message = SseMessage.notification(payload);
        sseConnectionPool.broadcast(message);

        log.info("[MetricsStreamConsumer] 触发熔断器告警 | instance: {}, cb: {}, severity: {}",
                instanceId, cbName, severity);
    }

    /**
     * 确定告警级别
     */
    private String determineAlertSeverity(String fromState, String toState) {
        if ("CLOSED".equals(fromState) && "OPEN".equals(toState)) {
            return "warning";
        } else if ("HALF_OPEN".equals(fromState) && "OPEN".equals(toState)) {
            return "error";
        }
        return null;
    }
```

- [ ] **Step 4: 添加必要的 import**

```java
import com.fasterxml.jackson.core.type.TypeReference;
import java.time.LocalDateTime;
```

并在类中添加 `objectMapper` 字段：

```java
    private final ObjectMapper objectMapper = new ObjectMapper();
```

- [ ] **Step 5: 验证修改**

Run: `grep -n "storeCircuitBreakerMetrics\|CB_KEY_PREFIX" blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/MetricsStreamConsumer.java`
Expected: 输出包含相关方法签名和常量

---

### Task 1.6: 编译验证后端数据采集模块

**Files:**
- Build: `blink-gateway/blink-gateway-reactive`
- Build: `blink-gateway/gateway-admin`

- [ ] **Step 1: 编译 gateway-reactive 模块**

Run: `./gradlew :blink-gateway:blink-gateway-reactive:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 编译 gateway-admin 模块**

Run: `./gradlew :blink-gateway:gateway-admin:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交 Phase 1 代码**

```bash
git add blink-gateway/blink-gateway-reactive/src/main/java/com/blink/gateway/monitor/
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/MetricsStreamConsumer.java
git commit -m "feat(circuit-breaker): Phase 1 - 后端数据采集实现

- 新增 CircuitBreakerMetric DTO
- 扩展 MetricsMessage 支持熔断器指标
- MetricsReporterImpl 新增熔断器指标采集方法
- 新增 CircuitBreakerStateTransitionListener 状态转换监听器
- MetricsStreamConsumer 新增熔断器指标存储和状态转换检测

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Phase 2: 后端 API 重构

### Task 2.1: 新增熔断器常量类

**Files:**
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/constants/CircuitBreakerConstant.java`

- [ ] **Step 1: 创建常量类**

```java
package com.blink.gateway.admin.constants;

/**
 * 熔断器相关常量
 *
 * @author binblink
 * @since 2026-04-16
 */
public interface CircuitBreakerConstant {

    /**
     * Redis Key 前缀：熔断器指标
     */
    String CB_KEY_PREFIX = "blink:gateway:circuitbreaker:";

    /**
     * Redis Key 前缀：状态转换历史
     */
    String CB_HISTORY_KEY_PREFIX = "blink:gateway:circuitbreaker:history:";

    /**
     * 熔断器指标 TTL（秒）
     */
    int CB_TTL_SECONDS = 90;

    /**
     * 历史记录 TTL（秒）- 7 天
     */
    int HISTORY_TTL_SECONDS = 7 * 24 * 60 * 60;

    /**
     * 默认历史记录查询数量
     */
    int DEFAULT_HISTORY_LIMIT = 20;

    /**
     * 状态：关闭（正常）
     */
    String STATE_CLOSED = "CLOSED";

    /**
     * 状态：开启（熔断）
     */
    String STATE_OPEN = "OPEN";

    /**
     * 状态：半开（探测）
     */
    String STATE_HALF_OPEN = "HALF_OPEN";
}
```

- [ ] **Step 2: 验证文件创建成功**

Run: `ls -la blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/constants/CircuitBreakerConstant.java`
Expected: 文件存在

---

### Task 2.2: 新增请求 DTO

**Files:**
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/GetCircuitBreakerOverviewReq.java`
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/GetCircuitBreakerDetailReq.java`
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/GetCircuitBreakerHistoryReq.java`

- [ ] **Step 1: 创建 GetCircuitBreakerOverviewReq**

```java
package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 获取熔断器总览请求
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class GetCircuitBreakerOverviewReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例ID（可选，不传则返回聚合视图）
     */
    private String instanceId;
}
```

- [ ] **Step 2: 创建 GetCircuitBreakerDetailReq**

```java
package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 获取熔断器详情请求
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class GetCircuitBreakerDetailReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 熔断器名称
     */
    private String name;

    /**
     * 实例ID（可选，不传则返回所有实例）
     */
    private String instanceId;
}
```

- [ ] **Step 3: 创建 GetCircuitBreakerHistoryReq**

```java
package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 获取状态转换历史请求
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class GetCircuitBreakerHistoryReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例ID
     */
    private String instanceId;

    /**
     * 熔断器名称
     */
    private String name;

    /**
     * 返回数量限制（默认 20）
     */
    private Integer limit;
}
```

- [ ] **Step 4: 验证文件创建成功**

Run: `ls -la blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/GetCircuitBreaker*.java`
Expected: 三个文件都存在

---

### Task 2.3: 新增响应 DTO

**Files:**
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/CircuitBreakerSummaryRsp.java`
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/CircuitBreakerInstanceRsp.java`
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/CircuitBreakerDetailRsp.java`
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/StateTransitionHistoryRsp.java`
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/InstanceSummaryRsp.java`

- [ ] **Step 1: 创建 CircuitBreakerSummaryRsp**

```java
package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 熔断器汇总响应
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class CircuitBreakerSummaryRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 熔断器名称
     */
    private String name;

    /**
     * 基础配置名称
     */
    private String baseConfig;

    /**
     * 失败率阈值
     */
    private Double failureRateThreshold;

    /**
     * 滑动窗口大小
     */
    private Integer slidingWindowSize;

    /**
     * 最小调用次数
     */
    private Integer minimumNumberOfCalls;

    /**
     * 开启状态等待时间（秒）
     */
    private Long waitDurationInOpenState;

    /**
     * CLOSED 状态实例数
     */
    private Integer closedCount;

    /**
     * OPEN 状态实例数
     */
    private Integer openCount;

    /**
     * HALF_OPEN 状态实例数
     */
    private Integer halfOpenCount;

    /**
     * 实例详情列表（聚合视图时返回）
     */
    private List<CircuitBreakerInstanceRsp> instances;
}
```

- [ ] **Step 2: 创建 CircuitBreakerInstanceRsp**

```java
package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 熔断器实例状态响应
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class CircuitBreakerInstanceRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例ID
     */
    private String instanceId;

    /**
     * 状态：CLOSED/OPEN/HALF_OPEN
     */
    private String state;

    /**
     * 失败率（%）
     */
    private Double failureRate;

    /**
     * 慢调用率（%）
     */
    private Double slowCallRate;

    /**
     * 总调用次数
     */
    private Integer numberOfCalls;

    /**
     * 失败调用次数
     */
    private Integer numberOfFailedCalls;

    /**
     * 成功调用次数
     */
    private Integer numberOfSuccessfulCalls;

    /**
     * 状态转换时间戳
     */
    private Long stateTransitionTime;

    /**
     * 指标采集时间戳
     */
    private Long timestamp;
}
```

- [ ] **Step 3: 创建 CircuitBreakerDetailRsp**

```java
package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 熔断器详情响应
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class CircuitBreakerDetailRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 配置信息
     */
    private CircuitBreakerConfigRsp config;

    /**
     * 实例状态列表
     */
    private List<CircuitBreakerInstanceRsp> instances;

    /**
     * 状态转换历史
     */
    private List<StateTransitionHistoryRsp> history;

    /**
     * 趋势数据（最近 30 分钟）
     */
    private List<TrendDataRsp> trend;
}
```

- [ ] **Step 4: 创建 StateTransitionHistoryRsp**

```java
package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 状态转换历史响应
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class StateTransitionHistoryRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 转换前状态
     */
    private String fromState;

    /**
     * 转换后状态
     */
    private String toState;

    /**
     * 转换时间戳
     */
    private Long timestamp;

    /**
     * 转换原因
     */
    private String reason;

    /**
     * 失败率
     */
    private Double failureRate;

    /**
     * 调用次数
     */
    private Integer numberOfCalls;
}
```

- [ ] **Step 5: 创建 InstanceSummaryRsp**

```java
package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 实例摘要响应
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class InstanceSummaryRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例ID
     */
    private String instanceId;

    /**
     * 主机地址
     */
    private String host;

    /**
     * 端口
     */
    private Integer port;

    /**
     * 状态：ONLINE/OFFLINE
     */
    private String status;

    /**
     * 健康状态
     */
    private String healthStatus;

    /**
     * 熔断器汇总
     */
    private CircuitBreakerSummary summary;

    /**
     * 熔断器汇总内部类
     */
    @Data
    public static class CircuitBreakerSummary implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Integer total;
        private Integer open;
        private Integer closed;
        private Integer halfOpen;
    }
}
```

- [ ] **Step 6: 创建 TrendDataRsp**

```java
package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 趋势数据响应
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class TrendDataRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 失败率
     */
    private Double failureRate;

    /**
     * 慢调用率
     */
    private Double slowCallRate;

    /**
     * 调用次数
     */
    private Integer numberOfCalls;
}
```

- [ ] **Step 7: 验证文件创建成功**

Run: `ls -la blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/CircuitBreaker*Rsp.java blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/StateTransitionHistoryRsp.java blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/InstanceSummaryRsp.java blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/TrendDataRsp.java`
Expected: 所有文件都存在

---

### Task 2.4: 重构 CircuitBreakerOverviewRsp

**Files:**
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/CircuitBreakerOverviewRsp.java`

- [ ] **Step 1: 重构 CircuitBreakerOverviewRsp**

```java
package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 熔断器总览响应
 *
 * @author binblink
 * @since 2026-04-15
 */
@Data
public class CircuitBreakerOverviewRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 熔断器列表
     */
    private List<CircuitBreakerSummaryRsp> circuitBreakers;

    /**
     * 熔断器总数
     */
    private Integer totalCircuitBreakers;

    /**
     * OPEN 状态数量
     */
    private Integer openCount;

    /**
     * CLOSED 状态数量
     */
    private Integer closedCount;

    /**
     * HALF_OPEN 状态数量
     */
    private Integer halfOpenCount;

    /**
     * 实例总数
     */
    private Integer totalInstances;

    /**
     * 健康度评分（0-100）
     */
    private Double healthScore;
}
```

- [ ] **Step 2: 验证修改**

Run: `grep -n "healthScore\|CircuitBreakerSummaryRsp" blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/CircuitBreakerOverviewRsp.java`
Expected: 输出包含相关字段

---

### Task 2.5: 新增 CircuitBreakerService

**Files:**
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/CircuitBreakerService.java`

- [ ] **Step 1: 创建 CircuitBreakerService**

```java
package com.blink.gateway.admin.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.constants.CircuitBreakerConstant;
import com.blink.gateway.admin.dto.rsp.*;
import com.blink.gateway.admin.sse.NotificationPayload;
import com.blink.gateway.admin.sse.SseConnectionPool;
import com.blink.gateway.admin.sse.SseMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

import static com.blink.gateway.admin.constants.CircuitBreakerConstant.*;

/**
 * 熔断器服务
 *
 * @author binblink
 * @since 2026-04-16
 */
@Service
@Slf4j
public class CircuitBreakerService {

    private final RedisClient redisClient;
    private final ObjectMapper objectMapper;
    private final SseConnectionPool sseConnectionPool;

    // 预定义的熔断器配置（与 application.yml 中配置一致）
    private static final Map<String, CircuitBreakerConfigRsp> PREDEFINED_CONFIGS = new LinkedHashMap<>();

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

    public CircuitBreakerService(RedisClient redisClient, SseConnectionPool sseConnectionPool) {
        this.redisClient = redisClient;
        this.objectMapper = new ObjectMapper();
        this.sseConnectionPool = sseConnectionPool;
    }

    /**
     * 获取实例列表
     */
    public List<InstanceSummaryRsp> getInstanceList() {
        Map<String, Object> instanceList = redisClient.hGetStringMap(INSTANCE_LIST_KEY);

        if (CollUtil.isEmpty(instanceList)) {
            return new ArrayList<>();
        }

        List<InstanceSummaryRsp> result = new ArrayList<>();

        for (String instanceId : instanceList.keySet()) {
            InstanceSummaryRsp summary = new InstanceSummaryRsp();
            summary.setInstanceId(instanceId);

            // 解析 host 和 port
            String[] parts = instanceId.split(":");
            if (parts.length >= 3) {
                summary.setHost(parts[1]);
                summary.setPort(Integer.parseInt(parts[2]));
            }

            summary.setStatus("ONLINE");
            summary.setHealthStatus("UP");

            // 获取熔断器汇总
            summary.setSummary(getCircuitBreakerSummary(instanceId));

            result.add(summary);
        }

        return result;
    }

    /**
     * 获取实例的熔断器汇总
     */
    private InstanceSummaryRsp.CircuitBreakerSummary getCircuitBreakerSummary(String instanceId) {
        InstanceSummaryRsp.CircuitBreakerSummary summary = new InstanceSummaryRsp.CircuitBreakerSummary();
        summary.setTotal(0);
        summary.setOpen(0);
        summary.setClosed(0);
        summary.setHalfOpen(0);

        String cbKey = CB_KEY_PREFIX + instanceId;
        Map<String, Object> cbData = redisClient.hGetStringMap(cbKey);

        if (CollUtil.isEmpty(cbData)) {
            return summary;
        }

        int total = 0, open = 0, closed = 0, halfOpen = 0;

        for (Map.Entry<String, Object> entry : cbData.entrySet()) {
            if ("timestamp".equals(entry.getKey())) continue;

            try {
                String json = String.valueOf(entry.getValue());
                Map<String, Object> metric = objectMapper.readValue(json,
                        new TypeReference<Map<String, Object>>() {});

                String state = (String) metric.get("state");
                total++;
                if (STATE_OPEN.equals(state)) open++;
                else if (STATE_HALF_OPEN.equals(state)) halfOpen++;
                else closed++;

            } catch (Exception e) {
                log.warn("[CircuitBreakerService] 解析熔断器指标失败 | key: {}", entry.getKey());
            }
        }

        summary.setTotal(total);
        summary.setOpen(open);
        summary.setClosed(closed);
        summary.setHalfOpen(halfOpen);

        return summary;
    }

    /**
     * 获取熔断器总览
     */
    public CircuitBreakerOverviewRsp getOverview(String instanceId) {
        CircuitBreakerOverviewRsp overview = new CircuitBreakerOverviewRsp();

        // 获取所有实例
        Map<String, Object> instanceList = redisClient.hGetStringMap(INSTANCE_LIST_KEY);

        if (CollUtil.isEmpty(instanceList)) {
            overview.setCircuitBreakers(new ArrayList<>());
            overview.setTotalCircuitBreakers(0);
            overview.setOpenCount(0);
            overview.setClosedCount(0);
            overview.setHalfOpenCount(0);
            overview.setTotalInstances(0);
            overview.setHealthScore(100.0);
            return overview;
        }

        // 按熔断器名称聚合数据
        Map<String, CircuitBreakerSummaryRsp> cbMap = new LinkedHashMap<>();

        // 初始化所有预定义的熔断器
        for (String name : PREDEFINED_CONFIGS.keySet()) {
            if (!name.equals("default") && !name.equals("strict") && !name.equals("lenient")) {
                CircuitBreakerSummaryRsp summary = new CircuitBreakerSummaryRsp();
                summary.setName(name);
                CircuitBreakerConfigRsp config = PREDEFINED_CONFIGS.get(name);
                summary.setBaseConfig(config.getBaseConfig());
                summary.setFailureRateThreshold(config.getFailureRateThreshold());
                summary.setSlidingWindowSize(config.getSlidingWindowSize());
                summary.setMinimumNumberOfCalls(config.getMinimumNumberOfCalls());
                summary.setWaitDurationInOpenState(config.getWaitDurationInOpenState());
                summary.setClosedCount(0);
                summary.setOpenCount(0);
                summary.setHalfOpenCount(0);
                summary.setInstances(new ArrayList<>());
                cbMap.put(name, summary);
            }
        }

        int totalOpen = 0, totalClosed = 0, totalHalfOpen = 0;

        // 遍历实例，聚合熔断器状态
        for (String instId : instanceList.keySet()) {
            // 如果指定了实例ID，只处理该实例
            if (StrUtil.isNotBlank(instanceId) && !instanceId.equals(instId)) {
                continue;
            }

            String cbKey = CB_KEY_PREFIX + instId;
            Map<String, Object> cbData = redisClient.hGetStringMap(cbKey);

            if (CollUtil.isEmpty(cbData)) {
                continue;
            }

            for (Map.Entry<String, Object> entry : cbData.entrySet()) {
                if ("timestamp".equals(entry.getKey())) continue;

                String cbName = entry.getKey();
                CircuitBreakerSummaryRsp summary = cbMap.get(cbName);

                if (summary == null) {
                    // 动态添加未预定义的熔断器
                    summary = new CircuitBreakerSummaryRsp();
                    summary.setName(cbName);
                    summary.setClosedCount(0);
                    summary.setOpenCount(0);
                    summary.setHalfOpenCount(0);
                    summary.setInstances(new ArrayList<>());
                    cbMap.put(cbName, summary);
                }

                try {
                    String json = String.valueOf(entry.getValue());
                    Map<String, Object> metric = objectMapper.readValue(json,
                            new TypeReference<Map<String, Object>>() {});

                    CircuitBreakerInstanceRsp instanceRsp = new CircuitBreakerInstanceRsp();
                    instanceRsp.setInstanceId(instId);
                    instanceRsp.setState((String) metric.get("state"));
                    instanceRsp.setFailureRate(toDouble(metric.get("failureRate")));
                    instanceRsp.setSlowCallRate(toDouble(metric.get("slowCallRate")));
                    instanceRsp.setNumberOfCalls(toInt(metric.get("numberOfCalls")));
                    instanceRsp.setNumberOfFailedCalls(toInt(metric.get("numberOfFailedCalls")));
                    instanceRsp.setNumberOfSuccessfulCalls(toInt(metric.get("numberOfSuccessfulCalls")));
                    instanceRsp.setTimestamp(toLong(metric.get("timestamp")));

                    summary.getInstances().add(instanceRsp);

                    // 更新状态计数
                    String state = instanceRsp.getState();
                    if (STATE_OPEN.equals(state)) {
                        summary.setOpenCount(summary.getOpenCount() + 1);
                        totalOpen++;
                    } else if (STATE_HALF_OPEN.equals(state)) {
                        summary.setHalfOpenCount(summary.getHalfOpenCount() + 1);
                        totalHalfOpen++;
                    } else {
                        summary.setClosedCount(summary.getClosedCount() + 1);
                        totalClosed++;
                    }

                } catch (Exception e) {
                    log.warn("[CircuitBreakerService] 解析熔断器指标失败 | name: {}", cbName);
                }
            }
        }

        // 计算健康度评分
        int totalCount = totalOpen + totalClosed + totalHalfOpen;
        double healthScore = totalCount > 0
                ? (double) totalClosed / totalCount * 100
                : 100.0;
        healthScore = BigDecimal.valueOf(healthScore).setScale(1, RoundingMode.HALF_UP).doubleValue();

        overview.setCircuitBreakers(new ArrayList<>(cbMap.values()));
        overview.setTotalCircuitBreakers(cbMap.size());
        overview.setOpenCount(totalOpen);
        overview.setClosedCount(totalClosed);
        overview.setHalfOpenCount(totalHalfOpen);
        overview.setTotalInstances(instanceList.size());
        overview.setHealthScore(healthScore);

        return overview;
    }

    /**
     * 获取熔断器详情
     */
    public CircuitBreakerDetailRsp getDetail(String name, String instanceId) {
        CircuitBreakerDetailRsp detail = new CircuitBreakerDetailRsp();

        // 获取配置
        CircuitBreakerConfigRsp config = PREDEFINED_CONFIGS.getOrDefault(name, PREDEFINED_CONFIGS.get("default"));
        detail.setConfig(config);

        // 获取实例状态
        List<CircuitBreakerInstanceRsp> instances = new ArrayList<>();
        Map<String, Object> instanceList = redisClient.hGetStringMap(INSTANCE_LIST_KEY);

        for (String instId : instanceList.keySet()) {
            if (StrUtil.isNotBlank(instanceId) && !instanceId.equals(instId)) {
                continue;
            }

            String cbKey = CB_KEY_PREFIX + instId;
            String json = (String) redisClient.hGet(cbKey, name);

            if (StrUtil.isBlank(json)) {
                continue;
            }

            try {
                Map<String, Object> metric = objectMapper.readValue(json,
                        new TypeReference<Map<String, Object>>() {});

                CircuitBreakerInstanceRsp instanceRsp = new CircuitBreakerInstanceRsp();
                instanceRsp.setInstanceId(instId);
                instanceRsp.setState((String) metric.get("state"));
                instanceRsp.setFailureRate(toDouble(metric.get("failureRate")));
                instanceRsp.setSlowCallRate(toDouble(metric.get("slowCallRate")));
                instanceRsp.setNumberOfCalls(toInt(metric.get("numberOfCalls")));
                instanceRsp.setNumberOfFailedCalls(toInt(metric.get("numberOfFailedCalls")));
                instanceRsp.setNumberOfSuccessfulCalls(toInt(metric.get("numberOfSuccessfulCalls")));
                instanceRsp.setTimestamp(toLong(metric.get("timestamp")));

                instances.add(instanceRsp);

            } catch (Exception e) {
                log.warn("[CircuitBreakerService] 解析熔断器指标失败 | name: {}, instance: {}", name, instId);
            }
        }

        detail.setInstances(instances);

        // 获取状态转换历史
        if (StrUtil.isNotBlank(instanceId)) {
            detail.setHistory(getHistory(instanceId, name, DEFAULT_HISTORY_LIMIT));
        } else {
            // 聚合所有实例的历史
            detail.setHistory(new ArrayList<>());
        }

        // 趋势数据暂不实现
        detail.setTrend(new ArrayList<>());

        return detail;
    }

    /**
     * 获取状态转换历史
     */
    public List<StateTransitionHistoryRsp> getHistory(String instanceId, String name, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = DEFAULT_HISTORY_LIMIT;
        }

        String historyKey = CB_HISTORY_KEY_PREFIX + instanceId + ":" + name;
        List<String> historyList = redisClient.lRange(historyKey, 0, limit - 1);

        if (CollUtil.isEmpty(historyList)) {
            return new ArrayList<>();
        }

        List<StateTransitionHistoryRsp> result = new ArrayList<>();

        for (String json : historyList) {
            try {
                Map<String, Object> item = objectMapper.readValue(json,
                        new TypeReference<Map<String, Object>>() {});

                StateTransitionHistoryRsp history = new StateTransitionHistoryRsp();
                history.setFromState((String) item.get("from"));
                history.setToState((String) item.get("to"));
                history.setTimestamp(toLong(item.get("time")));
                history.setReason((String) item.get("reason"));
                history.setFailureRate(toDouble(item.get("failureRate")));
                history.setNumberOfCalls(toInt(item.get("numberOfCalls")));

                result.add(history);

            } catch (Exception e) {
                log.warn("[CircuitBreakerService] 解析历史记录失败 | json: {}", json);
            }
        }

        return result;
    }

    /**
     * 推送熔断器数据更新到前端
     */
    public void pushCircuitBreakerUpdate() {
        CircuitBreakerOverviewRsp overview = getOverview(null);

        // 构建推送消息
        Map<String, Object> payload = new HashMap<>();
        payload.put("circuitBreakers", overview.getCircuitBreakers());
        payload.put("totalCircuitBreakers", overview.getTotalCircuitBreakers());
        payload.put("openCount", overview.getOpenCount());
        payload.put("closedCount", overview.getClosedCount());
        payload.put("halfOpenCount", overview.getHalfOpenCount());
        payload.put("totalInstances", overview.getTotalInstances());
        payload.put("healthScore", overview.getHealthScore());

        SseMessage<Map<String, Object>> message = SseMessage.of("circuit_breaker_data", payload);
        sseConnectionPool.broadcast(message);
    }

    private Double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private Integer toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }
}
```

- [ ] **Step 2: 验证文件创建成功**

Run: `ls -la blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/CircuitBreakerService.java`
Expected: 文件存在

---

### Task 2.6: 重构 CircuitBreakerController

**Files:**
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/controller/CircuitBreakerController.java`

- [ ] **Step 1: 重构 Controller**

```java
package com.blink.gateway.admin.controller;

import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.GetCircuitBreakerDetailReq;
import com.blink.gateway.admin.dto.req.GetCircuitBreakerHistoryReq;
import com.blink.gateway.admin.dto.req.GetCircuitBreakerOverviewReq;
import com.blink.gateway.admin.dto.rsp.CircuitBreakerDetailRsp;
import com.blink.gateway.admin.dto.rsp.CircuitBreakerOverviewRsp;
import com.blink.gateway.admin.dto.rsp.InstanceSummaryRsp;
import com.blink.gateway.admin.dto.rsp.StateTransitionHistoryRsp;
import com.blink.gateway.admin.service.CircuitBreakerService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @Resource
    private CircuitBreakerService circuitBreakerService;

    /**
     * 获取实例列表
     *
     * @param reqDto 请求参数
     * @return 实例列表
     */
    @PostMapping("/getInstanceList")
    public ResponseDTO<List<InstanceSummaryRsp>> getInstanceList(@RequestBody RequestDTO<Void> reqDto) {
        try {
            List<InstanceSummaryRsp> instances = circuitBreakerService.getInstanceList();
            log.info("[CircuitBreaker] 获取实例列表成功 | count: {}", instances.size());
            return ResponseDTO.newSuccessInstance(instances);
        } catch (Exception e) {
            log.error("[CircuitBreaker] 获取实例列表失败 | error: {}", e.getMessage(), e);
            return ResponseDTO.newSuccessInstance(List.of());
        }
    }

    /**
     * 获取熔断器监控总览
     *
     * @param reqDto 请求参数
     * @return 熔断器总览
     */
    @PostMapping("/getOverview")
    public ResponseDTO<CircuitBreakerOverviewRsp> getOverview(@RequestBody RequestDTO<GetCircuitBreakerOverviewReq> reqDto) {
        try {
            String instanceId = reqDto.getBody() != null ? reqDto.getBody().getInstanceId() : null;
            CircuitBreakerOverviewRsp overview = circuitBreakerService.getOverview(instanceId);

            log.info("[CircuitBreaker] 获取熔断器总览成功 | total: {}, instances: {}",
                    overview.getTotalCircuitBreakers(), overview.getTotalInstances());

            return ResponseDTO.newSuccessInstance(overview);
        } catch (Exception e) {
            log.error("[CircuitBreaker] 获取熔断器总览失败 | error: {}", e.getMessage(), e);
            return ResponseDTO.newSuccessInstance(new CircuitBreakerOverviewRsp());
        }
    }

    /**
     * 获取熔断器详情
     *
     * @param reqDto 请求参数
     * @return 熔断器详情
     */
    @PostMapping("/getDetail")
    public ResponseDTO<CircuitBreakerDetailRsp> getDetail(@RequestBody RequestDTO<GetCircuitBreakerDetailReq> reqDto) {
        try {
            String name = reqDto.getBody() != null ? reqDto.getBody().getName() : null;
            String instanceId = reqDto.getBody() != null ? reqDto.getBody().getInstanceId() : null;

            if (name == null || name.isEmpty()) {
                return ResponseDTO.newSuccessInstance(new CircuitBreakerDetailRsp());
            }

            CircuitBreakerDetailRsp detail = circuitBreakerService.getDetail(name, instanceId);

            log.info("[CircuitBreaker] 获取熔断器详情成功 | name: {}", name);

            return ResponseDTO.newSuccessInstance(detail);
        } catch (Exception e) {
            log.error("[CircuitBreaker] 获取熔断器详情失败 | error: {}", e.getMessage(), e);
            return ResponseDTO.newSuccessInstance(new CircuitBreakerDetailRsp());
        }
    }

    /**
     * 获取状态转换历史
     *
     * @param reqDto 请求参数
     * @return 状态转换历史
     */
    @PostMapping("/getHistory")
    public ResponseDTO<List<StateTransitionHistoryRsp>> getHistory(@RequestBody RequestDTO<GetCircuitBreakerHistoryReq> reqDto) {
        try {
            String instanceId = reqDto.getBody() != null ? reqDto.getBody().getInstanceId() : null;
            String name = reqDto.getBody() != null ? reqDto.getBody().getName() : null;
            Integer limit = reqDto.getBody() != null ? reqDto.getBody().getLimit() : null;

            if (instanceId == null || instanceId.isEmpty() || name == null || name.isEmpty()) {
                return ResponseDTO.newSuccessInstance(List.of());
            }

            List<StateTransitionHistoryRsp> history = circuitBreakerService.getHistory(instanceId, name, limit);

            log.info("[CircuitBreaker] 获取状态转换历史成功 | instance: {}, name: {}, count: {}",
                    instanceId, name, history.size());

            return ResponseDTO.newSuccessInstance(history);
        } catch (Exception e) {
            log.error("[CircuitBreaker] 获取状态转换历史失败 | error: {}", e.getMessage(), e);
            return ResponseDTO.newSuccessInstance(List.of());
        }
    }
}
```

- [ ] **Step 2: 删除旧的 PREDEFINED_CONFIGS 和 DiscoveryClient 依赖**

确认 Controller 中不再使用：
- `private static final Map<String, CircuitBreakerConfigRsp> PREDEFINED_CONFIGS`
- `private DiscoveryClient discoveryClient`

- [ ] **Step 3: 验证修改**

Run: `grep -n "CircuitBreakerService\|getInstanceList\|getHistory" blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/controller/CircuitBreakerController.java`
Expected: 输出包含相关内容

---

### Task 2.7: 编译验证后端 API 模块

**Files:**
- Build: `blink-gateway/gateway-admin`

- [ ] **Step 1: 编译 gateway-admin 模块**

Run: `./gradlew :blink-gateway:gateway-admin:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 提交 Phase 2 代码**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/constants/CircuitBreakerConstant.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/GetCircuitBreaker*.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/CircuitBreaker*Rsp.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/StateTransitionHistoryRsp.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/InstanceSummaryRsp.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/TrendDataRsp.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/CircuitBreakerService.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/controller/CircuitBreakerController.java
git commit -m "feat(circuit-breaker): Phase 2 - 后端 API 重构实现

- 新增 CircuitBreakerConstant 常量类
- 新增请求 DTO: GetCircuitBreakerOverviewReq/DetailReq/HistoryReq
- 新增响应 DTO: SummaryRsp/InstanceRsp/DetailRsp/HistoryRsp 等
- 新增 CircuitBreakerService 服务层
- 重构 CircuitBreakerController 从 Redis 读取真实数据

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Phase 3: 前端重构

### Task 3.1: 扩展 circuitBreaker.ts API 定义

**Files:**
- Modify: `frontend/packages/gateway-admin/src/api/circuitBreaker.ts`

- [ ] **Step 1: 扩展类型定义**

在现有类型定义后添加：

```typescript
// ==================== 新增类型定义 ====================

/**
 * 实例摘要
 */
export interface InstanceSummary {
  instanceId: string
  host: string
  port: number
  status: string
  healthStatus: string
  summary: {
    total: number
    open: number
    closed: number
    halfOpen: number
  }
}

/**
 * 熔断器汇总（重构后）
 */
export interface CircuitBreakerSummary {
  name: string
  baseConfig?: string
  failureRateThreshold: number
  slidingWindowSize: number
  minimumNumberOfCalls: number
  waitDurationInOpenState: number
  closedCount: number
  openCount: number
  halfOpenCount: number
  instances?: CircuitBreakerInstanceStatus[]
}

/**
 * 熔断器实例状态
 */
export interface CircuitBreakerInstanceStatus {
  instanceId: string
  state: string
  failureRate: number
  slowCallRate: number
  numberOfCalls: number
  numberOfFailedCalls: number
  numberOfSuccessfulCalls: number
  stateTransitionTime?: number
  timestamp: number
}

/**
 * 状态转换历史
 */
export interface StateTransitionHistory {
  fromState: string
  toState: string
  timestamp: number
  reason: string
  failureRate?: number
  numberOfCalls?: number
}

/**
 * 熔断器详情
 */
export interface CircuitBreakerDetail {
  config: CircuitBreakerConfig
  instances: CircuitBreakerInstanceStatus[]
  history: StateTransitionHistory[]
  trend: TrendData[]
}

/**
 * 趋势数据
 */
export interface TrendData {
  timestamp: number
  failureRate: number
  slowCallRate: number
  numberOfCalls: number
}

/**
 * 熔断器总览（重构后）
 */
export interface CircuitBreakerOverviewNew {
  circuitBreakers: CircuitBreakerSummary[]
  totalCircuitBreakers: number
  openCount: number
  closedCount: number
  halfOpenCount: number
  totalInstances: number
  healthScore: number
}
```

- [ ] **Step 2: 新增 API 函数**

在现有 API 函数后添加：

```typescript
// ==================== 新增 API 函数 ====================

/**
 * 获取实例列表
 */
export const getInstanceList = (): Promise<InstanceSummary[]> => {
  return request.post('/circuitBreaker/getInstanceList', { body: {} })
}

/**
 * 获取熔断器总览（支持按实例筛选）
 * @param instanceId 实例ID（可选）
 */
export const getOverviewNew = (instanceId?: string): Promise<CircuitBreakerOverviewNew> => {
  return request.post('/circuitBreaker/getOverview', { body: { instanceId } })
}

/**
 * 获取熔断器详情
 * @param name 熔断器名称
 * @param instanceId 实例ID（可选）
 */
export const getDetail = (name: string, instanceId?: string): Promise<CircuitBreakerDetail> => {
  return request.post('/circuitBreaker/getDetail', { body: { name, instanceId } })
}

/**
 * 获取状态转换历史
 * @param instanceId 实例ID
 * @param name 熔断器名称
 * @param limit 返回数量限制
 */
export const getHistory = (instanceId: string, name: string, limit?: number): Promise<StateTransitionHistory[]> => {
  return request.post('/circuitBreaker/getHistory', { body: { instanceId, name, limit } })
}
```

- [ ] **Step 3: 更新 API 对象导出**

```typescript
export const circuitBreakerApi = {
  getOverview,
  getConfig,
  getInstanceList,
  getOverviewNew,
  getDetail,
  getHistory,
}
```

- [ ] **Step 4: 验证修改**

Run: `grep -n "getInstanceList\|getHistory\|CircuitBreakerOverviewNew" frontend/packages/gateway-admin/src/api/circuitBreaker.ts`
Expected: 输出包含相关内容

---

### Task 3.2: 创建熔断器 Pinia Store

**Files:**
- Create: `frontend/packages/gateway-admin/src/stores/circuitBreaker.ts`

- [ ] **Step 1: 创建 Store**

```typescript
import { defineStore } from 'pinia'
import { ref, computed, readonly } from 'vue'
import {
  circuitBreakerApi,
  type CircuitBreakerOverviewNew,
  type CircuitBreakerSummary,
  type InstanceSummary,
  type CircuitBreakerDetail,
  type StateTransitionHistory,
} from '@/api/circuitBreaker'
import {
  registerCircuitBreakerCallback,
  unregisterCircuitBreakerCallback,
} from '@/stores/notification'

/**
 * SSE 熔断器数据推送类型
 */
export interface CircuitBreakerDataPayload {
  circuitBreakers: CircuitBreakerSummary[]
  totalCircuitBreakers: number
  openCount: number
  closedCount: number
  halfOpenCount: number
  totalInstances: number
  healthScore: number
}

/**
 * CircuitBreaker Store - SSE 消息总线模式
 *
 * 职责：
 * 1. 从后端 API 获取初始数据
 * 2. 通过 SSE 实时接收熔断器数据更新
 * 3. 提供实例切换和详情查询能力
 */
export const useCircuitBreakerStore = defineStore('circuitBreaker', () => {
  // ==================== 状态 ====================

  // 总览数据（全局共享）
  const overview = ref<CircuitBreakerOverviewNew | null>(null)

  // 实例列表
  const instances = ref<InstanceSummary[]>([])

  // 当前选中的实例ID（null 表示聚合视图）
  const selectedInstanceId = ref<string | null>(null)

  // 详情数据
  const detail = ref<CircuitBreakerDetail | null>(null)

  // 加载状态
  const loading = ref(false)
  const detailLoading = ref(false)

  // ==================== 计算属性 ====================

  // 是否有 OPEN 状态的熔断器
  const hasOpenCircuitBreaker = computed(() =>
    (overview.value?.openCount ?? 0) > 0
  )

  // 健康度等级
  const healthLevel = computed(() => {
    const score = overview.value?.healthScore ?? 100
    if (score >= 80) return 'success'
    if (score >= 60) return 'warning'
    return 'danger'
  })

  // ==================== SSE 数据更新回调 ====================

  /**
   * 处理 SSE 推送的熔断器数据
   */
  const handleCircuitBreakerData = (data: CircuitBreakerDataPayload) => {
    overview.value = {
      circuitBreakers: data.circuitBreakers,
      totalCircuitBreakers: data.totalCircuitBreakers,
      openCount: data.openCount,
      closedCount: data.closedCount,
      halfOpenCount: data.halfOpenCount,
      totalInstances: data.totalInstances,
      healthScore: data.healthScore,
    }
    console.log('[CircuitBreakerStore] SSE 数据更新 | healthScore:', data.healthScore)
  }

  // 注册 SSE 回调
  registerCircuitBreakerCallback(handleCircuitBreakerData)

  // ==================== 方法 ====================

  /**
   * 获取实例列表
   */
  const fetchInstances = async () => {
    try {
      instances.value = await circuitBreakerApi.getInstanceList()
    } catch (error) {
      console.error('[CircuitBreakerStore] 获取实例列表失败:', error)
    }
  }

  /**
   * 获取总览数据
   */
  const fetchOverview = async (instanceId?: string) => {
    loading.value = true
    try {
      overview.value = await circuitBreakerApi.getOverviewNew(instanceId ?? undefined)
    } catch (error) {
      console.error('[CircuitBreakerStore] 获取总览失败:', error)
    } finally {
      loading.value = false
    }
  }

  /**
   * 选择实例
   */
  const selectInstance = async (instanceId: string | null) => {
    selectedInstanceId.value = instanceId
    await fetchOverview(instanceId ?? undefined)
  }

  /**
   * 获取详情
   */
  const fetchDetail = async (name: string, instanceId?: string) => {
    detailLoading.value = true
    try {
      detail.value = await circuitBreakerApi.getDetail(name, instanceId)
    } catch (error) {
      console.error('[CircuitBreakerStore] 获取详情失败:', error)
    } finally {
      detailLoading.value = false
    }
  }

  /**
   * 获取状态转换历史
   */
  const fetchHistory = async (instanceId: string, name: string, limit?: number) => {
    try {
      return await circuitBreakerApi.getHistory(instanceId, name, limit)
    } catch (error) {
      console.error('[CircuitBreakerStore] 获取历史失败:', error)
      return []
    }
  }

  /**
   * 重置状态
   */
  const reset = () => {
    overview.value = null
    instances.value = []
    selectedInstanceId.value = null
    detail.value = null
  }

  return {
    // 状态
    overview: readonly(overview),
    instances: readonly(instances),
    selectedInstanceId: readonly(selectedInstanceId),
    detail: readonly(detail),
    loading: readonly(loading),
    detailLoading: readonly(detailLoading),

    // 计算属性
    hasOpenCircuitBreaker,
    healthLevel,

    // 方法
    fetchInstances,
    fetchOverview,
    selectInstance,
    fetchDetail,
    fetchHistory,
    reset,
    handleCircuitBreakerData,
  }
})
```

- [ ] **Step 2: 验证文件创建成功**

Run: `ls -la frontend/packages/gateway-admin/src/stores/circuitBreaker.ts`
Expected: 文件存在

---

### Task 3.3: 扩展 notification Store 支持熔断器 SSE 消息

**Files:**
- Modify: `frontend/packages/gateway-admin/src/stores/notification.ts`

- [ ] **Step 1: 新增熔断器 SSE 消息类型常量**

在 `SSE_MESSAGE_TYPE` 常量中添加：

```typescript
  const SSE_MESSAGE_TYPE = {
    HEARTBEAT: 'heartbeat',
    NOTIFICATION: 'notification',
    INSTANCE_STATUS: 'instance_status',
    DASHBOARD_DATA: 'dashboard_data',
    CIRCUIT_BREAKER_DATA: 'circuit_breaker_data',  // 新增
  }
```

- [ ] **Step 2: 新增熔断器回调注册机制**

在文件顶部，`instanceStatusCallbacks` 后添加：

```typescript
// 熔断器数据更新回调列表（用于通知 circuitBreaker store）
const circuitBreakerCallbacks: ((data: CircuitBreakerDataPayload) => void)[] = []

/**
 * 注册熔断器数据更新回调
 */
export function registerCircuitBreakerCallback(callback: (data: CircuitBreakerDataPayload) => void) {
  circuitBreakerCallbacks.push(callback)
}

/**
 * 移除熔断器数据更新回调
 */
export function unregisterCircuitBreakerCallback(callback: (data: CircuitBreakerDataPayload) => void) {
  const index = circuitBreakerCallbacks.indexOf(callback)
  if (index > -1) {
    circuitBreakerCallbacks.splice(index, 1)
  }
}
```

并添加类型导入：

```typescript
import type { CircuitBreakerDataPayload } from '@/stores/circuitBreaker'
```

- [ ] **Step 3: 在 handleSseMessage 中处理熔断器消息**

在 `handleSseMessage` 函数中，在 `DASHBOARD_DATA` 处理后添加：

```typescript
    // 熔断器数据推送：更新 circuitBreaker store
    if (sseType === SSE_MESSAGE_TYPE.CIRCUIT_BREAKER_DATA) {
      const cbData = rawMsg.data as CircuitBreakerDataPayload
      console.log('[Notification] 收到熔断器数据推送 | healthScore:', cbData.healthScore)

      // 通知所有注册的熔断器回调
      for (const callback of circuitBreakerCallbacks) {
        callback(cbData)
      }
      return
    }
```

- [ ] **Step 4: 验证修改**

Run: `grep -n "CIRCUIT_BREAKER_DATA\|circuitBreakerCallbacks\|registerCircuitBreakerCallback" frontend/packages/gateway-admin/src/stores/notification.ts`
Expected: 输出包含相关内容

---

### Task 3.4: 创建 useCircuitBreaker composable

**Files:**
- Create: `frontend/packages/gateway-admin/src/composables/useCircuitBreaker.ts`

- [ ] **Step 1: 创建 composable**

```typescript
/**
 * 熔断器数据管理 Composable
 *
 * 封装熔断器 Store 的常用操作，提供便捷的数据访问
 *
 * @example
 * ```ts
 * const { overview, instances, selectInstance, loading } = useCircuitBreaker()
 * ```
 */

import { onMounted, onUnmounted } from 'vue'
import { useCircuitBreakerStore } from '@/stores/circuitBreaker'

/**
 * 熔断器数据管理 Composable
 */
export function useCircuitBreaker() {
  const store = useCircuitBreakerStore()

  /**
   * 初始化数据
   */
  const init = async () => {
    await Promise.all([
      store.fetchInstances(),
      store.fetchOverview(),
    ])
  }

  /**
   * 刷新数据
   */
  const refresh = async () => {
    await store.fetchOverview(store.selectedInstanceId ?? undefined)
  }

  /**
   * 选择实例
   */
  const selectInstance = async (instanceId: string | null) => {
    await store.selectInstance(instanceId)
  }

  /**
   * 获取详情
   */
  const fetchDetail = async (name: string) => {
    await store.fetchDetail(name, store.selectedInstanceId ?? undefined)
  }

  /**
   * 获取历史
   */
  const fetchHistory = async (instanceId: string, name: string, limit?: number) => {
    return store.fetchHistory(instanceId, name, limit)
  }

  // 组件挂载时初始化
  onMounted(() => {
    if (!store.overview) {
      init()
    }
  })

  return {
    // 状态
    overview: store.overview,
    instances: store.instances,
    selectedInstanceId: store.selectedInstanceId,
    detail: store.detail,
    loading: store.loading,
    detailLoading: store.detailLoading,

    // 计算属性
    hasOpenCircuitBreaker: store.hasOpenCircuitBreaker,
    healthLevel: store.healthLevel,

    // 方法
    init,
    refresh,
    selectInstance,
    fetchDetail,
    fetchHistory,
    reset: store.reset,
  }
}

export default useCircuitBreaker
```

- [ ] **Step 2: 验证文件创建成功**

Run: `ls -la frontend/packages/gateway-admin/src/composables/useCircuitBreaker.ts`
Expected: 文件存在

---

### Task 3.5: 创建 InstancePanel 组件

**Files:**
- Create: `frontend/packages/gateway-admin/src/views/monitor/circuitBreaker/components/InstancePanel.vue`

- [ ] **Step 1: 创建组件**

```vue
<template>
  <div class="instance-panel">
    <div class="panel-header">
      <span class="title">{{ t('monitor.instanceList') }}</span>
    </div>

    <div class="instance-list">
      <!-- 聚合视图选项 -->
      <div
        class="instance-item aggregate"
        :class="{ active: !selectedInstanceId }"
        @click="selectInstance(null)"
      >
        <el-icon><DataLine /></el-icon>
        <span class="name">{{ t('monitor.aggregateView') }}</span>
        <el-tag v-if="overview" size="small" type="info">
          {{ overview.totalInstances }} {{ t('monitor.instances') }}
        </el-tag>
      </div>

      <el-divider />

      <!-- 实例列表 -->
      <div
        v-for="instance in instances"
        :key="instance.instanceId"
        class="instance-item"
        :class="{ active: selectedInstanceId === instance.instanceId }"
        @click="selectInstance(instance.instanceId)"
      >
        <div class="instance-info">
          <div class="instance-header">
            <span class="name">{{ instance.instanceId }}</span>
            <el-tag
              :type="instance.status === 'ONLINE' ? 'success' : 'danger'"
              size="small"
              effect="light"
            >
              {{ instance.status }}
            </el-tag>
          </div>
          <div class="instance-meta">
            <span>{{ instance.host }}:{{ instance.port }}</span>
          </div>
        </div>

        <div class="circuit-breaker-summary">
          <span
            v-if="instance.summary.open > 0"
            class="state-badge danger"
          >
            OPEN: {{ instance.summary.open }}
          </span>
          <span
            v-if="instance.summary.halfOpen > 0"
            class="state-badge warning"
          >
            HALF_OPEN: {{ instance.summary.halfOpen }}
          </span>
          <span class="state-badge success">
            CLOSED: {{ instance.summary.closed }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { DataLine } from '@element-plus/icons-vue'
import type { InstanceSummary, CircuitBreakerOverviewNew } from '@/api/circuitBreaker'

defineOptions({ name: 'InstancePanel' })

const props = defineProps<{
  instances: InstanceSummary[]
  selectedInstanceId: string | null
  overview: CircuitBreakerOverviewNew | null
}>()

const emit = defineEmits<{
  select: [instanceId: string | null]
}>()

const { t } = useI18n()

const selectInstance = (instanceId: string | null) => {
  emit('select', instanceId)
}
</script>

<style scoped lang="scss">
.instance-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--el-bg-color);
  border-radius: 8px;
  border: 1px solid var(--el-border-color-lighter);

  .panel-header {
    padding: 16px;
    border-bottom: 1px solid var(--el-border-color-lighter);

    .title {
      font-size: 14px;
      font-weight: 500;
    }
  }

  .instance-list {
    flex: 1;
    overflow-y: auto;
    padding: 8px;

    .el-divider {
      margin: 8px 0;
    }
  }

  .instance-item {
    padding: 12px;
    margin-bottom: 8px;
    border-radius: 6px;
    cursor: pointer;
    transition: all 0.2s;
    border: 1px solid transparent;

    &:hover {
      background: var(--el-fill-color-light);
    }

    &.active {
      background: var(--el-color-primary-light-9);
      border-color: var(--el-color-primary-light-5);
    }

    &.aggregate {
      display: flex;
      align-items: center;
      gap: 8px;

      .name {
        flex: 1;
        font-weight: 500;
      }
    }

    .instance-info {
      .instance-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 4px;

        .name {
          font-size: 13px;
          font-weight: 500;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }

      .instance-meta {
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }

    .circuit-breaker-summary {
      display: flex;
      gap: 8px;
      margin-top: 8px;
      flex-wrap: wrap;

      .state-badge {
        font-size: 11px;
        padding: 2px 6px;
        border-radius: 4px;

        &.success {
          background: var(--el-color-success-light-9);
          color: var(--el-color-success);
        }

        &.danger {
          background: var(--el-color-danger-light-9);
          color: var(--el-color-danger);
        }

        &.warning {
          background: var(--el-color-warning-light-9);
          color: var(--el-color-warning);
        }
      }
    }
  }
}
</style>
```

- [ ] **Step 2: 验证文件创建成功**

Run: `ls -la frontend/packages/gateway-admin/src/views/monitor/circuitBreaker/components/InstancePanel.vue`
Expected: 文件存在

---

### Task 3.6: 创建 SummaryCards 组件

**Files:**
- Create: `frontend/packages/gateway-admin/src/views/monitor/circuitBreaker/components/SummaryCards.vue`

- [ ] **Step 1: 创建组件**

```vue
<template>
  <el-row :gutter="16" class="summary-cards">
    <el-col :span="4">
      <el-card shadow="hover" class="summary-card">
        <div class="summary-item">
          <div class="summary-label">{{ t('monitor.totalCircuitBreakers') }}</div>
          <div class="summary-value">{{ overview?.totalCircuitBreakers || 0 }}</div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="4">
      <el-card shadow="hover" class="summary-card closed">
        <div class="summary-item">
          <div class="summary-label">{{ t('monitor.closedCount') }}</div>
          <div class="summary-value success">{{ overview?.closedCount || 0 }}</div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="4">
      <el-card shadow="hover" class="summary-card open">
        <div class="summary-item">
          <div class="summary-label">{{ t('monitor.openCount') }}</div>
          <div class="summary-value danger">{{ overview?.openCount || 0 }}</div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="4">
      <el-card shadow="hover" class="summary-card half-open">
        <div class="summary-item">
          <div class="summary-label">{{ t('monitor.halfOpenCount') }}</div>
          <div class="summary-value warning">{{ overview?.halfOpenCount || 0 }}</div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="4">
      <el-card shadow="hover" class="summary-card">
        <div class="summary-item">
          <div class="summary-label">{{ t('monitor.totalInstances') }}</div>
          <div class="summary-value">{{ overview?.totalInstances || 0 }}</div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="4">
      <el-card shadow="hover" class="summary-card health">
        <div class="summary-item">
          <div class="summary-label">{{ t('monitor.healthScore') }}</div>
          <div class="summary-value" :class="healthLevel">
            {{ (overview?.healthScore || 100).toFixed(1) }}%
          </div>
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { CircuitBreakerOverviewNew } from '@/api/circuitBreaker'

defineOptions({ name: 'SummaryCards' })

const props = defineProps<{
  overview: CircuitBreakerOverviewNew | null
}>()

const { t } = useI18n()

const healthLevel = computed(() => {
  const score = props.overview?.healthScore ?? 100
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
})
</script>

<style scoped lang="scss">
.summary-cards {
  margin-bottom: 16px;

  .summary-card {
    .summary-item {
      text-align: center;

      .summary-label {
        font-size: 12px;
        color: var(--el-text-color-secondary);
        margin-bottom: 8px;
      }

      .summary-value {
        font-size: 24px;
        font-weight: 600;

        &.success {
          color: var(--el-color-success);
        }
        &.danger {
          color: var(--el-color-danger);
        }
        &.warning {
          color: var(--el-color-warning);
        }
      }
    }
  }
}
</style>
```

- [ ] **Step 2: 验证文件创建成功**

Run: `ls -la frontend/packages/gateway-admin/src/views/monitor/circuitBreaker/components/SummaryCards.vue`
Expected: 文件存在

---

### Task 3.7: 创建 CircuitBreakerList 组件

**Files:**
- Create: `frontend/packages/gateway-admin/src/views/monitor/circuitBreaker/components/CircuitBreakerList.vue`

- [ ] **Step 1: 创建组件**

```vue
<template>
  <el-card shadow="never" class="circuit-breaker-list-card">
    <el-table
      :data="overview?.circuitBreakers || []"
      v-loading="loading"
      stripe
      @row-click="handleRowClick"
    >
      <el-table-column prop="name" :label="t('monitor.circuitBreakerName')" min-width="180">
        <template #default="{ row }">
          <span class="cb-name">{{ row.name }}</span>
          <el-tag
            v-if="row.baseConfig"
            size="small"
            type="info"
            effect="plain"
            class="base-config-tag"
          >
            {{ row.baseConfig }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        prop="failureRateThreshold"
        :label="t('monitor.failureRateThreshold')"
        width="100"
      >
        <template #default="{ row }">
          {{ row.failureRateThreshold }}%
        </template>
      </el-table-column>
      <el-table-column
        prop="slidingWindowSize"
        :label="t('monitor.slidingWindowSize')"
        width="100"
      />
      <el-table-column
        prop="minimumNumberOfCalls"
        :label="t('monitor.minimumNumberOfCalls')"
        width="100"
      />
      <el-table-column
        prop="waitDurationInOpenState"
        :label="t('monitor.waitDurationInOpenState')"
        width="120"
      >
        <template #default="{ row }">
          {{ row.waitDurationInOpenState }}s
        </template>
      </el-table-column>
      <el-table-column :label="t('monitor.stateDistribution')" min-width="200">
        <template #default="{ row }">
          <div class="state-distribution">
            <span class="state-item success">
              CLOSED: {{ row.closedCount || 0 }}
            </span>
            <span v-if="row.openCount > 0" class="state-item danger">
              OPEN: {{ row.openCount }}
            </span>
            <span v-if="row.halfOpenCount > 0" class="state-item warning">
              HALF_OPEN: {{ row.halfOpenCount }}
            </span>
          </div>
        </template>
      </el-table-column>
      <el-table-column :label="t('common.operation')" width="100">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="viewDetail(row)">
            {{ t('common.detail') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { CircuitBreakerOverviewNew, CircuitBreakerSummary } from '@/api/circuitBreaker'

defineOptions({ name: 'CircuitBreakerList' })

const props = defineProps<{
  overview: CircuitBreakerOverviewNew | null
  loading: boolean
}>()

const emit = defineEmits<{
  viewDetail: [cb: CircuitBreakerSummary]
}>()

const { t } = useI18n()

const viewDetail = (cb: CircuitBreakerSummary) => {
  emit('viewDetail', cb)
}

const handleRowClick = (row: CircuitBreakerSummary) => {
  viewDetail(row)
}
</script>

<style scoped lang="scss">
.circuit-breaker-list-card {
  .cb-name {
    font-weight: 500;
  }

  .base-config-tag {
    margin-left: 8px;
  }

  .state-distribution {
    display: flex;
    gap: 16px;

    .state-item {
      font-size: 13px;

      &.success {
        color: var(--el-color-success);
      }
      &.danger {
        color: var(--el-color-danger);
      }
      &.warning {
        color: var(--el-color-warning);
      }
    }
  }
}
</style>
```

- [ ] **Step 2: 验证文件创建成功**

Run: `ls -la frontend/packages/gateway-admin/src/views/monitor/circuitBreaker/components/CircuitBreakerList.vue`
Expected: 文件存在

---

### Task 3.8: 创建 StateHistory 组件

**Files:**
- Create: `frontend/packages/gateway-admin/src/views/monitor/circuitBreaker/components/StateHistory.vue`

- [ ] **Step 1: 创建组件**

```vue
<template>
  <div class="state-history">
    <div class="history-header">
      <span class="title">{{ t('monitor.stateTransitionHistory') }}</span>
      <span class="subtitle">{{ cbName }}</span>
    </div>

    <div v-if="loading" class="history-loading">
      <el-skeleton :rows="3" animated />
    </div>

    <div v-else-if="history.length === 0" class="history-empty">
      <el-empty :description="t('monitor.noHistory')" :image-size="60" />
    </div>

    <div v-else class="history-timeline">
      <el-timeline>
        <el-timeline-item
          v-for="(item, index) in history"
          :key="index"
          :type="getTimelineType(item.toState)"
          :timestamp="formatTime(item.timestamp)"
          placement="top"
        >
          <div class="timeline-content">
            <div class="transition-info">
              <el-tag :type="getStateType(item.fromState)" size="small">
                {{ item.fromState }}
              </el-tag>
              <el-icon><Right /></el-icon>
              <el-tag :type="getStateType(item.toState)" size="small">
                {{ item.toState }}
              </el-tag>
            </div>
            <div class="transition-meta">
              <span>{{ t('monitor.reason') }}: {{ item.reason }}</span>
              <span v-if="item.failureRate !== undefined">
                {{ t('monitor.failureRate') }}: {{ item.failureRate.toFixed(1) }}%
              </span>
              <span v-if="item.numberOfCalls !== undefined">
                {{ t('monitor.numberOfCalls') }}: {{ item.numberOfCalls }}
              </span>
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Right } from '@element-plus/icons-vue'
import type { StateTransitionHistory } from '@/api/circuitBreaker'
import { circuitBreakerApi } from '@/api/circuitBreaker'

defineOptions({ name: 'StateHistory' })

const props = defineProps<{
  instanceId: string
  cbName: string
}>()

const { t } = useI18n()

const loading = ref(false)
const history = ref<StateTransitionHistory[]>([])

const fetchHistory = async () => {
  if (!props.instanceId || !props.cbName) return

  loading.value = true
  try {
    history.value = await circuitBreakerApi.getHistory(props.instanceId, props.cbName, 20)
  } catch (error) {
    console.error('[StateHistory] 获取历史失败:', error)
  } finally {
    loading.value = false
  }
}

const getStateType = (state: string): 'success' | 'danger' | 'warning' | 'info' => {
  switch (state) {
    case 'CLOSED':
      return 'success'
    case 'OPEN':
      return 'danger'
    case 'HALF_OPEN':
      return 'warning'
    default:
      return 'info'
  }
}

const getTimelineType = (toState: string): 'success' | 'danger' | 'warning' | 'primary' => {
  switch (toState) {
    case 'CLOSED':
      return 'success'
    case 'OPEN':
      return 'danger'
    case 'HALF_OPEN':
      return 'warning'
    default:
      return 'primary'
  }
}

const formatTime = (timestamp: number): string => {
  return new Date(timestamp).toLocaleString()
}

watch(() => [props.instanceId, props.cbName], fetchHistory, { immediate: true })
onMounted(fetchHistory)
</script>

<style scoped lang="scss">
.state-history {
  .history-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;

    .title {
      font-size: 14px;
      font-weight: 500;
    }

    .subtitle {
      font-size: 13px;
      color: var(--el-text-color-secondary);
    }
  }

  .history-loading,
  .history-empty {
    padding: 16px;
  }

  .history-timeline {
    .timeline-content {
      .transition-info {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 8px;
      }

      .transition-meta {
        display: flex;
        gap: 16px;
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }
  }
}
</style>
```

- [ ] **Step 2: 验证文件创建成功**

Run: `ls -la frontend/packages/gateway-admin/src/views/monitor/circuitBreaker/components/StateHistory.vue`
Expected: 文件存在

---

### Task 3.9: 重构主页面 index.vue

**Files:**
- Modify: `frontend/packages/gateway-admin/src/views/monitor/circuitBreaker/index.vue`

- [ ] **Step 1: 重构主页面**

```vue
<template>
  <div class="circuit-breaker-page">
    <!-- Header -->
    <div class="page-header">
      <h3>{{ t('monitor.circuitBreaker') }}</h3>
      <div class="header-actions">
        <el-button @click="refresh" :loading="loading">
          <el-icon><Refresh /></el-icon>
          {{ t('common.refresh') }}
        </el-button>
      </div>
    </div>

    <!-- Main Content - Left Right Split -->
    <div class="page-content">
      <!-- Left Panel - Instance List -->
      <div class="left-panel">
        <InstancePanel
          :instances="instances"
          :selected-instance-id="selectedInstanceId"
          :overview="overview"
          @select="selectInstance"
        />
      </div>

      <!-- Right Panel - Details -->
      <div class="right-panel">
        <!-- Summary Cards -->
        <SummaryCards :overview="overview" />

        <!-- Circuit Breaker List -->
        <CircuitBreakerList
          :overview="overview"
          :loading="loading"
          @view-detail="viewDetail"
        />
      </div>
    </div>

    <!-- Detail Dialog -->
    <el-dialog
      v-model="detailVisible"
      :title="t('monitor.circuitBreakerDetail')"
      width="800px"
      :close-on-click-modal="false"
      :lock-scroll="false"
    >
      <div v-if="selectedCb" class="detail-content">
        <!-- Config Section -->
        <div class="detail-section">
          <h4>{{ t('monitor.configParams') }}</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item :label="t('monitor.circuitBreakerName')">
              {{ selectedCb.name }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('monitor.baseConfig')">
              {{ selectedCb.baseConfig || '-' }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('monitor.failureRateThreshold')">
              {{ selectedCb.failureRateThreshold }}%
            </el-descriptions-item>
            <el-descriptions-item :label="t('monitor.slidingWindowSize')">
              {{ selectedCb.slidingWindowSize }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('monitor.minimumNumberOfCalls')">
              {{ selectedCb.minimumNumberOfCalls }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('monitor.waitDurationInOpenState')">
              {{ selectedCb.waitDurationInOpenState }}s
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- Instance Status Section -->
        <div v-if="detail?.instances?.length" class="detail-section">
          <h4>{{ t('monitor.instanceStatus') }}</h4>
          <el-table :data="detail.instances" stripe size="small">
            <el-table-column prop="instanceId" :label="t('common.instanceId')" min-width="200" />
            <el-table-column prop="state" :label="t('monitor.state')" width="120">
              <template #default="{ row }">
                <el-tag :type="getStateType(row.state)" size="small">
                  {{ row.state }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="failureRate" :label="t('monitor.failureRate')" width="100">
              <template #default="{ row }">
                {{ row.failureRate?.toFixed(2) || 0 }}%
              </template>
            </el-table-column>
            <el-table-column prop="numberOfCalls" :label="t('monitor.numberOfCalls')" width="100" />
            <el-table-column prop="numberOfFailedCalls" :label="t('monitor.numberOfFailedCalls')" width="100" />
          </el-table>
        </div>

        <!-- State History Section -->
        <div v-if="selectedInstanceId" class="detail-section">
          <StateHistory
            :instance-id="selectedInstanceId"
            :cb-name="selectedCb.name"
          />
        </div>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">{{ t('common.close') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Refresh } from '@element-plus/icons-vue'
import { useCircuitBreaker } from '@/composables/useCircuitBreaker'
import type { CircuitBreakerSummary } from '@/api/circuitBreaker'
import InstancePanel from './components/InstancePanel.vue'
import SummaryCards from './components/SummaryCards.vue'
import CircuitBreakerList from './components/CircuitBreakerList.vue'
import StateHistory from './components/StateHistory.vue'

defineOptions({ name: 'CircuitBreakerMonitor' })

const { t } = useI18n()

const {
  overview,
  instances,
  selectedInstanceId,
  detail,
  loading,
  refresh,
  selectInstance,
  fetchDetail,
} = useCircuitBreaker()

const detailVisible = ref(false)
const selectedCb = ref<CircuitBreakerSummary | null>(null)

const viewDetail = async (cb: CircuitBreakerSummary) => {
  selectedCb.value = cb
  detailVisible.value = true
  await fetchDetail(cb.name)
}

const getStateType = (state: string): 'success' | 'danger' | 'warning' | 'info' => {
  switch (state) {
    case 'CLOSED':
      return 'success'
    case 'OPEN':
      return 'danger'
    case 'HALF_OPEN':
      return 'warning'
    default:
      return 'info'
  }
}

onMounted(() => {
  // 初始化数据由 useCircuitBreaker composable 自动处理
})
</script>

<style scoped lang="scss">
.circuit-breaker-page {
  height: 100%;
  display: flex;
  flex-direction: column;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    padding: 0 4px;

    h3 {
      font-size: 20px;
      font-weight: 600;
      margin: 0;
    }
  }

  .page-content {
    flex: 1;
    display: flex;
    gap: 16px;
    overflow: hidden;

    .left-panel {
      width: 280px;
      flex-shrink: 0;
    }

    .right-panel {
      flex: 1;
      overflow-y: auto;
      display: flex;
      flex-direction: column;
    }
  }

  .detail-content {
    .detail-section {
      margin-bottom: 24px;

      h4 {
        font-size: 14px;
        font-weight: 500;
        margin-bottom: 12px;
      }
    }
  }
}
</style>
```

- [ ] **Step 2: 验证修改**

Run: `grep -n "InstancePanel\|useCircuitBreaker\|selectInstance" frontend/packages/gateway-admin/src/views/monitor/circuitBreaker/index.vue`
Expected: 输出包含相关内容

---

### Task 3.10: 添加国际化文本

**Files:**
- Modify: `frontend/packages/gateway-admin/src/locales/zh-CN/monitor.ts`
- Modify: `frontend/packages/gateway-admin/src/locales/en-US/monitor.ts`

- [ ] **Step 1: 添加中文文本**

在 `monitor.ts` 中添加：

```typescript
export default {
  // ... 现有内容 ...
  instanceList: '实例列表',
  aggregateView: '聚合视图',
  instances: '实例',
  healthScore: '健康度',
  stateTransitionHistory: '状态转换历史',
  noHistory: '暂无状态转换历史',
  reason: '原因',
}
```

- [ ] **Step 2: 添加英文文本**

在 `monitor.ts` 中添加：

```typescript
export default {
  // ... existing content ...
  instanceList: 'Instance List',
  aggregateView: 'Aggregate View',
  instances: 'instances',
  healthScore: 'Health Score',
  stateTransitionHistory: 'State Transition History',
  noHistory: 'No state transition history',
  reason: 'Reason',
}
```

- [ ] **Step 3: 验证修改**

Run: `grep -n "instanceList\|aggregateView\|healthScore" frontend/packages/gateway-admin/src/locales/zh-CN/monitor.ts frontend/packages/gateway-admin/src/locales/en-US/monitor.ts`
Expected: 输出包含相关内容

---

### Task 3.11: 验证前端构建

**Files:**
- Build: `frontend/packages/gateway-admin`

- [ ] **Step 1: 安装依赖并检查类型**

Run: `cd frontend/packages/gateway-admin && npm run type-check`
Expected: 无类型错误

- [ ] **Step 2: 构建项目**

Run: `cd frontend/packages/gateway-admin && npm run build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交 Phase 3 代码**

```bash
git add frontend/packages/gateway-admin/src/api/circuitBreaker.ts
git add frontend/packages/gateway-admin/src/stores/circuitBreaker.ts
git add frontend/packages/gateway-admin/src/stores/notification.ts
git add frontend/packages/gateway-admin/src/composables/useCircuitBreaker.ts
git add frontend/packages/gateway-admin/src/views/monitor/circuitBreaker/
git add frontend/packages/gateway-admin/src/locales/
git commit -m "feat(circuit-breaker): Phase 3 - 前端重构实现

- 扩展 circuitBreaker.ts API 定义
- 新增 CircuitBreaker Pinia Store
- 扩展 notification Store 支持 SSE 熔断器消息
- 新增 useCircuitBreaker composable
- 新增 InstancePanel/SummaryCards/CircuitBreakerList/StateHistory 组件
- 重构主页面为左右分栏布局

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Phase 4: 测试和文档

### Task 4.1: 集成测试

**Files:**
- Test: 启动 gateway-reactive 和 gateway-admin

- [ ] **Step 1: 启动 Redis 服务**

确认 Redis 服务正常运行。

- [ ] **Step 2: 启动 gateway-reactive**

Run: `./gradlew :blink-gateway:blink-gateway-reactive:bootRun`
Expected: 服务启动成功，日志显示熔断器指标采集

- [ ] **Step 3: 启动 gateway-admin**

Run: `./gradlew :blink-gateway:gateway-admin:bootRun`
Expected: 服务启动成功

- [ ] **Step 4: 启动前端**

Run: `cd frontend/packages/gateway-admin && npm run dev`
Expected: 前端启动成功

- [ ] **Step 5: 验证数据流**

1. 打开浏览器访问熔断器监控页面
2. 验证左侧实例列表显示正常
3. 点击聚合视图，验证右侧显示所有实例的熔断器状态
4. 点击单个实例，验证右侧切换为单实例视图
5. 点击详情按钮，验证弹窗显示配置和实例状态

---

### Task 4.2: 更新设计文档

**Files:**
- Modify: `docs/superpowers/specs/2026-04-16-circuit-breaker-multi-instance-design.md`

- [ ] **Step 1: 添加实施完成标记**

在设计文档末尾添加实施状态：

```markdown
## 11. 实施状态

### 已完成

- [x] Phase 1: 后端数据采集
- [x] Phase 2: 后端 API 重构
- [x] Phase 3: 前端重构
- [ ] Phase 4: 测试和文档

### 测试清单

- [ ] 多实例熔断器指标采集正常
- [ ] Redis 存储和读取正常
- [ ] SSE 实时推送正常
- [ ] 前端实例切换正常
- [ ] 状态转换历史记录正常
```

- [ ] **Step 2: 提交文档更新**

```bash
git add docs/superpowers/specs/2026-04-16-circuit-breaker-multi-instance-design.md
git commit -m "docs(circuit-breaker): 更新实施状态

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## 总结

本实施计划将熔断器监控多实例切换重构分为 4 个阶段：

1. **Phase 1: 后端数据采集** - 扩展现有 Redis Stream 架构采集熔断器真实指标
2. **Phase 2: 后端 API 重构** - 新增服务层和 API 从 Redis 读取真实数据
3. **Phase 3: 前端重构** - 左右分栏布局，Pinia Store + SSE 实时更新
4. **Phase 4: 测试和文档** - 集成测试和文档更新

每个任务都有明确的文件路径和完整代码，便于按步骤执行。
