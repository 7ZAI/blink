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
