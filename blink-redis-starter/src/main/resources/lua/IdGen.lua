-- KEYS[1] key
-- ARGV[1] maxValue
-- ARGV[2] delta

-- 检查参数是否有效
if (ARGV[1] == nil or ARGV[2] == nil) then
    return redis.error_reply("maxValue and delta must be provided")
end

-- 转换参数为数字
local maxValue = tonumber(ARGV[1])
local delta = tonumber(ARGV[2])

if (maxValue == nil or delta == nil) then
    return redis.error_reply("maxValue and delta must be numbers")
end

-- 是否存在key
local existRdsVal = redis.call('EXISTS', KEYS[1])

-- 不存在 保存返回初值
if existRdsVal == 0 then
    redis.call('SET', KEYS[1], delta)
    return delta
end

-- 获取值
local rdsVal = redis.call('GET', KEYS[1])

-- 检查获取的值是否为有效数字
local currentVal = tonumber(rdsVal)
if currentVal == nil then
    -- 如果值不是有效数字，重置为 delta
    redis.call('SET', KEYS[1], delta)
    return delta
end

-- 当前值大于等于最大值 重置
if maxValue <= currentVal then
    redis.call('SET', KEYS[1], delta)
    return delta
-- 当前值 加上 delta 大于等于最大值 key设置为最大值 返回最大值
elseif maxValue <= (delta + currentVal) then
    redis.call('SET', KEYS[1], maxValue)
    return maxValue
else
    -- 正常递增
    return redis.call('INCRBY', KEYS[1], delta)
end