# 网关监控自上报架构实现方案

> 创建时间：2026-04-14
> 状态：进行中（集成测试暂缓）

## 一、架构概述

### 1.1 问题背景

当前监控方案采用 **HTTP 拉模式**，gateway-admin 每 30 秒主动请求各 gateway-reactive 实例的 Actuator 端点：

```
当前方案（拉模式）:
┌─────────────────┐     HTTP 轮询      ┌─────────────────┐
│ gateway-admin   │ ──────────────────▶│ gateway-reactive│
│  (采集者)       │   /actuator/metrics│   (被采集)       │
└─────────────────┘     每30秒          └─────────────────┘
```

**存在的问题：**
- 网络开销大：admin 需请求每个实例的多个端点
- 延迟高：采集周期 + HTTP 往返时间
- 扩展性差：实例越多，admin 压力越大
- 耦合度高：admin 需知道所有实例地址
- 可靠性低：admin 故障则监控全丢

### 1.2 新架构设计

采用 **Redis Stream 推模式**，各 gateway-reactive 实例主动上报指标：

```
新方案（推模式）:
┌─────────────────┐                     ┌─────────────────┐
│ gateway-reactive│ ───── 异步推送 ────▶│     Redis       │
│   (自上报)      │     Redis Stream    │   (数据源)       │
└─────────────────┘                     └────────┬────────┘
                                                 │
                                                 ▼ 消费
                                        ┌─────────────────┐
                                        │ gateway-admin   │
                                        │  (聚合+推送)    │
                                        └─────────────────┘
```

**优势：**
- 零 HTTP 开销：本地获取指标，直接写 Redis
- 低延迟：实例采集后立即上报
- 高扩展：负载分散到各实例
- 低耦合：实例只需知道 Redis 地址
- 高可靠：单实例故障不影响其他监控

---

## 二、Redis Key 设计

| Key 模式 | 类型 | 用途 | TTL |
|----------|------|------|-----|
| `blink:gateway:metrics:stream` | Stream | 指标消息流 | 永久 |
| `blink:gateway:metrics:{instanceId}` | Hash | 实例最新指标 | 90s |
| `blink:gateway:instance:ids` | Set | 所有实例ID列表 | 永久 |
| `blink:gateway:instance:snapshot:{instanceId}` | Hash | 状态快照(变化检测) | 300s |
| `blink:gateway:metrics:summary` | Hash | 汇总统计 | 90s |
| `blink:sse:connection:registry` | Hash | SSE连接注册表 | 300s |

---

## 三、消息格式

```json
{
  "instanceId": "gateway-app@192.168.1.100:8080",
  "serviceId": "gateway-app",
  "host": "192.168.1.100",
  "port": 8080,
  "timestamp": 1713075600000,
  "type": "METRICS",

  "heapUsed": 134217728,
  "heapMax": 536870912,
  "heapUsagePercent": 25.0,
  "nonHeapUsed": 67108864,
  "cpuUsage": 15.5,

  "youngGcCount": 120,
  "youngGcTime": 1500,
  "oldGcCount": 2,
  "oldGcTime": 200,

  "liveThreads": 150,
  "peakThreads": 200,
  "daemonThreads": 50,

  "totalRequests": 100000,
  "successRequests": 98500,
  "failedRequests": 1500,
  "avgResponseTime": 45,

  "healthStatus": "UP"
}
```

消息类型：
- `METRICS` - 定时指标上报
- `REGISTER` - 实例启动注册
- `UNREGISTER` - 实例关闭注销

---

## 四、任务清单

### Phase 1: 基础设施 (已完成)

| # | 任务 | 状态 | 完成时间 |
|---|------|------|----------|
| 1.1 | SSE 消息类型分离 (SseMessage, SseMessageType) | ✅ 完成 | 2026-04-14 |
| 1.2 | 通知载荷类 (NotificationPayload) | ✅ 完成 | 2026-04-14 |
| 1.3 | 实例状态载荷类 (InstanceStatusPayload) | ✅ 完成 | 2026-04-14 |
| 1.4 | 状态快照类 (InstanceStatusSnapshot) | ✅ 完成 | 2026-04-14 |
| 1.5 | SSE 配置类 (SseConfig) | ✅ 完成 | 2026-04-14 |
| 1.6 | 状态推送服务接口 (InstanceStatusPushService) | ✅ 完成 | 2026-04-14 |
| 1.7 | 状态推送服务实现 (InstanceStatusPushServiceImpl) | ✅ 完成 | 2026-04-14 |

### Phase 2: gateway-reactive 指标上报

| # | 任务 | 状态 | 完成时间 |
|---|------|------|----------|
| 2.1 | 创建指标上报服务接口 (MetricsReporter) | ✅ 完成 | 2026-04-14 |
| 2.2 | 实现本地指标采集 (从 MeterRegistry 获取) | ✅ 完成 | 2026-04-14 |
| 2.3 | 实现异步推送到 Redis Stream | ✅ 完成 | 2026-04-14 |
| 2.4 | 实现实例启动注册通知 | ✅ 完成 | 2026-04-14 |
| 2.5 | 实现实例关闭注销通知 | ✅ 完成 | 2026-04-14 |
| 2.6 | 添加配置项 (上报频率、Redis 地址等) | ✅ 完成 | 2026-04-14 |
| 2.7 | 单元测试 (MetricsReporterImplTest) | ✅ 完成 | 2026-04-14 |

### Phase 3: gateway-admin 消费聚合

| # | 任务 | 状态 | 完成时间 |
|---|------|------|----------|
| 3.1 | 创建 Stream 消费者服务 (MetricsStreamConsumer) | ✅ 完成 | 2026-04-14 |
| 3.2 | 实现 METRICS 消息处理与指标存储 | ✅ 完成 | 2026-04-14 |
| 3.3 | 实现 REGISTER/UNREGISTER 消息处理 | ✅ 完成 | 2026-04-14 |
| 3.4 | 实现实例列表更新 | ✅ 完成 | 2026-04-14 |
| 3.5 | 实现汇总统计更新 | ✅ 完成 | 2026-04-14 |
| 3.6 | 集成状态变化检测与 SSE 推送 | ✅ 完成 | 2026-04-14 |
| 3.7 | 单元测试 (MetricsStreamConsumerTest) | ✅ 完成 | 2026-04-14 |

### Phase 4: SSE 优化

| # | 任务 | 状态 | 完成时间 |
|---|------|------|----------|
| 4.1 | 应用 SseConfig 配置 | ✅ 完成 | 2026-04-14 |
| 4.2 | 实现单用户连接数限制 | ✅ 完成 | 2026-04-14 |
| 4.3 | 实现总连接数限制 | ✅ 完成 | 2026-04-14 |
| 4.4 | 优化心跳检测机制 | ✅ 完成 | 2026-04-14 |
| 4.5 | 单元测试 (SseConnectionPoolTest) | ✅ 完成 | 2026-04-14 |

### Phase 5: 前端改造

| # | 任务 | 状态 | 完成时间 |
|---|------|------|----------|
| 5.1 | 创建 useSse composable | ✅ 完成 | 2026-04-14 |
| 5.2 | 实现 SSE 连接建立 | ✅ 完成 | 2026-04-14 |
| 5.3 | 实现事件监听 (heartbeat/notification/instance_status) | ✅ 完成 | 2026-04-14 |
| 5.4 | 单元测试 (useSse.test.ts) | ✅ 完成 | 2026-04-14 |
| 5.5 | 改造实例管理页面 (移除轮询) | ✅ 完成 | 2026-04-14 |

### Phase 6: 清理与测试

| # | 任务 | 状态 | 完成时间 |
|---|------|------|----------|
| 6.1 | 移除 MetricsCollectorServiceImpl HTTP 采集逻辑 | ✅ 完成 | 2026-04-14 |
| 6.2 | 更新配置文件 | ✅ 完成 | 2026-04-14 |
| 6.3 | 单元测试 | ✅ 完成 | 2026-04-14 |
| 6.4 | 集成测试 | ⏳ 待开始 | - |

---

## 五、已完成的代码文件

### 5.1 SSE 消息体系

| 文件路径 | 说明 |
|----------|------|
| `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/sse/SseMessage.java` | 通用 SSE 消息类 |
| `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/sse/SseMessageType.java` | 消息类型常量 |
| `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/sse/NotificationPayload.java` | 通知消息载荷 |
| `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/sse/InstanceStatusPayload.java` | 实例状态载荷 |

### 5.2 状态检测与推送

| 文件路径 | 说明 |
|----------|------|
| `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/InstanceStatusSnapshot.java` | 状态快照(变化检测) |
| `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/SseConfig.java` | SSE 配置常量 |
| `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/InstanceStatusPushService.java` | 状态推送服务接口 |
| `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/InstanceStatusPushServiceImpl.java` | 状态推送服务实现 |

### 5.3 更新的文件

| 文件路径 | 修改内容 |
|----------|----------|
| `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/sse/SseConnectionPool.java` | 使用新的消息类型体系 |

### 5.4 gateway-reactive 指标上报

| 文件路径 | 说明 |
|----------|------|
| `blink-gateway/blink-gateway-reactive/src/main/java/com/blink/gateway/monitor/MetricsReporter.java` | 指标上报服务接口 |
| `blink-gateway/blink-gateway-reactive/src/main/java/com/blink/gateway/monitor/MetricsReporterImpl.java` | 指标上报服务实现 |
| `blink-gateway/blink-gateway-reactive/src/main/java/com/blink/gateway/monitor/MetricsReporterConfig.java` | 异步线程池配置 |
| `blink-gateway/blink-gateway-reactive/src/main/java/com/blink/gateway/monitor/dto/MetricsMessage.java` | 指标消息 DTO |

### 5.5 gateway-admin Stream 消费

| 文件路径 | 说明 |
|----------|------|
| `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/MetricsStreamConsumer.java` | Stream 消费者服务 |

### 5.6 单元测试

| 文件路径 | 说明 |
|----------|------|
| `blink-gateway/blink-gateway-reactive/src/test/java/com/blink/gateway/monitor/MetricsReporterImplTest.java` | MetricsReporter 单元测试 |
| `blink-gateway/gateway-admin/src/test/java/com/blink/gateway/admin/service/MetricsStreamConsumerTest.java` | Stream 消费者单元测试 |
| `blink-gateway/gateway-admin/src/test/java/com/blink/gateway/admin/sse/SseConnectionPoolTest.java` | SSE 连接池单元测试 |
| `frontend/packages/base-admin/src/__tests__/composables/useSse.test.ts` | useSse composable 单元测试 |

### 5.7 前端 SSE Composable

| 文件路径 | 说明 |
|----------|------|
| `frontend/packages/base-admin/src/composables/useSse.ts` | SSE 连接管理 Composable |

### 5.8 gateway-admin 前端改造

| 文件路径 | 说明 |
|----------|------|
| `frontend/packages/gateway-admin/src/composables/useInstanceStatus.ts` | 实例状态实时更新 Composable |
| `frontend/packages/gateway-admin/src/views/instance/index.vue` | 实例管理页面（SSE 替代轮询） |
| `frontend/packages/gateway-admin/src/views/monitor/index.vue` | 监控页面（SSE 替代轮询） |

### 5.9 Phase 6 新增测试

| 文件路径 | 说明 |
|----------|------|
| `blink-gateway/gateway-admin/src/test/java/com/blink/gateway/admin/service/impl/InstanceStatusPushServiceImplTest.java` | 状态推送服务单元测试 |

---

## 六、注意事项

1. **异步上报**：gateway-reactive 必须使用异步方式上报，避免阻塞主线程
2. **消费者组**：Redis Stream 使用消费者组，保证消息不丢失
3. **TTL 管理**：实例指标 Hash 设置 90 秒 TTL，超时判定离线
4. **连接限制**：SSE 连接需要限制数量，防止资源耗尽
5. **优雅关闭**：实例关闭时必须发送 UNREGISTER 消息

---

## 七、相关文档

- [CLAUDE.md](../CLAUDE.md) - 项目开发规范
- [frontend/RULES.md](../frontend/RULES.md) - 前端开发规范
