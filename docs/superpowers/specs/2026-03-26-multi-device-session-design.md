# 多设备登录会话管理重构设计

## 背景

当前系统存在以下问题：

1. **同一用户多次登录不被踢出**：Token 续期时只更新 `user:token:{token}`，未同步更新 `user:info:{userId}`，导致登录踢出逻辑失效
2. **不支持多设备登录限制**：`user:info:{userId}` 只能存储一个 token，无法追踪多个会话
3. **存在并发登录竞态条件**：极短时间内多次登录可能导致多 token 同时有效

## 目标

1. 支持多设备登录，可配置最大设备数量
2. 达到上限时自动踢出最早登录的会话
3. 解决 token 续期不一致问题
4. 解决并发登录竞态条件

## 设计方案

### Redis Key 结构

| Key | 类型 | 用途 | 过期时间 |
|-----|------|------|----------|
| `user:token:{token}` | String | 存储用户会话信息（UserInfoRedisDO） | 30 分钟 |
| `user:tokens:{userId}` | ZSet | 管理用户所有活跃 token，score=登录时间戳 | 30 分钟 |
| `user:token:old:{token}` | String | 被踢出的旧 token，用于提示 | 5 分钟 |

**废弃的 Key：**
- `user:info:{userId}` - 不再使用

### ZSet 结构说明

```
Key: user:tokens:123
Members: ["token-abc", "token-def", "token-ghi"]
Scores:  [1711440000000, 1711443600000, 1711447200000]  // 登录时间戳

特点：
- Score 最小的 member 是最早登录的 token
- ZCARD 可获取当前设备数量
- ZRANGE 0 0 可获取最早登录的 token
```

## 模块改动

### 1. 常量定义

**文件**: `blink-base-app/src/main/java/com/blink/base/constants/RedisKeyConstans.java`

```java
// 用户会话 ZSet（管理用户所有活跃 token）
String USER_TOKENS = "user:tokens:";

// 保留现有
String USER_TOKEN = "user:token:";
String USER_TOKEN_OLD = "user:token:old:";

// 废弃 USER_INFO
```

**文件**: `blink-base-app/src/main/java/com/blink/base/constants/CommonConstans.java`

```java
// 默认最大设备登录数
Integer DEFAULT_MAX_DEVICES = 3;
```

**文件**: `blink-gateway-reactive/src/main/java/com/blink/gateway/constant/RedisConstans.java`

```java
// 同步添加
String USER_TOKENS = "user:tokens:";
```

### 2. 登录流程

**文件**: `blink-base-app/src/main/java/com/blink/base/service/impl/SysUserAuthServiceImpl.java`

**方法**: `login()`

```
1. 验证用户名密码（现有逻辑不变）

2. 生成新 token

3. 获取最大设备数配置：
   - 从 sys_config 表读取 user_max_devices
   - 不存在则插入默认值
   - 读取失败则使用常量 DEFAULT_MAX_DEVICES（3）

4. 使用 Lua 脚本原子化处理会话（解决并发登录竞态条件）：
   - 脚本逻辑见下方 Lua 脚本定义

5. 返回登录响应
```

#### 登录 Lua 脚本

**文件**: `resources/lua/login_session.lua`

```lua
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

**Java 调用示例**:

```java
public void storeSessionWithLua(Integer userId, String newToken, UserInfoRedisDO userInfo, int maxDevices) {
    String luaScript = loadLuaScript("login_session.lua");
    List<String> keys = Arrays.asList(
        RedisKeyConstans.USER_TOKENS + userId,
        RedisKeyConstans.USER_TOKEN + newToken,
        newToken  // KEYS[3] for ZADD member
    );
    String userInfoJson = JacksonUtil.toJson(userInfo);
    long loginTime = System.currentTimeMillis();

    redisClient.execute(luaScript, keys,
        String.valueOf(maxDevices),
        String.valueOf(loginTime),
        userInfoJson,
        "1800",
        String.valueOf(userId)
    );
}
```

**新增私有方法**:

```java
/**
 * 获取最大设备登录数
 * 优先从数据库配置读取，失败则使用默认值
 */
private int getMaxDevices() {
    try {
        SysConfigVO config = sysConfigService.getOneConfigFromCacheOrDataBase(
            new QueryOneSysConfigReq("user_max_devices"));
        if (config != null && StrUtil.isNotBlank(config.getConfigValue())) {
            return Integer.parseInt(config.getConfigValue());
        }
        // 配置不存在，插入默认值
        insertDefaultMaxDevicesConfig();
        return CommonConstans.DEFAULT_MAX_DEVICES;
    } catch (Exception e) {
        log.warn("[Login] 获取最大设备数配置失败，使用默认值: {}", CommonConstans.DEFAULT_MAX_DEVICES);
        return CommonConstans.DEFAULT_MAX_DEVICES;
    }
}

/**
 * 插入默认的最大设备数配置
 * 使用 mcp_blink 工具插入 sys_config 表
 */
private void insertDefaultMaxDevicesConfig() {
    // SQL: INSERT INTO sys_config (config_key, config_value, config_name, config_group, status, remark, create_time)
    //      VALUES ('user_max_devices', '3', '用户最大设备登录数', '系统配置', 0, '控制同一用户可同时登录的最大设备数量', NOW())
}
```

### 3. 登出流程

**文件**: `SysUserAuthServiceImpl.java`

**方法**: `logout()`

```
1. 查询 user:token:{token} 获取用户信息
2. 验证 userId 和 token 匹配
3. 删除 user:token:{token}
4. ZREM user:tokens:{userId} {token}
5. 清理数据权限缓存（现有逻辑）
```

### 4. Token 续期

**文件**: `blink-gateway-reactive/src/main/java/com/blink/gateway/security/token/TokenAuthenticationSuccessHandler.java`

**方法**: `renewToken()`

```
当 token 剩余时间 <= 5 分钟时：

1. EXPIRE user:token:{token} 1800
2. EXPIRE user:tokens:{userId} 1800

注意：不再需要续期 user:info:{userId}
```

### 5. 在线用户管理

**文件**: `blink-base-app/src/main/java/com/blink/base/service/impl/OnlineUserServiceImpl.java`

**方法**: `kickoutUser()`

```
1. 查询 user:token:{token} 获取 userId
2. 删除 user:token:{token}
3. ZREM user:tokens:{userId} {token}
4. 设置 user:token:old:{token} = userId（5分钟）
```

**方法**: `kickoutUsersByUserIds()`

```
1. 遍历 userId 列表
2. 获取 user:tokens:{userId} 的所有 token
3. 批量删除所有 user:token:{token}
4. 删除 user:tokens:{userId}
```

**方法**: `getOnlineUserList()` - 保持现有实现，扫描 `user:token:*`

### 6. 数据权限缓存

**文件**: `blink-base-app/src/main/java/com/blink/base/service/impl/UserDataScopeCacheServiceImpl.java`

**方法**: `buildAndCache()`

**改动**: 从 `user:token:{token}` 获取用户信息，不再使用 `user:info:{userId}`

需要在登录时将用户信息存入 token 后，其他服务可以从 token 获取用户信息。

### 7. Dashboard 统计

**文件**: `blink-base-app/src/main/java/com/blink/base/service/impl/DashboardServiceImpl.java`

**方法**: `getDashboardData()` - 无需改动，继续扫描 `user:token:*`

## 配置项

### 数据库配置

**表**: `sys_config`

| 字段 | 值 |
|------|-----|
| config_key | `user_max_devices` |
| config_value | `3` |
| config_name | 用户最大设备登录数 |
| config_group | 系统配置 |
| status | 0（启用） |

## 兼容性考虑

### 数据迁移

本次重构废弃 `user:info:{userId}` key，但现有系统中可能存在该 key 的数据：

1. **无需主动迁移**：旧 key 会自然过期（30分钟）
2. **代码兼容**：移除对 `user:info:{userId}` 的所有引用

### 前端影响

- 登录/登出接口不变
- Token 传递方式不变
- 无前端改动需求

## 测试验证

### 功能测试

1. **单设备登录测试**：
   - 设置 maxDevices = 1
   - 同一用户第二次登录应踢出第一次

2. **多设备登录测试**：
   - 设置 maxDevices = 3
   - 第四次登录应踢出最早的会话

3. **续期测试**：
   - 用户活跃时触发续期
   - 验证 user:token:{token} 和 user:tokens:{userId} 都被续期

4. **登出测试**：
   - 验证 token 和 ZSet 都被清理

5. **强制下线测试**：
   - 验证被踢用户收到提示

### 边界测试

1. 并发登录测试
2. 配置项不存在/读取失败测试
3. Redis 连接失败测试

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 旧 key 遗留 | 低 | 自然过期，无影响 |
| 配置读取失败 | 中 | 使用默认值兜底 |
| Redis 连接失败 | 高 | 现有逻辑已有异常处理 |

## 实施步骤

1. 修改常量定义
2. 修改登录逻辑
3. 修改登出逻辑
4. 修改网关续期逻辑
5. 修改在线用户管理
6. 修改数据权限缓存
7. 添加配置项初始化
8. 测试验证