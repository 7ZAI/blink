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

-- 解析数字参数，兼容 JSON 序列化后的字符串（如 "5" 或 "\"5\""）
local function parseNumber(str)
    if str == nil then return nil end
    -- 去掉可能的 JSON 引号
    local cleaned = string.match(str, '^"?(.-)"?$')
    return tonumber(cleaned)
end

local maxDevices = parseNumber(ARGV[1]) or 3
local loginTime = parseNumber(ARGV[2]) or 0
local userInfoJson = ARGV[3]
local ttl = parseNumber(ARGV[4]) or 1800
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

-- 返回字符串类型，避免 Lettuce ValueOutput 不支持整数的问题
return {"1", kickedToken or ''}