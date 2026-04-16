# 实例路由获取方案设计

> 设计日期：2026-04-16
> 状态：已确认
> 关联问题：路由管理模块审查报告 P0-1.1 实例路由概念错误

---

## 一、背景与问题

### 1.1 问题描述

当前"实例路由"页面的 `getInstanceRoutes` 接口存在概念错误：

- 前端传入 `instanceId` 参数，但后端完全忽略该参数
- 后端从 Nacos/Redis 配置中心获取路由，与具体实例无关
- 用户选择不同实例，看到的是相同的路由列表
- 无法获取实例内存中实际加载的路由配置

### 1.2 问题影响

- 无法排查单个实例的路由加载问题
- 无法验证路由推送后实例是否正确加载
- 无法发现配置中心与实例实际路由的差异

---

## 二、方案选择

### 2.1 候选方案

| 方案 | 描述 | 优点 | 缺点 |
|------|------|------|------|
| A. Dubbo RPC | 通过 Dubbo 从网关实例获取路由 | 性能好 | 需要网关暴露 Dubbo 服务 |
| **B. HTTP Actuator** | 通过 Actuator 端点获取路由 | 简单通用 | 需要网络调用 |
| C. 重命名接口 | 重命名为 `getConfigCenterRoutes`，新增实例路由接口 | 向后兼容 | 接口语义变化 |

### 2.2 确定方案

采用 **方案B：HTTP Actuator 端点**，同时保留原接口用于对比。

**接口策略：**
- 新增 `getInstanceRoutesFromActuator` 接口 - 从实例获取真实路由
- 保留 `getInstanceRoutes` 接口 - 从配置中心获取（可用于对比）

---

## 三、架构设计

### 3.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                         前端 (Vue)                              │
│  实例路由页面                                                      │
│  ┌─────────────┐    ┌─────────────────────────────────────────┐ │
│  │ 实例列表     │───→│ 当前路由（从实例获取）                    │ │
│  │ - Instance A│    │ ┌─────────────────────────────────────┐ │ │
│  │ - Instance B│    │ │ Route 1: Path=/api/user/** → lb://  │ │ │
│  │ - Instance C│    │ │ Route 2: Path=/api/order/** → lb://  │ │ │
│  └─────────────┘    │ └─────────────────────────────────────┘ │ │
│                     └─────────────────────────────────────────┘ │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                    POST /route/getInstanceRoutesFromActuator
                               │
┌──────────────────────────────▼──────────────────────────────────┐
│                    gateway-admin 后端                            │
│  RouteController                                                 │
│  └── RoutePushService.getInstanceRoutesFromActuator()           │
│         │                                                        │
│         │ HTTP GET (WebClient)                                   │
│         ▼                                                        │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ http://{instanceHost}:{port}/actuator/gateway-routes      │   │
│  │ Authorization: Basic base64(admin:123456)                 │   │
│  └──────────────────────────────────────────────────────────┘   │
└──────────────────────────────┬──────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────┐
│                 gateway-reactive 网关实例                        │
│  GatewayRoutesEndpoint (新增)                                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ @Endpoint(id = "gateway-routes")                          │   │
│  │ @ReadOperation                                            │   │
│  │ public GatewayRoutesResponse getRoutes()                  │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  RouteDefinitionRepository                                       │
│  ├── NacosRouteDefinitionRepository (Nacos 模式)                 │
│  └── RedisRouteDefinitionRepository (Redis 模式)                 │
└──────────────────────────────────────────────────────────────────┘
```

### 3.2 数据流

1. 前端选择实例 → 调用 `getInstanceRoutesFromActuator(instanceId)`
2. gateway-admin 解析实例ID，获取 host:port
3. gateway-admin 通过 WebClient 调用实例的 Actuator 端点
4. 网关实例从 RouteDefinitionRepository 获取路由并返回
5. gateway-admin 将结果返回给前端

---

## 四、详细设计

### 4.1 网关端 Actuator 端点

#### 4.1.1 新增文件

**文件路径**: `blink-gateway/blink-gateway-reactive/src/main/java/com/blink/gateway/endpoint/GatewayRoutesEndpoint.java`

#### 4.1.2 类设计

```java
package com.blink.gateway.endpoint;

import com.blink.framework.common.utils.JacksonUtil;
import com.blink.gateway.config.prop.BlinkGatewayProperties;
import jakarta.annotation.Resource;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 网关路由 Actuator 端点
 * 用于获取当前实例内存中加载的路由定义
 *
 * @author binblink
 * @since 2026-04-16
 */
@Endpoint(id = "gateway-routes")
@Component
@Slf4j
public class GatewayRoutesEndpoint {

    @Resource
    private RouteDefinitionRepository routeDefinitionRepository;

    @Value("${server.port}")
    private String serverPort;

    /**
     * 获取当前实例的所有路由定义
     *
     * @return 路由列表响应
     */
    @ReadOperation
    public GatewayRoutesResponse getRoutes() {
        String instanceId = getInstanceId();

        try {
            List<RouteDefinition> routes = routeDefinitionRepository
                .getRouteDefinitions()
                .collectList()
                .block();

            GatewayRoutesResponse response = GatewayRoutesResponse.builder()
                .instanceId(instanceId)
                .timestamp(LocalDateTime.now())
                .routes(routes != null ? routes : new ArrayList<>())
                .routeCount(routes != null ? routes.size() : 0)
                .build();

            log.info("[GatewayRoutesEndpoint] 获取实例路由 | instanceId: {}, count: {}",
                instanceId, response.getRouteCount());

            return response;
        } catch (Exception e) {
            log.error("[GatewayRoutesEndpoint] 获取路由失败 | instanceId: {}, error: {}",
                instanceId, e.getMessage(), e);

            return GatewayRoutesResponse.builder()
                .instanceId(instanceId)
                .timestamp(LocalDateTime.now())
                .routes(new ArrayList<>())
                .routeCount(0)
                .error(e.getMessage())
                .build();
        }
    }

    /**
     * 获取实例 ID
     */
    private String getInstanceId() {
        try {
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            return "gateway-app:" + hostAddress + ":" + serverPort;
        } catch (Exception e) {
            return "gateway-app:unknown:" + serverPort;
        }
    }

    /**
     * 网关路由响应
     */
    @Data
    @Builder
    public static class GatewayRoutesResponse {
        private String instanceId;
        private LocalDateTime timestamp;
        private List<RouteDefinition> routes;
        private Integer routeCount;
        private String error;
    }
}
```

#### 4.1.3 响应格式

```json
{
  "instanceId": "gateway-app:10.141.92.120:8002",
  "timestamp": "2026-04-16T10:30:00",
  "routes": [
    {
      "id": "user-service-route",
      "uri": "lb://user-service",
      "order": 0,
      "predicates": [
        {
          "name": "Path",
          "args": {"pattern": "/api/user/**"}
        }
      ],
      "filters": [
        {
          "name": "StripPrefix",
          "args": {"parts": "1"}
        }
      ],
      "metadata": {}
    }
  ],
  "routeCount": 1
}
```

### 4.2 管理端接口

#### 4.2.1 请求 DTO

**文件路径**: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/GetInstanceRoutesFromActuatorReq.java`

```java
package com.blink.gateway.admin.dto.req;

import lombok.Data;

/**
 * 从实例获取路由请求
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class GetInstanceRoutesFromActuatorReq {

    /**
     * 实例ID，格式：gateway-app:host:port
     */
    private String instanceId;
}
```

#### 4.2.2 响应 DTO

**文件路径**: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/InstanceRoutesRsp.java`

```java
package com.blink.gateway.admin.dto.rsp;

import com.blink.gateway.admin.entity.GaRouteDO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 实例路由响应
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class InstanceRoutesRsp {

    /**
     * 实例ID
     */
    private String instanceId;

    /**
     * 获取时间
     */
    private LocalDateTime timestamp;

    /**
     * 路由列表
     */
    private List<GaRouteDO> rows;

    /**
     * 路由数量
     */
    private Integer total;

    /**
     * 是否从 Actuator 获取
     * true - 从实例获取
     * false - 从配置中心获取
     */
    private Boolean fromActuator;

    /**
     * 错误信息（获取失败时）
     */
    private String error;
}
```

#### 4.2.3 Service 接口

**文件路径**: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/RoutePushService.java`

新增方法：

```java
/**
 * 从网关实例获取实际加载的路由
 *
 * @param req 请求参数
 * @return 实例路由响应
 */
ResponseDTO<InstanceRoutesRsp> getInstanceRoutesFromActuator(GetInstanceRoutesFromActuatorReq req);
```

#### 4.2.4 Service 实现

**文件路径**: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/RoutePushServiceImpl.java`

新增实现：

```java
@Override
public ResponseDTO<InstanceRoutesRsp> getInstanceRoutesFromActuator(GetInstanceRoutesFromActuatorReq req) {
    // 参数校验
    if (StrUtil.isBlank(req.getInstanceId())) {
        BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
    }

    InstanceRoutesRsp rsp = new InstanceRoutesRsp();
    rsp.setInstanceId(req.getInstanceId());
    rsp.setFromActuator(true);

    // 解析实例ID获取 host:port
    String[] parts = req.getInstanceId().split(":");
    if (parts.length < 3) {
        log.warn("[RoutePush] 实例ID格式错误 | instanceId: {}", req.getInstanceId());
        rsp.setError("实例ID格式错误");
        rsp.setRows(new ArrayList<>());
        rsp.setTotal(0);
        return ResponseDTO.newSuccessInstance(rsp);
    }

    String host = parts[1];
    String port = parts[2];

    // 调用网关实例 Actuator 端点
    try {
        String actuatorUrl = String.format("http://%s:%s/actuator/gateway-routes", host, port);

        // 使用 WebClient 调用（带 Basic 认证）
        String response = webClient.get()
            .uri(actuatorUrl)
            .headers(headers -> headers.setBasicAuth("admin", "123456"))
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(5))
            .block();

        // 解析响应
        if (StrUtil.isNotBlank(response)) {
            Map<String, Object> responseMap = JacksonUtil.fromJson(response,
                new TypeReference<Map<String, Object>>() {});

            List<Map<String, Object>> routesList = (List<Map<String, Object>>) responseMap.get("routes");
            List<GaRouteDO> routes = convertToGaRouteDOList(routesList);

            rsp.setRows(routes);
            rsp.setTotal(routes.size());
            rsp.setTimestamp(LocalDateTime.now());
        }

        log.info("[RoutePush] 从实例获取路由成功 | instanceId: {}, count: {}",
            req.getInstanceId(), rsp.getTotal());

    } catch (Exception e) {
        log.error("[RoutePush] 从实例获取路由失败 | instanceId: {}, error: {}",
            req.getInstanceId(), e.getMessage(), e);
        rsp.setError("获取失败：" + e.getMessage());
        rsp.setRows(new ArrayList<>());
        rsp.setTotal(0);
    }

    return ResponseDTO.newSuccessInstance(rsp);
}
```

#### 4.2.5 Controller 接口

**文件路径**: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/controller/RouteController.java`

新增接口：

```java
@PostMapping("/getInstanceRoutesFromActuator")
@ApiOperation("从实例获取实际路由")
public ResponseDTO<InstanceRoutesRsp> getInstanceRoutesFromActuator(
    @RequestBody @Validated RequestDTO<GetInstanceRoutesFromActuatorReq> reqDto) {
    return routePushService.getInstanceRoutesFromActuator(reqDto.getBody());
}
```

### 4.3 前端调整

#### 4.3.1 API 文件

**文件路径**: `frontend/packages/gateway-admin/src/api/route.ts`

新增接口：

```typescript
/**
 * 从实例获取路由请求
 */
export interface GetInstanceRoutesFromActuatorReq {
  instanceId: string
}

/**
 * 从网关实例获取实际加载的路由
 */
export const getInstanceRoutesFromActuator = (
  params: GetInstanceRoutesFromActuatorReq,
): Promise<InstanceRoutesRsp> => {
  return request.post('/route/getInstanceRoutesFromActuator', { body: params })
}

/**
 * 实例路由响应
 */
export interface InstanceRoutesRsp {
  instanceId: string
  timestamp: string
  rows: RouteDefinition[]
  total: number
  fromActuator: boolean
  error?: string
}
```

#### 4.3.2 页面组件

**文件路径**: `frontend/packages/gateway-admin/src/views/instanceRoute/index.vue`

修改 `loadInstanceRoutes` 方法：

```typescript
// 加载实例路由
async function loadInstanceRoutes() {
  if (!selectedInstance.value) {
    instanceRoutes.value = []
    return
  }
  routesLoading.value = true
  try {
    const result = await routeApi.getInstanceRoutesFromActuator({
      instanceId: selectedInstance.value.instanceId,
    })
    if (result.error) {
      ElMessage.warning(result.error)
      instanceRoutes.value = []
    } else {
      instanceRoutes.value = result.rows || []
    }
  } catch (error) {
    ElMessage.error(t('message.fetchFailed'))
    instanceRoutes.value = []
  } finally {
    routesLoading.value = false
  }
}
```

---

## 五、安全设计

### 5.1 Actuator 认证

网关 Actuator 端点已配置 HTTP Basic 认证（见 `SecurityConfig.java`）：

```java
@Bean("actuatorWebFilterChain")
@Order(1)
public SecurityWebFilterChain actuatorWebFilterChain(ServerHttpSecurity http) {
    return http
        .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/actuator/**"))
        .authorizeExchange(exchanges -> exchanges
            .pathMatchers("/actuator/health", "/actuator/info").permitAll()
            .anyExchange().authenticated()
        )
        .httpBasic(withDefaults())
        .build();
}
```

### 5.2 认证凭据

管理端调用网关 Actuator 时使用固定的 Basic 认证：

- 用户名: `admin`
- 密码: `123456`

**后续优化建议**：将认证凭据移至配置文件，支持动态配置。

---

## 六、错误处理

### 6.1 错误场景

| 场景 | HTTP 状态码 | 错误信息 | 处理方式 |
|------|------------|---------|---------|
| 实例离线 | - | 连接超时/拒绝 | 返回 `error` 字段提示实例离线 |
| 网络超时 | - | 5秒超时 | 返回 `error` 字段提示获取超时 |
| 认证失败 | 401 | 认证失败 | 返回 `error` 字段提示认证失败 |
| 实例无路由 | 200 | - | 返回空列表 `rows: []` |
| 实例ID格式错误 | - | 格式错误 | 返回 `error` 字段提示格式错误 |

### 6.2 前端错误展示

- 获取失败时显示错误提示
- 实例离线时禁用"刷新路由"按钮或显示离线状态

---

## 七、实施步骤

### 7.1 开发顺序

1. **网关端**：新增 `GatewayRoutesEndpoint` Actuator 端点
2. **管理端 DTO**：新增请求/响应 DTO
3. **管理端 Service**：实现 `getInstanceRoutesFromActuator` 方法
4. **管理端 Controller**：新增 API 接口
5. **前端 API**：新增接口调用方法
6. **前端页面**：修改 `loadInstanceRoutes` 方法

### 7.2 测试验证

1. 单元测试：测试 Actuator 端点返回正确格式
2. 集成测试：测试管理端调用网关实例
3. 手动测试：
   - 选择在线实例，验证路由列表正确显示
   - 选择离线实例，验证错误提示正确
   - 对比配置中心路由与实例路由

---

## 八、后续优化

1. **认证凭据配置化**：将 Actuator 认证凭据移至配置文件
2. **路由对比功能**：前端实现配置中心路由与实例路由的对比视图
3. **缓存机制**：对频繁查询的实例路由增加短期缓存

---

*文档版本：v1.0*
*最后更新：2026-04-16*
