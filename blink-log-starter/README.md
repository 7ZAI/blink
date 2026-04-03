# blink-log-starter

Blink 框架日志组件，提供操作日志入库、Controller 控制台日志、敏感数据脱敏等功能。

## 功能特性

- **操作日志入库** - 通过 `@RecordLog` 注解自动记录操作日志到数据库
- **Controller 日志** - 自动记录 Controller 方法的入参、出参、耗时
- **敏感数据脱敏** - 支持手机号、身份证、邮箱等敏感信息脱敏
- **函数式解耦** - 通过函数式接口与业务实现解耦，灵活扩展

## 快速开始

### 1. 添加依赖

```groovy
implementation 'com.blink:blink-log-spring-boot-starter:1.0.0-SNAPSHOT'
```

### 2. 实现函数式接口（入库日志必需）

```java
@Configuration
public class LogFunctionConfiguration {

    /**
     * 日志持久化实现（必需）
     */
    @Bean
    public LogPersistFunction<SysOperationLogDO> logPersistFunction(SysOperationLogService service) {
        return entity -> service.asyncSaveLog(entity);
    }

    /**
     * 日志转换器实现（必需）
     */
    @Bean
    public LogConverter<SysOperationLogDO> logConverter() {
        return record -> {
            SysOperationLogDO entity = new SysOperationLogDO();
            BeanUtil.copyProperties(record, entity);
            entity.setCreateTime(LocalDateTime.now());
            return entity;
        };
    }

    /**
     * 日志开关判断实现（可选，默认启用）
     */
    @Bean
    public LogEnabledFunction logEnabledFunction(SysConfigService configService) {
        return logType -> {
            // 从配置中心获取开关状态
            return configService.getBooleanConfig("log." + logType + ".enabled", true);
        };
    }

    /**
     * 用户信息提供实现（可选）
     */
    @Bean
    public UserInfoProviderFunction userInfoProviderFunction() {
        return () -> {
            String userId = BlinkRequestContextHolder.getUserId();
            String loginName = BlinkRequestContextHolder.getLoginName();
            if (StrUtil.isBlank(userId)) {
                return null;
            }
            return new UserInfoProviderFunction.UserInfo(Integer.valueOf(userId), loginName);
        };
    }
}
```

## 注解说明

### @RecordLog - 操作日志入库

标注在 Controller 方法上，自动记录操作日志到数据库。

```java
@RecordLog(type = LogType.OPERATION, description = "新增系统用户")
@PostMapping("/saveUser")
public ResponseDTO<EmptyBody> saveUser(@RequestBody @Validated RequestDTO<AddUserReq> reqDto) {
    // ...
}
```

**参数说明：**

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `type` | LogType | OPERATION | 日志类型：LOGIN、SYSTEM、OPERATION |
| `description` | String | "" | 操作描述 |
| `saveRequest` | boolean | true | 是否保存请求参数 |
| `saveResponse` | boolean | true | 是否保存响应结果 |

### @ConsoleLog - 控制台日志

标注在方法上，细粒度控制日志输出，优先级高于默认的 Controller 切面日志。

```java
@ConsoleLog(level = ConsoleLog.LogLevel.DEBUG, logCostTime = true)
@PostMapping("/detail")
public ResponseDTO<UserVO> getUserDetail(@RequestBody RequestDTO<QueryUserReq> reqDto) {
    // ...
}
```

**参数说明：**

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `logRequest` | boolean | true | 是否记录请求参数 |
| `logResponse` | boolean | true | 是否记录响应结果 |
| `logCostTime` | boolean | true | 是否记录耗时 |
| `level` | LogLevel | INFO | 日志级别：DEBUG、INFO、WARN、ERROR |

### @SensitiveField - 敏感字段脱敏

标注在实体类字段上，日志输出时自动脱敏。

```java
public class UserDTO {

    @SensitiveField(type = SensitiveType.PHONE)
    private String phone;

    @SensitiveField(type = SensitiveType.ID_CARD)
    private String idCard;

    @SensitiveField(type = SensitiveType.EMAIL)
    private String email;

    @SensitiveField(type = SensitiveType.CUSTOM, prefixKeep = 2, suffixKeep = 2)
    private String customField;
}
```

**支持的脱敏类型：**

| 类型 | 示例 | 效果 |
|------|------|------|
| PHONE | 13812345678 | 138****5678 |
| ID_CARD | 110101199001011234 | 110101********1234 |
| BANK_CARD | 6222021234567890 | 6222****7890 |
| EMAIL | example@qq.com | exa***@qq.com |
| NAME | 张三 | 张* |
| PASSWORD | 123456 | ****** |
| CUSTOM | 自定义 | 可配置前后保留位数 |

## 配置说明

```yaml
blink:
  log:
    # 入库日志配置
    record:
      enabled: true                    # 是否启用入库日志
      save-request: true               # 是否记录请求参数
      save-response: true              # 是否记录响应结果
      max-request-length: 4000         # 请求参数最大长度
      max-response-length: 4000        # 响应数据最大长度
      max-error-msg-length: 500        # 错误信息最大长度
      max-user-agent-length: 500       # User-Agent 最大长度

    # 控制台日志配置
    console:
      enable-controller-log: true      # 是否启用 Controller 日志
      upper-limit: 1000                # 日志长度上限
      auto-skip: false                 # 超长自动截断
      enable-sensitive: false          # 是否开启敏感数据脱敏
```

## 核心组件

### 函数式接口

| 接口 | 必需 | 说明 |
|------|------|------|
| `LogPersistFunction<T>` | 是 | 日志持久化函数，将日志保存到存储 |
| `LogConverter<T>` | 是 | 日志转换函数，将通用模型转换为业务实体 |
| `LogEnabledFunction` | 否 | 日志开关判断函数，控制日志是否记录 |
| `UserInfoProviderFunction` | 否 | 用户信息提供函数，获取当前登录用户信息 |

### 模型类

**OperationLogRecord** - 通用日志记录模型

```java
public class OperationLogRecord {
    private String logType;           // 日志类型
    private String description;       // 操作描述
    private Integer userId;           // 用户ID
    private String loginName;         // 登录名
    private String requestUrl;        // 请求URL
    private String requestMethod;     // 请求方法
    private String requestParams;     // 请求参数（已脱敏）
    private String responseData;      // 响应数据（已脱敏）
    private Integer executeStatus;    // 执行状态 0成功 1失败
    private String errorMsg;          // 错误信息
    private Integer executeTimeMs;    // 执行时长(毫秒)
    private String ipAddress;         // IP地址
    private String userAgent;         // 浏览器UA
    private LocalDateTime operationTime; // 操作时间
    private Map<String, Object> extraFields; // 扩展字段
}
```

## 使用示例

### 完整示例

```java
@RestController
@RequestMapping("/user")
public class UserController {

    @RecordLog(type = LogType.OPERATION, description = "新增用户")
    @PostMapping("/add")
    public ResponseDTO<EmptyBody> addUser(@RequestBody @Validated RequestDTO<AddUserReq> reqDto) {
        userService.addUser(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    @RecordLog(type = LogType.OPERATION, description = "删除用户", saveResponse = false)
    @PostMapping("/delete")
    public ResponseDTO<EmptyBody> deleteUser(@RequestBody RequestDTO<DeleteUserReq> reqDto) {
        userService.deleteUser(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    // 不记录入库日志，仅控制台输出
    @ConsoleLog(level = ConsoleLog.LogLevel.DEBUG)
    @PostMapping("/list")
    public ResponseDTO<PageResult<UserVO>> getUserList(@RequestBody RequestDTO<QueryUserReq> reqDto) {
        return ResponseDTO.newSuccessInstance(userService.getUserList(reqDto.getBody()));
    }
}
```

## 注意事项

1. **入库日志** 需要业务模块实现 `LogPersistFunction` 和 `LogConverter` 接口
2. **Controller 日志** 默认对 `com.blink` 包下的所有 Controller 生效
3. **敏感数据脱敏** 需要配置 `blink.log.console.enable-sensitive: true` 或使用 `@SensitiveField` 注解
4. **异步持久化** 建议在 `LogPersistFunction` 实现中使用 `@Async` 或线程池

## 版本要求

- JDK 17+
- Spring Boot 3.2+
- Spring AOP