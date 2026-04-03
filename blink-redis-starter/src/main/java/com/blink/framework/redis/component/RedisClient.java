package com.blink.framework.redis.component;

import com.blink.framework.redis.entity.RedisException;
import io.lettuce.core.RedisBusyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Redis 客户端封装类
 * 提供同步 Redis 操作的统一封装
 *
 * @version 1.0
 */
@Slf4j
public class RedisClient {


    private final RedisTemplate<String, Object> template;

    private final RedisTemplate<String, Object>  streamRedisTemplate;

    private final RedisSerializer<String> keySerializer;

    private final RedisSerializer<Object> valueSerializer;

    /**
     * 构造函数
     *
     * @param redisTemplate RedisTemplate 实例
     */
    @SuppressWarnings("unchecked")
    public RedisClient(RedisTemplate<String, Object> redisTemplate, RedisTemplate<String, Object> streamRedisTemplate) {
        this.template = redisTemplate;
        this.streamRedisTemplate = streamRedisTemplate;
        RedisSerializer<?> keySer = redisTemplate.getKeySerializer();
        RedisSerializer<?> valSer = redisTemplate.getValueSerializer();
        if (keySer == null) {
            throw new IllegalStateException("Redis key serializer is not configured");
        }
        if (valSer == null) {
            throw new IllegalStateException("Redis value serializer is not configured");
        }
        this.keySerializer = (RedisSerializer<String>) keySer;
        this.valueSerializer = (RedisSerializer<Object>) valSer;
    }

    public RedisSerializer<Object> getValueSerializer() {
        return valueSerializer;
    }

    public RedisSerializer<String> getKeySerializer() {
        return keySerializer;
    }

    /**
     * 获取原始 RedisTemplate
     *
     * @return template 原始操作对象
     */
    public RedisTemplate<String, Object> getRedisTemplate() {
        return template;
    }


    /* ---------------------------- 通用操作 ---------------------------- */

    /**
     * 判断指定键是否存在
     *
     * @param key 键名
     * @return Boolean true-存在，false-不存在
     */
    public Boolean exists(String key) {
        return template.hasKey(key);
    }

    /**
     * 删除一个或多个键
     *
     * @param keys 要删除的键数组
     * @return Long 实际删除的键数量
     */
    public Long delete(String... keys) {
        return template.delete(List.of(keys));
    }

    /**
     * 删除单个键
     *
     * @param key 要删除的键名
     * @return Boolean true-删除成功，false-键不存在
     */
    public Boolean delete(String key) {
        return template.delete(key);
    }

    /**
     * 设置键的过期时间
     *
     * @param key     键名
     * @param timeout 过期时间
     * @return Boolean true-设置成功，false-键不存在或设置失败
     */
    public Boolean expire(String key, Duration timeout) {
        return template.expire(key, timeout);
    }

    /**
     * 设置键的过期时间（秒）
     *
     * @param key     键名
     * @param seconds 过期时间（秒）
     * @return Boolean true-设置成功，false-键不存在或设置失败
     */
    public Boolean expire(String key, long seconds) {
        return template.expire(key, seconds, TimeUnit.SECONDS);
    }

    /**
     * 获取键的剩余过期时间（秒）
     *
     * @param key 键名
     * @return Long 剩余过期时间（秒），永不过期返回 -1，键不存在返回 -2
     */
    public Long ttl(String key) {
        return template.getExpire(key);
    }

    /**
     * 获取键的剩余过期时间
     *
     * @param key      键名
     * @param timeUnit 时间单位
     * @return Long 剩余过期时间
     */
    public Long ttl(String key, TimeUnit timeUnit) {
        return template.getExpire(key, timeUnit);
    }

    /**
     * 移除键的过期时间，使其永久有效
     *
     * @param key 键名
     * @return Boolean true-移除成功，false-键不存在或本来就是永久有效
     */
    public Boolean persist(String key) {
        return template.persist(key);
    }


    /**
     * 获取键的数据类型
     *
     * @param key 键名
     * @return String 数据类型名称，如 "STRING", "HASH", "LIST" 等
     */
    public String type(String key) {
        return template.type(key).code();
    }

    /* ---------------------------- String 操作 ---------------------------- */

    /**
     * 设置键值对
     *
     * @param key   键名
     * @param value 值
     */
    public void set(String key, Object value) {
        template.opsForValue().set(key, value);
    }

    /**
     * 设置带过期时间的键值对
     *
     * @param key     键名
     * @param value   值
     * @param timeout 过期时间
     */
    public void setEx(String key, Object value, Duration timeout) {
        template.opsForValue().set(key, value, timeout);
    }

    /**
     * 设置带过期时间的键值对（秒）
     *
     * @param key     键名
     * @param value   值
     * @param seconds 过期时间（秒）
     */
    public void setEx(String key, Object value, long seconds) {
        template.opsForValue().set(key, value, seconds, TimeUnit.SECONDS);
    }

    /**
     * 仅当键不存在时设置值
     *
     * @param key   键名
     * @param value 值
     * @return Boolean true-设置成功，false-键已存在
     */
    public Boolean setIfAbsent(String key, Object value) {
        return template.opsForValue().setIfAbsent(key, value);
    }

    /**
     * 仅当键不存在时设置带过期时间的值（原子操作）
     *
     * @param key     键名
     * @param value   值
     * @param timeout 过期时间
     * @return Boolean true-设置成功，false-键已存在
     */
    public Boolean setIfAbsentWithExpire(String key, Object value, Duration timeout) {
        return template.opsForValue().setIfAbsent(key, value, timeout);
    }

    /**
     * 仅当键存在时设置值
     *
     * @param key   键名
     * @param value 值
     * @return Boolean true-设置成功，false-键不存在
     */
    public Boolean setIfPresent(String key, Object value) {
        return template.opsForValue().setIfPresent(key, value);
    }

    /**
     * 获取指定键的值
     *
     * @param key 键名
     * @return Object 键对应的值，键不存在返回 null
     */
    public Object get(String key) {
        return template.opsForValue().get(key);
    }

    /**
     * 获取并设置新值
     *
     * @param key   键名
     * @param value 新值
     * @return Object 旧值，键不存在返回 null
     */
    public Object getAndSet(String key, Object value) {
        return template.opsForValue().getAndSet(key, value);
    }

    /**
     * 对整数值进行递增操作
     *
     * @param key 键名
     * @return Long 递增后的值
     */
    public Long increment(String key) {
        return template.opsForValue().increment(key);
    }

    /**
     * 对整数值递增指定步长
     *
     * @param key   键名
     * @param delta 递增步长
     * @return Long 递增后的值
     */
    public Long incrementBy(String key, long delta) {
        return template.opsForValue().increment(key, delta);
    }

    /**
     * 对浮点数值递增指定步长
     *
     * @param key   键名
     * @param delta 递增步长
     * @return Double 递增后的值
     */
    public Double incrementBy(String key, double delta) {
        return template.opsForValue().increment(key, delta);
    }

    /**
     * 对整数值进行递减操作
     *
     * @param key 键名
     * @return Long 递减后的值
     */
    public Long decrement(String key) {
        return template.opsForValue().decrement(key);
    }

    /**
     * 对整数值递减指定步长
     *
     * @param key   键名
     * @param delta 递减步长
     * @return Long 递减后的值
     */
    public Long decrementBy(String key, long delta) {
        return template.opsForValue().decrement(key, delta);
    }

    /**
     * 获取字符串值的长度
     *
     * @param key 键名
     * @return Long 字符串长度，键不存在返回 0
     */
    public Long strLen(String key) {
        return template.opsForValue().size(key);
    }

    /**
     * 追加字符串值
     *
     * @param key   键名
     * @param value 要追加的值
     * @return Integer 追加后的字符串长度
     */
    public Integer append(String key, String value) {
        return template.opsForValue().append(key, value);
    }

    /* ---------------------------- Hash 操作 ---------------------------- */

    /**
     * 设置哈希字段的值
     *
     * @param key   哈希键名
     * @param field 字段名
     * @param value 字段值
     */
    public void hPutField(String key, String field, Object value) {
        template.opsForHash().put(key, field, value);
    }

    /**
     * 设置哈希字段
     *
     * @param key         哈希键名
     * @param fieldValues 字段-值映射表 map
     */
    public void hSet(String key, Map<String, Object> fieldValues) {
        template.opsForHash().putAll(key, fieldValues);
    }

    /**
     * 获取哈希字段的值
     *
     * @param key   哈希键名
     * @param field 字段名
     * @return Object 字段值，字段不存在返回 null
     */
    public Object hGetField(String key, String field) {
        return template.opsForHash().get(key, field);
    }

    /**
     * 获取当个Map中多个字段的值
     *
     * @param key    哈希键名
     * @param fields 字段名列表
     * @return List<Object> 字段值列表，不存在的字段对应位置为 null
     */
    public List<Object> hMultiGetFields(String key, List<String> fields) {
        return template.opsForHash().multiGet(key, List.copyOf(fields));
    }

    /**
     * 获取单个哈希的所有字段和值 (Map)
     *
     * @param key 哈希键名
     * @return Map<Object, Object> 字段-值映射表
     */
    public Map<?, Object> hGet(String key) {

        return template.opsForHash().entries(key);
    }

    /**
     * 获取key为String类型的 哈希集合
     *
     * @param key 哈希键名
     * @return Map<String, Object> 字段-值映射表
     */
    public Map<String, Object> hGetStringMap(String key) {
        Map<?, Object> rawMap = hGet(key);
        if (rawMap == null || rawMap.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = new HashMap<>(rawMap.size());
        for (Map.Entry<?, Object> entry : rawMap.entrySet()) {
            Object keyObj = entry.getKey();
            if (keyObj != null) {
                result.put(keyObj.toString(), entry.getValue());
            }
        }
        return result;
    }


    /**
     * 删除哈希中的一个或多个字段
     *
     * @param key    哈希键名
     * @param fields 要删除的字段名
     * @return Long 实际删除的字段数量
     */
    public Long hDeleteFields(String key, String... fields) {
        return template.opsForHash().delete(key, (Object[]) fields);
    }

    /**
     * 判断哈希中字段是否存在
     *
     * @param key   哈希键名
     * @param field 字段名
     * @return Boolean true-字段存在，false-字段不存在
     */
    public Boolean hExists(String key, String field) {
        return template.opsForHash().hasKey(key, field);
    }

    /**
     * 获取哈希中字段的数量
     *
     * @param key 哈希键名
     * @return Long 字段数量
     */
    public Long hSize(String key) {
        return template.opsForHash().size(key);
    }

    /**
     * 对哈希中的整数字段进行递增操作
     *
     * @param key   哈希键名
     * @param field 字段名
     * @param delta 递增步长
     * @return Long 递增后的值
     */
    public Long hIncrement(String key, String field, long delta) {
        return template.opsForHash().increment(key, field, delta);
    }



    /* ---------------------------- List 操作 ---------------------------- */

    /**
     * 从列表左侧推入一个或多个值
     *
     * @param key    列表键名
     * @param values 要推入的值数组
     * @return Long 推入后列表的长度
     */
    public Long lPush(String key, Object... values) {
        return template.opsForList().leftPushAll(key, values);
    }

    /**
     * 从列表右侧推入一个或多个值
     *
     * @param key    列表键名
     * @param values 要推入的值数组
     * @return Long 推入后列表的长度
     */
    public Long rPush(String key, Object... values) {

        return template.opsForList().rightPushAll(key, values);
    }

    /**
     * 从列表左侧弹出一个值
     *
     * @param key 列表键名
     * @return Object 弹出的值，列表为空返回 null
     */
    public Object lPop(String key) {
        return template.opsForList().leftPop(key);
    }

    /**
     * 从列表右侧弹出一个值
     *
     * @param key 列表键名
     * @return Object 弹出的值，列表为空返回 null
     */
    public Object rPop(String key) {
        return template.opsForList().rightPop(key);
    }

    /**
     * 从列表左侧弹出值，如果列表为空则等待指定时间
     *
     * @param key     列表键名
     * @param timeout 等待时间
     * @param unit    时间单位
     * @return Object 弹出的值，超时返回 null
     */
    public Object lPop(String key, long timeout, TimeUnit unit) {
        return template.opsForList().leftPop(key, timeout, unit);
    }

    /**
     * 从列表右侧弹出值，如果列表为空则等待指定时间
     *
     * @param key     列表键名
     * @param timeout 等待时间
     * @param unit    时间单位
     * @return Object 弹出的值，超时返回 null
     */
    public Object rPop(String key, long timeout, TimeUnit unit) {
        return template.opsForList().rightPop(key, timeout, unit);
    }

    /**
     * 获取列表指定范围内的元素
     *
     * @param key   列表键名
     * @param start 起始索引（包含）
     * @param end   结束索引（包含）
     * @return List<Object> 元素列表
     */
    public List<Object> lRange(String key, long start, long end) {
        return template.opsForList().range(key, start, end);
    }

    /**
     * 获取列表长度
     *
     * @param key 列表键名
     * @return Long 列表长度
     */
    public Long lLen(String key) {
        return template.opsForList().size(key);
    }

    /**
     * 根据索引获取列表元素
     *
     * @param key   列表键名
     * @param index 元素索引
     * @return Object 指定索引的元素，索引越界返回 null
     */
    public Object lIndex(String key, long index) {
        return template.opsForList().index(key, index);
    }

    /**
     * 设置列表指定索引的元素值
     *
     * @param key   列表键名
     * @param index 元素索引
     * @param value 新值
     */
    public void lSet(String key, long index, Object value) {
        template.opsForList().set(key, index, value);
    }

    /**
     * 从列表中移除指定数量的元素
     *
     * @param key   列表键名
     * @param count 移除数量
     * @param value 要移除的值
     * @return Long 实际移除的元素数量
     */
    public Long lRemove(String key, long count, Object value) {
        return template.opsForList().remove(key, count, value);
    }

    /**
     * 修剪列表，只保留指定范围内的元素
     *
     * @param key   列表键名
     * @param start 起始索引
     * @param end   结束索引
     */
    public void lTrim(String key, long start, long end) {
        template.opsForList().trim(key, start, end);
    }

    /* ---------------------------- Set 操作 ---------------------------- */

    /**
     * 向集合中添加一个或多个成员
     *
     * @param key    集合键名
     * @param values 要添加的成员数组
     * @return Long 实际添加的成员数量（已存在的成员不会被重复添加）
     */
    public Long sAdd(String key, Object... values) {
        return template.opsForSet().add(key, values);
    }

    /**
     * 获取集合中的所有成员
     *
     * @param key 集合键名
     * @return Set<Object> 成员集合
     */
    public Set<Object> sMembers(String key) {
        return template.opsForSet().members(key);
    }

    /**
     * 判断指定值是否是集合的成员
     *
     * @param key   集合键名
     * @param value 要判断的值
     * @return Boolean true-是成员，false-不是成员
     */
    public Boolean sIsMember(String key, Object value) {
        return template.opsForSet().isMember(key, value);
    }

    /**
     * 获取集合的成员数量
     *
     * @param key 集合键名
     * @return Long 成员数量
     */
    public Long sSize(String key) {
        return template.opsForSet().size(key);
    }

    /**
     * 从集合中移除一个或多个成员
     *
     * @param key    集合键名
     * @param values 要移除的成员数组
     * @return Long 实际移除的成员数量
     */
    public Long sRemove(String key, Object... values) {
        return template.opsForSet().remove(key, values);
    }

    /**
     * 随机从集合中弹出一个成员
     *
     * @param key 集合键名
     * @return Object 弹出的成员，集合为空返回 null
     */
    public Object sPop(String key) {
        return template.opsForSet().pop(key);
    }

    /**
     * 随机从集合中获取指定数量的成员（不删除）
     *
     * @param key   集合键名
     * @param count 成员数量
     * @return List<Object> 随机成员列表
     */
    public List<Object> sRandomMembers(String key, long count) {
        return template.opsForSet().randomMembers(key, count);
    }

    /**
     * 求多个集合的并集
     *
     * @param keys 集合键名列表
     * @return Set<Object> 并集结果
     */
    public Set<Object> sUnion(List<String> keys) {
        return template.opsForSet().union(keys.get(0), keys.subList(1, keys.size()));
    }

    /**
     * 求多个集合的交集
     *
     * @param keys 集合键名列表
     * @return Set<Object> 交集结果
     */
    public Set<Object> sIntersect(List<String> keys) {
        return template.opsForSet().intersect(keys.get(0), keys.subList(1, keys.size()));
    }

    /**
     * 求多个集合的差集
     *
     * @param keys 集合键名列表
     * @return Set<Object> 差集结果
     */
    public Set<Object> sDifference(List<String> keys) {
        return template.opsForSet().difference(keys.get(0), keys.subList(1, keys.size()));
    }

    /* ---------------------------- ZSet 操作 ---------------------------- */

    /**
     * 向有序集合添加一个成员
     *
     * @param key   有序集合键名
     * @param value 成员值
     * @param score 成员分数
     * @return Boolean true-添加成功，false-成员已存在且分数被更新
     */
    public Boolean zAdd(String key, Object value, double score) {
        return template.opsForZSet().add(key, value, score);
    }

    /**
     * 批量向有序集合添加成员
     *
     * @param key         有序集合键名
     * @param valueScores 成员-分数映射表
     * @return Long 实际添加的成员数量
     */
    public Long zAdd(String key, Map<Object, Double> valueScores) {
        Set<ZSetOperations.TypedTuple<Object>> tuples =
                valueScores.entrySet().stream()
                        .map(entry -> org.springframework.data.redis.core.ZSetOperations.TypedTuple.of(
                                entry.getKey(), entry.getValue()))
                        .collect(java.util.stream.Collectors.toSet());
        return template.opsForZSet().add(key, tuples);
    }

    /**
     * 获取有序集合指定索引范围的成员（按分数升序）
     *
     * @param key   有序集合键名
     * @param start 起始索引（包含）
     * @param end   结束索引（包含）
     * @return Set<Object> 成员集合
     */
    public Set<Object> zRange(String key, long start, long end) {
        return template.opsForZSet().range(key, start, end);
    }

    /**
     * 获取有序集合指定分数范围的成员（按分数升序）
     *
     * @param key 有序集合键名
     * @param min 最小分数（包含）
     * @param max 最大分数（包含）
     * @return Set<Object> 成员集合
     */
    public Set<Object> zRangeByScore(String key, double min, double max) {
        return template.opsForZSet().rangeByScore(key, min, max);
    }

    /**
     * 获取有序集合指定分数范围的成员（带偏移量和数量限制）
     *
     * @param key    有序集合键名
     * @param min    最小分数
     * @param max    最大分数
     * @param offset 偏移量
     * @param count  数量限制
     * @return Set<Object> 成员集合
     */
    public Set<Object> zRangeByScore(String key, double min, double max, long offset, long count) {
        return template.opsForZSet().rangeByScore(key, min, max, offset, count);
    }

    /**
     * 获取有序集合中指定成员的分数
     *
     * @param key   有序集合键名
     * @param value 成员值
     * @return Double 成员分数，成员不存在返回 null
     */
    public Double zScore(String key, Object value) {
        return template.opsForZSet().score(key, value);
    }

    /**
     * 获取有序集合的成员数量
     *
     * @param key 有序集合键名
     * @return Long 成员数量
     */
    public Long zSize(String key) {
        return template.opsForZSet().size(key);
    }

    /**
     * 获取有序集合中指定分数范围内的成员数量
     *
     * @param key 有序集合键名
     * @param min 最小分数
     * @param max 最大分数
     * @return Long 成员数量
     */
    public Long zCount(String key, double min, double max) {
        return template.opsForZSet().count(key, min, max);
    }

    /**
     * 获取有序集合中指定成员的排名（按分数升序，0表示第一名）
     *
     * @param key   有序集合键名
     * @param value 成员值
     * @return Long 成员排名，成员不存在返回 null
     */
    public Long zRank(String key, Object value) {
        return template.opsForZSet().rank(key, value);
    }

    /**
     * 获取有序集合中指定成员的排名（按分数降序，0表示第一名）
     *
     * @param key   有序集合键名
     * @param value 成员值
     * @return Long 成员排名，成员不存在返回 null
     */
    public Long zReverseRank(String key, Object value) {
        return template.opsForZSet().reverseRank(key, value);
    }

    /**
     * 从有序集合中移除一个或多个成员
     *
     * @param key    有序集合键名
     * @param values 要移除的成员数组
     * @return Long 实际移除的成员数量
     */
    public Long zRemove(String key, Object... values) {
        return template.opsForZSet().remove(key, values);
    }

    /**
     * 移除有序集合中指定排名范围的成员
     *
     * @param key   有序集合键名
     * @param start 起始排名
     * @param end   结束排名
     * @return Long 实际移除的成员数量
     */
    public Long zRemoveRange(String key, long start, long end) {
        return template.opsForZSet().removeRange(key, start, end);
    }

    /**
     * 移除有序集合中指定分数范围的成员
     *
     * @param key 有序集合键名
     * @param min 最小分数
     * @param max 最大分数
     * @return Long 实际移除的成员数量
     */
    public Long zRemoveRangeByScore(String key, double min, double max) {
        return template.opsForZSet().removeRangeByScore(key, min, max);
    }

    /**
     * 对有序集合中指定成员的分数进行递增操作
     *
     * @param key   有序集合键名
     * @param value 成员值
     * @param delta 递增步长
     * @return Double 递增后的分数
     */
    public Double zIncrementScore(String key, Object value, double delta) {
        return template.opsForZSet().incrementScore(key, value, delta);
    }

    /* ---------------------------- 发布订阅操作 ---------------------------- */

    /**
     * 向指定频道发布消息
     *
     * @param channel 频道名称
     * @param message 消息内容
     */
    public void publish(String channel, Object message) {
        template.convertAndSend(channel, message);
    }

    /* ---------------------------- Lua 脚本执行 ---------------------------- */

    /**
     * 执行 RedisScript 对象
     *
     * @param <T>    返回结果类型
     * @param script RedisScript 对象
     * @param keys   脚本中使用的键列表
     * @param args   脚本参数列表
     * @return T 脚本执行结果
     */
    public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
        return template.execute(script, keys, args);
    }

    /**
     * 执行 Lua 脚本字符串
     *
     * @param scriptStr Lua 脚本字符串
     * @param keys      脚本中使用的键列表
     * @param args      脚本参数列表
     * @return Object 脚本执行结果
     */
    public Object execute(String scriptStr, List<String> keys, List<Object> args) {
        RedisScript<Object> script = RedisScript.of(scriptStr);
        return template.execute(script, keys, args.toArray());
    }

    /**
     * 执行 Lua 脚本
     *
     * @param scriptStr        Lua 脚本字符串
     * @param resultSerializer 结果序列化
     * @param keys             脚本中使用的键列表
     * @param args             脚本参数列表
     * @param <T>
     * @return
     */
    public <T> T execute(RedisScript<T> scriptStr, RedisSerializer<T> resultSerializer, List<String> keys, Object... args) {

        return template.execute(scriptStr, new StringRedisSerializer(), resultSerializer, keys, args);
    }

    /**
     * 执行 Lua 脚本（使用 StringRedisSerializer 作为结果序列化器）
     * 适用于 Lua 脚本返回非 JSON 格式的数据（如整数、字符串列表等）
     *
     * @param script RedisScript 对象
     * @param keys   脚本中使用的键列表
     * @param args   脚本参数列表
     * @return Object 脚本执行结果
     */
    public Object executeWithStringSerializer(RedisScript<?> script, List<String> keys, Object... args) {
        // 使用 RedisCallback 直接执行，避免 GenericJackson2JsonRedisSerializer 反序列化问题
        return template.execute((RedisCallback<Object>) connection -> {
            // 序列化 keys
            byte[][] keysBytes = keys.stream()
                .map(key -> keySerializer.serialize(key))
                .toArray(byte[][]::new);

            // 序列化 args（使用字符串序列化器）
            byte[][] argsBytes = new byte[args.length][];
            for (int i = 0; i < args.length; i++) {
                argsBytes[i] = keySerializer.serialize(String.valueOf(args[i]));
            }

            // 合并 keys 和 args
            byte[][] allBytes = new byte[keysBytes.length + argsBytes.length][];
            System.arraycopy(keysBytes, 0, allBytes, 0, keysBytes.length);
            System.arraycopy(argsBytes, 0, allBytes, keysBytes.length, argsBytes.length);

            // 执行脚本
            return connection.scriptingCommands().eval(
                script.getScriptAsString().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                org.springframework.data.redis.connection.ReturnType.fromJavaType(script.getResultType()),
                keysBytes.length,
                allBytes
            );
        });
    }





    /* ---------------------------- 管道操作 ---------------------------- */

    /**
     * 开启管道执行批量操作
     *
     * @param callback RedisCallback 回调接口
     * @return Object 操作结果
     */
    public Object executePipelined(RedisCallback<?> callback) {
        return template.executePipelined(callback);
    }

    /**
     * 开启管道执行批量操作
     *
     * @param callback SessionCallback 回调接口
     * @return List<Object> 操作结果列表
     */
    public List<Object> executePipelined(SessionCallback<?> callback) {
        return template.executePipelined(callback);
    }

    /* ---------------------------- 扫描操作 ---------------------------- */

    /**
     * 扫描匹配指定模式的键
     *
     * @param pattern 键的模式
     * @param count   每次扫描的数量
     * @return Cursor<String> 键的游标
     */
    public Cursor<String> scan(String pattern, long count) {
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(count).build();
        return template.scan(options);
    }

    /* ---------------------------- 工具方法 ---------------------------- */

    /**
     * 获取原始 RedisTemplate 实例
     *
     * @return RedisTemplate<String, Object> 原始模板实例
     */
    public RedisTemplate<String, Object> getTemplate() {
        return template;
    }

    /**
     * 安全获取操作，如果键不存在返回默认值
     *
     * @param key          键名
     * @param defaultValue 默认值
     * @return Object 键值或默认值
     */
    public Object getOrDefault(String key, Object defaultValue) {
        Object value = get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 带重试机制的设置操作
     *
     * @param key        键名
     * @param value      值
     * @param maxRetries 最大重试次数
     * @return Boolean true-设置成功，false-设置失败（包括重试后仍然失败）
     */
    public Boolean setWithRetry(String key, Object value, int maxRetries) {
        int retries = 0;
        while (retries < maxRetries) {
            try {
                set(key, value);
                return true;
            } catch (Exception e) {
                retries++;
                log.warn("Set operation failed, retry {}/{}", retries, maxRetries, e);
                if (retries >= maxRetries) {
                    log.error("Set operation failed after {} retries", maxRetries, e);
                    return false;
                }
                try {
                    Thread.sleep(100L * retries); // 指数退避
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * 批量获取多个键的值
     *
     * @param keys 键名列表
     * @return List<Object> 值列表
     */
    public List<Object> multiGet(List<String> keys) {

        return template.opsForValue().multiGet(keys);
    }

    /**
     * 批量获取多个键的值
     *
     * @param keys 键列表
     * @return Map<String, Object> 键值映射
     */
    public Map<String, Object> batchGet(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            List<Object> values = template.opsForValue().multiGet(keys);
            Map<String, Object> result = new HashMap<>();

            for (int i = 0; i < keys.size(); i++) {
                if (i < values.size() && values.get(i) != null) {
                    result.put(keys.get(i), values.get(i));
                }
            }

            return result;
        } catch (Exception e) {
            log.error("Batch get failed, keys count: {}", keys.size(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * 批量设置多个键的值
     *
     * @param map 键值映射表
     */
    public void batchSet(Map<String, Object> map) {
        template.opsForValue().multiSet(map);
    }

    /**
     * 批量设置键值对，并设置统一的过期时间
     *
     * @param keyValueMap 键值对映射
     * @param expire      过期时间
     * @param timeUnit    时间单位
     * @return Boolean true-设置成功
     */
    public Boolean batchSetWithExpire(Map<String, Object> keyValueMap, long expire, TimeUnit timeUnit) {
        if (keyValueMap == null || keyValueMap.isEmpty()) {
            log.warn("Batch set with expire: keyValueMap is null or empty");
            return true;
        }
        long start = System.currentTimeMillis();
        try {
            template.executePipelined(
                    new org.springframework.data.redis.core.RedisCallback<Object>() {
                        @Override
                        public Object doInRedis(RedisConnection connection) {
                            keyValueMap.forEach((key, value) -> {
                                byte[] keyBytes = keySerializer.serialize(key);
                                byte[] valueBytes = valueSerializer.serialize(value);

                                if (keyBytes != null && valueBytes != null) {
                                    connection.stringCommands().setEx(keyBytes, timeUnit.toSeconds(expire), valueBytes);
                                }
                            });
                            return null;
                        }
                    }
            );
            log.info("Batch set with expire completed, keys count: {}, expire: {} {} , cost:{} ms",
                    keyValueMap.size(), expire, timeUnit, System.currentTimeMillis() - start);
            return true;
        } catch (Exception e) {
            log.error("Batch set with expire failed, keys count: {}", keyValueMap.size(), e);
            return false;
        }
    }


    /**
     * 仅当所有键都不存在时批量设置值
     *
     * @param map 键值映射表
     * @return Boolean true-设置成功，false-至少有一个键已存在
     */
    public Boolean multiSetIfAbsent(Map<String, Object> map) {
        return template.opsForValue().multiSetIfAbsent(map);
    }

    /* ---------------------------- 批量删除方法 ---------------------------- */

    /**
     * 根据前缀批量删除键（使用 KEYS 命令，不推荐在生产环境使用）
     *
     * @param pattern 键的前缀模式
     * @return Long 实际删除的键数量
     * @deprecated 在生产环境不推荐使用，可能阻塞 Redis 服务
     */
    @Deprecated
    public Long deleteByPrefix(String pattern) {
        try {
            String matchPattern = pattern.endsWith("*") ? pattern : pattern + "*";
            Set<String> keys = template.keys(matchPattern);
            if (keys != null && !keys.isEmpty()) {
                return template.delete(keys);
            }
            return 0L;
        } catch (Exception e) {
            log.error("Delete by prefix failed, pattern: {}", pattern, e);
            return 0L;
        }
    }

    /**
     * 根据前缀批量删除键（使用 SCAN 命令，推荐在生产环境使用）
     *
     * @param pattern 键的前缀模式
     * @return Long 实际删除的键数量
     */
    public Long deleteByPrefixScan(String pattern) {
        return deleteByPrefixScan(pattern, 100);
    }

    /**
     * 根据前缀批量删除键（使用 SCAN 命令，可配置批次大小）
     *
     * @param pattern   键的前缀模式
     * @param batchSize 每次扫描的批次大小
     * @return Long 实际删除的键数量
     */
    public Long deleteByPrefixScan(String pattern, int batchSize) {
        AtomicLong totalDeleted = new AtomicLong(0);
        log.info("Start Batch DeleteByPrefixScan with pattern: {}，batchsize: {}", pattern, batchSize);
        long start = System.currentTimeMillis();
        try {
            String matchPattern = pattern.endsWith("*") ? pattern : pattern + "*";
            ScanOptions options = ScanOptions.scanOptions()
                    .match(matchPattern)
                    .count(batchSize)
                    .build();

            Cursor<String> cursor = template.scan(options);
            List<String> keysToDelete = new ArrayList<>();

            while (cursor.hasNext()) {
                String key = cursor.next();
                keysToDelete.add(key);

                // 批量删除，避免单次删除操作过多
                if (keysToDelete.size() >= batchSize) {
                    Long deleted = template.delete(keysToDelete);
                    totalDeleted.addAndGet(deleted != null ? deleted : 0);
                    keysToDelete.clear();
                    log.debug("Deleted batch of {} keys, total deleted: {}", batchSize, totalDeleted.get());

                    // 防止长时间占用，添加短暂延迟
                    try {
                        TimeUnit.MILLISECONDS.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            // 删除剩余的键
            if (!keysToDelete.isEmpty()) {
                Long deleted = template.delete(keysToDelete);
                totalDeleted.addAndGet(deleted != null ? deleted : 0);
                log.debug("Deleted final batch of {} keys, total deleted: {}", keysToDelete.size(), totalDeleted.get());
            }

            cursor.close();

        } catch (Exception e) {
            log.error("Delete by prefix scan failed, pattern: {}", pattern, e);
        }

        log.info("Deleted {} keys with pattern: {}, cost: {} ms", totalDeleted.get(), pattern, System.currentTimeMillis() - start);
        return totalDeleted.get();
    }

    /**
     * 根据前缀批量删除键（使用管道操作，最高效的方式）
     *
     * @param pattern 键的前缀模式
     * @return Long 实际删除的键数量
     */
    public Long deleteByPrefixPipeline(String pattern) {
        return deleteByPrefixPipeline(pattern, 100);
    }

    /**
     * 根据前缀批量删除键（使用管道操作，可配置批次大小）
     *
     * @param pattern   键的前缀模式
     * @param batchSize 每次处理的批次大小
     * @return Long 实际删除的键数量
     */
    public Long deleteByPrefixPipeline(String pattern, int batchSize) {

        log.info("Start Batch DeleteByPrefixPipeline with pattern: {}，batchsize: {}", pattern, batchSize);
        long start = System.currentTimeMillis();
        AtomicLong totalDeleted = new AtomicLong(0);

        try {
            String matchPattern = pattern.endsWith("*") ? pattern : pattern + "*";

            List<Object> results = template.executePipelined(
                    new org.springframework.data.redis.core.RedisCallback<Object>() {
                        @Override
                        public Object doInRedis(org.springframework.data.redis.connection.RedisConnection connection) {
                            ScanOptions options = ScanOptions.scanOptions()
                                    .match(matchPattern)
                                    .count(batchSize)
                                    .build();

                            List<byte[]> keysToDelete = new ArrayList<>();
                            
                            try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                                while (cursor.hasNext()) {
                                    byte[] key = cursor.next();
                                    keysToDelete.add(key);

                                    if (keysToDelete.size() >= batchSize) {
                                        connection.keyCommands().del(keysToDelete.toArray(new byte[0][]));
                                        totalDeleted.addAndGet(keysToDelete.size());
                                        keysToDelete.clear();
                                    }
                                }

                                if (!keysToDelete.isEmpty()) {
                                    connection.keyCommands().del(keysToDelete.toArray(new byte[0][]));
                                    totalDeleted.addAndGet(keysToDelete.size());
                                }
                            }
                            return null;
                        }
                    }
            );

        } catch (Exception e) {
            log.error("Delete by prefix pipeline failed, pattern: {}", pattern, e);
        }

        log.info("Deleted {} keys with pattern: {} using pipeline cost: {} ms", totalDeleted.get(), pattern, System.currentTimeMillis() - start);


        return totalDeleted.get();
    }

    /**
     * 使用 Lua 脚本批量删除（原子操作，最高效）
     *
     * @param pattern 键的前缀模式
     * @return Long 实际删除的键数量
     */
    public Long deleteByPrefixLua(String pattern) {
        try {
            // Lua 脚本：扫描并删除匹配的键
            String luaScript =
                    "local keys = redis.call('keys', ARGV[1]) " +
                            "local deleted = 0 " +
                            "for i, key in ipairs(keys) do " +
                            "    redis.call('del', key) " +
                            "    deleted = deleted + 1 " +
                            "end " +
                            "return deleted";

            RedisScript<Long> script = new DefaultRedisScript<>(luaScript, Long.class);
            String matchPattern = pattern.endsWith("*") ? pattern : pattern + "*";

            Long result = template.execute(script, null, matchPattern);
            log.info("Deleted {} keys with pattern: {} using Lua script", result, pattern);
            return result != null ? result : 0L;

        } catch (Exception e) {
            log.error("Delete by prefix Lua script failed, pattern: {}", pattern, e);
            // Lua 脚本失败时回退到 SCAN 方式
            return deleteByPrefixScan(pattern);
        }
    }

    /**
     * 安全删除 - 带重试机制的批量删除
     *
     * @param pattern    键的前缀模式
     * @param maxRetries 最大重试次数
     * @return Boolean true-删除成功，false-删除失败
     */
    public Boolean deleteByPrefixSafely(String pattern, int maxRetries) {
        return deleteByPrefixSafely(pattern, 100, maxRetries);
    }

    /**
     * 安全删除 - 带重试机制的批量删除
     *
     * @param pattern    键的前缀模式
     * @param batchSize  批次大小
     * @param maxRetries 最大重试次数
     * @return Boolean true-删除成功，false-删除失败
     */
    public Boolean deleteByPrefixSafely(String pattern, int batchSize, int maxRetries) {
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                Long deletedCount = deleteByPrefixPipeline(pattern, batchSize);
                log.info("Successfully deleted {} keys with pattern: {}", deletedCount, pattern);
                return true;

            } catch (Exception e) {
                retryCount++;
                log.warn("Delete operation failed, retry {}/{}, pattern: {}",
                        retryCount, maxRetries, pattern, e);

                if (retryCount >= maxRetries) {
                    log.error("Delete operation failed after {} retries, pattern: {}",
                            maxRetries, pattern, e);
                    return false;
                }

                // 指数退避
                try {
                    TimeUnit.MILLISECONDS.sleep(100 * (long) Math.pow(2, retryCount));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        return false;
    }


    /**
     * 获取匹配前缀的键数量（不删除）
     *
     * @param pattern 键的前缀模式
     * @return Long 匹配的键数量
     */
    public Long countByPrefix(String pattern) {
        AtomicLong count = new AtomicLong(0);

        try {
            String matchPattern = pattern.endsWith("*") ? pattern : pattern + "*";
            ScanOptions options = ScanOptions.scanOptions()
                    .match(matchPattern)
                    .count(1000)
                    .build();

            Cursor<String> cursor = template.scan(options);

            while (cursor.hasNext()) {
                cursor.next();
                count.incrementAndGet();
            }

            cursor.close();

        } catch (Exception e) {
            log.error("Count by prefix failed, pattern: {}", pattern, e);
        }

        return count.get();
    }

    /**
     * 分页获取匹配前缀的键
     *
     * @param pattern  键的前缀模式
     * @param pageSize 每页大小
     * @param page     页码（从0开始）
     * @return List<String> 键列表
     */
    public List<String> getKeysByPrefix(String pattern, int pageSize, int page) {
        List<String> keys = new ArrayList<>();

        try {
            String matchPattern = pattern.endsWith("*") ? pattern : pattern + "*";
            ScanOptions options = ScanOptions.scanOptions()
                    .match(matchPattern)
                    .count(pageSize)
                    .build();

            Cursor<String> cursor = template.scan(options);

            // 跳过前面的页
            int skipCount = page * pageSize;
            for (int i = 0; i < skipCount && cursor.hasNext(); i++) {
                cursor.next();
            }

            // 获取当前页的键
            for (int i = 0; i < pageSize && cursor.hasNext(); i++) {
                keys.add(cursor.next());
            }

            cursor.close();

        } catch (Exception e) {
            log.error("Get keys by prefix failed, pattern: {}", pattern, e);
        }

        return keys;
    }

    /**
     * 批量删除指定的键列表
     *
     * @param keys 要删除的键列表
     * @return Long 实际删除的键数量
     */
    public Long deleteKeys(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }

        try {
            return template.delete(keys);
        } catch (Exception e) {
            log.error("Delete keys failed, keys count: {}", keys.size(), e);
            return 0L;
        }
    }

    /* ---------------------------- Stream 操作 ---------------------------- */

    /**
     * 发送消息到指定的 Stream
     *
     * @param streamKey     Stream 的键
     * @param fieldValueMap 消息字段和值的映射
     * @return 消息ID，如果发送失败返回 null
     * @throws RedisException 当 Redis 操作失败时抛出
     */
    public String xAdd(String streamKey, Map<String, Object> fieldValueMap) {
        try {
            RecordId recordId = streamRedisTemplate.opsForStream().add(streamKey,fieldValueMap);
            return recordId != null ? recordId.getValue() : null;
        } catch (Exception e) {
            log.error("Failed to add message to stream: {}", streamKey, e);
            throw e;
        }
    }

    /**
     * 发送消息到指定的 Stream
     *
     * @param streamKey     Stream 的键
     * @param value 值
     * @return 消息ID，如果发送失败返回 null
     * @throws RedisException 当 Redis 操作失败时抛出
     */
    public String xAdd(String streamKey, Object  value) {
        try {
            // 构建消息
            ObjectRecord<String, Object> record = StreamRecords.newRecord()
                    .in(streamKey)
                    .ofObject(value);
            RecordId recordId = streamRedisTemplate.opsForStream().add(record);
            return recordId != null ? recordId.getValue() : null;
        } catch (Exception e) {
            log.error("Failed to add message to stream: {}", streamKey, e);
            throw e;
        }
    }

    /**
     * 从 Stream 读取消息
     *
     * @param streamKey Stream 的键
     * @param startId   起始消息ID
     * @param count     要读取的消息数量
     * @return 消息列表，如果不存在返回空列表
     * @throws RedisException 当 Redis 操作失败时抛出
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> xRead(String streamKey, String startId, int count) {
        try {

            StreamOffset<String> offset = StreamOffset.from(StreamRecords.newRecord().in(streamKey).withId(startId).ofObject(null));
            StreamReadOptions options = StreamReadOptions.empty().count(count);

            List<MapRecord<String, Object, Object>> records = streamRedisTemplate.opsForStream()
                    .read(options, offset);

            if(records == null || records.isEmpty()) {
                return Collections.emptyList();
            }

            return records.stream()
                    .map(record -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", record.getId().getValue());
                        map.put("body", record.getValue());
                        return map;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to read messages from stream: {}", streamKey, e);
            throw new RedisException("Redis stream read operation failed", e);
        }
    }

    /**
     * 创建消费者组
     *
     * @param streamKey Stream 的键
     * @param groupName 消费者组名称
     * @param startId   起始消息ID (通常使用 "0-0" 从开始读取)
     * @return 创建成功返回 true，如果组已存在返回 false
     * @throws RedisException 当 Redis 操作失败时抛出
     */
    public boolean xGroupCreate(String streamKey, String groupName, String startId) {
        try {
            streamRedisTemplate.opsForStream().createGroup(streamKey, ReadOffset.from(startId), groupName);
            return true;
        } catch (RedisSystemException e) {
            if (e.getCause() instanceof RedisBusyException) {
                log.debug("Consumer group already exists: {}", groupName);
                return false;
            }
            log.error("Failed to create consumer group: {}", groupName, e);
            throw new RedisException("Redis stream group creation failed", e);
        } catch (Exception e) {
            log.error("Failed to create consumer group: {}", groupName, e);
            throw new RedisException("Redis stream group creation failed", e);
        }
    }

    /**
     * 从消费者组读取消息
     *
     * @param consumer    消费者信息
     * @param streamKey   Stream 的键
     * @param groupName   消费者组名称
     * @param count       要读取的消息数量
     * @param blockMillis 阻塞时间（毫秒）
     * @return 消息列表，如果无消息返回空列表
     * @throws RedisException 当 Redis 操作失败时抛出
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> xReadGroup(Consumer consumer, String streamKey,
                                                String groupName, int count, long blockMillis) {
        try {
            StreamOffset<String> offset = StreamOffset.create(streamKey, ReadOffset.lastConsumed());
            StreamReadOptions options = StreamReadOptions.empty()
                    .count(count)
                    .block(Duration.ofMillis(blockMillis));

            List<MapRecord<String, Object, Object>> records = streamRedisTemplate.opsForStream().read(consumer, options, offset);

            return records.stream()
                    .map(record -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", record.getId().getValue());
                        map.put("body", record.getValue());
                        return map;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to read messages from consumer group: {}", groupName, e);
            throw new RedisException("Redis stream group read operation failed", e);
        }
    }

    /**
     * 确认消息已被处理
     *
     * @param streamKey Stream 的键
     * @param groupName 消费者组名称
     * @param messageId 要确认的消息ID
     * @return 成功确认返回 true，失败返回 false
     * @throws RedisException 当 Redis 操作失败时抛出
     */
    public boolean xAck(String streamKey, String groupName, String messageId) {
        try {
            Long acked = streamRedisTemplate.opsForStream().acknowledge(streamKey, groupName, messageId);
            return acked != null && acked > 0;
        } catch (Exception e) {
            log.error("Failed to acknowledge message: {} in group: {}", messageId, groupName, e);
            throw new RedisException("Redis stream acknowledge operation failed", e);
        }
    }

    /**
     * 获取 Stream 信息
     *
     * @param streamKey Stream 的键
     * @return Stream 信息对象，如果 Stream 不存在返回 null
     * @throws RedisException 当 Redis 操作失败时抛出
     */
    public StreamInfo.XInfoStream xInfo(String streamKey) {
        try {
            return streamRedisTemplate.opsForStream().info(streamKey);
        } catch (Exception e) {
            log.error("Failed to get stream info: {}", streamKey, e);
            throw new RedisException("Redis stream info operation failed", e);
        }
    }

    /**
     * 删除消息
     *
     * @param streamKey  Stream 的键
     * @param messageIds 要删除的消息ID列表
     * @return 实际删除的消息数量
     * @throws RedisException 当 Redis 操作失败时抛出
     */
    public long xDel(String streamKey, String... messageIds) {
        try {
            Long deleted = streamRedisTemplate.opsForStream().delete(streamKey, messageIds);
            return deleted != null ? deleted : 0L;
        } catch (Exception e) {
            log.error("Failed to delete messages from stream: {}", streamKey, e);
            throw new RedisException("Redis stream delete operation failed", e);
        }
    }

    /**
     * 修剪 Stream，限制长度
     *
     * @param streamKey Stream 的键
     * @param maxLength 最大保留消息数量
     * @return 实际删除的消息数量
     * @throws RedisException 当 Redis 操作失败时抛出
     */
    public long xTrim(String streamKey, long maxLength) {
        try {
            Long trimmed = streamRedisTemplate.opsForStream().trim(streamKey, maxLength);
            return trimmed != null ? trimmed : 0L;
        } catch (Exception e) {
            log.error("Failed to trim stream: {}", streamKey, e);
            throw new RedisException("Redis stream trim operation failed", e);
        }
    }

    /**
     * 获取消费者组信息
     *
     * @param streamKey Stream 的键
     * @return 消费者组信息列表
     * @throws RedisException 当 Redis 操作失败时抛出
     */
    public StreamInfo.XInfoGroups xInfoGroups(String streamKey) {
        try {
            return streamRedisTemplate.opsForStream().groups(streamKey);
        } catch (Exception e) {
            log.error("Failed to get consumer groups for stream: {}", streamKey, e);
            throw new RedisException("Redis stream groups operation failed", e);
        }
    }

    /**
     * 获取指定消费者组的消费者信息
     *
     * @param streamKey Stream 的键
     * @param groupName 消费者组名称
     * @return 消费者信息列表
     * @throws RedisException 当 Redis 操作失败时抛出
     */
    public StreamInfo.XInfoConsumers xInfoConsumers(String streamKey, String groupName) {
        try {
            return streamRedisTemplate.opsForStream().consumers(streamKey, groupName);
        } catch (Exception e) {
            log.error("Failed to get consumers for group: {} in stream: {}", groupName, streamKey, e);
            throw new RedisException("Redis stream consumers operation failed", e);
        }
    }


}