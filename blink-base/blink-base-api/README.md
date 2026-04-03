# blink-base-api (已弃用)

> ⚠️ **注意**：此模块已弃用，仅作为工程示例保留。请使用 `blink-base-api-dubbo` 进行服务间调用。

## 说明

此模块原用于封装 Feign 客户端和 DTO，供其他服务通过 HTTP 方式调用 Base 服务。

当前 Blink 框架已改用 **Dubbo** 作为服务间调用方案：
- ✅ 更好的性能
- ✅ 支持响应式编程
- ✅ 更完善的服务治理能力
- ✅ 支持 Triple 协议（基于 HTTP/2）

## 替代方案

使用 Dubbo 进行服务间调用：

```java
// 1. 引入依赖
implementation 'com.blink:blink-base-api-dubbo:1.0.0-SNAPSHOT'

// 2. 注入 Dubbo 服务
@DubboReference
private BaseDubboService baseDubboService;

// 3. 调用服务
ResponseDTO<SysConfigCacheDO> response =
    baseDubboService.getOneConfig(requestDto);
```

## 服务间调用方案对比

### 方案一：DTO 和 Feign 封装成独立 JAR 包

**优点：**
- 保证一致性：所有服务使用相同的 DTO 定义
- 减少重复代码：避免重复定义相同的 DTO 类
- 易于维护：DTO 变更只需修改一处

**缺点：**
- 耦合性增加：所有服务依赖同一个 JAR 包
- 版本管理复杂：DTO 变更可能引发连锁更新
- 开发效率：频繁更新依赖增加复杂度

**适用场景：**
- 服务数量不多，关系紧密
- 变更频率较低
- 团队规模不大

### 方案二：各个服务自己编写 DTO 和 Feign

**优点：**
- 服务独立：每个服务只关心自己的 DTO
- 低耦合：服务间没有共享 DTO 依赖
- 灵活性高：按需定义 DTO

**缺点：**
- 重复代码：可能定义相同的 DTO
- 一致性难保证：字段定义需保持一致
- 维护成本：底层变更需修改多个服务

**适用场景：**
- 服务数量较多，不同团队维护
- 服务间相对独立
- 团队倾向松耦合架构

## Blink 框架采用的方式

采用方案一的变体：**复制提供外部调用接口所涉及的 DTO 到 api 模块中，包名保持一致**。

这样既保证了 DTO 一致性，又通过 Dubbo 获得了更好的性能和响应式支持。

## Feign 示例（仅作参考）

```java
@FeignClient("base-app")
public interface AuthServiceClient {

    @PostMapping("/system/login")
    ResponseDTO<SysLoginRspDTO> login(
        @Validated @RequestBody RequestDTO<SysLoginReqDTO> requestDTO
    ) throws BlinkException;

}
```

**负载均衡说明：**

OpenFeign 本身不负责负载均衡，通过服务发现获取 base-app 所有实例，再根据外部配置的 LoadBalancer 选择一个实例调用。

## 相关模块

- [blink-base-api-dubbo](../blink-base-api-dubbo/README.md)：Dubbo 接口定义（推荐使用）
- [blink-framework-openfeign](../../blink-framework-openfeign/README.md)：Feign 封装模块
