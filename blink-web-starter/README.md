# blink-web-starter

Web 应用通用功能封装，提供全局异常处理、限流熔断、请求日志脱敏、动态线程池等开箱即用的能力。

## 功能特性

| 功能模块 | 说明 |
|---------|------|
| 全局异常处理 | 统一异常响应格式，支持多语言错误消息 |
| 限流熔断 | 基于 Resilience4j 的限流、熔断、重试 |
| 请求日志脱敏 | 自动脱敏敏感字段（手机号、身份证等） |
| 动态线程池 | CPU/IO/定时任务三种线程池，支持动态配置 |
| 请求上下文 | 跨方法传递请求元信息 |
| MDC 链路追踪 | traceId、userName 日志追踪 |

## 快速开始

### 引入依赖

```gradle
implementation 'com.blink:blink-web-spring-boot-starter:1.0.0-SNAPSHOT'
```

### 配置

```yaml
blink:
  web:
    log:
      auto-skip: true           # 自动截断超长日志
      upper-limit: 1000         # 日志最大长度
    thread-pool:
      cpu:
        core-size: 4
        max-size: 8
      io:
        core-size: 8
        max-size: 16
    resilience:
      enabled: true             # 启用限流熔断
```

---

## 全局异常处理

所有 `BlinkException` 会被统一捕获，根据异常代码和当前语言环境返回友好错误信息。

### 使用方式

```java
// 业务代码中抛出异常
throw new BlinkException("SYS00001");

// 带自定义消息
throw new BlinkException("SYS00001", "自定义错误信息");
```

### 响应格式

```json
{
  "code": "SYS00001",
  "message": "系统异常，请稍后重试",
  "timestamp": 1699999999999,
  "traceId": "abc123"
}
```

### 规范

- 业务错误 HTTP 状态码依然为 200
- 捕获其他异常时，请转换为 `BlinkException` 后再抛出

---

## 限流熔断

基于 Resilience4j 实现的弹性能力，支持限流、熔断、重试。

### 预定义策略

| 策略 | 限流 (次/秒) | 熔断 (失败率/等待时间) | 重试 (次数/间隔) |
|------|-------------|----------------------|-----------------|
| **default** | 100 | 50% / 60s | 3次 / 500ms |
| **strict** | 50 | 30% / 120s | 2次 / 200ms |
| **lenient** | 200 | 70% / 30s | 5次 / 1000ms |

### 限流 @RateLimit

```java
// 基本用法
@RateLimit(name = "queryApi")
public Result<List<User>> queryUsers() { ... }

// 自定义限流参数
@RateLimit(
    name = "orderApi",
    limitForPeriod = 50,
    limitRefreshPeriod = 1,
    timeoutDuration = 0
)
public Result<Order> createOrder(OrderDTO dto) { ... }

// 使用严格策略
@RateLimit(name = "sensitiveApi", configName = "strict")
public Result<Data> sensitiveOperation() { ... }

// 带降级方法
@RateLimit(name = "api", fallbackMethod = "queryFallback")
public Result<Data> query() { ... }

public Result<Data> queryFallback(Throwable t) {
    return Result.fail("系统繁忙，请稍后重试");
}
```

### 熔断 @CircuitBreaker

```java
// 基本用法
@CircuitBreaker(name = "externalApi")
public Result<Data> callExternal() { ... }

// 使用严格策略
@CircuitBreaker(name = "paymentApi", configName = "strict")
public Result<Payment> processPayment() { ... }

// 带降级方法
@CircuitBreaker(name = "userApi", fallbackMethod = "getUserFallback")
public Result<User> getUser(Long id) { ... }

public Result<User> getUserFallback(Long id, Throwable t) {
    return Result.fail("服务暂时不可用");
}
```

### 重试 @Retry

```java
// 基本用法
@Retry(name = "unstableApi")
public Result<Data> callUnstable() { ... }

// 自定义重试参数
@Retry(name = "networkApi", maxAttempts = 5, waitDuration = 1000)
public Result<Data> networkCall() { ... }

// 使用快速重试策略
@Retry(name = "quickApi", configName = "quick")
public Result<Data> quickRetry() { ... }
```

### 组合使用

```java
@RateLimit(name = "api")
@CircuitBreaker(name = "api", configName = "strict")
@Retry(name = "api", configName = "quick")
public Result<Data> protectedApi() { ... }
```

---

## 请求日志脱敏

自动对日志中的敏感字段进行脱敏处理。

### 使用方式

在 DTO 字段上添加 `@SensitiveField` 注解：

```java
import com.blink.log.annotation.SensitiveField;
import com.blink.framework.core.sensitive.SensitiveType;

@Data
public class UserRegisterDTO {

    private String userName;

    @SensitiveField(type = SensitiveType.PHONE)
    private String phone;           // 138****8888

    @SensitiveField(type = SensitiveType.ID_CARD)
    private String idCard;          // 110***********1234

    @SensitiveField(type = SensitiveType.PASSWORD)
    private String password;        // ******

    @SensitiveField(type = SensitiveType.EMAIL)
    private String email;           // abc****@qq.com

    @SensitiveField(type = SensitiveType.BANK_CARD)
    private String bankCard;        // 6222****1234

    @SensitiveField(type = SensitiveType.CUSTOM, prefixKeep = 2, suffixKeep = 2)
    private String customField;     // ab****yz
}
```

### 支持的脱敏类型

| 类型 | 原始值 | 脱敏后 |
|------|--------|--------|
| PHONE | 13812345678 | 138****5678 |
| ID_CARD | 110101199001011234 | 110101********1234 |
| BANK_CARD | 6222021234567890 | 6222****7890 |
| EMAIL | abcdef@qq.com | abc****@qq.com |
| NAME | 张三丰 | 张* |
| PASSWORD | mypassword123 | ****** |
| ADDRESS | 北京市海淀区中关村大街1号 | 北京市海淀区中**** |

---

## 动态线程池

根据 CPU 核心数动态计算线程池参数，配合 `@Async` 使用。

### 配置

```yaml
blink:
  web:
    thread-pool:
      cpu:
        core-size: 4
        max-size: 8
        queue-capacity: 100
        dynamic-based-on-cpu: true
      io:
        core-size: 8
        max-size: 32
        queue-capacity: 500
      scheduled:
        core-size: 2
```

### 使用

```java
@Service
public class AsyncService {

    @Async("cpuTaskExecutor")
    public void cpuIntensiveTask() {
        // CPU 密集型任务
    }

    @Async("ioTaskExecutor")
    public void ioIntensiveTask() {
        // IO 密集型任务
    }

    @Async("scheduledTaskExecutor")
    public void scheduledTask() {
        // 定时任务
    }
}
```

---

## 请求上下文

跨方法传递请求元信息，避免显式传参。

### 使用方式

```java
// 获取当前请求上下文
BlinkRequestContext context = BlinkRequestContextHolder.getContext();

String requestId = context.getRequestId();
String appName = context.getAppName();
String clientIp = context.getClientIp();
String uri = context.getUri();
String method = context.getMethod();
Map<String, String> headers = context.getHeaders();

// 设置自定义属性
context.setAttribute("userId", "12345");
String userId = context.getAttribute("userId");
```

---

## 缓存预热

应用启动前执行预加载操作。

### 使用方式

```java
@Component
@PreHeatData(enable = true, method = "preloadData")
public class DataPreloader {

    @Resource
    private SysDataDictMapper dataDictMapper;

    @Resource
    private CacheComponent cacheComponent;

    public void preloadData() {
        // 预加载数据字典
        List<SysDataDictDO> dictList = dataDictMapper.selectList(null);
        for (SysDataDictDO dict : dictList) {
            cacheComponent.set("dict:" + dict.getDictCode(), dict.getDictValue());
        }
    }
}
```

---

## Controller 日志切面

自动记录 Controller 方法的入参、出参、耗时。

### 跳过日志记录

```java
@RestController
public class UserController {

    @LogExecution  // 标记此注解，跳过 AOP 日志记录
    public Result<User> getUser(Long id) {
        return Result.success(user);
    }
}
```

---

## MDC 链路追踪

自动在日志中添加 traceId 和 userName，便于链路追踪。

### 日志格式配置

```xml
<!-- logback-spring.xml -->
<pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{traceId}] [%X{userName}] [%thread] %-5level %logger{36} - %msg%n</pattern>
```

### 日志输出示例

```
2024-01-01 12:00:00.000 [abc123] [admin] [http-nio-8080-exec-1] INFO  c.b.example.controller.UserController - 进入方法
```

---


---

## 自动配置

模块自动配置以下组件：

| 组件 | 条件 |
|------|------|
| GlobalExceptionHandler | 默认启用 |
| BlinkControllerLogAspect | 默认启用 |
| DynamicThreadPoolAutoConfig | 默认启用 |
| ResilienceAutoConfiguration | `blink.web.resilience.enabled=true` |
