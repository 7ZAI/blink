# blink-base-api-dubbo

Base 服务的 Dubbo 接口定义模块，提供服务间调用的接口和 DTO。

## 功能特性

- **Dubbo 服务接口定义**：`BaseDubboService` 接口，供其他服务调用 Base 服务的能力
- **DTO 定义**：请求和响应的数据传输对象
- **同步/异步双模式**：支持同步调用和 CompletableFuture 异步调用

## 服务接口

### BaseDubboService

| 方法 | 说明 | 同步/异步 |
|------|------|----------|
| `getOneConfig` | 根据配置 key 获取单个配置参数 | ✅ 同步 |
| `getOneConfigAsync` | 根据配置 key 获取单个配置参数 | ✅ 异步 |
| `getErrorMsgInfo` | 获取错误提示信息（国际化） | ✅ 同步 |
| `getErrorMsgInfoAsync` | 获取错误提示信息（国际化） | ✅ 异步 |
| `getUserPermissionsByUerId` | 根据用户 ID 获取用户权限标识 | ✅ 同步 |
| `getUserPermissionsByUerIdAsync` | 根据用户 ID 获取用户权限标识 | ✅ 异步 |
| `getUserPermissionsByPath` | 根据请求路径获取权限标识 | ✅ 同步 |
| `getUserPermissionsByPathAsync` | 根据请求路径获取权限标识 | ✅ 异步 |
| `getAllApiPermissions` | 获取所有接口权限 | ✅ 同步 |
| `getAllApiPermissionsAsync` | 获取所有接口权限 | ✅ 异步 |

## 快速开始

### 引入依赖

```gradle
implementation 'com.blink:blink-base-api-dubbo:1.0.0-SNAPSHOT'
```

### 消费服务

```java
// 注入 Dubbo 服务
@DubboReference
private BaseDubboService baseDubboService;

// 同步调用
RequestDTO<QueryOneSysConfigReq> reqDto = RequestDTO.newInstance(new QueryOneSysConfigReq());
reqDto.getBody().setConfigKey("system.name");
ResponseDTO<SysConfigCacheDO> response = baseDubboService.getOneConfig(reqDto);

// 异步调用（适合响应式场景）
CompletableFuture<ResponseDTO<SysConfigCacheDO>> future =
    baseDubboService.getOneConfigAsync(reqDto);
```

## DTO 结构

### 请求 DTO

| 类名 | 说明 |
|------|------|
| `QueryOneSysConfigReq` | 查询单个系统配置请求 |
| `QueryErrMsgReq` | 查询错误信息请求 |
| `QueryUserPermissionReq` | 查询用户权限请求 |
| `GetAllApiPermissionsReq` | 获取所有接口权限请求 |
| `QueryOneChannelReq` | 查询单个渠道请求 |
| `QueryConfigByGroupKeyReq` | 按分组 key 查询配置请求 |

### 响应 DTO

| 类名 | 说明 |
|------|------|
| `ConfigGroupRsp` | 配置分组响应 |
| `QueryErrMsgRsp` | 错误信息响应 |
| `QueryUserPermissionRsp` | 用户权限响应 |
| `GetAllApiPermissionsRsp` | 所有接口权限响应 |
| `SysLoginRsp` | 登录响应 |
| `QueryBlinkChannelRsp` | 渠道信息响应 |

### VO 对象

| 类名 | 说明 |
|------|------|
| `ChannelVO` | 渠道信息 VO |
| `SysConfigVO` | 系统配置 VO |
| `SysMenuVO` | 菜单信息 VO |
| `SysPermissionVO` | 权限信息 VO |
| `SysUserVO` | 用户信息 VO |
| `CaptchaVO` | 验证码 VO |
| `PointVO` | 坐标点 VO |

## 服务提供者

`blink-base-app` 模块实现了此接口：

```java
@DubboService(async = true, timeout = 10000)
public class BaseDubboServiceImpl implements BaseDubboService {
    // 实现...
}
```

## 使用场景

- **网关鉴权**：网关通过 Dubbo 调用获取用户权限、配置信息等
- **跨服务数据查询**：其他服务查询 Base 服务的配置、错误信息等基础数据
- **响应式集成**：网关等响应式服务使用异步方法避免阻塞

## 相关模块

- `blink-base-app`：服务实现 Provider
- `blink-gateway-reactive`：服务消费者 Consumer
