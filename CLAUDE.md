# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build all modules
./gradlew build

# Build specific module
./gradlew :blink-base:blink-base-app:build

# Run tests
./gradlew test

# Run single test class
./gradlew :blink-base:blink-base-app:test --tests "com.blink.base.controller.SysUserControllerTest"

# Publish modules to local Maven repository
./gradlew publishToMavenLocal

# Publish to Nexus (requires credentials in gradle.properties)
./gradlew publish
```

## Frontend Commands (blink-base-web)

```bash
cd blink-base/blink-base-web

# Install dependencies
npm install

# Development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

## Architecture Overview

This is a microservice framework built with Spring Boot 3.2 + Spring Cloud Alibaba. Key design principle: **modular Starter packaging** similar to Spring Boot Starters - each module is independently publishable with auto-configuration.

### Module Categories

**Basic Modules (Starter libraries)**:
- `blink-framework-common`: Common utilities, DTOs, exceptions, JWT handling
- `blink-datasource-starter`: MyBatis-Plus + Druid + code generator
- `blink-redis-starter`: Redis operations, distributed ID generation
- `blink-web-starter`: Global exception handling, logging, cache preheating
- `blink-framework-mq`: RabbitMQ message queue封装
- `blink-framework-validation`: Custom validation annotations

**Application Modules**:
- `blink-base`: RBAC backend management service (multi-module: `blink-base-app` for implementation, `blink-base-api-dubbo` for Dubbo interfaces)
- `blink-gateway-reactive`: Reactive API Gateway based on Spring Cloud Gateway
- `blink-base-web`: Vue 3 + TypeScript + Element Plus frontend

### Key Technologies

| Layer | Technology |
|-------|------------|
| JDK | 17+ |
| Framework | Spring Boot 3.2, Spring Cloud 2023.0.3, Spring Cloud Alibaba 2023.0.3.2 |
| RPC | Dubbo 3.3 (Triple protocol) |
| ORM | MyBatis-Plus 3.5 |
| Cache | Redis 7.0+ |
| Registry/Config | Nacos 2.3+ |
| Database | MySQL 8.0+ |
| MQ | RabbitMQ |
| Build | Gradle 8.8+ |

## Critical Development Rules

### Controller Development (重要)

**1. 只能使用 POST 请求**

整个 Blink 项目的业务开发只能使用 POST 请求，禁止使用 GET、PUT、DELETE 等其他 HTTP 方法。

```java
// Correct
@PostMapping("/getUserList")
public ResponseDTO<UserRsp> getUserList(@RequestBody RequestDTO<QueryUserReq> reqDto) { }

// Wrong
@GetMapping("/getUserList")  // 禁止使用 GET
@PutMapping("/updateUser")   // 禁止使用 PUT
@DeleteMapping("/deleteUser") // 禁止使用 DELETE
```

**2. 统一使用 RequestDTO/ResponseDTO 包裹入参出参**

所有 Controller 方法的入参必须是 `RequestDTO<T>`，出参必须是 `ResponseDTO<T>`，真正的业务参数放在 `body` 中。

```java
// Correct - 使用专门的 DTO 类
@PostMapping("/saveUser")
public ResponseDTO<EmptyBody> saveUser(@RequestBody @Validated RequestDTO<AddUserReq> reqDto) {
    userService.saveUser(reqDto.getBody());
    return ResponseDTO.newSuccessInstance();
}

// Wrong - 禁止使用 Map、String、List 等作为 body 类型
@PostMapping("/saveRoute")
public ResponseDTO<EmptyBody> saveRoute(@RequestBody RequestDTO<Map<String, Object>> reqDto) { } // ❌

@PostMapping("/deleteRoute")
public ResponseDTO<EmptyBody> deleteRoute(@RequestBody RequestDTO<List<String>> reqDto) { } // ❌

@PostMapping("/getDetail")
public ResponseDTO<Object> getDetail(@RequestBody RequestDTO<String> reqDto) { } // ❌
```

**3. 分页查询规范 (重要)**

**继承规则：**
- **请求 DTO** 继承 `Page`（不包含结果集，仅分页参数）
- **响应 DTO** 继承 `PageDTO<T>`（包含结果集 `rows`）

**原因：** `Page` 是通用分页基类，已实现 `Serializable`；`PageDTO<T>` 继承 `Page` 并添加了 `rows` 字段用于存放结果集。请求 DTO 只需要分页参数，无需 `rows` 字段。

**禁止重复实现 Serializable：** `Page` 已实现 `Serializable`，子类无需再声明。

```java
// 分页请求 DTO - 继承 Page（无泛型，无需 Serializable）
@Getter
@Setter
public class QuerySysUserReq extends Page {
    private String loginName;
}

// 分页响应 DTO - 继承 PageDTO<T>（无需 Serializable）
@Getter
@Setter
public class SysUserRsp extends PageDTO<SysUserVO> {
}

// Service 实现
@Override
public SysUserRsp getSysUserList(QuerySysUserReq req) {
    SysUserRsp rsp = new SysUserRsp();
    return PageUtils.queryPage(req, () -> sysUserMapper.selectUserList(req), rsp);
}
```

**错误示例：**

```java
// ❌ Wrong - 请求 DTO 不应继承 PageDTO
public class QuerySysUserReq extends PageDTO { }

// ❌ Wrong - 请求 DTO 不应继承 PageDTO<T>
public class QuerySysUserReq extends PageDTO<Void> { }

// ❌ Wrong - 父类已实现 Serializable，无需重复声明
public class QuerySysUserReq extends Page implements Serializable { }
```

### Boolean Field Naming (重要)

**禁止使用 `boolean isXxx` 格式命名字段，必须使用 `Boolean xxx` 格式。**

当 `boolean` 类型字段名以 `is` 开头时，Lombok 生成的 getter/setter 与 Jackson 期望不匹配，导致反序列化失败。

```java
// Wrong - boolean isXxx 导致反序列化失败
@Data
public class DeleteReq {
    private boolean isBatchDelete;  // ❌
}
// Lombok 生成: isBatchDelete() / setBatchDelete()
// Jackson 期望: setIsBatchDelete() → 匹配失败，字段值永远为 false

// Correct - 使用 Boolean 包装类型，去掉 is 前缀
@Data
public class DeleteReq {
    private Boolean batchDelete;  // ✅
}
// Lombok 生成: getBatchDelete() / setBatchDelete()
// Jackson 正确匹配 JSON 字段 "batchDelete"
```

**判断逻辑必须使用 `Boolean.TRUE.equals()` 避免空指针：**

```java
// Correct
if (Boolean.TRUE.equals(req.getBatchDelete())) {
    // 批量删除逻辑
}

// Wrong - 可能 NPE
if (req.getBatchDelete()) {  // ❌ 如果 batchDelete 为 null 会 NPE
}
```

### Exception Handling (重要)

**1. 业务异常 - 使用预定义错误码**

业务异常指可预见的业务逻辑错误（数据不存在、状态校验失败、权限不足等），必须使用预定义错误码。

```java
// Correct
BlinkException.throwBusinessException(ErrCodeConstant.USER_NOT_EXIST);

// Wrong
throw new BlinkException("用户不存在", "USER_NOT_FOUND");  // ❌
```

**2. 系统异常 - 使用模块前缀错误码**

系统异常指不可预见的运行时错误（数据库连接失败、IO异常等），使用 `BlinkException(message, e, errorCode)` 格式。

```java
try {
    // 系统操作
} catch (BlinkException e) {
    throw e;  // 业务异常直接抛出
} catch (Exception e) {
    log.error("操作失败: {}", e.getMessage(), e);
    throw new BlinkException("操作失败: " + e.getMessage(), e, "GATE0001");
}
```

**3. 错误码定义规范**

在模块的 `ErrCodeConstant` 中预定义错误码，按模块使用不同前缀：

| 模块 | 前缀 | 示例 |
|------|------|------|
| 通用业务 | `BUSS00XX` | `BUSS0001` |
| 网关模块 | `GATE00XX` | `GATE0001` |
| 参数校验 | `PARAM00XX` | `PARAM0001` |

```java
// ErrCodeConstant.java
public interface ErrCodeConstant {
    // 业务错误码
    String DATA_NOT_EXIST = "GATE0020";
    String CHANNEL_NOT_EXIST = "GATE0022";

    // 参数校验错误码
    String PARAMETER_NOT_NULL = "PARAM0001";
}
```

### Constant Class (重要)

**常量类按类型分离，禁止混合定义：**

```java
// Correct - 按类型分离
public interface ErrCodeConstant {
    String DATA_NOT_EXIST = "GATE0020";
}

public interface RedisKeyConstant {
    String CHANNEL_INFO = "blink:channel:info:";
}

public interface ConfigValueConstant {
    Byte SWITCH_OPEN = 0;
}

// Wrong - 混合定义
public interface ErrCodeConstant {
    String DATA_NOT_EXIST = "GATE0020";
    String CHANNEL_INFO = "blink:channel:info:";  // ❌ Redis Key
    Byte SWITCH_OPEN = 0;                          // ❌ 配置值
}
```

### Code Comments

### Logging Specification (重要)

**1. 日志框架**

所有类必须使用 Lombok 的 `@Slf4j` 注解生成日志对象，禁止手动创建 Logger。

```java
// Correct
@Slf4j
@Service
public class SysUserServiceImpl implements SysUserService {
    public void someMethod() {
        log.info("User saved successfully, userId: {}", userId);
    }
}

// Wrong - 禁止手动创建 Logger
private static final Logger log = LoggerFactory.getLogger(SysUserServiceImpl.class);  // ❌
```

**2. 日志级别使用规范**

| 级别 | 使用场景 |
|------|----------|
| `trace` | 详细业务流程跟踪（生产环境默认关闭） |
| `debug` | 开发调试信息（生产环境默认关闭） |
| `info` | 关键业务节点、接口调用成功、系统状态变化 |
| `warn` | 可恢复的异常、参数校验失败、业务边界条件 |
| `error` | 系统异常、数据库错误、第三方服务调用失败 |

```java
// info - 关键业务节点
log.info("User login successful, loginName: {}, userId: {}", loginName, userId);

// warn - 业务边界条件
log.warn("Data filter entity not registered: {}", entityClass);

// error - 系统异常需包含堆栈
log.error("Failed to save user, loginName: {}", loginName, e);
```

**3. Service 层日志规范**

关键操作必须记录日志：

```java
@Slf4j
@Service
public class SysDataFilterServiceImpl implements SysDataFilterService {

    // 保存操作
    @Override
    public void saveDataFilter(AddDataFilterReq req) {
        // 业务逻辑...
        log.info("新增数据过滤规则成功，ID: {}, 名称: {}", dataFilter.getDataFilterId(), dataFilter.getDataFilterName());
    }

    // 更新操作
    @Override
    public void updateDataFilter(UpdateDataFilterReq req) {
        // 业务逻辑...
        log.info("更新数据过滤规则成功，ID: {}", req.getDataFilterId());
    }

    // 删除操作
    @Override
    public void deleteDataFilter(DataFilterIdReq req) {
        // 业务逻辑...
        log.info("删除数据过滤规则成功，ID: {}", req.getDataFilterId());
    }

    // 异常处理
    @Override
    public void refreshCache() {
        try {
            // 刷新缓存逻辑
            log.info("已刷新所有数据权限缓存");
        } catch (Exception e) {
            log.error("刷新数据权限缓存失败", e);
            throw e;
        }
    }
}
```

**4. 禁止使用的日志方式**

```java
// Wrong - 禁止使用 System.out/err
System.out.println("debug info");     // ❌
System.err.println("error info");     // ❌

// Wrong - 禁止使用 printStackTrace
try {
    // some code
} catch (Exception e) {
    e.printStackTrace();  // ❌
}

// Wrong - 敏感信息禁止记录日志
log.info("User password: {}", user.getPassword());  // ❌

// Wrong - 禁止记录大型对象
log.info("Request params: {}", largeObject);  // ❌
```

**5. 日志格式规范**

```
[模块名] 操作描述 | 参数1: {}, 参数2: {}
```

```java
log.info("[SysUser] 用户登录成功 | loginName: {}, ip: {}", loginName, ip);
log.warn("[SysDataFilter] 实体类未注册 | entityClass: {}", entityClass);
log.error("[SysUser] 保存用户失败 | loginName: {}, error: {}", loginName, e.getMessage(), e);
```

**6. Controller 层日志规范**

Controller 层建议记录请求入口日志，异常由全局异常处理器统一处理：

```java
@Slf4j
@RestController
@RequestMapping("/user")
public class SysUserController {

    @PostMapping("/login")
    public ResponseDTO<LoginRsp> login(@RequestBody @Valid RequestDTO<LoginReq> reqDto) {
        log.info("[SysUser] 收到登录请求 | loginName: {}", reqDto.getBody().getLoginName());
        // 业务逻辑...
        return ResponseDTO.newSuccessInstance(rsp);
    }
}
```

### Null Check Specification (重要)

**1. 统一使用 Hutool 工具类**

项目已引入 Hutool 库，需统一使用其工具类进行空值判断：

| 场景 | 推荐使用 | 说明 |
|------|----------|------|
| 字符串判空 | `StrUtil.isBlank()` / `StrUtil.isNotBlank()` | 优于 `== null \|\| "".equals()` |
| 字符串非空 | `StrUtil.isNotEmpty()` | 仅判断 null 和空串 |
| 集合判空 | `CollUtil.isEmpty()` / `CollUtil.isNotEmpty()` | 优于 `== null \|\| .isEmpty()` |
| 数组判空 | `ArrayUtil.isEmpty()` | - |
| 通用对象 | `ObjectUtil.isNull()` / `ObjectUtil.isNotNull()` | - |

```java
// Correct
if (StrUtil.isBlank(userName)) {
    // 用户名为空
}
if (CollUtil.isEmpty(userList)) {
    // 用户列表为空
}
if (ObjectUtil.isNotNull(user)) {
    // 用户不为空
}

// Wrong - 禁止使用
if (userName == null || userName.equals("")) { }  // ❌
if (userList == null || userList.size() == 0) { }  // ❌
```

**2. 返回值判空处理**

```java
// 推荐： Optional 处理可能为 null 的返回值
UserDO user = Optional.ofNullable(userMapper.selectById(userId))
    .orElseThrow(() -> BlinkException.throwBusinessException(BaseErrCodeConstant.DATA_NOT_EXIST));

// 推荐：使用三目运算符处理默认值
String nickName = ObjectUtil.isNotNull(user) ? user.getNickName() : "默认用户";

// 推荐：集合空安全处理
List<UserVO> userVOList = CollUtil.isEmpty(userDOList)
    ? new ArrayList<>()
    : userDOList.stream().map(this::convertToVO).collect(Collectors.toList());
```

**3. 链式调用空安全**

```java
// 推荐：使用 map 转换 Optional
Optional.ofNullable(user.getAddress())
    .map(Address::getCity)
    .ifPresent(city -> log.info("用户所在城市: {}", city));
```

**4. 禁止的空值判断模式**

```java
// 禁止：直接使用 == null 作为判断唯一条件（应说明为什么判断为空）
if (user == null) { }  // ❌

// 禁止：使用 equals 判断 null
if (userName.equals("")) { }  // ❌ 如果 userName 为 null 会 NPE

// 禁止：在 for 循环中不做空安全判断
for (User u : userList) { }  // ❌ 需先判断 CollUtil.isNotEmpty(userList)

// 禁止：使用 String.trim() 判断前不判空
if (str.trim().isEmpty()) { }  // ❌ 如果 str 为 null 会 NPE
```

### Code Comments

**1. 类和方法必须有 Javadoc 注释**

```java
/**
 * 系统用户服务实现类
 *
 * @author binblink
 * @since 2023-12-26
 */
@Service
public class SysUserServiceImpl implements SysUserService {

    /**
     * 根据用户ID查询用户信息
     *
     * @param userId 用户ID
     * @return 用户信息VO
     * @throws BlinkException 用户不存在时抛出异常
     */
    public UserVO getUserById(Integer userId) throws BlinkException {
        // 校验用户ID有效性
        if (userId == null || userId <= 0) {
            BlinkException.throwBusinessException(ErrCodeConstant.PARAMETER_OUT_RANGE);
        }
        // 查询用户信息
        SysUserDO user = sysUserMapper.selectById(userId);
        return BeanUtil.copyProperties(user, UserVO.class);
    }
}
```

**2. 注释位置规范**

- 行注释放在代码行**正上方**，禁止使用尾部注释

```java
// Correct - 头部注释
// 参数校验通过后保存用户
sysUserService.saveSysUser(reqDto.getBody());

// Wrong - 尾部注释
sysUserService.saveSysUser(reqDto.getBody()); // 保存用户 ❌
```

### Object Conversion

使用 `BeanUtil` 进行 DTO/VO/Entity 转换：

```java
BeanUtil.copyProperties(sourceDTO, targetEntity);
```

### Database Data Generation (重要)

**超级管理员角色(role_id=1)不需要生成关联数据**

超级管理员在程序中特殊处理，自动拥有所有权限。生成菜单、权限等关联数据时，**禁止**为超级管理员角色插入 `sys_role_menu_rela`、`sys_role_perm_rela` 等关联表数据。

```sql
-- Wrong - 超级管理员不需要关联数据
INSERT INTO sys_role_menu_rela (role_id, menu_id) VALUES (1, 27);  -- ❌

INSERT INTO sys_role_perm_rela (role_id, ac_id) VALUES (1, 139);   -- ❌

-- Correct - 只为普通角色生成关联数据
INSERT INTO sys_role_menu_rela (role_id, menu_id) VALUES (13, 27); -- ✅

INSERT INTO sys_role_perm_rela (role_id, ac_id) VALUES (13, 139);  -- ✅
```

**原因：** 超级管理员的权限校验在代码层面通过 `superFlag` 字段直接绕过，无需查询关联表。

### Dependency Module Changes

When modifying dependency modules (e.g., `blink-web-starter`, `blink-redis-starter`) without main class:
1. Build and publish the module: `./gradlew :module-name:publishToMavenLocal`
2. Rebuild the target application

## Code Generation

Use `CodeGenerator` in `blink-datasource-starter` to generate CRUD code:

```java
String url = "jdbc:mysql://localhost:3306/blink?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8";
String username = "root";
String password = "123456";
CodeGenerator.generateByCustomTemplate(url, username, password);
```

Generates: Controller, Service, ServiceImpl, Mapper, mapper.xml, DO entity, and DTO classes.

## Gateway Architecture

The reactive gateway (`blink-gateway-reactive`) uses a filter chain:

```
Request → LogFilter → IpFilter → RequestHeaderValidationFilter → Security → SignatureFilter → ReplayAttackPreventionFilter → CryptFilter → RewriteRequestBodyFilter → Downstream
```

Key features:
- Dynamic routing (Nacos or Redis-based)
- Multi-channel management with appKey/appSecret
- JWT authentication for channels, stateful token for internal users
- AES+RSA hybrid encryption, SHA-256 signing
- Rate limiting with Redis token bucket


## Naming Conventions

| Type | Convention | Example |
|------|------------|---------|
| Database table | `sys_` prefix, snake_case | `sys_user`, `sys_config_group` |
| Entity class | Suffix `DO` | `SysUserDO` |
| Request DTO | Suffix `Req` | `AddSysUserReq`, `QueryUserReq` |
| Response DTO | Suffix `Rsp` | `QueryUserRsp` |
| Mapper | Suffix `Mapper` | `SysUserMapper` |
| Service impl | Suffix `ServiceImpl` | `SysUserServiceImpl` |
| Error codes | Module prefix in `ErrCodeConstant` | `GATE0001`, `BUSS0001`, `PARAM0001` |
| Redis Key | `RedisKeyConstant` | `CHANNEL_INFO` |
| Config Value | `ConfigValueConstant` | `SWITCH_OPEN` |

## Dubbo Integration

Provider (`blink-base-app`):
```java
@DubboService(async = true, timeout = 10000)
public class BaseDubboServiceImpl implements BaseDubboService { }
```

Consumer (gateway reactive):
```java
// Wrap synchronous Dubbo call in Mono for reactive return
public Mono<Result> someMethod() {
    CompletableFuture<Result> future = CompletableFuture.supplyAsync(
        () -> baseDubboService.someMethod()
    );
    return Mono.fromFuture(future);
}
```

## Request/Response Format

All APIs use unified DTO format:

```json
{
  "requestId": "uuid",
  "traceId": "distributed-trace-id",
  "token": "user-token",
  "loginName": "username",
  "body": { /* actual business data */ }
}
```

Response:
```json
{
  "code": "00000",
  "msg": "success",
  "body": { /* response data */ }
}
```

## Frontend Development Rules (blink-base-web)

详见 [docs/rules/frontend-rules.md](./docs/rules/frontend-rules.md)





