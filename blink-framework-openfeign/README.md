# blink-framework-openfeign

OpenFeign 封装模块，提供服务间 HTTP 调用的统一配置。

## 功能特性

- ✅ JSON 转换配置
- 🚧 调用日志
- 🚧 外部配置覆盖默认配置
- 🚧 负载均衡
- 🚧 服务降级

## 快速开始

### 引入依赖

```gradle
implementation 'com.blink:blink-framework-openfeign:1.0.0-SNAPSHOT'
```

### 启用 Feign

```java
@SpringBootApplication
@EnableFeignClients(basePackages = "com.blink")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## 使用方式

### 定义 Feign 客户端

```java
@FeignClient(
    name = "base-app",
    path = "/sysUser",
    configuration = FeignClientConfig.class
)
public interface UserServiceClient {

    @PostMapping("/getSysUserList")
    ResponseDTO<PageDTO<SysUserVO>> getUserList(@RequestBody RequestDTO<QueryUserReq> request);

    @PostMapping("/getSysUserDetail")
    ResponseDTO<SysUserVO> getUserDetail(@RequestBody RequestDTO<QueryUserReq> request);
}
```

### 使用 Feign 客户端

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserServiceClient userServiceClient;

    public List<SysUserVO> getUsers() {
        RequestDTO<QueryUserReq> request = RequestDTO.newInstance(new QueryUserReq());
        ResponseDTO<PageDTO<SysUserVO>> response = userServiceClient.getUserList(request);
        return response.getBody().getRows();
    }
}
```

## 配置说明

### 默认配置

```yaml
blink:
  openfeign:
    # 连接超时（毫秒）
    connect-timeout: 5000
    # 读取超时（毫秒）
    read-timeout: 10000
    # 日志级别
    logger-level: full
    # 重试次数
    retry-count: 3
```

### Feign 配置类

```java
public class FeignClientConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            // 添加请求头
            template.header("X-Source", "feign-client");
            template.header("X-Trace-Id", TraceIdUtil.getTraceId());
        };
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            // 错误解码
            return new BlinkException("服务调用失败: " + methodKey);
        };
    }
}
```

## 注意事项

- 当前模块仅做了基本配置，功能待完善
- 建议使用 Dubbo 替代 Feign 进行服务间调用
- Feign 适合与非 Dubbo 服务集成

## 与 Dubbo 的对比

| 特性 | Feign | Dubbo |
|------|-------|-------|
| 协议 | HTTP | Triple（基于 HTTP/2）|
| 性能 | 一般 | 高 |
| 负载均衡 | 支持 | 支持 |
| 服务治理 | 基础 | 完善 |
| 响应式 | 不支持 | 支持 |
| 适用场景 | 外部服务 | 内部服务 |

## 相关模块

- `blink-base-api-dubbo`：Dubbo 接口定义
- `blink-gateway-admin-api-dubbo`：Gateway Admin Dubbo 接口
