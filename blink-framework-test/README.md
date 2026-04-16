# blink-framework-test

> Blink 测试框架模块 - 统一管理所有后端应用的测试依赖和通用测试工具

## 概述

本模块提供统一的测试基础设施，包括：
- **测试依赖统一管理** - 使用 `api` 暴露依赖，避免各模块重复配置
- **测试基类** - 单元测试、集成测试、数据层测试、响应式测试基类
- **测试工具类** - Mock 辅助、JSON 测试、反射测试、断言辅助等
- **Testcontainers 配置** - MySQL/Redis 容器化管理
- **WireMock 支持** - HTTP API Mock，用于集成测试
- **Spring Security Test** - 安全上下文 Mock 支持
- **Fixture 管理** - 共享测试数据管理
- **测试分类注解** - 支持 Gradle 任务按类型运行测试

---

## 快速开始

### 1. 引入依赖

在需要测试的模块 `build.gradle` 中添加：

```groovy
dependencies {
    // 引入测试框架，自动获得所有测试依赖
    testImplementation 'com.blink:blink-framework-test:1.0.0-SNAPSHOT'
}
```

引入后自动获得以下依赖（无需重复配置）：
- JUnit 5 (`junit-jupiter`, `junit-jupiter-params`)
- Mockito (`mockito-core`, `mockito-junit-jupiter`)
- AssertJ (`assertj-core`)
- Spring Boot Test (`spring-boot-starter-test`)
- Testcontainers (`testcontainers`, `junit-jupiter`, `mysql`)
- H2 内存数据库
- JsonPath
- Awaitility
- WireMock (`wiremock-standalone`)
- Spring Security Test (`spring-security-test`)
- Reactor Test (`reactor-test`)
- WebFlux (`spring-boot-starter-webflux`)

### 2. 选择测试基类

| 测试类型 | 基类 | 适用场景 |
|---------|------|---------|
| `BlinkUnitTest` | 单元测试 | 不依赖 Spring 容器的纯逻辑测试 |
| `BlinkIntegrationTest` | 集成测试 | 需要 Spring Boot 容器和 Testcontainers |
| `BlinkRepositoryTest` | 数据层测试 | Mapper/Repository 层测试（H2 内存数据库） |
| `BlinkReactiveTest` | 响应式测试 | WebFlux 响应式应用测试（gateway-reactive） |

---

## 使用示例

### 单元测试

```java
import com.blink.framework.test.annotation.UnitTest;
import com.blink.framework.test.base.BlinkUnitTest;
import com.blink.framework.test.builder.TestDataBuilder;
import com.blink.framework.test.helper.MockHelper;
import com.blink.framework.common.context.BlinkRequestContextHolder;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@UnitTest
class SysUserServiceTest extends BlinkUnitTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @InjectMocks
    private SysUserServiceImpl sysUserService;

    @Test
    void should_return_user_when_exists() {
        // 使用 TestDataBuilder 构建测试数据
        RequestDTO<QuerySysUserReq> request = TestDataBuilder.requestDTO(
            new QuerySysUserReq(1, "admin")
        );

        // Mock 数据
        SysUserDO user = new SysUserDO();
        user.setUserId(1);
        user.setLoginName("admin");
        when(sysUserMapper.selectById(1)).thenReturn(user);

        // 执行测试
        SysUserVO result = sysUserService.getUserDetail(request.getBody());

        // AssertJ 断言
        assertThat(result.getLoginName()).isEqualTo("admin");
    }

    @Test
    void should_mock_static_method() {
        // Mock 静态方法（需要在 try-with-resources 中使用）
        try (MockedStatic<BlinkRequestContextHolder> mock =
                MockHelper.mockRequestContext("1", "admin")) {
            
            // 此时 BlinkRequestContextHolder.getUserId() 返回 "1"
            String userId = BlinkRequestContextHolder.getUserId();
            assertThat(userId).isEqualTo("1");
        }
    }
}
```

### 集成测试（使用 Testcontainers）

```java
import com.blink.framework.test.annotation.IntegrationTest;
import com.blink.framework.test.base.BlinkIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class SysUserControllerIT extends BlinkIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void should_create_user_successfully() {
        // 构建请求
        AddSysUserReq req = new AddSysUserReq();
        req.setLoginName("newuser");
        req.setUsername("新用户");

        RequestDTO<AddSysUserReq> request = TestDataBuilder.requestDTO(req);

        // 发送 HTTP 请求（自动使用 Testcontainers 的 MySQL/Redis）
        ResponseEntity<ResponseDTO> response = restTemplate.postForEntity(
            "/api/user/save", 
            request, 
            ResponseDTO.class
        );

        // 验证响应
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().getMsgCode()).isEqualTo("BLINK0000");
    }
}
```

**注意**：集成测试需要本地安装 Docker 并启动。

### 数据层测试（使用 H2）

```java
import com.blink.framework.test.annotation.RepositoryTest;
import com.blink.framework.test.base.BlinkRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import static org.assertj.core.api.Assertions.assertThat;

@RepositoryTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SysUserMapperTest extends BlinkRepositoryTest {

    @Autowired
    private SysUserMapper sysUserMapper;

    @BeforeEach
    void setup() {
        // 加载测试数据脚本
        executeInitScript("schema-test.sql");
    }

    @Test
    void should_insert_and_select_user() {
        SysUserDO user = new SysUserDO();
        user.setLoginName("testuser");
        user.setUsername("测试用户");
        
        sysUserMapper.insert(user);
        
        SysUserDO found = sysUserMapper.selectById(user.getUserId());
        assertThat(found.getLoginName()).isEqualTo("testuser");
    }

    @Test
    void should_clear_table_after_test() {
        // 测试后清理
        clearTable("sys_user");
        resetAutoIncrement("sys_user");
    }
}
```

### 响应式测试（WebFlux）

用于测试 `blink-gateway-reactive` 等响应式应用：

```java
import com.blink.framework.test.annotation.IntegrationTest;
import com.blink.framework.test.base.BlinkReactiveTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class GatewayHandlerTest extends BlinkReactiveTest {

    @Test
    void should_return_response_when_route_exists() {
        RequestDTO<QueryRouteReq> request = TestDataBuilder.requestDTO(new QueryRouteReq());

        webTestClient
            .post()
            .uri("/api/route/getRouteList")
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody(ResponseDTO.class)
            .value(response -> assertThat(response.getMsgCode()).isEqualTo("BLINK0000"));
    }

    @Test
    void should_return_error_when_unauthorized() {
        webTestClient
            .get()
            .uri("/api/admin/protected")
            .exchange()
            .expectStatus().is4xxClientError();
    }
}
```

---

## 测试工具类

### TestDataBuilder（测试数据构建器）

```java
// 构建 RequestDTO（自动生成 requestId/traceId）
RequestDTO<MyReq> request = TestDataBuilder.requestDTO(new MyReq());

// 构建空 RequestDTO
RequestDTO<Void> empty = TestDataBuilder.emptyRequestDTO();

// 构建分页请求
Page page = TestDataBuilder.page(1, 10);  // 第1页，10条
Page defaultPage = TestDataBuilder.defaultPage();  // 默认分页

// 生成随机测试数据
String loginName = TestDataBuilder.randomLoginName();  // test_a1b2c3d4
String email = TestDataBuilder.randomEmail();          // test_x@test.com
String phone = TestDataBuilder.randomPhone();          // 13812345678
```

### MockHelper（Mock 辅助工具）

```java
// Mock 静态方法
MockedStatic<MyUtils> mock = MockHelper.mockStatic(MyUtils.class);

// Mock BlinkRequestContextHolder（常用场景）
MockedStatic<BlinkRequestContextHolder> mock = 
    MockHelper.mockRequestContext("1", "admin");
// 自动设置 getUserId() 返回 "1"，getLoginName() 返回 "admin"

// 仅 Mock userId
MockedStatic<BlinkRequestContextHolder> mock = MockHelper.mockUserId("1");

// 创建 ArgumentCaptor
ArgumentCaptor<SysUserDO> captor = MockHelper.captor(SysUserDO.class);

// 重置 Mock
MockHelper.resetMocks(mock1, mock2);
```

### WireMockHelper（HTTP Mock 辅助工具）

用于集成测试中 Mock 外部 HTTP API：

```java
// 注册 WireMockExtension
@RegisterExtension
static WireMockExtension wireMock = WireMockExtension.newInstance()
    .options(WireMockConfiguration.wireMockConfig().dynamicPort())
    .build();

// Mock GET 请求
WireMockHelper.mockGet(wireMock, "/api/users/1", "{\"id\":1,\"name\":\"test\"}");

// Mock POST 请求（验证请求体）
WireMockHelper.mockPost(wireMock, "/api/save", 
    "{\"name\":\"test\"}",  // 期望的请求体
    "{\"id\":1}");          // 响应体

// Mock 延迟响应（测试超时）
WireMockHelper.mockGetWithDelay(wireMock, "/api/slow", "{}", 5000);

// Mock 错误响应
WireMockHelper.mockError(wireMock, "/api/error");

// 验证请求被调用
WireMockHelper.verifyCalled(wireMock, "/api/users/1", "GET");
WireMockHelper.verifyCalledTimes(wireMock, "/api/users/1", 3);
```

### SecurityMockHelper（安全上下文 Mock）

用于测试 Spring Security 相关功能：

```java
// 设置管理员上下文
SecurityMockHelper.setSuperAdmin();

// 设置普通用户
SecurityMockHelper.setNormalUser("testuser");

// 自定义角色
SecurityMockHelper.setAuthentication("user1", "ADMIN", "USER");

// 清除安全上下文
SecurityMockHelper.clearAuthentication();

// 检查认证状态
boolean authed = SecurityMockHelper.isAuthenticated();
boolean hasAdmin = SecurityMockHelper.hasRole("ADMIN");
```

### AssertionHelper（Blink 专用断言）

用于 Blink 项目特有的断言：

```java
// 断言 ResponseDTO 成功
AssertionHelper.assertThatSuccess(response);
AssertionHelper.assertThatSuccessWithBody(response);

// 断言 ResponseDTO 失败
AssertionHelper.assertThatError(response, "BUSS0001");
AssertionHelper.assertThatErrorPrefix(response, "GATE");

// 断言 PageDTO 分页数据
AssertionHelper.assertThatPage(pageRsp, 10, 100);
AssertionHelper.assertThatPageNotEmpty(pageRsp);
AssertionHelper.assertThatPageSize(pageRsp, 5);

// 软断言（收集所有错误）
AssertionHelper.assertSoftly(soft -> {
    soft.assertThat(response.getMsgCode()).isEqualTo("BLINK0000");
    soft.assertThat(response.getBody()).isNotNull();
    soft.assertThat(response.getBody().getUserId()).isEqualTo(1);
});
```

### FixtureHelper（测试数据 Fixture 管理）

用于共享测试数据的加载和复用：

```java
// 从 JSON 文件加载
AddUserReq req = FixtureHelper.loadFixture("fixtures/add-user.json", AddUserReq.class);
List<UserVO> users = FixtureHelper.loadFixtureList("fixtures/users.json", UserVO.class);

// 注册 Fixture（延迟创建）
FixtureHelper.registerFixture("defaultUser", () -> createDefaultUser());

// 获取 Fixture（自动缓存）
SysUserDO user = FixtureHelper.getFixture("defaultUser");

// 获取或计算（缓存模式）
SysUserDO cached = FixtureHelper.getOrCompute("testUser", () -> createUser("test"));

// 清理缓存
FixtureHelper.clearAll();
FixtureHelper.clear("defaultUser");
```

### JsonTestHelper（JSON 测试辅助）

```java
// 对象转 JSON
String json = JsonTestHelper.toJson(myObject);
String pretty = JsonTestHelper.toPrettyJson(myObject);  // 格式化输出

// JSON 转对象
MyObj obj = JsonTestHelper.fromJson(json, MyObj.class);

// JsonPath 提取
String name = JsonTestHelper.extract(json, "$.body.name");
List<String> list = JsonTestHelper.extractList(json, "$.body.items");

// Blink 项目专用方法
String msgCode = JsonTestHelper.extractMsgCode(json);  // $.msgCode
String msg = JsonTestHelper.extractMsg(json);          // $.msg
JsonNode body = JsonTestHelper.extractBody(json);      // $.body

// JSON 比较（忽略顺序）
boolean equals = JsonTestHelper.jsonEquals(json1, json2);

// 检查字段
boolean has = JsonTestHelper.hasField(json, "userId");
boolean bodyHas = JsonTestHelper.bodyHasField(json, "name");
```

### ReflectionTestHelper（反射测试辅助）

```java
// 设置私有字段
ReflectionTestHelper.setField(target, "privateField", value);

// 获取私有字段
Object value = ReflectionTestHelper.getField(target, "privateField");
String name = ReflectionTestHelper.getField(target, "name", String.class);

// 调用私有方法
Object result = ReflectionTestHelper.invokeMethod(target, "privateMethod", arg1, arg2);
Integer count = ReflectionTestHelper.invokeMethod(target, "calc", Integer.class, param);
```

---

## Gradle 测试任务

### 按类型运行测试

```bash
# 运行所有单元测试（快速，无需 Docker）
./gradlew unitTest

# 运行所有集成测试（需要 Docker）
./gradlew integrationTest

# 运行所有数据层测试（使用 H2）
./gradlew repositoryTest

# 运行所有测试
./gradlew test
```

### CI 任务

```bash
# 快速 CI（仅单元测试）
./gradlew ciQuickTest

# 完整 CI（所有测试 + 覆盖率）
./gradlew ciFullTest

# 后端完整 CI
./gradlew ciBackend
```

---

## Testcontainers 配置

### MySQL 容器

```java
// 默认配置（MySQL 8.0.33）
MySQLContainer<?> mysql = MySQLContainerConfig.create();

// 指定版本
MySQLContainer<?> mysql = MySQLContainerConfig.create("mysql:5.7");

// 带初始化脚本
MySQLContainer<?> mysql = MySQLContainerConfig.createWithInitScript("schema.sql");

// 自定义配置
MySQLContainer<?> mysql = MySQLContainerConfig.createCustom("mydb", "user", "pass");
```

### Redis 容器

```java
// 默认配置（Redis 7.0-alpine）
GenericContainer<?> redis = RedisContainerConfig.create();

// 指定版本
GenericContainer<?> redis = RedisContainerConfig.create("redis:6.2-alpine");

// 带密码
GenericContainer<?> redis = RedisContainerConfig.createWithPassword("secret123");

// 获取连接信息
int port = RedisContainerConfig.getMappedPort(redis);
String url = RedisContainerConfig.getRedisUrl(redis);  // redis://host:port
```

### 统一容器管理

继承 `BlinkTestcontainers` 自动获得 MySQL 和 Redis 容器：

```java
@SpringBootTest
@Testcontainers
class MyIntegrationTest extends BlinkTestcontainers {
    // 自动配置 MySQL/Redis
    // 自动注入 spring.datasource.url 等属性
}
```

---

## 测试注解/标签

| 注解 | 标签 | 说明 |
|------|------|------|
| `@UnitTest` | `unit` | 单元测试，不依赖外部容器 |
| `@IntegrationTest` | `integration` | 集成测试，需要 Testcontainers |
| `@RepositoryTest` | `repository` | 数据层测试，使用 H2 |

**配合 Gradle 任务**：
```java
@UnitTest
class MyServiceTest { }  // ./gradlew unitTest 会运行

@IntegrationTest
class MyControllerIT { }  // ./gradlew integrationTest 会运行

@RepositoryTest
class MyMapperTest { }  // ./gradlew repositoryTest 会运行
```

---

## 测试配置文件

模块提供以下默认配置：

### application-test.yml

- H2 内存数据库配置（MySQL 模式）
- Redis 默认配置
- 禁用 Nacos/Dubbo（测试环境）
- 日志级别配置

### logback-test.xml

- Blink 包 DEBUG 级别
- Spring/Hibernate WARN 级别
- Testcontainers INFO 级别

### schema-test.sql

- 基础表结构（sys_user, sys_role, sys_menu 等）
- 默认测试数据（admin 用户，测试用户等）

---

## 最佳实践

### 1. 测试命名规范

```
类名：{被测类}Test（单元）/{被测类}IT（集成）
方法名：should_{期望结果}_when_{条件}
```

示例：
```java
class SysUserServiceTest {  // 单元测试
    void should_return_user_when_exists() { }
    void should_throw_exception_when_not_found() { }
}

class SysUserControllerIT {  // 集成测试
    void should_create_user_via_http() { }
}
```

### 2. 使用 @Nested 组织测试

```java
@UnitTest
class SysUserServiceTest extends BlinkUnitTest {

    @Nested
    class CreateUserTests {
        @Test
        void should_create_successfully() { }
        @Test
        void should_fail_when_login_name_exists() { }
    }

    @Nested
    class QueryUserTests {
        @Test
        void should_return_list() { }
        @Test
        void should_return_empty_when_no_data() { }
    }
}
```

### 3. 测试数据隔离

```java
@BeforeEach
void setup() {
    // 每个测试前清理数据
    clearTable("sys_user");
}

@AfterEach
void teardown() {
    // 每个测试后清理
    clearTable("sys_user");
}
```

### 4. 使用 AssertJ 流式断言

```java
// 推荐：AssertJ 流式断言
assertThat(user)
    .isNotNull()
    .extracting("loginName", "username")
    .containsExactly("admin", "管理员");

// 不推荐：JUnit 传统断言
assertEquals("admin", user.getLoginName());
assertEquals("管理员", user.getUsername());
```

---

## 常见问题

### Q: 集成测试启动失败？

确保 Docker 已安装并运行：
```bash
docker ps  # 检查 Docker 状态
```

### Q: 测试数据互相干扰？

使用 `@BeforeEach` 清理数据：
```java
@BeforeEach
void setup() {
    clearTable("sys_user");
    resetAutoIncrement("sys_user");
}
```

### Q: 静态方法 Mock 不生效？

确保在 `try-with-resources` 中使用：
```java
try (MockedStatic<MyUtils> mock = MockHelper.mockStatic(MyUtils.class)) {
    // 在此范围内 Mock 生效
}
// 离开范围后自动关闭
```

---

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0.0-SNAPSHOT | 2026-04-16 | 初始版本，包含测试基类、工具类、Testcontainers 配置 |

---

## 作者

- binblink (2026-04-16)

---

## 许可证

Apache License 2.0