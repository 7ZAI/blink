# Gateway 缓存同步消息机制

本文档描述 gateway-admin 与 gateway-reactive 之间的缓存同步消息机制，包括消息发送、消费确认、以及 SSE 实时通知的完整流程。

## 概述

当用户在 gateway-admin 中执行缓存相关操作（如修改渠道信息）时，系统会通过 Redis Stream 发送缓存同步消息到 gateway-reactive。gateway-reactive 消费消息后，通过 Dubbo ACK 反馈消费结果，gateway-admin 收到 ACK 后更新消息状态并通过 SSE 推送通知给操作用户。

## 架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    消息发送-消费-SSE通知 完整流程                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  用户操作（如修改渠道）                                                    │
│       │                                                                 │
│       │ ① GateWayStreamMessageProducer.sendCacheSyncMsg()               │
│       │    自动获取当前登录用户 → 设置 operatorUser/operatorName          │
│       ↓                                                                 │
│  Redis Stream (blink:stream:gateway:event)                              │
│       │    CacheMsg 携带 operatorUser/operatorName                       │
│       │                                                                 │
│       │ ② gateway-reactive 消费消息                                     │
│       ↓                                                                 │
│  CommonEventStreamListener                                              │
│       │                                                                 │
│       │ ③ 处理消息 (缓存同步/路由刷新)                                    │
│       │                                                                 │
│       │ ④ Redis Stream XACK                                             │
│       │                                                                 │
│       │ ⑤ Dubbo ACK 异步调用 (携带 operatorUser)                         │
│       │    不阻塞主流程，失败仅记录日志                                    │
│       ↓                                                                 │
│  gateway-admin (GatewayAdminDubboServiceImpl)                           │
│       │                                                                 │
│       │ ⑥ 更新 redis_mq 表状态                                           │
│       │    success → msg_status = 3 (已确认)                             │
│       │    fail    → msg_status = 4 (消费失败)                           │
│       │                                                                 │
│       │ ⑦ SseConnectionPool.sendToUser(operatorUser, notification)       │
│       ↓                                                                 │
│  用户浏览器收到 SSE 通知                                                  │
│       │                                                                 │
│       │  成功: "您好 xxx，您的缓存操作已成功同步到网关"                     │
│       │  失败: "您好 xxx，缓存同步失败：xxx"                              │
│       ↓                                                                 │
│  完成                                                                   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

## 消息状态流转

| 状态码 | 含义 | 使用场景 |
|--------|------|----------|
| 0 | 未读 | 消息发送成功后 |
| 1 | 已读 | 未使用 |
| 2 | 发送失败 | 发送到 Redis Stream 失败 |
| 3 | 已确认消费 | gateway-reactive ACK 成功 |
| 4 | 消费失败 | gateway-reactive ACK 失败 |

## 核心组件

### 1. CacheMsg 消息结构

```java
public class CacheMsg {
    private String key;           // 缓存 key
    private Object value;         // 缓存值
    private Integer version;      // 版本号（乐观锁）
    private String operator;      // 操作类型: A(新增)/M(修改)/D(删除)
    private Integer operatorUser; // 操作人用户ID（用于 SSE 通知）
    private String operatorName;  // 操作人用户名
}
```

### 2. MessageAckReq ACK 请求结构

```java
public class MessageAckReq {
    private String streamId;      // Redis Stream recordId
    private String msgId;         // StreamMessage.msgId
    private Boolean success;      // 消费是否成功
    private String consumer;      // 消费者名称
    private String errorMsg;      // 错误信息（失败时填写）
    private Integer operatorUser; // 操作人用户ID
    private String operatorName;  // 操作人用户名
}
```

### 3. NotificationMsg SSE 通知结构

```java
public class NotificationMsg {
    private Long notificationId;
    private String title;         // 通知标题
    private String content;       // 通知内容
    private String type;          // cache_sync_success / cache_sync_failed
    private String severity;      // info / error
    private String targetType;    // user
    private Integer targetUserId; // 目标用户ID
    private String sourceRef;     // 消息ID
    private LocalDateTime createdTime;
}
```

## PEL 清理机制

为避免重复消费历史消息，gateway-reactive 启动时会清理 PEL（Pending Entry List）中的过期消息：

1. 创建消费者组时使用 `$` 作为起始 ID，只消费新消息
2. 启动时清理空闲时间超过 60 秒的历史消息
3. 使用 `ReadOffset.lastConsumed()` 只读取新消息

详见 `GateWayUtil.cleanupPel()` 方法。

## SSE 连接管理

- 用户登录后建立 SSE 连接，连接注册到 Redis
- 支持多实例部署，通过 Redis 注册表判断消息应由哪个实例推送
- 心跳保活机制，每 60 秒发送心跳并刷新 Redis TTL
- 实例关闭时自动清理连接注册

详见 `SseConnectionPool` 类。

## 关键代码位置

| 功能 | 模块 | 类/文件 |
|------|------|---------|
| 发送消息 | gateway-admin | `GateWayStreamMessageProducer` |
| 设置操作人 | gateway-admin | `GateWayStreamMessageProducer.setOperatorInfo()` |
| 消费消息 | gateway-reactive | `CommonEventStreamListener` |
| Dubbo ACK 接口 | admin-api-dubbo | `GatewayAdminDubboService.ackMessageAsync()` |
| ACK 处理 | gateway-admin | `MessageAckServiceImpl` |
| SSE 推送 | gateway-admin | `SseConnectionPool.sendToUser()` |
| PEL 清理 | gateway-reactive | `GateWayUtil.cleanupPel()` |

## 构建顺序

由于模块间存在依赖，需按以下顺序构建：

```bash
# 1. 构建 API 模块（发布到本地 Maven）
./gradlew :blink-gateway:blink-gateway-admin-api-dubbo:publishToMavenLocal

# 2. 构建 gateway-admin
./gradlew :blink-gateway:gateway-admin:build

# 3. 构建 gateway-reactive
./gradlew :blink-gateway:blink-gateway-reactive:build
```

## 部署验证

重启服务后，可通过以下方式验证：

### gateway-reactive 日志
```
stream消息消费 ack结果:true
[DubboACK] 异步通知成功 | streamId: xxx, acked: true
```

### gateway-admin 日志
```
[MessageAck] 收到 ACK 确认 | streamId: xxx, success: true
[MessageAck] 状态更新成功 | newStatus: 3(已确认)
[MessageAck] SSE 通知已推送 | userId: xxx, type: cache_sync_success
```

### 数据库验证
```sql
SELECT msg_id, msg_status, extra FROM redis_mq WHERE stream_id = 'xxx';
-- msg_status = 3 表示已确认消费
```

## 异常处理

| 异常场景 | 处理方式 |
|----------|----------|
| Redis Stream 发送失败 | 记录到 redis_mq (status=2)，可通过 CacheSyncFailureService 重试 |
| 消费处理失败 | 消息留在 PEL 中，由 PendingMessageScheduler 定时重试 |
| Dubbo ACK 失败 | 仅记录日志，不影响主流程 |
| SSE 推送失败 | 仅记录日志，不影响主流程 |

## 版本号机制

为防止消息乱序，CacheMsg 支持 version 字段：

- 发送时使用时间戳作为版本号
- gateway-reactive 消费时检查版本号，忽略过期消息
- 版本号存储在 Redis key + ":version"

---

*文档更新时间: 2026-04-10*