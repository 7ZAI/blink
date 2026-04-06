# Blink Gateway 网关模块概述

## 目录结构

```
blink-gateway/
├── blink-gateway-admin-api-dubbo/    # Dubbo API 模块（网关管理接口）
├── blink-gateway-reactive/           # 响应式网关（核心网关服务）
├── gateway-admin/                    # 网关管理后台（服务端）
└── gateway-admin-web/                # 网关管理后台（前端）
```

## 1. blink-gateway-reactive（响应式网关）

### 核心功能
- 基于 Spring Cloud Gateway 的响应式 API 网关
- 请求过滤链：日志 → IP 过滤 → 请求头验证 → 安全认证 → 签名验证 → 防重放 → 加解密 → 路由转发
- 动态路由（支持 Nacos/Redis 两种方式）
- 多渠道管理（appKey/appSecret 认证）
- 限流（Redis 令牌桶算法）
- JWT 认证

### 关键组件

#### 过滤器链 (Filters)
- `LogFilter` - 请求日志记录
- `IpFilter` - IP 黑白名单过滤
- `RequestValidateFilter` - 请求头验证
- `SignatureFilter` - 签名验证（支持 RSA/ECDSA/HMAC）
- `ReplayAttackPreventionFilter` - 防重放攻击
- `CryptFilter` - AES+RSA 混合加密
- `RewriteRequestBodyFilter` - 重写请求体

#### 安全认证
- `SecurityConfig` - Spring Security 配置
- `JwtAuthenticationManager` - JWT 认证管理器
- `TokenAuthenticationManager` - Token 认证管理器
- `BlinkAuthorizationManager` - 授权管理器

#### 动态路由
- `NacosRouteDefinitionRepository` - Nacos 路由存储
- `RedisRouteDefinitionRepository` - Redis 路由存储
- `NacosDynamicRouteListener` - Nacos 路由变更监听

#### 限流
- `BlinkRedisRateLimiter` - 基于 Redis 的限流器
- `RateLimiterConfig` - 限流配置
- `Resilience4jConfig` - 熔断配置

### 依赖
- Spring Cloud Gateway
- Spring Cloud Alibaba (Nacos)
- Spring Security
- Resilience4j（熔断、限流）
- Redis Reactive
- Dubbo（消费者）
- blink-framework-common
- blink-redis-starter

---

## 2. gateway-admin（网关管理后台服务端）

### 核心功能
- 渠道管理（Channel）：appKey/appSecret 管理、Token 签发
- 路由管理（Route）：动态路由配置
- 配置管理（Config）：网关系统配置
- 监控（Monitor）：网关实例健康检查、流量统计

### 技术栈
- Spring Boot + Dubbo Provider
- MyBatis-Plus + MySQL
- Nacos（注册中心/配置中心）
- Redis

### Controller 列表
- `ChannelController` - 渠道管理
- `RouteController` - 路由管理
- `ConfigController` - 配置管理
- `MonitorController` - 监控

### 核心实体
- `BlinkChannelDO` - 渠道实体
- `RouteDefinitionDO` - 路由定义
- `FilterDefinitionDO` - 过滤器定义
- `PredicateDefinitionDO` - 断言定义

### 服务
- `RouteService` - 路由服务
- `MonitorService` - 监控服务
- `ConfigService` - 配置服务

### 组件
- `NacosConfigComponent` - Nacos 配置操作
- `SecretConfigComponent` - 密钥配置组件
- `GateWayStreamMessageProducer` - 流消息生产者（通知网关刷新缓存）

---

## 3. gateway-admin-web（网关管理后台前端）

### 技术栈
- Vue 3 + TypeScript
- Element Plus
- Vite
- Pinia（状态管理）
- Vue Router
- Vue i18n（国际化）

### 页面结构
```
src/views/
├── dashboard/     # 仪表盘
├── route/         # 路由管理
├── channel/       # 渠道管理
├── config/        # 配置管理
├── monitor/       # 监控
└── layout/        # 布局
```

### 核心依赖
- Vue 3.4+
- Element Plus 2.6+
- Axios
- Vue i18n
- Pinia

---

## 4. blink-gateway-admin-api-dubbo（Dubbo API 模块）

### 作用
- 定义 gateway-admin 提供给 blink-gateway-reactive 的 Dubbo 接口
- DTO 数据传输对象

### 核心接口
- 渠道查询接口
- 路由同步接口

### 核心 DTO
- `RouteSyncMsg` - 路由同步消息
- `CacheMsg` - 缓存刷新消息
- `ChannelVO` - 渠道视图对象

---

## 架构流程

### 请求处理流程
```
客户端请求
    ↓
LogFilter（日志记录）
    ↓
IpFilter（IP 过滤）
    ↓
RequestValidateFilter（请求头验证）
    ↓
Security（JWT/Token 认证）
    ↓
SignatureFilter（签名验证）
    ↓
ReplayAttackPreventionFilter（防重放）
    ↓
CryptFilter（解密）
    ↓
RewriteRequestBodyFilter（重写请求体）
    ↓
路由匹配 → 下游服务
```

### 渠道认证流程
```
1. 管理员在 gateway-admin 创建渠道
   → 生成 appKey/appSecret
   → 存入 MySQL
   → 发送 Dubbo 事件通知网关刷新缓存

2. 客户端请求
   → 携带 appKey
   → 网关从缓存获取 appSecret
   → 验证签名
   → 验证通过后分发 JWT
```

### 动态路由流程
```
1. 管理员在 gateway-admin 配置路由
   → 存入 MySQL
   → 同步到 Nacos/Redis
   → 发送 Dubbo 事件

2. blink-gateway-reactive
   → NacosDynamicRouteListener 监听变更
   → 从 Nacos/Redis 加载路由
   → 更新路由表
```

---

## 缓存同步机制

### 概述

gateway-admin 与 gateway-reactive 之间通过 **Redis Stream** 实现缓存同步，确保渠道信息、路由配置等数据在多实例环境下的一致性。

### 核心组件

| 组件 | 位置 | 说明 |
|------|------|------|
| `CacheMsg` | blink-gateway-admin-api-dubbo | 缓存消息 DTO，包含 key、value、operator、version |
| `GateWayStreamMessageProducer` | gateway-admin | Redis Stream 消息生产者 |
| `ChannelAsyncSyncService` | gateway-admin | 渠道异步同步服务，带重试和失败补偿 |
| `CommonEventStreamListener` | gateway-reactive | Redis Stream 消息消费者 |
| `MultiLevelCacheComponent` | gateway-reactive | 多级缓存组件（本地 + Redis） |
| `RedisCacheKeyConstant` | blink-framework-common | 共享缓存 Key 常量 |

### 消息结构

```java
public class CacheMsg {
    private String key;        // 缓存 key，格式: blink:channel:{appKey}
    private Object value;      // 缓存数据
    private String operator;   // 操作类型: A(新增)/M(修改)/D(删除)
    private Integer version;   // 版本号（时间戳），防止消息乱序
}
```

### 操作类型说明

| operator | 含义 | 行为 |
|----------|------|------|
| `A` | Add | 新增缓存，同时写入本地缓存和 Redis |
| `M` | Modify | 修改缓存，同时更新本地缓存和 Redis |
| `D` | Delete | 删除缓存，同时删除本地缓存和 Redis |

### 数据流架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            gateway-admin                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│  ChannelServiceImpl                                                          │
│    ├─ saveChannel()                                                          │
│    │    1. DB.insert()                                                       │
│    │    2. channelSecretSyncService.addChannelSecretConfigAsync()           │
│    │    3. channelAsyncSyncService.syncAddChannel(appKey, channelInfo)      │
│    │                                                                         │
│    ├─ modifyChannel()                                                        │
│    │    1. DB.update()                                                       │
│    │    2. channelAsyncSyncService.syncModifyChannel(appKey, channelInfo)   │
│    │    (不再先删除 Redis 缓存，直接推送新值)                                   │
│    │                                                                         │
│    └─ deleteChannel()                                                        │
│         1. DB.delete()                                                       │
│         2. redisClient.delete(cacheKey)                                      │
│         3. channelAsyncSyncService.syncDeleteChannel(appKey)                │
│         4. channelSecretSyncService.deleteChannelSecretConfigAsync(appKey)  │
│                                                                              │
│  ChannelAsyncSyncServiceImpl (@Async "ioIntensiveThreadPool")               │
│    ├─ syncAddChannel()    → CacheMsg(operator="A", value, version)          │
│    ├─ syncModifyChannel() → CacheMsg(operator="M", value, version)          │
│    └─ syncDeleteChannel() → CacheMsg(operator="D")                          │
│                                                                              │
│  GateWayStreamMessageProducer                                                │
│    └─ sendCacheSyncMsg(cacheMsg) → 带重试(3次) + 失败记录                     │
│                                                                              │
│  CacheSyncFailureServiceImpl                                                 │
│    ├─ recordFailure()           → 记录失败消息到 redis_mq 表                  │
│    ├─ retryFailedMessage()      → 单条重试                                   │
│    └─ retryAllFailedMessages()  → 批量重试                                   │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │ Redis Stream
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          gateway-reactive                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│  CommonEventStreamListener                                                   │
│    └─ cacheMsgHandler(CacheMsg)                                              │
│         ├─ operator="D" → evictTransactional() 删除本地+Redis 缓存           │
│         └─ operator="A"/"M" → checkVersionAndUpdate()                        │
│                                ├─ 版本号检查 (防止消息乱序)                    │
│                                └─ setLocalAndRedisCache() 更新本地+Redis     │
│                                                                              │
│  MultiLevelCacheComponent                                                    │
│    ├─ get() → 本地缓存 → Redis → 远程服务 (带分布式锁保护)                     │
│    ├─ setLocalAndRedisCache() → 同时更新本地和 Redis                         │
│    └─ evictTransactional() → 同时删除本地和 Redis                            │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 并发保护机制

#### 1. 单实例缓存击穿保护

Caffeine `AsyncCache.get(key, loader)` 自动合并同一 key 的并发请求：

```java
// 多个并发请求同一个 key → 只执行一次 loader
CompletableFuture<V> future = cache.get(key, (k, executor) -> {
    return loader.get().subscribeOn(Schedulers.boundedElastic()).toFuture();
});
```

#### 2. 多实例缓存击穿保护

使用 Redis 分布式锁，防止多个实例同时调用远程服务：

```java
private <T> Mono<T> getWithDistributedLock(String key, Class<T> clazz, RemoteService<T> service) {
    String lockKey = key + ":lock";
    
    return tryAcquireLock(lockKey)
            .flatMap(acquired -> {
                if (acquired) {
                    // 获取锁成功，调用远程服务
                    return service.call(key, clazz)
                            .flatMap(cache -> setRedisCache(key, cache))
                            .doFinally(signal -> releaseLock(lockKey));
                } else {
                    // 未获取锁，等待其他实例写入后从 Redis 获取
                    return waitForRedisValue(key, clazz);
                }
            });
}
```

#### 3. 版本号检查机制

使用时间戳作为版本号，防止消息乱序：

```java
// 发送端：使用当前时间戳
cacheMsg.setVersion((int) (System.currentTimeMillis() / 1000));

// 接收端：检查版本号
if (incomingVersion <= currentVersion) {
    log.warn("忽略过期消息 | currentVersion: {}, incomingVersion: {}", 
             currentVersion, incomingVersion);
    return Mono.just(true);
}
```

### 失败补偿机制

当消息发送失败时，自动记录到 `redis_mq` 表，支持手动或定时重试：

```java
// CacheSyncFailureServiceImpl
public void recordFailure(CacheMsg cacheMsg, Exception e) {
    // 记录失败消息到数据库
    RedisMqDO redisMqDO = new RedisMqDO();
    redisMqDO.setMsgStatus(MessageStatusConstant.REDIS_MSG_STATUS_SEND_FAILED);
    redisMqDO.setPayload(JacksonUtil.toJson(cacheMsg));
    redisMqMapper.insert(redisMqDO);
}

public boolean retryFailedMessage(String msgId) {
    // 从数据库读取失败消息并重试发送
    CacheMsg cacheMsg = JacksonUtil.fromJson(payload, CacheMsg.class);
    gateWayStreamMessageProducer.sendCacheSyncMsg(cacheMsg);
    // 更新消息状态
}
```

### 缓存 Key 常量管理

所有缓存 Key 前缀统一定义在 `RedisCacheKeyConstant`：

```java
public interface RedisCacheKeyConstant {
    String CHANNEL_CACHE_PREFIX = "blink:channel:";           // 渠道信息
    String GATEWAY_CONFIG_PREFIX = "blink:config:gateway:";   // 网关配置
    String GATEWAY_STREAM_EVENT = "blink:stream:gateway:event"; // 同步 Stream
}
```

### 配置项

```yaml
# gateway-admin (application.yml)
web:
  async:
    thread-pool:
      io:
        enabled: true
        coreSize: 4
        maxSize: 8
        queueCapacity: 1000
        keepAliveSeconds: 30

# gateway-reactive (application.yml)
blink:
  gateway:
    event-stream-enable: true  # 启用 Redis Stream 监听
```

### 最佳实践

1. **新增渠道**：使用 `syncAddChannel()`，operator="A"，直接推送新值
2. **修改渠道**：使用 `syncModifyChannel()`，operator="M"，不再先删除缓存
3. **删除渠道**：使用 `syncDeleteChannel()`，operator="D"，同时删除本地和 Redis
4. **失败处理**：定期检查 `redis_mq` 表中的失败消息并重试
5. **监控告警**：监控 Stream 消息积压和消费延迟

---

## 配置说明

### 响应式网关配置 (application.yml)
```yaml
server:
  port: 8080  # 网关端口

spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true  # 启用服务发现
      routes:
        - id: blink-base
          uri: lb://blink-base
          predicates:
            - Path=/api/**
```

### 网关管理后台配置
```yaml
server:
  port: 8081  # 管理后台端口

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/blink_gateway
    username: root
    password: xxx
  redis:
    host: localhost
    port: 6379
  cloud:
    nacos:
      server-addr: localhost:8848
```
