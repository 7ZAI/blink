# Gateway Admin - 网关运维管理平台

## 模块概述

Gateway Admin 是 Blink 微服务框架的网关运维管理平台，提供渠道管理、路由管理、配置管理、监控运维等核心功能。通过 Dubbo RPC 与网关实例通信，实现配置的动态推送和实时监控。

## 设计目标

> **重要说明**：Gateway Admin 的管理目标是**单一网关集群**，而非多集群管理。

本平台专注于对**一个网关集群**的全生命周期管理，包括：

- 单一集群内的所有网关实例统一管控
- 集群级别的渠道、路由、配置管理
- 实例级别的上下线、健康监控
- 配置的集中式管理和推送

如需多集群管理场景，建议为每个集群部署独立的 Gateway Admin 实例，通过上层平台进行聚合展示。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2 | 基础框架 |
| Spring Cloud Alibaba | 2023.0.3.2 | 微服务组件 |
| Nacos | 2.3+ | 注册中心/配置中心 |
| Dubbo | 3.3 | RPC 框架（Triple 协议） |
| MyBatis-Plus | 3.5 | ORM 框架 |
| Redis | 7.0+ | 缓存/分布式锁 |
| MySQL | 8.0+ | 关系型数据库 |

## 功能模块

### 1. 渠道管理（ChannelController）

对外部接入渠道进行统一管理，支持多渠道接入和认证。

| 接口 | 路径 | 功能 |
|------|------|------|
| getChannelList | POST /channel/getChannelList | 查询渠道列表（分页） |
| getChannel | POST /channel/getChannel | 获取单个渠道详情 |
| saveChannel | POST /channel/saveChannel | 新增渠道 |
| modifyChannel | POST /channel/modifyChannel | 修改渠道信息 |
| deleteChannel | POST /channel/deleteChannel | 删除渠道 |
| refreshChannelKey | POST /channel/refreshChannelKey | 刷新渠道密钥 |
| refreshSystemKey | POST /channel/refreshSystemKey | 刷新系统密钥 |
| issueChannelToken | POST /channel/issueChannelToken | 签发渠道 Token |

### 2. 路由管理（RouteController）

管理网关动态路由配置，支持路由的增删改查和实时刷新。

| 接口 | 路径 | 功能 |
|------|------|------|
| getRouteList | POST /route/getRouteList | 查询路由列表（分页） |
| saveRoute | POST /route/saveRoute | 保存路由（新增/修改） |
| deleteRoute | POST /route/deleteRoute | 删除路由 |
| refreshRoutes | POST /route/refreshRoutes | 刷新路由缓存 |

### 3. 配置管理（ConfigController）

管理网关运行配置参数，支持动态修改和实时生效。

| 接口 | 路径 | 功能 |
|------|------|------|
| modifySysConfig | POST /config/modifySysConfig | 修改系统配置 |
| getOneConfig | POST /config/getOneConfig | 查询单个配置 |

### 4. 配置推送（ConfigPushController）

将配置推送到 Nacos，支持版本管理和配置回滚。

| 接口 | 路径 | 功能 |
|------|------|------|
| pushConfig | POST /configPush/pushConfig | 推送配置到 Nacos |
| getConfigHistory | POST /configPush/getConfigHistory | 获取配置历史列表 |
| rollbackConfig | POST /configPush/rollbackConfig | 回滚配置到指定版本 |

### 5. 数据同步（DataSyncController）

将管理后台的数据同步到网关实例，确保配置一致性。

| 接口 | 路径 | 功能 |
|------|------|------|
| syncChannelData | POST /dataSync/syncChannelData | 同步渠道数据到网关 |
| syncRouteData | POST /dataSync/syncRouteData | 同步路由数据到网关 |
| syncConfigData | POST /dataSync/syncConfigData | 同步配置数据到网关 |

### 6. 网关实例管理（GatewayInstanceController）

管理网关实例的上下线和状态监控。

| 接口 | 路径 | 功能 |
|------|------|------|
| getGatewayInstances | POST /gatewayInstance/getGatewayInstances | 获取网关实例列表 |
| getGatewayInstanceDetail | POST /gatewayInstance/getGatewayInstanceDetail | 获取实例详情 |
| offlineInstance | POST /gatewayInstance/offlineInstance | 下线网关实例 |
| onlineInstance | POST /gatewayInstance/onlineInstance | 上线网关实例 |

### 7. 监控服务（MonitorController）

提供网关实例状态和监控数据查询。

| 接口 | 路径 | 功能 |
|------|------|------|
| getGatewayInstances | POST /monitor/getGatewayInstances | 获取网关实例列表 |
| getStatistics | POST /monitor/getStatistics | 获取网关统计数据 |
| getHealthStatus | POST /monitor/getHealthStatus | 获取网关健康状态 |
| getGatewayMetrics | POST /monitor/getGatewayMetrics | 获取网关指标数据 |

## Dubbo 服务

### GatewayAdminDubboService

作为 Dubbo Provider，为其他服务提供基础数据查询能力。

```java
// 同步接口
ResponseDTO<ChannelInfoRedisDO> getChannelInfo(RequestDTO<QueryOneChannelReq> reqDto);
ResponseDTO<SysConfigCacheDO> getChannelConfig(RequestDTO<QueryChannelConfigReq> reqDto);

// 异步接口
CompletableFuture<ResponseDTO<ChannelInfoRedisDO>> getChannelInfoAsync(RequestDTO<QueryOneChannelReq> reqDto);
CompletableFuture<ResponseDTO<SysConfigCacheDO>> getChannelConfigAsync(RequestDTO<QueryChannelConfigReq> reqDto);
```

## 项目结构

```
gateway-admin/
├── src/main/java/com/blink/gateway/admin/
│   ├── GatewayAdminApplication.java    # 启动类
│   ├── component/                       # 组件
│   │   ├── CacheDataPreLoad.java       # 缓存数据预加载
│   │   ├── NacosConfigComponent.java   # Nacos 配置组件
│   │   └── SecretConfigComponent.java  # 密钥配置组件
│   ├── config/                          # 配置类
│   │   └── DubboConfig.java            # Dubbo 配置
│   ├── constans/                        # 常量定义
│   │   ├── ConfigValueConstant.java    # 配置值常量
│   │   ├── ErrCodeConstant.java        # 错误码常量
│   │   ├── NacosConfigConstant.java    # Nacos 配置常量
│   │   └── RedisKeyConstant.java       # Redis Key 常量
│   ├── controller/                      # 控制器层
│   │   ├── ChannelController.java      # 渠道管理
│   │   ├── ConfigController.java       # 配置管理
│   │   ├── ConfigPushController.java   # 配置推送
│   │   ├── DataSyncController.java     # 数据同步
│   │   ├── GatewayInstanceController.java # 实例管理
│   │   ├── MonitorController.java      # 监控服务
│   │   └── RouteController.java        # 路由管理
│   ├── dto/                             # 数据传输对象
│   │   ├── req/                        # 请求 DTO
│   │   ├── rsp/                        # 响应 DTO
│   │   └── vo/                         # 视图对象
│   ├── dubbo/                           # Dubbo 服务
│   │   └── GatewayAdminDubboServiceImpl.java
│   ├── entity/                          # 实体类
│   ├── mapper/                          # MyBatis Mapper
│   ├── producer/                        # 消息生产者
│   │   └── GateWayStreamMessageProducer.java
│   └── service/                         # 服务层
│       └── impl/                        # 服务实现
├── src/main/resources/
│   ├── application.yml                  # 应用配置
│   ├── logback-spring.xml              # 日志配置
│   ├── mapper/                          # MyBatis XML
│   └── db/migration/                    # 数据库迁移脚本
└── build.gradle                         # 构建配置
```

## 配置说明

### application.yml 核心配置

```yaml
server:
  port: 8008
  servlet:
    context-path: /gateway-admin

spring:
  application:
    name: gateway-admin
  cloud:
    nacos:
      server-addr: 127.0.0.1:8848
      discovery:
        namespace: ${NACOS_NAMESPACE}
      config:
        namespace: ${NACOS_NAMESPACE}
  config:
    import:
      - nacos:gateway-admin.yaml?refreshEnabled=true

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

### 数据库表

| 表名 | 说明 |
|------|------|
| ga_channel | 渠道信息表 |
| sys_config | 系统配置表 |
| route_definition | 路由定义表 |
| gateway_instance | 网关实例表 |
| redis_mq | Redis 消息队列表 |

## 构建与运行

### 本地构建

```bash
# 构建模块
./gradlew :blink-gateway:gateway-admin:build

# 发布到本地 Maven 仓库
./gradlew :blink-gateway:gateway-admin:publishToMavenLocal
```

### 启动服务

1. 确保 Nacos、MySQL、Redis 已启动
2. 配置 `application.yml` 中的数据库和 Nacos 连接信息
3. 运行 `GatewayAdminApplication.main()`

### 环境依赖

- JDK 17+
- MySQL 8.0+
- Redis 7.0+
- Nacos 2.3+

## 与网关的交互

```
┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
│   管理后台 UI    │──────│  Gateway Admin  │──────│     Nacos       │
└─────────────────┘ HTTP └─────────────────┘      └─────────────────┘
                              │                          │
                              │ Dubbo RPC                │ 配置推送
                              ▼                          ▼
                      ┌─────────────────┐      ┌─────────────────┐
                      │ Gateway Reactive│◄─────│   配置监听       │
                      └─────────────────┘      └─────────────────┘
```

**交互流程：**

1. 管理员通过管理后台 UI 进行配置操作
2. Gateway Admin 接收请求并持久化到数据库
3. 通过 Dubbo RPC 同步数据到网关实例
4. 通过 Nacos 推送配置变更通知
5. 网关实例监听配置变更并热更新

## 错误码规范

| 前缀 | 模块 | 示例 |
|------|------|------|
| GATE00XX | 网关管理 | GATE0001 |
| PARAM00XX | 参数校验 | PARAM0001 |

详细错误码定义见 `ErrCodeConstant.java`。

## 相关模块

- `blink-gateway-reactive`: 响应式 API 网关
- `blink-gateway-admin-api-dubbo`: Dubbo 接口定义
- `blink-base-api-dubbo`: 基础服务 Dubbo 接口

## 作者

binblink