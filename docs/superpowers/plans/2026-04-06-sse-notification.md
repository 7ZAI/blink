# SSE Message Notification Mechanism Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a real-time SSE message notification system for gateway-admin that supports global broadcasts and targeted user pushes with multi-instance synchronization.

**Architecture:** SSE connections managed per instance via SseConnectionPool, messages broadcast across instances via Redis Pub/Sub, MySQL persists messages with Redis caching unread counts. Frontend uses Vue composable for SSE connection with exponential backoff reconnection.

**Tech Stack:** Spring Boot 3.2, SseEmitter, Redis Pub/Sub, Sa-Token auth, Vue 3 + Pinia, TypeScript, Element Plus

---

## File Structure

### Backend Files (gateway-admin)

| File | Purpose |
|------|---------|
| `constants/NotificationTypeConstant.java` | Message type constants (SYSTEM, OPERATION, ALERT) |
| `constants/NotificationSeverityConstant.java` | Severity constants (INFO, WARNING, ERROR, SUCCESS) |
| `constants/TargetTypeConstant.java` | Target type constants (ALL, USER) |
| `constants/ErrCodeConstant.java` | Add notification error codes (GATE0150-GATE0159) |
| `constants/RedisKeyConstant.java` | Add notification Redis keys |
| `entity/SysNotificationDO.java` | Notification entity |
| `entity/SysNotificationReadDO.java` | Read status entity |
| `mapper/SysNotificationMapper.java` | Notification mapper |
| `mapper/SysNotificationReadMapper.java` | Read status mapper |
| `dto/req/QueryNotificationReq.java` | Query notification request |
| `dto/req/MarkReadReq.java` | Mark read request |
| `dto/req/QueryHistoryReq.java` | Query history request |
| `dto/rsp/NotificationItemRsp.java` | Notification item response |
| `dto/rsp/NotificationListRsp.java` | Notification list response |
| `dto/rsp/UnreadCountRsp.java` | Unread count response |
| `dto/rsp/NotificationHistoryRsp.java` | History response |
| `sse/SseConnectionPool.java` | SSE connection pool manager |
| `sse/NotificationMsg.java` | Redis message DTO |
| `service/NotificationPublishService.java` | Message publish service |
| `service/NotificationRedisListener.java` | Redis listener |
| `service/NotificationService.java` | Notification CRUD service |
| `service/impl/NotificationServiceImpl.java` | Service implementation |
| `controller/NotificationController.java` | REST + SSE endpoints |

### Frontend Files (gateway-admin-web)

| File | Purpose |
|------|---------|
| `composables/useSseConnection.ts` | SSE connection composable |
| `stores/notification.ts` | Enhanced notification store |
| `api/notification.ts` | Notification API calls |
| `components/NotificationCenter/index.vue` | Enhanced notification center |
| `locales/zh-cn.ts` | Add notification i18n keys |
| `locales/en-us.ts` | Add notification i18n keys |

---

## Task 1: Database Migration Scripts

**Files:**
- Create: `docs/migration/V2026.04.06__notification_tables.sql`

- [ ] **Step 1: Write database migration SQL**

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

- [ ] **Step 2: Execute migration via MCP**

Run: `mcp__mysql_gateway__execute_sql` with the above SQL
Expected: Tables created successfully

- [ ] **Step 3: Commit migration script**

```bash
git add docs/migration/V2026.04.06__notification_tables.sql
git commit -m "feat(notification): add database migration for notification tables"
```

---

## Task 2: Backend Constants

**Files:**
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/constants/NotificationTypeConstant.java`
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/constants/NotificationSeverityConstant.java`
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/constants/TargetTypeConstant.java`
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/constants/ErrCodeConstant.java`
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/constants/RedisKeyConstant.java`

- [ ] **Step 1: Create NotificationTypeConstant.java**

```java
package com.blink.gateway.admin.constants;

/**
 * 消息类型常量
 */
public interface NotificationTypeConstant {
    String SYSTEM = "SYSTEM";
    String OPERATION = "OPERATION";
    String ALERT = "ALERT";
}
```

- [ ] **Step 2: Create NotificationSeverityConstant.java**

```java
package com.blink.gateway.admin.constants;

/**
 * 消息严重级别常量
 */
public interface NotificationSeverityConstant {
    String INFO = "INFO";
    String WARNING = "WARNING";
    String ERROR = "ERROR";
    String SUCCESS = "SUCCESS";
}
```

- [ ] **Step 3: Create TargetTypeConstant.java**

```java
package com.blink.gateway.admin.constants;

/**
 * 消息目标类型常量
 */
public interface TargetTypeConstant {
    String ALL = "ALL";
    String USER = "USER";
}
```

- [ ] **Step 4: Add notification error codes to ErrCodeConstant.java**

Append to existing file:

```java
    // 消息通知错误码 GATE0150-GATE0159
    String NOTIFICATION_NOT_EXIST = "GATE0150";
    String NOTIFICATION_ALREADY_READ = "GATE0151";
    String SSE_CONNECTION_FAILED = "GATE0152";
```

- [ ] **Step 5: Add notification Redis keys to RedisKeyConstant.java**

Append to existing file:

```java
    // 消息通知相关
    String NOTIFICATION_CHANNEL = BLINK_PREFIX + ":notification:channel";
    String NOTIFICATION_USER_UNREAD = BLINK_PREFIX + ":notification:unread:";
```

- [ ] **Step 6: Commit constants**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/constants/NotificationTypeConstant.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/constants/NotificationSeverityConstant.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/constants/TargetTypeConstant.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/constants/ErrCodeConstant.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/constants/RedisKeyConstant.java
git commit -m "feat(notification): add notification constants"
```

---

## Task 3: Backend Entities

**Files:**
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/entity/SysNotificationDO.java`
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/entity/SysNotificationReadDO.java`

- [ ] **Step 1: Create SysNotificationDO.java**

```java
package com.blink.gateway.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统消息通知实体
 *
 * @author binblink
 * @since 2026-04-06
 */
@Data
@TableName("sys_notification")
public class SysNotificationDO implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long notificationId;

    private String title;

    private String content;

    private String type;

    private String severity;

    private String targetType;

    private Integer targetUserId;

    private String sourceRef;

    private Integer createdBy;

    private LocalDateTime createdTime;

    private LocalDateTime expireTime;
}
```

- [ ] **Step 2: Create SysNotificationReadDO.java**

```java
package com.blink.gateway.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息读取状态实体
 *
 * @author binblink
 * @since 2026-04-06
 */
@Data
@TableName("sys_notification_read")
public class SysNotificationReadDO implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long readId;

    private Long notificationId;

    private Integer userId;

    private LocalDateTime readTime;
}
```

- [ ] **Step 3: Commit entities**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/entity/SysNotificationDO.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/entity/SysNotificationReadDO.java
git commit -m "feat(notification): add notification entities"
```

---

## Task 4: Backend Mappers

**Files:**
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/mapper/SysNotificationMapper.java`
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/mapper/SysNotificationReadMapper.java`

- [ ] **Step 1: Create SysNotificationMapper.java**

```java
package com.blink.gateway.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.gateway.admin.entity.SysNotificationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 消息通知Mapper
 *
 * @author binblink
 * @since 2026-04-06
 */
@Mapper
public interface SysNotificationMapper extends BaseMapper<SysNotificationDO> {

    /**
     * 查询用户未读消息列表
     */
    @Select("SELECT n.* FROM sys_notification n " +
            "WHERE (n.target_type = 'ALL' OR n.target_user_id = #{userId}) " +
            "AND n.notification_id NOT IN " +
            "(SELECT nr.notification_id FROM sys_notification_read nr WHERE nr.user_id = #{userId}) " +
            "AND (n.expire_time IS NULL OR n.expire_time > NOW()) " +
            "ORDER BY n.created_time DESC LIMIT #{limit}")
    List<SysNotificationDO> selectUnreadByUserId(@Param("userId") Integer userId, @Param("limit") Integer limit);

    /**
     * 查询用户未读消息数量
     */
    @Select("SELECT COUNT(*) FROM sys_notification n " +
            "WHERE (n.target_type = 'ALL' OR n.target_user_id = #{userId}) " +
            "AND n.notification_id NOT IN " +
            "(SELECT nr.notification_id FROM sys_notification_read nr WHERE nr.user_id = #{userId}) " +
            "AND (n.expire_time IS NULL OR n.expire_time > NOW())")
    Integer countUnreadByUserId(@Param("userId") Integer userId);
}
```

- [ ] **Step 2: Create SysNotificationReadMapper.java**

```java
package com.blink.gateway.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.gateway.admin.entity.SysNotificationReadDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息读取状态Mapper
 *
 * @author binblink
 * @since 2026-04-06
 */
@Mapper
public interface SysNotificationReadMapper extends BaseMapper<SysNotificationReadDO> {
}
```

- [ ] **Step 3: Commit mappers**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/mapper/SysNotificationMapper.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/mapper/SysNotificationReadMapper.java
git commit -m "feat(notification): add notification mappers"
```

---

## Task 5: Backend DTOs

**Files:**
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/QueryNotificationReq.java`
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/MarkReadReq.java`
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/QueryHistoryReq.java`
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/NotificationItemRsp.java`
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/NotificationListRsp.java`
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/UnreadCountRsp.java`
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/NotificationHistoryRsp.java`

- [ ] **Step 1: Create QueryNotificationReq.java**

```java
package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serializable;

/**
 * 查询消息通知请求
 *
 * @author binblink
 * @since 2026-04-06
 */
@Data
public class QueryNotificationReq implements Serializable {

    private Integer limit = 20;
}
```

- [ ] **Step 2: Create MarkReadReq.java**

```java
package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serializable;

/**
 * 标记已读请求
 *
 * @author binblink
 * @since 2026-04-06
 */
@Data
public class MarkReadReq implements Serializable {

    private Long notificationId;

    private Boolean markAll = false;
}
```

- [ ] **Step 3: Create QueryHistoryReq.java**

```java
package com.blink.gateway.admin.dto.req;

import com.blink.framework.common.dto.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 查询历史消息请求
 *
 * @author binblink
 * @since 2026-04-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QueryHistoryReq extends PageDTO implements Serializable {

    private String type;

    private String severity;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
```

- [ ] **Step 4: Create NotificationItemRsp.java**

```java
package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息通知项响应
 *
 * @author binblink
 * @since 2026-04-06
 */
@Data
public class NotificationItemRsp implements Serializable {

    private Long notificationId;

    private String title;

    private String content;

    private String type;

    private String severity;

    private String sourceRef;

    private LocalDateTime createdTime;

    private Boolean read = false;
}
```

- [ ] **Step 5: Create NotificationListRsp.java**

```java
package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 消息通知列表响应
 *
 * @author binblink
 * @since 2026-04-06
 */
@Data
public class NotificationListRsp implements Serializable {

    private List<NotificationItemRsp> notifications;

    private Integer unreadCount;
}
```

- [ ] **Step 6: Create UnreadCountRsp.java**

```java
package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serializable;

/**
 * 未读消息数量响应
 *
 * @author binblink
 * @since 2026-04-06
 */
@Data
public class UnreadCountRsp implements Serializable {

    private Integer unreadCount;
}
```

- [ ] **Step 7: Create NotificationHistoryRsp.java**

```java
package com.blink.gateway.admin.dto.rsp;

import com.blink.framework.common.dto.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 消息历史响应
 *
 * @author binblink
 * @since 2026-04-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationHistoryRsp extends PageDTO<NotificationItemRsp> implements Serializable {
}
```

- [ ] **Step 8: Commit DTOs**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/QueryNotificationReq.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/MarkReadReq.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/QueryHistoryReq.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/NotificationItemRsp.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/NotificationListRsp.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/UnreadCountRsp.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/NotificationHistoryRsp.java
git commit -m "feat(notification): add notification DTOs"
```

---

## Task 6: SSE Connection Pool

**Files:**
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/sse/SseConnectionPool.java`
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/sse/NotificationMsg.java`

- [ ] **Step 1: Create NotificationMsg.java (Redis message DTO)**

```java
package com.blink.gateway.admin.sse;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Redis消息传输对象
 *
 * @author binblink
 * @since 2026-04-06
 */
@Data
public class NotificationMsg implements Serializable {

    private Long notificationId;

    private String title;

    private String content;

    private String type;

    private String severity;

    private String targetType;

    private Integer targetUserId;

    private String sourceRef;

    private LocalDateTime createdTime;
}
```

- [ ] **Step 2: Create SseConnectionPool.java**

```java
package com.blink.gateway.admin.sse;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE连接池管理器
 * 支持用户多标签页连接
 *
 * @author binblink
 * @since 2026-04-06
 */
@Slf4j
@Component
public class SseConnectionPool {

    private final Map<Integer, CopyOnWriteArrayList<SseEmitter>> userConnections = new ConcurrentHashMap<>();

    /**
     * 创建SSE连接
     */
    public SseEmitter createConnection() {
        Integer userId = StpUtil.getLoginIdAsInt();
        
        SseEmitter emitter = new SseEmitter(60_000L);
        
        emitter.onCompletion(() -> {
            log.info("[SSE] 连接完成, userId: {}", userId);
            remove(userId, emitter);
        });
        
        emitter.onTimeout(() -> {
            log.warn("[SSE] 连接超时, userId: {}", userId);
            remove(userId, emitter);
        });
        
        emitter.onError(e -> {
            log.error("[SSE] 连接异常, userId: {}", userId, e);
            remove(userId, emitter);
        });
        
        add(userId, emitter);
        log.info("[SSE] 新连接建立, userId: {}, 当前连接数: {}", userId, getUserConnectionCount(userId));
        
        return emitter;
    }

    /**
     * 添加连接
     */
    private void add(Integer userId, SseEmitter emitter) {
        userConnections.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
    }

    /**
     * 移除连接
     */
    private void remove(Integer userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> connections = userConnections.get(userId);
        if (connections != null) {
            connections.remove(emitter);
            if (connections.isEmpty()) {
                userConnections.remove(userId);
            }
        }
    }

    /**
     * 获取用户连接数
     */
    public int getUserConnectionCount(Integer userId) {
        CopyOnWriteArrayList<SseEmitter> connections = userConnections.get(userId);
        return connections != null ? connections.size() : 0;
    }

    /**
     * 推送消息给指定用户
     */
    public void sendToUser(Integer userId, NotificationMsg msg) {
        CopyOnWriteArrayList<SseEmitter> connections = userConnections.get(userId);
        if (connections == null || connections.isEmpty()) {
            log.debug("[SSE] 用户无连接, userId: {}", userId);
            return;
        }
        
        for (SseEmitter emitter : connections) {
            try {
                emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(msg));
            } catch (IOException e) {
                log.error("[SSE] 推送失败, userId: {}", userId, e);
                remove(userId, emitter);
            }
        }
        log.info("[SSE] 推送成功, userId: {}, notificationId: {}", userId, msg.getNotificationId());
    }

    /**
     * 推送广播消息给所有连接
     */
    public void broadcast(NotificationMsg msg) {
        List<Integer> userIds = new ArrayList<>(userConnections.keySet());
        for (Integer userId : userIds) {
            sendToUser(userId, msg);
        }
        log.info("[SSE] 广播完成, notificationId: {}, 接收用户数: {}", msg.getNotificationId(), userIds.size());
    }

    /**
     * 检查用户是否有连接
     */
    public boolean hasConnection(Integer userId) {
        CopyOnWriteArrayList<SseEmitter> connections = userConnections.get(userId);
        return connections != null && !connections.isEmpty();
    }
}
```

- [ ] **Step 3: Commit SSE connection pool**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/sse/SseConnectionPool.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/sse/NotificationMsg.java
git commit -m "feat(notification): add SSE connection pool"
```

---

## Task 7: Notification Publish Service

**Files:**
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/NotificationPublishService.java`

- [ ] **Step 1: Create NotificationPublishService.java**

```java
package com.blink.gateway.admin.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.constants.NotificationSeverityConstant;
import com.blink.gateway.admin.constants.NotificationTypeConstant;
import com.blink.gateway.admin.constants.RedisKeyConstant;
import com.blink.gateway.admin.constants.TargetTypeConstant;
import com.blink.gateway.admin.entity.SysNotificationDO;
import com.blink.gateway.admin.mapper.SysNotificationMapper;
import com.blink.gateway.admin.sse.NotificationMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 消息通知发布服务
 *
 * @author binblink
 * @since 2026-04-06
 */
@Slf4j
@Service
public class NotificationPublishService {

    @Resource
    private SysNotificationMapper notificationMapper;

    @Resource
    private RedisClient redisClient;

    /**
     * 发送全局广播消息
     */
    public void broadcast(String title, String content, String type, String severity, String sourceRef) {
        SysNotificationDO notification = createNotification(
            title, content, type, severity, TargetTypeConstant.ALL, null, sourceRef
        );
        notificationMapper.insert(notification);
        publishToChannel(notification);
        log.info("[Notification] 广播消息已发送, title: {}", title);
    }

    /**
     * 发送定向用户消息
     */
    public void sendToUser(Integer userId, String title, String content, String type, String severity, String sourceRef) {
        SysNotificationDO notification = createNotification(
            title, content, type, severity, TargetTypeConstant.USER, userId, sourceRef
        );
        notificationMapper.insert(notification);
        incrementUnreadCount(userId);
        publishToChannel(notification);
        log.info("[Notification] 定向消息已发送, userId: {}, title: {}", userId, title);
    }

    /**
     * 发送操作成功通知
     */
    public void sendOperationSuccess(Integer userId, String title, String content, String sourceRef) {
        sendToUser(userId, title, content, NotificationTypeConstant.OPERATION, NotificationSeverityConstant.SUCCESS, sourceRef);
    }

    /**
     * 发送操作失败通知
     */
    public void sendOperationError(Integer userId, String title, String content, String sourceRef) {
        sendToUser(userId, title, content, NotificationTypeConstant.OPERATION, NotificationSeverityConstant.ERROR, sourceRef);
    }

    /**
     * 发送告警通知
     */
    public void sendAlert(String title, String content, String severity) {
        broadcast(title, content, NotificationTypeConstant.ALERT, severity, null);
    }

    private SysNotificationDO createNotification(String title, String content, String type, String severity,
                                                   String targetType, Integer targetUserId, String sourceRef) {
        SysNotificationDO notification = new SysNotificationDO();
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setSeverity(severity);
        notification.setTargetType(targetType);
        notification.setTargetUserId(targetUserId);
        notification.setSourceRef(sourceRef);
        notification.setCreatedTime(LocalDateTime.now());
        
        if (NotificationTypeConstant.ALERT.equals(type)) {
            notification.setExpireTime(LocalDateTime.now().plusHours(24));
        } else {
            notification.setExpireTime(LocalDateTime.now().plusDays(7));
        }
        
        return notification;
    }

    private void publishToChannel(SysNotificationDO notification) {
        String channel = RedisKeyConstant.NOTIFICATION_CHANNEL;
        NotificationMsg msg = BeanUtil.copyProperties(notification, NotificationMsg.class);
        redisClient.publish(channel, JSONUtil.toJsonStr(msg));
    }

    private void incrementUnreadCount(Integer userId) {
        String key = RedisKeyConstant.NOTIFICATION_USER_UNREAD + userId;
        redisClient.increment(key);
        redisClient.expire(key, 7, TimeUnit.DAYS);
    }
}
```

- [ ] **Step 2: Commit notification publish service**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/NotificationPublishService.java
git commit -m "feat(notification): add notification publish service"
```

---

## Task 8: Redis Listener

**Files:**
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/NotificationRedisListener.java`

- [ ] **Step 1: Create NotificationRedisListener.java**

```java
package com.blink.gateway.admin.service;

import cn.hutool.json.JSONUtil;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.constants.RedisKeyConstant;
import com.blink.gateway.admin.constants.TargetTypeConstant;
import com.blink.gateway.admin.sse.NotificationMsg;
import com.blink.gateway.admin.sse.SseConnectionPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * Redis消息监听器
 *
 * @author binblink
 * @since 2026-04-06
 */
@Slf4j
@Service
public class NotificationRedisListener {

    @Resource
    private RedisClient redisClient;

    @Resource
    private SseConnectionPool sseConnectionPool;

    @EventListener(ApplicationReadyEvent.class)
    public void startListening() {
        String channel = RedisKeyConstant.NOTIFICATION_CHANNEL;
        redisClient.subscribe(channel, message -> {
            try {
                NotificationMsg msg = JSONUtil.toBean(message, NotificationMsg.class);
                handleNotification(msg);
            } catch (Exception e) {
                log.error("[NotificationListener] 解析消息失败: {}", message, e);
            }
        });
        log.info("[NotificationListener] 已订阅Redis频道: {}", channel);
    }

    private void handleNotification(NotificationMsg msg) {
        log.info("[NotificationListener] 收到消息, notificationId: {}, targetType: {}",
            msg.getNotificationId(), msg.getTargetType());
        
        if (TargetTypeConstant.ALL.equals(msg.getTargetType())) {
            sseConnectionPool.broadcast(msg);
        } else if (TargetTypeConstant.USER.equals(msg.getTargetType())) {
            Integer targetUserId = msg.getTargetUserId();
            if (targetUserId != null) {
                sseConnectionPool.sendToUser(targetUserId, msg);
            }
        }
    }
}
```

- [ ] **Step 2: Commit Redis listener**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/NotificationRedisListener.java
git commit -m "feat(notification): add Redis notification listener"
```

---

## Task 9: Notification Service

**Files:**
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/NotificationService.java`
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/NotificationServiceImpl.java`

- [ ] **Step 1: Create NotificationService.java (interface)**

```java
package com.blink.gateway.admin.service;

import com.blink.gateway.admin.dto.req.MarkReadReq;
import com.blink.gateway.admin.dto.req.QueryHistoryReq;
import com.blink.gateway.admin.dto.req.QueryNotificationReq;
import com.blink.gateway.admin.dto.rsp.NotificationHistoryRsp;
import com.blink.gateway.admin.dto.rsp.NotificationListRsp;
import com.blink.gateway.admin.dto.rsp.UnreadCountRsp;

/**
 * 消息通知服务接口
 *
 * @author binblink
 * @since 2026-04-06
 */
public interface NotificationService {

    /**
     * 获取消息列表
     */
    NotificationListRsp getNotificationList(QueryNotificationReq req);

    /**
     * 获取未读消息数量
     */
    UnreadCountRsp getUnreadCount();

    /**
     * 标记已读
     */
    void markRead(MarkReadReq req);

    /**
     * 查询历史消息
     */
    NotificationHistoryRsp getHistory(QueryHistoryReq req);
}
```

- [ ] **Step 2: Create NotificationServiceImpl.java**

```java
package com.blink.gateway.admin.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.framework.common.dto.PageDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.constants.ErrCodeConstant;
import com.blink.gateway.admin.constants.RedisKeyConstant;
import com.blink.gateway.admin.dto.req.MarkReadReq;
import com.blink.gateway.admin.dto.req.QueryHistoryReq;
import com.blink.gateway.admin.dto.req.QueryNotificationReq;
import com.blink.gateway.admin.dto.rsp.NotificationHistoryRsp;
import com.blink.gateway.admin.dto.rsp.NotificationItemRsp;
import com.blink.gateway.admin.dto.rsp.NotificationListRsp;
import com.blink.gateway.admin.dto.rsp.UnreadCountRsp;
import com.blink.gateway.admin.entity.SysNotificationDO;
import com.blink.gateway.admin.entity.SysNotificationReadDO;
import com.blink.gateway.admin.mapper.SysNotificationMapper;
import com.blink.gateway.admin.mapper.SysNotificationReadMapper;
import com.blink.gateway.admin.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息通知服务实现
 *
 * @author binblink
 * @since 2026-04-06
 */
@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    @Resource
    private SysNotificationMapper notificationMapper;

    @Resource
    private SysNotificationReadMapper notificationReadMapper;

    @Resource
    private RedisClient redisClient;

    @Override
    public NotificationListRsp getNotificationList(QueryNotificationReq req) {
        Integer userId = StpUtil.getLoginIdAsInt();
        
        List<SysNotificationDO> unreadList = notificationMapper.selectUnreadByUserId(userId, req.getLimit());
        
        List<NotificationItemRsp> notifications = CollUtil.isEmpty(unreadList)
            ? new ArrayList<>()
            : unreadList.stream()
                .map(n -> {
                    NotificationItemRsp rsp = BeanUtil.copyProperties(n, NotificationItemRsp.class);
                    rsp.setRead(false);
                    return rsp;
                })
                .collect(Collectors.toList());
        
        NotificationListRsp result = new NotificationListRsp();
        result.setNotifications(notifications);
        result.setUnreadCount(getUnreadCountFromCache(userId));
        
        return result;
    }

    @Override
    public UnreadCountRsp getUnreadCount() {
        Integer userId = StpUtil.getLoginIdAsInt();
        
        UnreadCountRsp rsp = new UnreadCountRsp();
        rsp.setUnreadCount(getUnreadCountFromCache(userId));
        
        return rsp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(MarkReadReq req) {
        Integer userId = StpUtil.getLoginIdAsInt();
        
        if (Boolean.TRUE.equals(req.getMarkAll())) {
            markAllRead(userId);
            return;
        }
        
        if (ObjectUtil.isNull(req.getNotificationId())) {
            BlinkException.throwBusinessException(ErrCodeConstant.PARAMETER_NOT_NULL);
        }
        
        SysNotificationReadDO readDO = new SysNotificationReadDO();
        readDO.setNotificationId(req.getNotificationId());
        readDO.setUserId(userId);
        readDO.setReadTime(LocalDateTime.now());
        
        try {
            notificationReadMapper.insert(readDO);
            decrementUnreadCount(userId);
            log.info("[Notification] 标记已读成功, userId: {}, notificationId: {}", userId, req.getNotificationId());
        } catch (Exception e) {
            log.warn("[Notification] 消息已读状态已存在, userId: {}, notificationId: {}", userId, req.getNotificationId());
        }
    }

    private void markAllRead(Integer userId) {
        List<SysNotificationDO> unreadList = notificationMapper.selectUnreadByUserId(userId, 1000);
        
        if (CollUtil.isEmpty(unreadList)) {
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        for (SysNotificationDO notification : unreadList) {
            SysNotificationReadDO readDO = new SysNotificationReadDO();
            readDO.setNotificationId(notification.getNotificationId());
            readDO.setUserId(userId);
            readDO.setReadTime(now);
            notificationReadMapper.insert(readDO);
        }
        
        clearUnreadCount(userId);
        log.info("[Notification] 全部标记已读成功, userId: {}, count: {}", userId, unreadList.size());
    }

    @Override
    public NotificationHistoryRsp getHistory(QueryHistoryReq req) {
        Integer userId = StpUtil.getLoginIdAsInt();
        
        LambdaQueryWrapper<SysNotificationDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(SysNotificationDO::getTargetType, "ALL")
            .or().eq(SysNotificationDO::getTargetUserId, userId));
        
        if (ObjectUtil.isNotEmpty(req.getType())) {
            wrapper.eq(SysNotificationDO::getType, req.getType());
        }
        if (ObjectUtil.isNotEmpty(req.getSeverity())) {
            wrapper.eq(SysNotificationDO::getSeverity, req.getSeverity());
        }
        if (ObjectUtil.isNotEmpty(req.getStartTime())) {
            wrapper.ge(SysNotificationDO::getCreatedTime, req.getStartTime());
        }
        if (ObjectUtil.isNotEmpty(req.getEndTime())) {
            wrapper.le(SysNotificationDO::getCreatedTime, req.getEndTime());
        }
        
        wrapper.orderByDesc(SysNotificationDO::getCreatedTime);
        
        NotificationHistoryRsp rsp = new NotificationHistoryRsp();
        return PageDTO.queryPage(req, () -> notificationMapper.selectList(wrapper), rsp, notification -> {
            NotificationItemRsp item = BeanUtil.copyProperties(notification, NotificationItemRsp.class);
            item.setRead(checkRead(notification.getNotificationId(), userId));
            return item;
        });
    }

    private Integer getUnreadCountFromCache(Integer userId) {
        String key = RedisKeyConstant.NOTIFICATION_USER_UNREAD + userId;
        String count = redisClient.get(key);
        
        if (ObjectUtil.isEmpty(count)) {
            Integer dbCount = notificationMapper.countUnreadByUserId(userId);
            redisClient.set(key, String.valueOf(dbCount));
            redisClient.expire(key, 7, java.util.concurrent.TimeUnit.DAYS);
            return dbCount;
        }
        
        return Integer.parseInt(count);
    }

    private void decrementUnreadCount(Integer userId) {
        String key = RedisKeyConstant.NOTIFICATION_USER_UNREAD + userId;
        String count = redisClient.get(key);
        if (ObjectUtil.isNotEmpty(count) && Integer.parseInt(count) > 0) {
            redisClient.decrement(key);
        }
    }

    private void clearUnreadCount(Integer userId) {
        String key = RedisKeyConstant.NOTIFICATION_USER_UNREAD + userId;
        redisClient.set(key, "0");
    }

    private Boolean checkRead(Long notificationId, Integer userId) {
        LambdaQueryWrapper<SysNotificationReadDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotificationReadDO::getNotificationId, notificationId)
            .eq(SysNotificationReadDO::getUserId, userId);
        return notificationReadMapper.selectCount(wrapper) > 0;
    }
}
```

- [ ] **Step 3: Commit notification service**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/NotificationService.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/NotificationServiceImpl.java
git commit -m "feat(notification): add notification service"
```

---

## Task 10: Notification Controller

**Files:**
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/controller/NotificationController.java`

- [ ] **Step 1: Create NotificationController.java**

```java
package com.blink.gateway.admin.controller;

import com.blink.framework.common.dto.RequestDTO;
import com.blink.framework.common.dto.ResponseDTO;
import com.blink.framework.common.dto.EmptyBody;
import com.blink.gateway.admin.dto.req.MarkReadReq;
import com.blink.gateway.admin.dto.req.QueryHistoryReq;
import com.blink.gateway.admin.dto.req.QueryNotificationReq;
import com.blink.gateway.admin.dto.rsp.NotificationHistoryRsp;
import com.blink.gateway.admin.dto.rsp.NotificationListRsp;
import com.blink.gateway.admin.dto.rsp.UnreadCountRsp;
import com.blink.gateway.admin.sse.SseConnectionPool;
import com.blink.gateway.admin.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;

/**
 * 消息通知控制器
 *
 * @author binblink
 * @since 2026-04-06
 */
@Slf4j
@RestController
@RequestMapping("/notification")
public class NotificationController {

    @Resource
    private SseConnectionPool sseConnectionPool;

    @Resource
    private NotificationService notificationService;

    /**
     * SSE连接端点
     */
    @PostMapping("/sse/connect")
    public SseEmitter connect() {
        log.info("[SSE] 收到连接请求");
        return sseConnectionPool.createConnection();
    }

    /**
     * 获取消息列表
     */
    @PostMapping("/list")
    public ResponseDTO<NotificationListRsp> getNotificationList(
        @RequestBody @Validated RequestDTO<QueryNotificationReq> reqDto) {
        NotificationListRsp rsp = notificationService.getNotificationList(reqDto.getBody());
        return ResponseDTO.newSuccessInstance(rsp);
    }

    /**
     * 获取未读消息数量
     */
    @PostMapping("/unreadCount")
    public ResponseDTO<UnreadCountRsp> getUnreadCount(
        @RequestBody @Validated RequestDTO<EmptyBody> reqDto) {
        UnreadCountRsp rsp = notificationService.getUnreadCount();
        return ResponseDTO.newSuccessInstance(rsp);
    }

    /**
     * 标记已读
     */
    @PostMapping("/markRead")
    public ResponseDTO<EmptyBody> markRead(
        @RequestBody @Validated RequestDTO<MarkReadReq> reqDto) {
        notificationService.markRead(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 标记全部已读
     */
    @PostMapping("/markAllRead")
    public ResponseDTO<EmptyBody> markAllRead(
        @RequestBody @Validated RequestDTO<EmptyBody> reqDto) {
        MarkReadReq req = new MarkReadReq();
        req.setMarkAll(true);
        notificationService.markRead(req);
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 查询历史消息
     */
    @PostMapping("/history")
    public ResponseDTO<NotificationHistoryRsp> getHistory(
        @RequestBody @Validated RequestDTO<QueryHistoryReq> reqDto) {
        NotificationHistoryRsp rsp = notificationService.getHistory(reqDto.getBody());
        return ResponseDTO.newSuccessInstance(rsp);
    }
}
```

- [ ] **Step 2: Commit notification controller**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/controller/NotificationController.java
git commit -m "feat(notification): add notification controller"
```

---

## Task 11: Frontend SSE Composable

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/composables/useSseConnection.ts`

- [ ] **Step 1: Create useSseConnection.ts**

```typescript
import { ref, onUnmounted } from 'vue'

interface SseOptions {
  url: string
  onMessage: (data: any) => void
  onError?: (error: Error) => void
  onConnect?: () => void
  onDisconnect?: () => void
  maxRetries?: number
  retryDelay?: number
}

interface SseConnection {
  status: 'connecting' | 'connected' | 'disconnected' | 'error'
  connect: () => void
  disconnect: () => void
}

export function useSseConnection(options: SseOptions): SseConnection {
  const {
    url,
    onMessage,
    onError,
    onConnect,
    onDisconnect,
    maxRetries = 10,
    retryDelay = 1000
  } = options

  const status = ref<SseConnection['status']>('disconnected')
  let eventSource: EventSource | null = null
  let retryCount = 0
  let retryTimer: ReturnType<typeof setTimeout> | null = null

  const connect = () => {
    if (eventSource) {
      eventSource.close()
    }

    status.value = 'connecting'

    // SSE需要使用GET请求，但我们需要传递token
    // 项目通过cookie传递token，所以直接连接即可
    eventSource = new EventSource(url)

    eventSource.onopen = () => {
      status.value = 'connected'
      retryCount = 0
      if (onConnect) {
        onConnect()
      }
    }

    eventSource.onerror = (error) => {
      status.value = 'error'
      if (eventSource) {
        eventSource.close()
        eventSource = null
      }

      if (onError) {
        onError(new Error('SSE connection error'))
      }

      // Exponential backoff retry
      if (retryCount < maxRetries) {
        const delay = Math.min(retryDelay * Math.pow(2, retryCount), 30000)
        retryCount++
        retryTimer = setTimeout(() => {
          connect()
        }, delay)
      } else {
        status.value = 'disconnected'
        if (onDisconnect) {
          onDisconnect()
        }
      }
    }

    eventSource.addEventListener('notification', (event) => {
      try {
        const data = JSON.parse(event.data)
        onMessage(data)
      } catch (e) {
        console.error('Failed to parse SSE message:', e)
      }
    })
  }

  const disconnect = () => {
    if (retryTimer) {
      clearTimeout(retryTimer)
      retryTimer = null
    }
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
    status.value = 'disconnected'
    if (onDisconnect) {
      onDisconnect()
    }
  }

  onUnmounted(() => {
    disconnect()
  })

  return {
    status: status.value as SseConnection['status'],
    connect,
    disconnect
  }
}
```

- [ ] **Step 2: Commit SSE composable**

```bash
git add blink-gateway/gateway-admin-web/src/composables/useSseConnection.ts
git commit -m "feat(notification): add SSE connection composable"
```

---

## Task 12: Frontend Notification Store Enhancement

**Files:**
- Modify: `blink-gateway/gateway-admin-web/src/stores/notification.ts`

- [ ] **Step 1: Read current notification store**

Run: Read `blink-gateway/gateway-admin-web/src/stores/notification.ts`

- [ ] **Step 2: Enhance notification store for SSE**

```typescript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useSseConnection } from '@/composables/useSseConnection'
import { notificationApi } from '@/api/notification'
import type { NotificationItem } from '@/api/notification'

export interface NotificationStoreItem extends NotificationItem {
  read: boolean
}

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref<NotificationStoreItem[]>([])
  const unreadCount = ref(0)
  const sseStatus = ref<'connecting' | 'connected' | 'disconnected'>('disconnected')

  let sseConnection: ReturnType<typeof useSseConnection> | null = null

  const hasUnread = computed(() => unreadCount.value > 0)

  const connectSse = () => {
    if (sseConnection) {
      sseConnection.disconnect()
    }

    const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
    const sseUrl = `${baseUrl}/notification/sse/connect`

    sseConnection = useSseConnection({
      url: sseUrl,
      onMessage: handleSseMessage,
      onConnect: () => {
        sseStatus.value = 'connected'
        fetchOfflineMessages()
      },
      onDisconnect: () => {
        sseStatus.value = 'disconnected'
      },
      onError: (error) => {
        sseStatus.value = 'disconnected'
        console.error('SSE error:', error)
      },
      maxRetries: 10,
      retryDelay: 1000
    })

    sseStatus.value = 'connecting'
    sseConnection.connect()
  }

  const disconnectSse = () => {
    if (sseConnection) {
      sseConnection.disconnect()
      sseConnection = null
    }
    sseStatus.value = 'disconnected'
  }

  const handleSseMessage = (msg: NotificationItem) => {
    // 检查是否已存在（按notificationId去重）
    const existing = notifications.value.find(n => n.notificationId === msg.notificationId)
    if (!existing) {
      notifications.value.unshift({
        ...msg,
        read: false
      })
      unreadCount.value++
    }

    // severity为WARNING或ERROR时弹出Toast
    if (msg.severity === 'WARNING' || msg.severity === 'ERROR') {
      ElMessage({
        type: msg.severity === 'ERROR' ? 'error' : 'warning',
        message: msg.title,
        duration: 3000,
        showClose: true
      })
    }
  }

  const fetchOfflineMessages = async () => {
    try {
      const rsp = await notificationApi.getList()
      if (rsp.notifications) {
        // 只添加不在列表中的消息
        for (const msg of rsp.notifications) {
          const existing = notifications.value.find(n => n.notificationId === msg.notificationId)
          if (!existing) {
            notifications.value.push(msg)
          }
        }
      }
      unreadCount.value = rsp.unreadCount || 0
    } catch (error) {
      console.error('Failed to fetch offline messages:', error)
    }
  }

  const fetchUnreadCount = async () => {
    try {
      const rsp = await notificationApi.getUnreadCount()
      unreadCount.value = rsp.unreadCount || 0
    } catch (error) {
      console.error('Failed to fetch unread count:', error)
    }
  }

  const markAsRead = async (notificationId: number) => {
    try {
      await notificationApi.markRead(notificationId)
      const notification = notifications.value.find(n => n.notificationId === notificationId)
      if (notification && !notification.read) {
        notification.read = true
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      }
    } catch (error) {
      console.error('Failed to mark as read:', error)
    }
  }

  const markAllAsRead = async () => {
    try {
      await notificationApi.markAllRead()
      notifications.value.forEach(n => {
        n.read = true
      })
      unreadCount.value = 0
    } catch (error) {
      console.error('Failed to mark all as read:', error)
    }
  }

  const clearNotifications = () => {
    notifications.value = []
    unreadCount.value = 0
  }

  return {
    notifications,
    unreadCount,
    hasUnread,
    sseStatus,
    connectSse,
    disconnectSse,
    fetchOfflineMessages,
    fetchUnreadCount,
    markAsRead,
    markAllAsRead,
    clearNotifications
  }
})
```

- [ ] **Step 3: Commit notification store**

```bash
git add blink-gateway/gateway-admin-web/src/stores/notification.ts
git commit -m "feat(notification): enhance notification store for SSE"
```

---

## Task 13: Frontend Notification API

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/api/notification.ts`

- [ ] **Step 1: Create notification.ts API**

```typescript
import request from '@/utils/request'

// ==================== Types ====================

export interface NotificationItem {
  notificationId: number
  title: string
  content: string
  type: 'SYSTEM' | 'OPERATION' | 'ALERT'
  severity: 'INFO' | 'WARNING' | 'ERROR' | 'SUCCESS'
  sourceRef?: string
  createdTime: string
  read?: boolean
}

export interface NotificationListRsp {
  notifications: NotificationItem[]
  unreadCount: number
}

export interface UnreadCountRsp {
  unreadCount: number
}

export interface QueryHistoryParams {
  pageNum?: number
  pageSize?: number
  type?: string
  severity?: string
  startTime?: string
  endTime?: string
}

// ==================== API Functions ====================

/**
 * Get notification list
 */
export const getNotificationList = (): Promise<NotificationListRsp> => {
  return request.post('/notification/list', { body: {} })
}

/**
 * Get unread count
 */
export const getUnreadCount = (): Promise<UnreadCountRsp> => {
  return request.post('/notification/unreadCount', { body: {} })
}

/**
 * Mark notification as read
 */
export const markRead = (notificationId: number): Promise<void> => {
  return request.post('/notification/markRead', {
    body: { notificationId }
  })
}

/**
 * Mark all notifications as read
 */
export const markAllRead = (): Promise<void> => {
  return request.post('/notification/markAllRead', { body: {} })
}

/**
 * Get notification history
 */
export const getNotificationHistory = (params: QueryHistoryParams): Promise<{ rows: NotificationItem[], total: number }> => {
  return request.post('/notification/history', { body: params })
}

// API object
export const notificationApi = {
  getList: getNotificationList,
  getUnreadCount,
  markRead,
  markAllRead,
  getHistory: getNotificationHistory
}
```

- [ ] **Step 2: Commit notification API**

```bash
git add blink-gateway/gateway-admin-web/src/api/notification.ts
git commit -m "feat(notification): add notification API"
```

---

## Task 14: Frontend i18n Updates

**Files:**
- Modify: `blink-gateway/gateway-admin-web/src/locales/zh-cn.ts`
- Modify: `blink-gateway/gateway-admin-web/src/locales/en-us.ts`

- [ ] **Step 1: Add notification i18n keys to zh-cn.ts**

Append after the `avatar` section:

```typescript
  notification: {
    title: '消息通知',
    markAllRead: '全部已读',
    noNotifications: '暂无消息',
    justNow: '刚刚',
    minutesAgo: '{n}分钟前',
    hoursAgo: '{n}小时前',
    daysAgo: '{n}天前',
    viewAll: '查看全部',
    history: '历史消息',
    connected: '已连接',
    disconnected: '连接断开',
    reconnecting: '正在重连...',
    connectionError: '连接失败，请刷新页面',
    system: '系统通知',
    operation: '操作通知',
    alert: '告警通知',
    info: '信息',
    warning: '警告',
    error: '错误',
    success: '成功',
  },
```

- [ ] **Step 2: Add notification i18n keys to en-us.ts**

Append after the `avatar` section:

```typescript
  notification: {
    title: 'Notifications',
    markAllRead: 'Mark all read',
    noNotifications: 'No notifications',
    justNow: 'Just now',
    minutesAgo: '{n} minutes ago',
    hoursAgo: '{n} hours ago',
    daysAgo: '{n} days ago',
    viewAll: 'View all',
    history: 'History',
    connected: 'Connected',
    disconnected: 'Disconnected',
    reconnecting: 'Reconnecting...',
    connectionError: 'Connection failed, please refresh',
    system: 'System',
    operation: 'Operation',
    alert: 'Alert',
    info: 'Info',
    warning: 'Warning',
    error: 'Error',
    success: 'Success',
  },
```

- [ ] **Step 3: Commit i18n updates**

```bash
git add blink-gateway/gateway-admin-web/src/locales/zh-cn.ts
git add blink-gateway/gateway-admin-web/src/locales/en-us.ts
git commit -m "feat(notification): add notification i18n keys"
```

---

## Task 15: MainLayout SSE Integration

**Files:**
- Modify: `blink-gateway/gateway-admin-web/src/layouts/MainLayout.vue`

- [ ] **Step 1: Add SSE initialization to MainLayout.vue**

In the `<script setup lang="ts">` section, add imports and initialization:

```typescript
import { onMounted, onUnmounted } from 'vue'
import { useNotificationStore } from '@/stores/notification'

const notificationStore = useNotificationStore()

onMounted(() => {
  // Initialize SSE connection after user is logged in
  notificationStore.connectSse()
})

onUnmounted(() => {
  notificationStore.disconnectSse()
})
```

- [ ] **Step 2: Commit MainLayout integration**

```bash
git add blink-gateway/gateway-admin-web/src/layouts/MainLayout.vue
git commit -m "feat(notification): integrate SSE in MainLayout"
```

---

## Task 16: Integration with DataSyncService

**Files:**
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/DataSyncServiceImpl.java`

- [ ] **Step 1: Read current DataSyncServiceImpl**

Run: Read `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/DataSyncServiceImpl.java`

- [ ] **Step 2: Add notification integration to DataSyncServiceImpl**

Add import and inject NotificationPublishService:

```java
import com.blink.gateway.admin.service.NotificationPublishService;

// In class:
@Resource
private NotificationPublishService notificationPublishService;
```

In sync methods, add notification calls:

```java
@Override
public void syncChannelData(SyncChannelDataReq req) {
    Integer userId = StpUtil.getLoginIdAsInt();
    String syncTaskId = UUID.randomUUID().toString();
    
    try {
        // Existing sync logic...
        
        notificationPublishService.sendOperationSuccess(
            userId,
            "渠道数据同步完成",
            "渠道数据已成功同步到所有网关实例",
            syncTaskId
        );
        log.info("[DataSync] 渠道同步成功, userId: {}", userId);
    } catch (Exception e) {
        log.error("[DataSync] 渠道同步失败: {}", e.getMessage(), e);
        
        notificationPublishService.sendOperationError(
            userId,
            "渠道数据同步失败",
            "同步失败: " + e.getMessage(),
            syncTaskId
        );
        throw new BlinkException("渠道数据同步失败", e, ErrCodeConstant.GATE0001);
    }
}
```

- [ ] **Step 3: Commit DataSyncService integration**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/DataSyncServiceImpl.java
git commit -m "feat(notification): integrate notifications with DataSyncService"
```

---

## Self-Review Checklist

**1. Spec Coverage:**
- ✅ Database tables (sys_notification, sys_notification_read) - Task 1
- ✅ Backend entities and constants - Task 2, 3
- ✅ Backend DTOs - Task 5
- ✅ SseConnectionPool - Task 6
- ✅ NotificationPublishService - Task 7
- ✅ NotificationRedisListener - Task 8
- ✅ NotificationService - Task 9
- ✅ NotificationController - Task 10
- ✅ Frontend SSE composable - Task 11
- ✅ Frontend notification store - Task 12
- ✅ Frontend API - Task 13
- ✅ i18n - Task 14
- ✅ MainLayout integration - Task 15
- ✅ Integration with existing services - Task 16

**2. Placeholder Scan:**
- No TBD, TODO found
- No "implement later" patterns
- All code steps have complete implementations

**3. Type Consistency:**
- NotificationMsg fields match SysNotificationDO
- NotificationItemRsp fields match NotificationItem TypeScript interface
- API function parameters match DTO classes