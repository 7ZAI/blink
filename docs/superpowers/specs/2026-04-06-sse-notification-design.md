# SSE 消息通知机制设计文档

## 1. 概述

为 gateway-admin 及其前端 gateway-admin-web 设计一套消息通知机制，实现服务器向用户推送实时消息通知。

### 1.1 技术选型

| 组件 | 技术 | 说明 |
|------|------|------|
| 推送协议 | SSE (Server-Sent Events) | 单向推送，轻量级，适合实时通知场景 |
| 多实例同步 | Redis Pub/Sub | 广播机制，天然支持多实例 |
| 消息持久化 | MySQL + Redis | 数据库长期存储，Redis 缓存未读计数 |

### 1.2 使用场景

主要用于系统运维场景的异步消息通知：
- 缓存同步结果通知
- 路由同步状态
- 配置推送结果
- 网关实例状态变更告警
- 数据同步任务完成/失败通知

### 1.3 推送模式

混合模式：支持全局广播和定向推送
- **全局广播**：系统级告警（网关异常、实例下线），所有在线用户接收
- **定向推送**：用户触发的异步操作结果（同步任务、配置推送），仅目标用户接收

---

## 2. 数据模型设计

### 2.1 数据库表设计

```sql
-- 消息通知表
CREATE TABLE sys_notification (
    notification_id      BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    title                VARCHAR(100) NOT NULL COMMENT '消息标题',
    content              VARCHAR(500) NOT NULL COMMENT '消息内容',
    type                 VARCHAR(20) NOT NULL COMMENT '消息类型: SYSTEM/OPERATION/ALERT',
    severity             VARCHAR(20) NOT NULL DEFAULT 'INFO' COMMENT '严重级别: INFO/WARNING/ERROR/SUCCESS',
    target_type          VARCHAR(20) NOT NULL DEFAULT 'ALL' COMMENT '目标类型: ALL/USER',
    target_user_id       INT NULL COMMENT '目标用户ID，定向推送时使用',
    source_ref           VARCHAR(100) NULL COMMENT '来源关联ID，如同步任务ID、配置ID',
    created_by           INT NULL COMMENT '创建人',
    created_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    expire_time          DATETIME NULL COMMENT '过期时间，过期后不再展示',
    INDEX idx_target_user (target_user_id, created_time),
    INDEX idx_created_time (created_time),
    INDEX idx_type_severity (type, severity)
) COMMENT '系统消息通知表';

-- 用户消息读取状态表
CREATE TABLE sys_notification_read (
    read_id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    notification_id      BIGINT NOT NULL COMMENT '消息ID',
    user_id              INT NOT NULL COMMENT '用户ID',
    read_time            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '读取时间',
    UNIQUE KEY uk_notification_user (notification_id, user_id),
    INDEX idx_user_read (user_id, read_time)
) COMMENT '消息读取状态表';
```

### 2.2 后端实体命名

| 类型 | 命名 |
|------|------|
| 消息实体 | `SysNotificationDO` |
| 读取状态实体 | `SysNotificationReadDO` |
| 消息类型常量 | `NotificationTypeConstant`: SYSTEM, OPERATION, ALERT |
| 严重级别常量 | `NotificationSeverityConstant`: INFO, WARNING, ERROR, SUCCESS |
| 目标类型常量 | `TargetTypeConstant`: ALL, USER |

---

## 3. 后端 SSE 服务架构

### 3.1 架构总览

```
┌─────────────────────────────────────────────────────────────┐
│                    gateway-admin 实例                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────┐    ┌─────────────────────────────────┐│
│  │ SseConnectionPool│    │ NotificationPublishService      ││
│  │ (连接池管理)      │◄───│ (消息发布服务)                   ││
│  │                  │    │                                 ││
│  │ Map<userId,      │    │ 1. 消息入库                     ││
│  │   List<SseEmitter│    │ 2. 发布到 Redis Pub/Sub         ││
│  │ >                │    │ 3. 处理广播/定向推送             ││
│  └──────────────────┘    └─────────────────────────────────┘│
│           │                        │                         │
│           ▼                        ▼                         │
│  ┌──────────────────────────────────────────────────────────┐│
│  │ NotificationRedisListener                                ││
│  │ (Redis Pub/Sub 监听器)                                    ││
│  │                                                           ││
│  │ - 订阅 blink:gateway:admin:notification:channel           ││
│  │ - 收到消息后检查 SseConnectionPool                        ││
│  │ - 推送给匹配的用户连接                                     ││
│  └──────────────────────────────────────────────────────────┘│
│                                                              │
└─────────────────────────────────────────────────────────────┘
                           │
                           ▼
                   Redis Pub/Sub
        (blink:gateway:admin:notification:channel)
```

### 3.2 核心组件职责

| 组件 | 职责 |
|------|------|
| `SseConnectionPool` | 管理当前实例所有 SSE 连接，支持用户多标签页连接 |
| `NotificationPublishService` | 消息发布入口，入库 + Redis Pub/Sub 广播 |
| `NotificationRedisListener` | 监听 Redis 频道，收到消息后匹配本地 SSE 连接推送 |
| `NotificationController` | 提供 SSE 连接端点、消息查询、标记已读等 API |
| `NotificationService` | 消息 CRUD、未读计数、历史查询业务逻辑 |

### 3.3 API 接口设计

遵循项目 POST-only 规范：

```java
// SSE 连接端点（特殊，返回 SseEmitter）
@PostMapping("/sse/connect")
public SseEmitter connect() // 建立 SSE 连接

// 消息相关接口
@PostMapping("/notification/list")
public ResponseDTO<NotificationListRsp> getNotificationList(RequestDTO<QueryNotificationReq> req)

@PostMapping("/notification/unreadCount")
public ResponseDTO<UnreadCountRsp> getUnreadCount(RequestDTO<EmptyBody> req)

@PostMapping("/notification/markRead")
public ResponseDTO<EmptyBody> markRead(RequestDTO<MarkReadReq> req)

@PostMapping("/notification/markAllRead")
public ResponseDTO<EmptyBody> markAllRead(RequestDTO<EmptyBody> req)

@PostMapping("/notification/history")
public ResponseDTO<NotificationHistoryRsp> getHistory(RequestDTO<QueryHistoryReq> req)
```

### 3.4 Redis Key 设计

```java
// RedisKeyConstant 新增
String NOTIFICATION_CHANNEL = BLINK_PREFIX + ":notification:channel";
// 完整: blink:gateway:admin:notification:channel

String NOTIFICATION_USER_UNREAD = BLINK_PREFIX + ":notification:unread:";
// 完整: blink:gateway:admin:notification:unread:{userId}
```

### 3.5 消息推送流程

**全局广播流程：**
```
1. 创建消息记录 → 入库 sys_notification (targetType=ALL)
2. 发布到 Redis Pub/Sub
3. 所有实例收到消息
4. 每个实例遍历本地所有 SSE 连接，推送消息
5. 更新各用户未读计数缓存
```

**定向推送流程：**
```
1. 创建消息记录 → 入库 sys_notification (targetType=USER, targetUserId=xxx)
2. 更新目标用户未读计数缓存
3. 发布到 Redis Pub/Sub
4. 所有实例收到消息
5. 每个实例检查: 本实例是否有 userId=xxx 的 SSE 连接?
   - 有 → 推送消息
   - 无 → 忽略（用户可能在其他实例或离线）
6. 用户上线后，通过 API 拉取离线消息
```

---

## 4. 前端 SSE 与通知组件

### 4.1 前端架构

```
┌─────────────────────────────────────────────────────────────┐
│                       前端应用                               │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ composables/useSseConnection.ts                         ││
│  │                                                          ││
│  │ - 建立 SSE 连接                                           ││
│  │ - 断线自动重连（指数退避策略）                              ││
│  │ - 解析消息并触发回调                                       ││
│  │ - 连接状态管理（连接中/已连接/断开）                        ││
│  └─────────────────────────────────────────────────────────┘│
│                           │                                  │
│                           ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ stores/notification.ts (增强版)                          ││
│  │                                                          ││
│  │ - 接收 SSE 推送的消息                                      ││
│  │ - 管理 notification 列表                                  ││
│  │ - 未读计数                                                ││
│  │ - 调用 API 拉取离线消息                                    ││
│  │ - 标记已读                                                ││
│  └─────────────────────────────────────────────────────────┘│
│                           │                                  │
│           ┌───────────────┴───────────────┐                  │
│           ▼                               ▼                  │
│  ┌─────────────────────┐    ┌──────────────────────────────┐│
│  │ NotificationCenter  │    │ NotificationToast            ││
│  │ (右上角下拉面板)      │    │ (Toast 弹窗组件)              ││
│  │                      │    │                              ││
│  │ - 消息列表展示        │    │ - severity=WARNING/ERROR    ││
│  │ - 未读数 Badge        │    │   时弹出                     ││
│  │ - 标记已读            │    │ - 3秒自动消失                ││
│  │ - 全部已读            │    │ - 点击跳转详情                ││
│  └─────────────────────┘    └──────────────────────────────┘│
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 SSE 连接 Composable

```typescript
// composables/useSseConnection.ts
interface SseOptions {
  url: string
  onMessage: (data: NotificationMessage) => void
  onError?: (error: Error) => void
  onConnect?: () => void
  onDisconnect?: () => void
  maxRetries?: number      // 最大重试次数，默认 10
  retryDelay?: number      // 初始重试延迟，默认 1000ms
}

interface SseConnection {
  status: 'connecting' | 'connected' | 'disconnected' | 'error'
  connect: () => void
  disconnect: () => void
}

export function useSseConnection(options: SseOptions): SseConnection
```

**重连策略（指数退避）：**
```
断开 → 等待 1s → 重连
失败 → 等待 2s → 重连
失败 → 等待 4s → 重连
...最大等待 30s
超过最大次数 → 停止重连，提示用户手动刷新
```

### 4.3 Notification Store

```typescript
// stores/notification.ts
export interface NotificationItem {
  notificationId: number
  title: string
  content: string
  type: 'SYSTEM' | 'OPERATION' | 'ALERT'
  severity: 'INFO' | 'WARNING' | 'ERROR' | 'SUCCESS'
  sourceRef?: string
  createdTime: string
  read: boolean
}

export const useNotificationStore = defineStore('notification', () => {
  // 状态
  const notifications = ref<NotificationItem[]>([])
  const unreadCount = ref(0)
  const sseStatus = ref<'connecting' | 'connected' | 'disconnected'>('disconnected')

  // 方法
  const connectSse = () => void            // 建立 SSE 连接
  const disconnectSse = () => void         // 断开连接
  const handleSseMessage = (msg) => void   // 处理 SSE 推送消息
  const fetchOfflineMessages = () => void  // 拉取离线消息
  const fetchUnreadCount = () => void      // 获取未读计数
  const markAsRead = (id) => void          // 标记单条已读
  const markAllAsRead = () => void         // 标记全部已读
  const fetchHistory = (params) => void    // 分页查询历史消息
})
```

### 4.4 Toast 弹窗逻辑

```typescript
// 消息接收时判断是否弹窗
const handleSseMessage = (msg: NotificationItem) => {
  // 添加到通知列表（按 notificationId 去重）
  if (!notifications.value.find(n => n.notificationId === msg.notificationId)) {
    notifications.value.unshift(msg)
    unreadCount.value++
  }

  // severity 为 WARNING 或 ERROR 时弹出 Toast
  if (msg.severity === 'WARNING' || msg.severity === 'ERROR') {
    ElMessage({
      type: msg.severity === 'ERROR' ? 'error' : 'warning',
      message: msg.title,
      duration: 3000,
      showClose: true,
      onClick: () => navigateToDetail(msg)
    })
  }
}
```

### 4.5 API 接口封装

```typescript
// api/notification.ts
export const getNotificationList = (params: QueryNotificationParams): Promise<NotificationListResult>

export const getUnreadCount = (): Promise<number>

export const markRead = (notificationId: number): Promise<void>

export const markAllRead = (): Promise<void>

export const getNotificationHistory = (params: QueryHistoryParams): Promise<PageResult<NotificationItem>>
```

### 4.6 i18n 国际化

```typescript
// locales/zh-cn.ts
notification: {
  title: '消息通知',
  markAllRead: '全部已读',
  noNotifications: '暂无消息',
  justNow: '刚刚',
  minutesAgo: '{n} 分钟前',
  hoursAgo: '{n} 小时前',
  daysAgo: '{n} 天前',
  viewAll: '查看全部',
  history: '历史消息',
  connected: '已连接',
  disconnected: '连接断开',
  reconnecting: '正在重连...',
  connectionError: '连接失败，请刷新页面',
  system: '系统通知',
  operation: '操作通知',
  alert: '告警通知'
}

// locales/en-us.ts
notification: {
  title: 'Notifications',
  markAllRead: 'Mark all read',
  noNotifications: 'No notifications',
  // ...对应英文
}
```

---

## 5. 消息来源集成点

### 5.1 业务场景与消息发送点

| 场景 | 消息类型 | severity | target | 触发时机 |
|------|----------|----------|--------|----------|
| 缓存同步完成 | OPERATION | SUCCESS/ERROR | USER | 用户发起同步后 |
| 路由同步完成 | OPERATION | SUCCESS/ERROR | USER | 路由变更推送后 |
| 配置推送完成 | OPERATION | SUCCESS/ERROR | USER | 配置下发后 |
| 网关实例上线 | ALERT | INFO | ALL | 实例状态变更 |
| 网关实例下线 | ALERT | WARNING | ALL | 实例心跳超时 |
| 渠道状态异常 | ALERT | ERROR | ALL | 渠道验证失败 |
| 数据同步任务完成 | OPERATION | SUCCESS/WARNING | USER | DataSyncService 执行后 |

### 5.2 NotificationPublishService 设计

```java
/**
 * 消息通知发布服务
 */
@Service
@Slf4j
public class NotificationPublishService {

    @Resource
    private SysNotificationMapper notificationMapper;

    @Resource
    private RedisClient redisClient;

    /**
     * 发送全局广播消息
     */
    public void broadcast(String title, String content,
                          String type, String severity,
                          String sourceRef) {
        SysNotificationDO notification = createNotification(
            title, content, type, severity,
            TargetTypeConstant.ALL, null, sourceRef
        );
        notificationMapper.insert(notification);
        publishToChannel(notification);
        log.info("[Notification] 广播消息已发送, title: {}", title);
    }

    /**
     * 发送定向用户消息
     */
    public void sendToUser(Integer userId, String title, String content,
                           String type, String severity,
                           String sourceRef) {
        SysNotificationDO notification = createNotification(
            title, content, type, severity,
            TargetTypeConstant.USER, userId, sourceRef
        );
        notificationMapper.insert(notification);
        incrementUnreadCount(userId);
        publishToChannel(notification);
        log.info("[Notification] 定向消息已发送, userId: {}, title: {}", userId, title);
    }

    private void publishToChannel(SysNotificationDO notification) {
        String channel = RedisKeyConstant.NOTIFICATION_CHANNEL;
        NotificationMsg msg = convertToMessage(notification);
        redisClient.publish(channel, JacksonUtil.toJson(msg));
    }

    private void incrementUnreadCount(Integer userId) {
        String key = RedisKeyConstant.NOTIFICATION_USER_UNREAD + userId;
        redisClient.increment(key);
        redisClient.expire(key, 7, TimeUnit.DAYS);
    }
}
```

### 5.3 集成示例

```java
// 在现有 Service 中注入 NotificationPublishService
@Service
public class DataSyncServiceImpl implements DataSyncService {

    @Resource
    private NotificationPublishService notificationPublishService;

    @Override
    public void syncChannelData(SyncChannelDataReq req) {
        Integer userId = getCurrentUserId();
        String syncTaskId = generateTaskId();

        try {
            // 执行同步逻辑...

            notificationPublishService.sendToUser(
                userId,
                "数据同步完成",
                "渠道数据已成功同步到所有网关实例",
                NotificationTypeConstant.OPERATION,
                NotificationSeverityConstant.SUCCESS,
                syncTaskId
            );
        } catch (Exception e) {
            log.error("[DataSync] 同步失败: {}", e.getMessage(), e);

            notificationPublishService.sendToUser(
                userId,
                "数据同步失败",
                "同步失败: " + e.getMessage(),
                NotificationTypeConstant.OPERATION,
                NotificationSeverityConstant.ERROR,
                syncTaskId
            );
        }
    }
}
```

### 5.4 消息过期策略

```java
public interface NotificationExpireConstant {
    Long DEFAULT_EXPIRE_DAYS = 7L;      // 默认7天过期
    Long ALERT_EXPIRE_HOURS = 24L;      // 告警类24小时过期
}
```

---

## 6. 错误处理与边界情况

### 6.1 SSE 连接异常处理

| 异常场景 | 处理策略 |
|----------|----------|
| 网络断开 | 前端自动重连（指数退避，最大10次） |
| 后端服务重启 | SSE 连接断开，前端重连，重连成功后拉取离线消息 |
| 用户 Token 过期 | SSE 返回 401，前端跳转登录页 |
| 连接超时（30秒无心跳） | 前端主动断开并重连 |
| 达到最大重连次数 | 前端显示连接状态为"已断开"，提示用户刷新页面 |

### 6.2 后端 SSE 异常处理

```java
// SseEmitter 超时设置
SseEmitter emitter = new SseEmitter(60_000L); // 60秒超时

emitter.onTimeout(() -> {
    log.warn("[SSE] 连接超时，userId: {}", userId);
    sseConnectionPool.remove(userId, emitter);
});

emitter.onCompletion(() -> {
    log.info("[SSE] 连接完成，userId: {}", userId);
    sseConnectionPool.remove(userId, emitter);
});

emitter.onError(e -> {
    log.error("[SSE] 连接异常，userId: {}", userId, e);
    sseConnectionPool.remove(userId, emitter);
});
```

### 6.3 并发与幂等处理

| 场景 | 处理方案 |
|------|----------|
| 用户多标签页连接 | 允许多个 SSE 连接，每个标签页独立接收消息 |
| 消息重复推送 | 前端按 notificationId 去重，避免重复展示 |
| 未读计数不一致 | 优先读取 Redis 缓存，缓存不存在时查数据库重新计算 |
| 标记已读并发冲突 | 数据库 unique key 保证，API 支持幂等调用 |

### 6.4 性能考虑

| 场景 | 优化方案 |
|------|----------|
| 消息量过大 | 前端通知中心只保留最近100条，历史消息分页查询 |
| 广播消息推送 | 异步推送，不阻塞业务主流程 |
| 未读计数查询 | Redis 缓存计数，设置 TTL，定期同步数据库 |
| 历史消息查询 | 添加 created_time 索引，支持分页，默认按时间倒序 |

### 6.5 安全考虑

| 场景 | 安全措施 |
|------|----------|
| SSE 连接鉴权 | 必须携带有效 Token，后端验证后才建立连接 |
| 定向推送验证 | 推送前验证目标用户是否存在，避免无效推送 |
| 消息内容过滤 | 入库前校验内容长度，防止超长消息 |
| API 权限控制 | 标记已读、历史查询等接口需要登录态校验 |

---

## 7. 实现计划概要

### 7.1 后端实现模块

1. **数据库层**：创建表结构、生成实体类和 Mapper
2. **核心组件**：SseConnectionPool、NotificationPublishService、NotificationRedisListener
3. **Controller层**：SSE连接端点、消息查询接口、已读标记接口
4. **Service层**：消息CRUD、未读计数管理、历史查询
5. **集成点**：DataSyncService、ConfigPushService、GatewayInstanceService 等场景集成

### 7.2 前端实现模块

1. **Composable**：useSseConnection.ts（SSE连接管理）
2. **Store增强**：notification.ts（消息状态管理）
3. **API封装**：notification.ts（接口调用）
4. **组件优化**：NotificationCenter（右上角通知面板）
5. **i18n扩展**：中英文国际化文案
6. **集成**：MainLayout 集成通知中心、App.vue 初始化 SSE 连接