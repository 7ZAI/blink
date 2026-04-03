# 多设备登录会话管理重构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构会话管理，支持多设备登录（可配置最大设备数），解决 token 续期不一致和并发登录竞态条件问题。

**Architecture:** 使用 Redis ZSet 追踪用户所有活跃会话，通过 Lua 脚本实现原子化登录操作，废弃 `user:info:{userId}` key。

**Tech Stack:** Spring Boot 3.2, Redis, Lua Script, MyBatis-Plus

---

## File Structure

| File | Action | Purpose |
|------|--------|---------|
| `blink-base-app/.../constants/RedisKeyConstans.java` | Modify | 添加 USER_TOKENS 常量 |
| `blink-base-app/.../constants/CommonConstans.java` | Modify | 添加 DEFAULT_MAX_DEVICES 常量 |
| `blink-gateway-reactive/.../constant/RedisConstans.java` | Modify | 添加 USER_TOKENS 常量 |
| `blink-base-app/src/main/resources/lua/login_session.lua` | Create | 登录会话管理 Lua 脚本 |
| `blink-base-app/.../service/impl/SysUserAuthServiceImpl.java` | Modify | 重构登录/登出逻辑 |
| `blink-gateway-reactive/.../TokenAuthenticationSuccessHandler.java` | Modify | 修改续期逻辑 |
| `blink-base-app/.../service/impl/OnlineUserServiceImpl.java` | Modify | 修改强制下线逻辑 |
| `blink-base-app/.../service/impl/UserDataScopeCacheServiceImpl.java` | Modify | 从 USER_TOKEN 获取用户信息 |

---

## Task 1: 添加 Redis Key 常量

**Files:**
- Modify: `blink-base-app/src/main/java/com/blink/base/constants/RedisKeyConstans.java`
- Modify: `blink-gateway-reactive/src/main/java/com/blink/gateway/constant/RedisConstans.java`

- [ ] **Step 1: 修改 blink-base-app 的 RedisKeyConstans.java**

在 `RedisKeyConstans.java` 中添加 `USER_TOKENS` 常量：

```java
package com.blink.base.constants;

/**
 * @author binblink
 */
public interface RedisKeyConstans {

    String BASE_APP = "base-app:";

    String USER_TOKEN = "user:token:";

    String USER_TOKEN_OLD = "user:token:old:";

    // 新增：用户会话 ZSet（管理用户所有活跃 token）
    String USER_TOKENS = "user:tokens:";

    String USER_INFO = "user:info:";

    // ... 其余常量保持不变
}
```

- [ ] **Step 2: 修改 blink-gateway-reactive 的 RedisConstans.java**

在 `RedisConstans.java` 中添加 `USER_TOKENS` 常量：

```java
package com.blink.gateway.constant;

/**
 * redis常量
 *
 * @author binblink
 */
public interface RedisConstans {

    // ... 现有常量

    /**
     * 认证token
     */
    String USER_TOKEN = "user:token:";

    /**
     * 用户会话 ZSet（管理用户所有活跃 token）
     */
    String USER_TOKENS = "user:tokens:";

    // ... 其余常量保持不变
}
```

- [ ] **Step 3: 提交常量修改**

```bash
git add blink-base/blink-base-app/src/main/java/com/blink/base/constants/RedisKeyConstans.java
git add blink-gateway/blink-gateway-reactive/src/main/java/com/blink/gateway/constant/RedisConstans.java
git commit -m "feat(constants): 添加 USER_TOKENS 常量用于多设备会话管理"
```

---

## Task 2: 添加默认最大设备数常量

**Files:**
- Modify: `blink-base-app/src/main/java/com/blink/base/constants/CommonConstans.java`

**说明**: `CommonConstans.SysConfigKeys.SESSION_MAX_CONCURRENT` 常量已存在（值为 `blink:base:session:maxConcurrent`），无需添加配置 key 常量。

- [ ] **Step 1: 在 CommonConstans.java 中添加默认最大设备数**

在接口中添加默认设备数常量（可放在 `SUPER_ADMIN_YES` 附近）：

```java
//超级管理员标志 0-否 1-是
Integer SUPER_ADMIN_NO = 0;

Integer SUPER_ADMIN_YES = 1;

/**
 * 默认最大设备登录数
 */
Integer DEFAULT_MAX_DEVICES = 3;
```

- [ ] **Step 2: 提交常量修改**

```bash
git add blink-base/blink-base-app/src/main/java/com/blink/base/constants/CommonConstans.java
git commit -m "feat(constants): 添加 DEFAULT_MAX_DEVICES 默认最大设备登录数"
```

---

## Task 3: 创建登录会话管理 Lua 脚本

**Files:**
- Create: `blink-base-app/src/main/resources/lua/login_session.lua`

- [ ] **Step 1: 创建 lua 目录和脚本文件**

创建目录结构并添加 Lua 脚本：

```lua
-- login_session.lua
-- KEYS[1] = user:tokens:{userId}   (ZSet key)
-- KEYS[2] = user:token:{newToken}  (新 token 存储key)
-- KEYS[3] = newToken               (新 token 值，用于 ZADD member)
-- ARGV[1] = maxDevices             (最大设备数)
-- ARGV[2] = loginTime              (登录时间戳)
-- ARGV[3] = userInfoJson           (用户信息 JSON)
-- ARGV[4] = ttl                    (过期时间，秒)
-- ARGV[5] = userId                 (用户ID，用于旧 token 标记)
-- Returns: {success, kickedToken or ''}

local tokensKey = KEYS[1]
local newTokenKey = KEYS[2]
local newToken = KEYS[3]
local maxDevices = tonumber(ARGV[1])
local loginTime = tonumber(ARGV[2])
local userInfoJson = ARGV[3]
local ttl = tonumber(ARGV[4])
local userId = ARGV[5]

-- 获取当前设备数
local currentCount = redis.call('ZCARD', tokensKey)

local kickedToken = nil

-- 如果达到上限，踢出最早登录的
if currentCount >= maxDevices then
    -- 获取最早登录的 token（score 最小）
    local oldest = redis.call('ZRANGE', tokensKey, 0, 0)
    if oldest and #oldest > 0 then
        kickedToken = oldest[1]
        -- 删除旧 token
        redis.call('DEL', 'user:token:' .. kickedToken)
        -- 设置旧 token 标记（用于提示）
        redis.call('SETEX', 'user:token:old:' .. kickedToken, 300, userId)
        -- 从 ZSet 移除
        redis.call('ZREM', tokensKey, kickedToken)
    end
end

-- 存储新 token
redis.call('SETEX', newTokenKey, ttl, userInfoJson)

-- 添加到 ZSet
redis.call('ZADD', tokensKey, loginTime, newToken)

-- 设置 ZSet 过期时间
redis.call('EXPIRE', tokensKey, ttl)

return {1, kickedToken or ''}
```

- [ ] **Step 2: 提交 Lua 脚本**

```bash
git add blink-base/blink-base-app/src/main/resources/lua/login_session.lua
git commit -m "feat(lua): 添加登录会话管理 Lua 脚本，原子化处理多设备登录"
```

---

## Task 4: 重构登录逻辑

**Files:**
- Modify: `blink-base-app/src/main/java/com/blink/base/service/impl/SysUserAuthServiceImpl.java`

- [ ] **Step 1: 添加 Lua 脚本加载和执行方法**

在 `SysUserAuthServiceImpl` 类中添加以下私有方法：

**首先添加必要的 import：**

```java
import java.util.Arrays;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import java.nio.charset.StandardCharsets;
```

**然后添加方法：**

```java
/**
 * 使用 Lua 脚本存储会话信息
 * 支持多设备登录管理
 *
 * @param userId 用户ID
 * @param newToken 新token
 * @param userInfo 用户信息
 * @param maxDevices 最大设备数
 * @return 被踢出的token，如果没有则返回null
 */
private String storeSessionWithLua(Integer userId, String newToken, UserInfoRedisDO userInfo, int maxDevices) {
    try {
        String luaScript = loadLuaScript("lua/login_session.lua");
        List<String> keys = Arrays.asList(
            RedisKeyConstans.USER_TOKENS + userId,
            RedisKeyConstans.USER_TOKEN + newToken,
            newToken
        );
        String userInfoJson = JacksonUtil.toJson(userInfo);
        long loginTime = System.currentTimeMillis();
        long ttl = 1800L; // 30分钟

        List<Object> args = Arrays.asList(
            String.valueOf(maxDevices),
            String.valueOf(loginTime),
            userInfoJson,
            String.valueOf(ttl),
            String.valueOf(userId)
        );

        Object result = redisClient.execute(luaScript, keys, args);

        if (result instanceof List) {
            List<?> resultList = (List<?>) result;
            if (resultList.size() >= 2 && "1".equals(String.valueOf(resultList.get(0)))) {
                String kickedToken = (String) resultList.get(1);
                return StrUtil.isNotBlank(kickedToken) ? kickedToken : null;
            }
        }
        log.warn("[Login] Lua脚本执行返回异常结果: {}", result);
        return null;
    } catch (Exception e) {
        log.error("[Login] Lua脚本执行失败，降级为普通存储: {}", e.getMessage(), e);
        // 降级处理：直接存储
        fallbackStoreSession(newToken, userInfo);
        return null;
    }
}

/**
 * 降级存储会话（不使用Lua脚本）
 */
private void fallbackStoreSession(String token, UserInfoRedisDO userInfo) {
    Long expireTime = 1800L;
    redisClient.setEx(RedisKeyConstans.USER_TOKEN + token, userInfo, expireTime);
}

/**
 * 加载 Lua 脚本
 */
private String loadLuaScript(String path) {
    try {
        org.springframework.core.io.Resource resource = new org.springframework.core.io.ClassPathResource(path);
        return new String(resource.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    } catch (Exception e) {
        log.error("[Login] 加载Lua脚本失败: {}", path, e);
        throw new BlinkException("加载Lua脚本失败: " + path, e, "BUSS0002");
    }
}

/**
 * 获取最大设备登录数
 * 优先从数据库配置读取，失败则使用默认值
 */
private int getMaxDevices() {
    try {
        Integer maxDevices = sysConfigService.getIntegerConfig(
            CommonConstans.SysConfigKeys.SESSION_MAX_CONCURRENT,
            CommonConstans.DEFAULT_MAX_DEVICES
        );
        return maxDevices != null && maxDevices > 0 ? maxDevices : CommonConstans.DEFAULT_MAX_DEVICES;
    } catch (Exception e) {
        log.warn("[Login] 获取最大设备数配置失败，使用默认值: {}", CommonConstans.DEFAULT_MAX_DEVICES);
        return CommonConstans.DEFAULT_MAX_DEVICES;
    }
}
```

- [ ] **Step 2: 修改 login() 方法**

替换原有的踢出逻辑，修改 `login()` 方法中的以下部分：

**原代码（删除）：**
```java
//踢出久登入
UserInfoRedisDO older = JacksonUtil.convert(redisClient.get(RedisKeyConstans.USER_INFO + loginUser.getUserId()), UserInfoRedisDO.class);

if (Objects.nonNull(older)) {
    redisClient.delete(RedisKeyConstans.USER_TOKEN + older.getToken());
    //保存被顶替登入的用户的旧token 用来提示用户在别处登入了
    redisClient.setEx(RedisKeyConstans.USER_TOKEN_OLD + older.getToken(), older.getUserId(), Long.valueOf(60 * 5));
}

//存入redis
storeUserInfoToRedis(result);
```

**新代码（替换为）：**
```java
// 使用 Lua 脚本存储会话（支持多设备登录）
UserInfoRedisDO userInfoRedis = buildUserInfoRedisDO(result);
int maxDevices = getMaxDevices();
String kickedToken = storeSessionWithLua(loginUser.getUserId(), token, userInfoRedis, maxDevices);

if (StrUtil.isNotBlank(kickedToken)) {
    log.info("[Login] 踢出较早登录的设备 | userId: {}, kickedToken: {}", loginUser.getUserId(), kickedToken);
}
```

- [ ] **Step 3: 添加 buildUserInfoRedisDO 辅助方法**

```java
/**
 * 构建用户 Redis 存储对象
 */
private UserInfoRedisDO buildUserInfoRedisDO(SysLoginRsp rspInfo) {
    SysUserVO userInfo = rspInfo.getUserInfo();
    UserInfoRedisDO userInfoRedis = new UserInfoRedisDO();
    BeanUtil.copyProperties(userInfo, userInfoRedis);
    userInfoRedis.setLoginDateTime(userInfo.getLastLoginTime());
    userInfoRedis.setPermissions(rspInfo.getPermissions());
    userInfoRedis.setToken(rspInfo.getToken());
    userInfoRedis.setRoleIds(rspInfo.getRoleIds());
    return userInfoRedis;
}
```

- [ ] **Step 4: 修改 storeUserInfoToRedis 方法**

移除对 `USER_INFO` 的存储（该 key 将被废弃）：

```java
/**
 * 保存token和用户信息到redis
 * 注意：不再存储 USER_INFO，改为使用 ZSet 管理多设备
 *
 * @param rspInfo 登录响应信息
 */
private void storeUserInfoToRedis(SysLoginRsp rspInfo) {
    SysUserVO userInfo = rspInfo.getUserInfo();
    UserInfoRedisDO userInfoRedis = buildUserInfoRedisDO(rspInfo);

    // 过期时间 30分钟
    Long expireTime = 1800L;

    // 只存储 USER_TOKEN，废弃 USER_INFO
    redisClient.setEx(RedisKeyConstans.USER_TOKEN + rspInfo.getToken(), userInfoRedis, expireTime);
}
```

- [ ] **Step 5: 提交登录逻辑修改**

```bash
git add blink-base/blink-base-app/src/main/java/com/blink/base/service/impl/SysUserAuthServiceImpl.java
git commit -m "feat(auth): 重构登录逻辑支持多设备登录，使用Lua脚本原子化操作"
```

---

## Task 5: 重构登出逻辑

**Files:**
- Modify: `blink-base-app/src/main/java/com/blink/base/service/impl/SysUserAuthServiceImpl.java`

- [ ] **Step 1: 修改 logout() 方法**

修改 `logout()` 方法，添加 ZSet 移除操作：

```java
/**
 * 退出登入
 *
 * @param logoutParam 登出参数
 * @return EmptyBody
 * @throws BlinkException
 */
@Override
public void logout(SysLogoutReq logoutParam) throws BlinkException {
    UserInfoRedisDO userInfo = JacksonUtil.convert(
        redisClient.get(RedisKeyConstans.USER_TOKEN + logoutParam.getToken()),
        UserInfoRedisDO.class
    );

    if (Objects.isNull(userInfo)) {
        // 用户已登出
        BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIT);
    }

    if (logoutParam.getToken().equals(userInfo.getToken())
            && logoutParam.getUserId().equals(String.valueOf(userInfo.getUserId()))) {
        // 删除 token
        redisClient.delete(RedisKeyConstans.USER_TOKEN + logoutParam.getToken());
        // 从 ZSet 中移除（使用 zRemove 方法）
        redisClient.zRemove(RedisKeyConstans.USER_TOKENS + logoutParam.getUserId(), logoutParam.getToken());
        // 清除用户数据权限缓存
        userDataScopeCacheService.clearCache(Integer.valueOf(logoutParam.getUserId()));
        log.info("[Logout] 用户登出成功 | userId: {}, token: {}", logoutParam.getUserId(), logoutParam.getToken());
        return;
    }

    log.error("非法登出请求 requestID {}", BlinkRequestContextHolder.getRequestId());
    // userId 和 token不匹配
    BlinkException.throwBusinessException();
}
```

- [ ] **Step 2: 提交登出逻辑修改**

```bash
git add blink-base/blink-base-app/src/main/java/com/blink/base/service/impl/SysUserAuthServiceImpl.java
git commit -m "feat(auth): 登出时从 ZSet 移除 token，废弃 USER_INFO 操作"
```

---

## Task 6: 修改网关 Token 续期逻辑

**Files:**
- Modify: `blink-gateway-reactive/src/main/java/com/blink/gateway/security/token/TokenAuthenticationSuccessHandler.java`

- [ ] **Step 1: 修改 renewToken 方法**

修改 `renewToken()` 方法，同时续期 `USER_TOKENS` ZSet：

```java
/**
 * 续期token
 * 同时续期 USER_TOKEN 和 USER_TOKENS（ZSet）
 *
 * @param redisKey USER_TOKEN 的 redis key
 * @param userInfo 用户信息，用于获取 userId 来构建 USER_TOKENS key
 * @return 是否续期成功
 */
private Mono<Boolean> renewToken(String redisKey, UserInfoRedisDO userInfo) {
    String userTokensKey = RedisConstans.USER_TOKENS + userInfo.getUserId();

    return Mono.zip(
                    // 续期 USER_TOKEN
                    redisClient.expire(redisKey, GatewayConstant.TOKEN_TTL)
                            .doOnSuccess(result -> {
                                if (result) {
                                    log.info("token续期成功, redisKey: {}", redisKey);
                                } else {
                                    log.error("token续期失败, redisKey: {}", redisKey);
                                }
                            })
                            .onErrorResume(error -> {
                                log.error("token续期异常, redisKey: {}, error: {}",
                                        redisKey, error.getMessage(), error);
                                return Mono.just(false);
                            }),
                    // 续期 USER_TOKENS（ZSet）
                    redisClient.expire(userTokensKey, GatewayConstant.TOKEN_TTL)
                            .doOnSuccess(result -> {
                                if (result) {
                                    log.debug("USER_TOKENS续期成功, userId: {}", userInfo.getUserId());
                                } else {
                                    log.warn("USER_TOKENS续期失败(可能已过期), userId: {}", userInfo.getUserId());
                                }
                            })
                            .onErrorResume(error -> {
                                log.error("USER_TOKENS续期异常, userId: {}, error: {}",
                                        userInfo.getUserId(), error.getMessage(), error);
                                return Mono.just(false);
                            })
            )
            .map(tuple -> tuple.getT1() && tuple.getT2());
}
```

- [ ] **Step 2: 更新 import 语句**

修改文件顶部的 import：

```java
// 添加
import static com.blink.gateway.constant.RedisConstans.USER_TOKENS;

// 移除（已废弃）
// import static com.blink.gateway.constant.RedisConstans.USER_INFO;
```

- [ ] **Step 3: 提交续期逻辑修改**

```bash
git add blink-gateway/blink-gateway-reactive/src/main/java/com/blink/gateway/security/token/TokenAuthenticationSuccessHandler.java
git commit -m "fix(gateway): Token续期同时续期USER_TOKENS ZSet，废弃USER_INFO"
```

---

## Task 7: 修改在线用户管理服务

**Files:**
- Modify: `blink-base-app/src/main/java/com/blink/base/service/impl/OnlineUserServiceImpl.java`

- [ ] **Step 1: 修改 kickoutUser 方法**

修改强制下线方法，添加 ZSet 移除操作：

```java
/**
 * 强制用户下线
 * @param kickoutUserReq 强制下线请求
 * @throws BlinkException 业务异常
 */
@Override
public void kickoutUser(KickoutUserReq kickoutUserReq) throws BlinkException {
    String token = kickoutUserReq.getToken();

    Object userInfoObj = redisClient.get(RedisKeyConstans.USER_TOKEN + token);
    if (userInfoObj == null) {
        BlinkException.throwBusinessException(BaseErrCodeConstant.TOKEN_EXPIRED);
    }

    UserInfoRedisDO userInfo = JacksonUtil.convert(userInfoObj, UserInfoRedisDO.class);
    Integer userId = userInfo.getUserId();

    // 删除 token
    redisClient.delete(RedisKeyConstans.USER_TOKEN + token);
    // 从 ZSet 移除（使用 zRemove 方法）
    redisClient.zRemove(RedisKeyConstans.USER_TOKENS + userId, token);
    // 设置旧 token 标记（用于提示）
    redisClient.setEx(RedisKeyConstans.USER_TOKEN_OLD + token, userId, 300L);

    log.info("[OnlineUser] 强制用户下线 | userId: {}, token: {}", userId, token);
}
```

- [ ] **Step 2: 修改 kickoutUsersByUserIds 方法**

修改批量下线方法：

```java
/**
 * 根据用户ID列表强制下线
 *
 * @param userIdList 用户ID列表
 */
@Override
public void kickoutUsersByUserIds(List<Integer> userIdList) {
    if (CollUtil.isEmpty(userIdList)) {
        return;
    }

    for (Integer userId : userIdList) {
        // 获取该用户的所有 token（使用 zRange 方法）
        Set<Object> tokens = redisClient.zRange(RedisKeyConstans.USER_TOKENS + userId, 0, -1);
        if (CollUtil.isNotEmpty(tokens)) {
            for (Object tokenObj : tokens) {
                String token = String.valueOf(tokenObj);
                redisClient.delete(RedisKeyConstans.USER_TOKEN + token);
                log.info("[OnlineUser] 强制用户下线 | userId: {}, token: {}", userId, token);
            }
        }
        // 删除整个 ZSet
        redisClient.delete(RedisKeyConstans.USER_TOKENS + userId);
    }
}
```

- [ ] **Step 3: 添加必要的 import**

```java
import java.util.Set;
import cn.hutool.core.collection.CollUtil;
```

- [ ] **Step 4: 提交在线用户管理修改**

```bash
git add blink-base/blink-base-app/src/main/java/com/blink/base/service/impl/OnlineUserServiceImpl.java
git commit -m "feat(online-user): 强制下线时同步更新ZSet，支持批量下线所有设备"
```

---

## Task 8: 修改数据权限缓存服务

**Files:**
- Modify: `blink-base-app/src/main/java/com/blink/base/service/impl/UserDataScopeCacheServiceImpl.java`

- [ ] **Step 1: 修改 buildAndCache 方法**

修改获取用户信息的方式，从 `USER_TOKEN` 获取（需要传入 token）：

由于 `buildAndCache` 方法目前只有 `userId` 参数，无法直接获取 token，需要添加新的方法签名或修改调用方式。

**方案：修改方法签名，增加 token 参数**

首先修改接口：

```java
// UserDataScopeCacheService.java
/**
 * 构建并缓存用户数据权限信息
 *
 * @param userId 用户ID
 * @param token  用户token（可选，用于获取用户信息）
 * @return 用户数据权限信息
 */
UserDataScopeInfo buildAndCache(Integer userId, String token);
```

然后修改实现：

```java
@Override
public UserDataScopeInfo buildAndCache(Integer userId, String token) {
    if (userId == null || userId <= 0) {
        return new UserDataScopeInfo();
    }

    UserDataScopeInfo dataScopeInfo = new UserDataScopeInfo();
    dataScopeInfo.setUserId(userId);

    UserInfoRedisDO userInfo = null;

    // 优先通过 token 获取用户信息
    if (StrUtil.isNotBlank(token)) {
        userInfo = JacksonUtil.convert(
            redisClient.get(RedisKeyConstans.USER_TOKEN + token),
            UserInfoRedisDO.class
        );
    }

    // 如果 token 方式获取失败，尝试扫描 USER_TOKEN:* 查找
    if (ObjectUtil.isNull(userInfo)) {
        userInfo = findUserInfoByUserId(userId);
    }

    if (ObjectUtil.isNull(userInfo)) {
        log.warn("[UserDataScopeCache] 用户信息不存在 | userId: {}", userId);
        return dataScopeInfo;
    }

    // ... 后续逻辑保持不变
}

/**
 * 通过扫描 USER_TOKEN:* 查找用户信息（降级方案）
 */
private UserInfoRedisDO findUserInfoByUserId(Integer userId) {
    String pattern = RedisKeyConstans.USER_TOKEN + "*";
    String oldTokenPrefix = RedisKeyConstans.USER_TOKEN_OLD;
    Cursor<String> cursor = redisClient.scan(pattern, 100);

    try {
        while (cursor.hasNext()) {
            String key = cursor.next();
            if (key.startsWith(oldTokenPrefix)) {
                continue;
            }
            Object value = redisClient.get(key);
            if (value != null) {
                UserInfoRedisDO userInfo = JacksonUtil.convert(value, UserInfoRedisDO.class);
                if (userInfo != null && userId.equals(userInfo.getUserId())) {
                    return userInfo;
                }
            }
        }
    } finally {
        cursor.close();
    }
    return null;
}
```

- [ ] **Step 2: 修改 SysUserAuthServiceImpl 中的调用**

**注意**：接口签名变更后，需要更新所有调用 `buildAndCache()` 的地方。使用 Grep 搜索 `buildAndCache` 找到所有调用点并更新。

在登录时传入 token：

```java
// 生成并缓存用户数据权限信息
userDataScopeCacheService.buildAndCache(loginUser.getUserId(), token);
```

- [ ] **Step 3: 提交数据权限缓存修改**

```bash
git add blink-base/blink-base-app/src/main/java/com/blink/base/service/impl/UserDataScopeCacheServiceImpl.java
git add blink-base/blink-base-app/src/main/java/com/blink/base/service/UserDataScopeCacheService.java
git commit -m "refactor(data-scope): 从USER_TOKEN获取用户信息，废弃USER_INFO"
```

---

## Task 9: 添加数据库配置项

**使用 mcp_blink 工具插入 sys_config 表**

**说明**: 配置 key 使用 `base:session:maxConcurrent`，与 `CommonConstans.SysConfigKeys.SESSION_MAX_CONCURRENT` 常量对应（去掉 `blink:` 前缀）。

- [ ] **Step 1: 插入配置项**

使用 MCP 工具执行 SQL：

```sql
INSERT INTO sys_config (config_key, config_value, config_name, config_group, status, remark, create_time)
VALUES ('base:session:maxConcurrent', '3', '最大并发会话数', '系统配置', 0, '控制同一用户可同时登录的最大设备数量', NOW())
ON DUPLICATE KEY UPDATE config_value = '3', update_time = NOW();
```

- [ ] **Step 2: 验证配置项已添加**

查询验证：

```sql
SELECT * FROM sys_config WHERE config_key = 'base:session:maxConcurrent';
```

---

## Task 10: 编译和验证

- [ ] **Step 1: 编译项目**

```bash
./gradlew :blink-base:blink-base-app:build -x test
./gradlew :blink-gateway:blink-gateway-reactive:build -x test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 运行测试（如果有）**

```bash
./gradlew :blink-base:blink-base-app:test
```

Expected: Tests pass

- [ ] **Step 3: 最终提交**

```bash
git add .
git commit -m "feat(session): 完成多设备登录会话管理重构

- 使用 Redis ZSet 追踪用户所有活跃会话
- Lua 脚本原子化处理登录操作
- 支持可配置的最大设备数（默认3）
- 解决 token 续期不一致问题
- 解决并发登录竞态条件"
```

---

## 测试验证清单

### 功能测试

- [ ] 单设备登录测试（maxDevices=1）：第二次登录踢出第一次
- [ ] 多设备登录测试（maxDevices=3）：第四次登录踢出最早的会话
- [ ] Token 续期测试：验证 USER_TOKEN 和 USER_TOKENS 都被续期
- [ ] 登出测试：验证 token 和 ZSet 都被清理
- [ ] 强制下线测试：验证被踢用户可以收到提示

### 边界测试

- [ ] 并发登录测试：同时发起多个登录请求
- [ ] 配置项不存在测试：使用默认值
- [ ] Redis 连接失败测试：降级处理