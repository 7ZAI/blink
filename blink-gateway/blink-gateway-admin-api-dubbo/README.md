# blink-gateway-admin-api-dubbo

Gateway Admin 服务的 Dubbo 接口定义模块，提供网关管理相关的 Dubbo 服务接口。

## 功能特性

- **Dubbo 服务接口定义**：`GatewayAdminDubboService` 接口，供网关实例调用
- **DTO 定义**：请求和响应的数据传输对象
- **同步/异步双模式**：支持同步调用和 CompletableFuture 异步调用

## 服务接口

### GatewayAdminDubboService

| 方法 | 说明 | 同步/异步 |
|------|------|----------|
| `getChannelInfo` | 根据 appKey 获取渠道信息 | ✅ 同步 |
| `getChannelInfoAsync` | 根据 appKey 获取渠道信息 | ✅ 异步 |
| `getChannelConfig` | 获取渠道配置信息 | ✅ 同步 |
| `getChannelConfigAsync` | 获取渠道配置信息 | ✅ 异步 |

## 快速开始

### 引入依赖

```gradle
implementation 'com.blink:blink-gateway-admin-api-dubbo:1.0.0-SNAPSHOT'
```

### 消费服务

```java
// 注入 Dubbo 服务
@DubboReference
private GatewayAdminDubboService gatewayAdminDubboService;

// 同步调用
RequestDTO<QueryOneChannelReq> reqDto = RequestDTO.newInstance(new QueryOneChannelReq());
reqDto.getBody().setAppKey("your-app-key");
ResponseDTO<ChannelInfoRedisDO> response = gatewayAdminDubboService.getChannelInfo(reqDto);

// 异步调用（适合响应式场景）
CompletableFuture<ResponseDTO<ChannelInfoRedisDO>> future =
    gatewayAdminDubboService.getChannelInfoAsync(reqDto);
```

## DTO 结构

### 请求 DTO

| 类名 | 说明 |
|------|------|
| `QueryOneChannelReq` | 查询单个渠道请求 |
| `QueryChannelConfigReq` | 查询渠道配置请求 |
| `QueryBlinkChannelReq` | 查询渠道列表请求 |

### 响应 DTO

| 类名 | 说明 |
|------|------|
| `QueryBlinkChannelRsp` | 渠道查询响应 |

### VO 对象

| 类名 | 说明 |
|------|------|
| `ChannelVO` | 渠道信息 VO |

### 消息对象

| 类名 | 说明 |
|------|------|
| `CacheMsg` | 缓存同步消息 |
| `RouteSyncMsg` | 路由同步消息 |

## 服务提供者

`gateway-admin` 模块实现了此接口：

```java
@DubboService(async = true, timeout = 10000)
public class GatewayAdminDubboServiceImpl implements GatewayAdminDubboService {
    // 实现...
}
```

## 使用场景

- **网关渠道认证**：网关通过 Dubbo 调用获取渠道信息、密钥等
- **配置同步**：网关获取渠道相关的配置信息
- **响应式集成**：网关使用异步方法避免阻塞

## 交互流程

```
┌─────────────────────┐         ┌─────────────────────┐
│ blink-gateway-      │  Dubbo  │   gateway-admin     │
│ reactive (网关)      │  RPC    │  (运维管理平台)       │
│                     │────────▶│                     │
│ 获取渠道信息、配置      │         │  提供渠道管理服务     │
└─────────────────────┘         └─────────────────────┘
```

## 相关模块

- `gateway-admin`：服务实现 Provider
- `blink-gateway-reactive`：服务消费者 Consumer
