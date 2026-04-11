# Gateway Admin 实例管理页面设计文档

## 概述

为 gateway-admin 添加独立的实例管理功能，支持：
- 实例配置管理（数据库 CRUD）
- 实例上下线操作
- 健康检测详情
- JVM 参数监控（内存、GC、线程）
- HTTP 请求数统计

---

## 1. 功能需求

### 1.1 实例配置管理

| 功能 | 描述 |
|------|------|
| 列表查看 | 分页展示数据库配置的实例列表 |
| 新增实例 | 配置 serviceId、host、port、metadata |
| 编辑实例 | 修改实例配置信息 |
| 删除实例 | 从数据库删除实例配置 |

### 1.2 实例生命周期管理

| 功能 | 描述 |
|------|------|
| 上线操作 | 标记实例为在线状态，允许接收请求 |
| 下线操作 | 标记实例为下线状态，停止接收请求 |

### 1.3 监控功能

| 功能 | 描述 |
|------|------|
| 健康检测 | 调用 /actuator/health 端点展示健康状态详情 |
| JVM 内存 | 堆内存使用、非堆内存、内存使用率 |
| GC 统计 | GC 次数、GC 时间、各代 GC 统计 |
| 线程信息 | 活跃线程数、峰值线程数、守护线程数 |
| HTTP 统计 | 请求数、成功/失败数、成功率、响应时间 |

---

## 2. 前端设计

### 2.1 页面布局

```
┌─────────────────────────────────────────────────────────────┐
│ 实例管理                                                     │
├─────────────────────────────────────────────────────────────┤
│ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ │
│ │ 总实例: 10    │ │ 在线: 8      │ │ 健康: 7      │ │ CPU: 45%    │ │
│ └──────────────┘ └──────────────┘ ┌──────────────┘ └──────────────┘ │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ [serviceId▼] [搜索] [重置]          [新增实例] [刷新]    │ │
│ │                                                         │ │
│ │ ┌─────────────────────────────────────────────────────┐ │ │
│ │ │ 实例ID │ 服务ID │ 主机 │ 端口 │ 状态 │ 健康 │ 操作   │ │ │
│ │ ├─────────────────────────────────────────────────────┤ │ │
│ │ │ inst-01 │ gateway │ 192.168.1.10 │ 8080 │ 在线 │ ✓ │ 详情│下线│编辑│删除│ │ │
│ │ │ inst-02 │ gateway │ 192.168.1.11 │ 8080 │ 下线 │ ✗ │ 详情│上线│编辑│删除│ │ │
│ │ └─────────────────────────────────────────────────────┘ │ │
│ │                                                         │ │
│ │ 分页: 共 10 条                                           │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 详情弹窗布局

```
┌─────────────────────────────────────────────────────────────┐
│ 实例详情 - gateway-reactive-01                         [×] │
├─────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 基本信息                                                 │ │
│ │ ┌─────────────────────┐ ┌─────────────────────────────┐ │ │
│ │ │ 实例ID: inst-01      │ │ 服务ID: gateway-app         │ │ │
│ │ │ 主机: 192.168.1.10   │ │ 端口: 8080                  │ │ │
│ │ │ URI: http://...      │ │ 状态: 在线                  │ │ │
│ │ │ 上线时间: 2026-04-11  │ │ 元数据: {...}               │ │ │
│ │ └─────────────────────┘ └─────────────────────────────┘ │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 健康状态                                                 │ │
│ │ ┌─────────────────────┐ ┌─────────────────────────────┐ │ │
│ │ │ 状态: UP             │ │ 详情:                       │ │ │
│ │ │                      │ │ diskSpace: UP               │ │ │
│ │ │                      │ │ redis: UP                   │ │ │
│ │ │                      │ │ db: UP                      │ │ │
│ │ └─────────────────────┘ └─────────────────────────────┘ │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ JVM 监控                                                 │ │
│ │ ┌─────────────────────────────────────────────────────┐ │ │
│ │ │ 堆内存使用: 256MB / 512MB (50%)                     │ │ │
│ │ │ 非堆内存: 64MB                                       │ │ │
│ │ │ GC 次数: Young GC 120次, Full GC 2次                │ │ │
│ │ │ GC 时间: Young 800ms, Full 200ms                    │ │ │
│ │ │ 线程数: 活跃 50, 峰值 80, 守护 25                    │ │ │
│ │ └─────────────────────────────────────────────────────┘ │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ HTTP 请求统计                                            │ │
│ │ ┌─────────────────────────────────────────────────────┐ │ │
│ │ │ 总请求: 10,000  成功: 9,800  失败: 200              │ │ │
│ │ │ 成功率: 98%                                         │ │ │
│ │ │ 平均响应时间: 45ms                                   │ │ │
│ │ └─────────────────────────────────────────────────────┘ │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│                                    [关闭]                   │
└─────────────────────────────────────────────────────────────┘
```

### 2.3 新增/编辑实例弹窗

```
┌─────────────────────────────────────────────────────────────┐
│ 新增实例                                              [×] │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ 服务ID *    [gateway-app        ▼]                         │
│                                                             │
│ 主机IP *    [192.168.1.10           ]                       │
│                                                             │
│ 端口 *      [8080                  ]                       │
│                                                             │
│ 元数据      [{ "weight": 100 }    ]                       │
│             (JSON 格式，可选)                              │
│                                                             │
│                                    [取消] [保存]           │
└─────────────────────────────────────────────────────────────┘
```

### 2.4 路由配置

新增路由：

```typescript
{
  path: 'instance',
  name: 'Instance',
  component: () => import('@/views/instance/index.vue'),
  meta: { title: 'instance.title' },
}
```

### 2.5 API 模块

**`/api/instance.ts`**：

```typescript
// 查询实例列表（分页）
export const getInstanceList = (params: QueryInstanceParams): Promise<InstanceListResult>

// 获取实例详情（含监控指标）
export const getInstanceDetail = (instanceId: string): Promise<InstanceDetail>

// 保存实例（新增/编辑）
export const saveInstance = (params: SaveInstanceParams): Promise<void>

// 删除实例
export const deleteInstance = (instanceId: string): Promise<void>

// 下线实例
export const offlineInstance = (params: OfflineInstanceParams): Promise<void>

// 上线实例
export const onlineInstance = (params: OnlineInstanceParams): Promise<void>

// 获取实例实时监控指标
export const getInstanceMetrics = (instanceId: string): Promise<InstanceMetrics>
```

---

## 3. 后端设计

### 3.1 Controller 接口

#### GatewayInstanceController 扩展

```java
/**
 * 分页查询实例列表
 */
@PostMapping("/queryInstanceList")
ResponseDTO<QueryInstanceListRsp> queryInstanceList(@RequestBody @Validated RequestDTO<QueryInstanceReq> reqDto);

/**
 * 保存实例（新增/编辑）
 */
@PostMapping("/saveInstance")
ResponseDTO<EmptyBody> saveInstance(@RequestBody @Validated RequestDTO<SaveInstanceReq> reqDto);

/**
 * 删除实例
 */
@PostMapping("/deleteInstance")
ResponseDTO<EmptyBody> deleteInstance(@RequestBody @Validated RequestDTO<DeleteInstanceReq> reqDto);

/**
 * 获取实例详情（含监控指标）
 */
@PostMapping("/getInstanceDetailWithMetrics")
ResponseDTO<InstanceDetailRsp> getInstanceDetailWithMetrics(@RequestBody @Validated RequestDTO<GetInstanceDetailReq> reqDto);
```

#### MonitorController 扩展（监控指标详情）

```java
/**
 * 获取实例 JVM 监控指标
 */
@PostMapping("/getJvmMetrics")
ResponseDTO<JvmMetricsRsp> getJvmMetrics(@RequestBody @Validated RequestDTO<GetJvmMetricsReq> reqDto);

/**
 * 获取实例健康检测详情
 */
@PostMapping("/getHealthDetail")
ResponseDTO<HealthDetailRsp> getHealthDetail(@RequestBody @Validated RequestDTO<GetHealthDetailReq> reqDto);
```

### 3.2 新增 DTO

#### 3.2.1 QueryInstanceReq

```java
@Getter
@Setter
public class QueryInstanceReq extends Page {
    
    /** 服务ID */
    private String serviceId;
    
    /** 主机地址 */
    private String host;
    
    /** 实例状态 */
    private Byte status;
}
```

#### 3.2.2 SaveInstanceReq

```java
@Getter
@Setter
public class SaveInstanceReq implements Serializable {
    
    /** 主键 ID（编辑时必填） */
    private Integer id;
    
    /** 服务ID */
    @NotBlank(message = "服务ID不能为空")
    private String serviceId;
    
    /** 主机地址 */
    @NotBlank(message = "主机地址不能为空")
    private String host;
    
    /** 端口 */
    @NotNull(message = "端口不能为空")
    private Integer port;
    
    /** 元数据（JSON 格式） */
    private String metadata;
}
```

#### 3.2.3 DeleteInstanceReq

```java
@Getter
@Setter
public class DeleteInstanceReq implements Serializable {
    
    /** 实例 ID */
    @NotBlank(message = "实例ID不能为空")
    private String instanceId;
}
```

#### 3.2.4 InstanceDetailRsp

```java
@Getter
@Setter
public class InstanceDetailRsp implements Serializable {
    
    /** 基本信息 */
    private InstanceInfoVO instanceInfo;
    
    /** 健康状态详情 */
    private HealthDetailVO healthDetail;
    
    /** JVM 监控指标 */
    private JvmMetricsVO jvmMetrics;
    
    /** HTTP 请求统计 */
    private HttpMetricsVO httpMetrics;
}
```

#### 3.2.5 JvmMetricsVO

```java
@Getter
@Setter
public class JvmMetricsVO implements Serializable {
    
    /** 堆内存使用量 (bytes) */
    private Long heapUsed;
    
    /** 堆内存最大值 (bytes) */
    private Long heapMax;
    
    /** 堆内存使用率 (%) */
    private Double heapUsagePercent;
    
    /** 非堆内存使用量 (bytes) */
    private Long nonHeapUsed;
    
    /** 年轻代 GC 次数 */
    private Long youngGcCount;
    
    /** 年轻代 GC 时间 (ms) */
    private Long youngGcTime;
    
    /** 老年代 GC 次数 */
    private Long oldGcCount;
    
    /** 老年代 GC 时间 (ms) */
    private Long oldGcTime;
    
    /** 活跃线程数 */
    private Integer liveThreads;
    
    /** 峰值线程数 */
    private Integer peakThreads;
    
    /** 守护线程数 */
    private Integer daemonThreads;
    
    /** 采样时间 */
    private Long timestamp;
}
```

#### 3.2.6 HealthDetailVO

```java
@Getter
@Setter
public class HealthDetailVO implements Serializable {
    
    /** 整体状态 */
    private String status;
    
    /** 各组件健康状态 */
    private List<ComponentHealthVO> components;
}

@Getter
@Setter
public class ComponentHealthVO implements Serializable {
    
    /** 组件名称 */
    private String name;
    
    /** 状态 */
    private String status;
    
    /** 详情 */
    private Map<String, Object> details;
}
```

#### 3.2.7 HttpMetricsVO

```java
@Getter
@Setter
public class HttpMetricsVO implements Serializable {
    
    /** 总请求数 */
    private Long totalRequests;
    
    /** 成功请求数 */
    private Long successRequests;
    
    /** 失败请求数 */
    private Long failedRequests;
    
    /** 成功率 (%) */
    private Double successRate;
    
    /** 平均响应时间 (ms) */
    private Long avgResponseTime;
    
    /** 当前连接数 */
    private Integer activeConnections;
    
    /** 采样时间 */
    private Long timestamp;
}
```

### 3.3 Service 层扩展

#### 3.3.1 GatewayInstanceService 接口扩展

```java
public interface GatewayInstanceService {
    
    // 现有方法保持不变
    ResponseDTO<GatewayInstanceListRsp> getGatewayInstances();
    ResponseDTO<GatewayInstanceVO> getGatewayInstanceDetail(GetGatewayInstanceDetailReq req);
    ResponseDTO<EmptyBody> offlineInstance(OfflineGatewayInstanceReq req);
    ResponseDTO<EmptyBody> onlineInstance(OnlineGatewayInstanceReq req);
    void syncInstanceStatus();
    
    // 新增方法
    ResponseDTO<QueryInstanceListRsp> queryInstanceList(QueryInstanceReq req);
    ResponseDTO<EmptyBody> saveInstance(SaveInstanceReq req);
    ResponseDTO<EmptyBody> deleteInstance(DeleteInstanceReq req);
    ResponseDTO<InstanceDetailRsp> getInstanceDetailWithMetrics(GetInstanceDetailReq req);
}
```

#### 3.3.2 MonitorService 接口扩展

```java
public interface MonitorService {
    
    // 现有方法保持不变
    ResponseDTO<GatewayInstanceListRsp> getGatewayInstances(QueryGatewayInstanceReq req);
    ResponseDTO<GatewayStatisticsRsp> getStatistics(QueryStatisticsReq req);
    ResponseDTO<GatewayHealthStatusRsp> getHealthStatus(QueryHealthStatusReq req);
    ResponseDTO<GatewayMetricsRsp> getGatewayMetrics(GetGatewayMetricsReq req);
    
    // 新增方法
    ResponseDTO<JvmMetricsRsp> getJvmMetrics(GetJvmMetricsReq req);
    ResponseDTO<HealthDetailRsp> getHealthDetail(GetHealthDetailReq req);
}
```

### 3.4 MetricsCollectorService 扩展

扩展指标采集，增加 GC、线程等指标：

```java
private InstanceMetrics collectInstanceMetrics(ServiceInstance instance) {
    InstanceMetrics metrics = new InstanceMetrics();
    
    // 1. 获取健康状态详情
    Map<String, Object> health = fetchActuatorEndpoint(baseUrl + "/actuator/health");
    metrics.healthDetail = parseHealthDetail(health);
    
    // 2. 获取 JVM 内存指标
    metrics.heapUsed = extractMetricValue(baseUrl + "/actuator/metrics/jvm.memory.used?tag=area:heap");
    metrics.heapMax = extractMetricValue(baseUrl + "/actuator/metrics/jvm.memory.max?tag=area:heap");
    metrics.nonHeapUsed = extractMetricValue(baseUrl + "/actuator/metrics/jvm.memory.used?tag=area:nonheap");
    
    // 3. 获取 GC 指标
    metrics.youngGcCount = extractMetricCount(baseUrl + "/actuator/metrics/jvm.gc.pause?tag=action:end of minor GC");
    metrics.youngGcTime = extractMetricSum(baseUrl + "/actuator/metrics/jvm.gc.pause?tag=action:end of minor GC");
    metrics.oldGcCount = extractMetricCount(baseUrl + "/actuator/metrics/jvm.gc.pause?tag=action:end of major GC");
    metrics.oldGcTime = extractMetricSum(baseUrl + "/actuator/metrics/jvm.gc.pause?tag=action:end of major GC");
    
    // 4. 获取线程指标
    metrics.liveThreads = extractMetricValue(baseUrl + "/actuator/metrics/jvm.threads.live");
    metrics.peakThreads = extractMetricValue(baseUrl + "/actuator/metrics/jvm.threads.peak");
    metrics.daemonThreads = extractMetricValue(baseUrl + "/actuator/metrics/jvm.threads.daemon");
    
    // 5. HTTP 请求指标（已有）
    // ...
    
    return metrics;
}
```

---

## 4. 数据流设计

### 4.1 实例配置管理

```
┌─────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   前端页面   │────▶│GatewayInstance   │────▶│ MySQL           │
│             │     │Controller/Service│     │ gateway_instance│
└─────────────┘     └─────────────────┘     └─────────────────┘
```

### 4.2 监控指标获取

```
┌─────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   前端页面   │────▶│ MonitorService   │────▶│ Redis           │
│ (详情弹窗)   │     │                 │     │ 实时指标缓存     │
└─────────────┘     └─────────────────┘     └─────────────────┘
                           │
                           ▼
                    ┌─────────────────┐
                    │ Actuator 端点    │
                    │ /actuator/health │
                    │ /actuator/metrics│
                    └─────────────────┘
```

### 4.3 指标采集流程

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│定时任务(30s)     │────▶│DiscoveryClient   │────▶│gateway-reactive │
│MetricsCollector │     │获取实例列表       │     │ Actuator 端点   │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                                               │
        │                                               ▼
        │                                       ┌─────────────────┐
        │                                       │ 返回指标数据     │
        │                                       │ (内存/GC/线程)  │
        │                                       └─────────────────┘
        │                                               │
        ▼                                               ▼
┌─────────────────┐                            ┌─────────────────┐
│ Redis 存储       │                            │ MySQL 存储      │
│ 实时数据(60s过期)│                            │ 历史数据(7天)   │
└─────────────────┘                            └─────────────────┘
```

---

## 5. 国际化配置

### 5.1 中文配置

```typescript
instance: {
  title: '实例管理',
  instanceId: '实例ID',
  serviceId: '服务ID',
  host: '主机IP',
  port: '端口',
  uri: 'URI地址',
  metadata: '元数据',
  status: '状态',
  statusOnline: '在线',
  statusOffline: '离线',
  statusDown: '已下线',
  healthStatus: '健康状态',
  healthy: '健康',
  unhealthy: '不健康',
  onlineTime: '上线时间',
  offlineTime: '下线时间',
  lastHeartbeat: '最后心跳',
  
  // 操作
  addInstance: '新增实例',
  editInstance: '编辑实例',
  deleteInstance: '删除实例',
  onlineInstance: '上线实例',
  offlineInstance: '下线实例',
  viewDetail: '查看详情',
  
  // 监控
  jvmMetrics: 'JVM 监控',
  heapMemory: '堆内存',
  nonHeapMemory: '非堆内存',
  memoryUsage: '内存使用率',
  gcStatistics: 'GC 统计',
  youngGc: '年轻代 GC',
  oldGc: '老年代 GC',
  gcCount: 'GC 次数',
  gcTime: 'GC 时间',
  threadInfo: '线程信息',
  liveThreads: '活跃线程',
  peakThreads: '峰值线程',
  daemonThreads: '守护线程',
  httpStatistics: 'HTTP 统计',
  totalRequests: '总请求数',
  successRequests: '成功请求数',
  failedRequests: '失败请求数',
  successRate: '成功率',
  avgResponseTime: '平均响应时间',
  
  // 提示
  deleteConfirm: '确定要删除该实例吗？',
  offlineConfirm: '确定要下线该实例吗？下线后该实例将停止接收请求。',
  onlineConfirm: '确定要上线该实例吗？上线后该实例将开始接收请求。',
  
  // 统计卡片
  totalInstances: '总实例数',
  onlineInstances: '在线实例',
  healthyInstances: '健康实例',
  avgCpuUsage: '平均 CPU',
  avgMemoryUsage: '平均内存',
}
```

---

## 6. 验收标准

| 功能 | 验收点 |
|------|--------|
| 实例列表 | 分页展示正确，支持 serviceId/host/status 筛选 |
| 新增实例 | 配置信息保存成功，自动生成 instanceId |
| 编辑实例 | 修改后数据正确更新 |
| 删除实例 | 删除成功，列表刷新 |
| 上线操作 | 状态变更为在线，记录上线时间 |
| 下线操作 | 状态变更为下线，记录下线时间 |
| 详情弹窗 | 显示完整的基本信息、健康状态、JVM 指标、HTTP 统计 |
| JVM 监控 | 堆内存、GC、线程数据实时展示 |
| 健康检测 | 显示各组件健康状态详情 |
| 国际化 | 中英文切换正常 |

---

## 7. 模块影响范围

### 7.1 后端模块

| 模块 | 改动内容 |
|------|----------|
| gateway-admin | Controller 新增接口、Service 扩展、DTO 新增 |
| MetricsCollectorService | 扩展 GC、线程指标采集 |

### 7.2 前端模块

| 文件 | 改动内容 |
|------|----------|
| views/instance/index.vue | 新增实例管理页面 |
| api/instance.ts | 新增实例 API 模块 |
| router/index.ts | 新增路由配置 |
| locales/zh-cn.ts | 新增 instance 国际化配置 |
| locales/en-us.ts | 新增 instance 国际化配置 |

---

## 8. 风险评估

| 风险 | 级别 | 缓解措施 |
|------|------|----------|
| Actuator 端点调用超时 | 中 | 设置合理超时时间，异步采集 |
| 实例离线时无法获取指标 | 低 | 显示最近一次采集数据 + 提示离线 |
| 大量实例性能 | 低 | 分页查询，指标采集间隔合理 |
| 元数据 JSON 格式错误 | 低 | 前端校验 + 后端容错处理 |

---

## 9. 实现优先级

| 优先级 | 功能 | 说明 |
|--------|------|------|
| P0 | 实例列表、新增、编辑、删除 | 核心配置管理功能 |
| P0 | 上线、下线操作 | 核心生命周期管理 |
| P1 | 详情弹窗（基本信息） | 基础详情展示 |
| P1 | JVM 监控指标采集与展示 | 扩展指标采集 |
| P2 | 健康检测详情 | Actuator health 组件详情 |
| P2 | GC 统计、线程信息 | 高级 JVM 指标 |