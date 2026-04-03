# blink-framework-common

通用基础模块，提供全局通用的工具类、数据传输对象、异常处理、JWT 认证等能力。

## 功能特性

| 功能模块 | 说明 |
|---------|------|
| 通用 DTO | RequestDTO、ResponseDTO、PageDTO 标准化 API 请求响应格式 |
| 异常处理 | BlinkException 自定义异常，支持错误码和业务异常标识 |
| JWT 认证 | JwtProvider JWT 生成、验证、刷新 |
| 加密工具 | AESUtils、RSAUtils 加密解密工具 |
| JSON 工具 | JacksonUtil JSON 序列化/反序列化 |
| 上下文工具 | ApplicationContextUtil 静态获取 Spring Bean |

## 快速开始

### 引入依赖

```gradle
implementation 'com.blink:blink-framework-common:1.0.0-SNAPSHOT'
```

---

## 通用请求响应 DTO

采用信封模型设计：通用元数据 + 业务正文，实现所有 API 请求、响应格式的标准化。

### RequestDTO

```java
// 空请求体（只有元数据）
RequestDTO<EmptyBody> request = RequestDTO.newInstance();

// 带业务数据的请求
UserLoginDTO loginDTO = new UserLoginDTO();
loginDTO.setUsername("admin");
loginDTO.setPassword("123456");

RequestDTO<UserLoginDTO> request = RequestDTO.newInstance(loginDTO);
request.setRequestId("req-001");
request.setTraceId("trace-001");
request.setUserId("user-001");
request.setClientIp("192.168.1.1");
```

### ResponseDTO

```java
// 成功响应（无数据）
ResponseDTO<EmptyBody> response = ResponseDTO.newSuccessInstance();

// 成功响应（带数据）
UserInfoDTO userInfo = new UserInfoDTO();
userInfo.setUsername("admin");

ResponseDTO<UserInfoDTO> response = ResponseDTO.newSuccessInstance(userInfo);

// 失败响应
ResponseDTO<EmptyBody> response = ResponseDTO.newFailInstance();
response.setMsgCode("SYS00001");
response.setMsgInfo("系统异常");
```

### PageDTO 分页

```java
// 分页请求
public class QueryUserReqDTO extends PageDTO {
    private String username;
    private Integer status;
    // getter/setter
}

// 分页响应
public class QueryUserRspDTO extends PageDTO<UserDO> {
    // 继承分页字段：pageNum, pageSize, total, pages, records
}

// 使用
QueryUserReqDTO reqDTO = new QueryUserReqDTO();
reqDTO.setPageNum(1);
reqDTO.setPageSize(10);
reqDTO.setUsername("admin");
```

---

## 异常处理

### BlinkException

```java
// 抛出业务异常
throw new BlinkException("BUSS00001");  // 使用错误码

// 抛出业务异常（带标识）
BlinkException.throwBusinessException("BUSS00001");

// 抛出系统异常
throw new BlinkException("SYS00001");

// 包装原始异常
try {
    // 业务代码
} catch (Exception e) {
    throw new BlinkException(e, "SYS00001");
}

// 带自定义消息
throw new BlinkException("操作失败", "SYS00001");
```

### 错误码规范

| 前缀 | 类型 | 示例 |
|------|------|------|
| `BLINK` | 业务错误 | BLINK0002 - Token 失效 |
| `BUSS` | 业务异常 | BUSS00001 - 业务异常 |
| `SYS` | 系统异常 | SYS00001 - 系统异常 |
| `SYS004xx` | HTTP 错误 | SYS00401 - 未授权 |

---

## JWT 认证

### 配置

```java
JwtConfig jwtConfig = new JwtConfig();
jwtConfig.setJwtSecret("your-base64-encoded-secret-key");
jwtConfig.setIssuer("blink-app");
jwtConfig.setAudience("blink-user");
jwtConfig.setAccessTokenExpiration(7200000L);  // 2小时
jwtConfig.setRefreshTokenExpiration(604800000L);  // 7天

JwtProvider jwtProvider = new JwtProvider(jwtConfig);
```

### 生成 Token

```java
// 生成 Access Token
String accessToken = jwtProvider.generateAccessToken("admin", List.of("ROLE_ADMIN", "ROLE_USER"));

// 生成 Refresh Token
String refreshToken = jwtProvider.generateRefreshToken("admin");

// 生成 Token 对
TokenPair tokenPair = jwtProvider.generateTokenPair("admin", List.of("ROLE_ADMIN"));
String accessToken = tokenPair.getAccessToken();
String refreshToken = tokenPair.getRefreshToken();
```

### 验证 Token

```java
// 验证是否有效
boolean isValid = jwtProvider.validateToken(token);

// 详细验证结果
ValidationResult result = jwtProvider.validateTokenDetailed(token);
if (result.isValid()) {
    // Token 有效
} else if (result.isExpired()) {
    // Token 已过期
}
```

### 解析 Token

```java
// 获取用户名
String username = jwtProvider.getUsernameFromToken(token);

// 获取角色
List<String> roles = jwtProvider.getRolesFromToken(token);

// 获取所有 Claims
Claims claims = jwtProvider.getAllClaims(token);

// 获取自定义数据
Map<String, Object> customData = jwtProvider.getCustomClaims(token);
```

### 刷新 Token

```java
// 使用 Refresh Token 刷新 Access Token
String newAccessToken = jwtProvider.refreshAccessToken(refreshToken);

// 刷新整个 Token 对
TokenPair newTokenPair = jwtProvider.refreshTokenPair(refreshToken);
```

---

## 加密工具

### AESUtils

```java
// 生成随机密钥
SecretKey key = AESUtils.generateRandomAESKey();
byte[] iv = AESUtils.generateIV();

// AES-CBC 加密
String ciphertext = AESUtils.encrypt(key, iv, "敏感数据");

// AES-CBC 解密
String plaintext = AESUtils.decrypt(key, iv, ciphertext);

// 使用 Base64 参数解密
String plaintext = AESUtils.decryptWithBase64(keyBase64, ivBase64, ciphertext);

// 一键加密（自动生成密钥和 IV）
AESUtils.CompleteEncryptionResult result = AESUtils.encryptComplete("敏感数据");
String encryptedData = result.getEncryptedData();
String key = result.getKey();
String iv = result.getIv();

// 一键解密
String plaintext = AESUtils.decryptComplete(result);
```

### RSAUtils

```java
// 生成密钥对
KeyPair keyPair = RSAUtils.generateKeyPair();
String publicKeyBase64 = RSAUtils.publicKeyToBase64(keyPair.getPublic());
String privateKeyBase64 = RSAUtils.privateKeyToBase64(keyPair.getPrivate());

// 公钥加密
PublicKey publicKey = RSAUtils.base64ToPublicKey(publicKeyBase64);
String ciphertext = RSAUtils.encryptToBase64("敏感数据", publicKey);

// 私钥解密
PrivateKey privateKey = RSAUtils.base64ToPrivateKey(privateKeyBase64);
String plaintext = RSAUtils.decryptFromBase64(ciphertext, privateKey);
```

---

## JSON 工具

### JacksonUtil

```java
// 对象转 JSON
String json = JacksonUtil.toJson(userDTO);

// JSON 转对象
UserDTO user = JacksonUtil.fromJson(json, UserDTO.class);

// JSON 转 List
List<UserDTO> users = JacksonUtil.fromJsonToList(json, UserDTO.class);

// JSON 转 Map
Map<String, Object> map = JacksonUtil.fromJsonToMap(json, String.class, Object.class);

// 复杂泛型
List<UserDTO> users = JacksonUtil.fromJson(json, new TypeReference<List<UserDTO>>() {});

// 对象转换
UserVO userVO = JacksonUtil.convert(userDTO, UserVO.class);

// 深度拷贝
UserDTO copy = JacksonUtil.deepCopy(userDTO, UserDTO.class);

// 判断是否是有效 JSON
boolean isValid = JacksonUtil.isValidJson(json);

// 安全转换（失败返回 null）
UserDTO user = JacksonUtil.safeFromJson(json, UserDTO.class);

// 安全转换（带默认值）
UserDTO user = JacksonUtil.safeFromJson(json, UserDTO.class, defaultUser);
```

---

## 上下文工具

### ApplicationContextUtil

```java
// 获取 Bean
UserService userService = ApplicationContextUtil.getBean(UserService.class);

// 通过名称获取 Bean
Object bean = ApplicationContextUtil.getBean("userService");

// 通过名称和类型获取 Bean
UserService userService = ApplicationContextUtil.getBean("userService", UserService.class);

// 获取配置属性
String value = ApplicationContextUtil.getProperty("app.name");
String value = ApplicationContextUtil.getProperty("app.name", "default");

// 判断环境
boolean isDev = ApplicationContextUtil.isDev();
boolean isProd = ApplicationContextUtil.isProd();

// 获取当前激活的环境
String[] profiles = ApplicationContextUtil.getActiveProfiles();
```

---

## 数据传输对象

| 类名 | 说明 |
|------|------|
| `RequestDTO<T>` | 通用请求 DTO，包含请求元数据 |
| `ResponseDTO<T>` | 通用响应 DTO，包含响应状态和数据 |
| `PageDTO<T>` | 分页 DTO，请求和响应通用 |
| `MqGenericDTO<T>` | MQ 消息传输对象 |
| `EmptyBody` | 空请求体标识 |

## 注解

| 注解 | 说明 |
|------|------|
| `@PreInsert` | 插入前自动填充字段 |
| `@PreUpdate` | 更新前自动填充字段 |

## 常量类

| 类名 | 说明 |
|------|------|
| `SysConstant` | 系统常量（成功/失败码等） |
| `ResponseMsgType` | 响应消息类型枚举 |

---

## 自动配置

模块自动配置以下组件：

| 组件 | 说明 |
|------|------|
| `ApplicationContextUtil` | 实现 ApplicationContextAware，自动注入 Spring 上下文 |

---

## 设计原则

1. **尽量减少依赖** - 只引入必要的依赖
2. **全局可用** - 通用类不涉及具体业务
3. **版本解耦** - 修改后发布新版本，不影响其他模块
