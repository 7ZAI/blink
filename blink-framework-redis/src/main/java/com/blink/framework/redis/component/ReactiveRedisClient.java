package com.blink.framework.redis.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.ReactiveRedisCallback;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.RedisElementReader;
import org.springframework.data.redis.serializer.RedisElementWriter;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Reactive Redis 客户端封装类
 * 提供响应式 Redis 操作的统一封装，支持字符串、哈希、列表、集合、有序集合等数据结构操作
 *
 * @author binblink
 * @version 1.0
 */
@Slf4j
public class ReactiveRedisClient {


    private final ReactiveRedisTemplate<String, Object> template;

    /**
     * 构造函数
     *
     * @param redisTemplate ReactiveRedisTemplate 实例
     */
    public ReactiveRedisClient(ReactiveRedisTemplate<String, Object> redisTemplate) {
        this.template = redisTemplate;
    }

    /* ---------------------------- 通用操作 ---------------------------- */

    /**
     * 判断指定键是否存在
     *
     * @param key 键名
     * @return Mono<Boolean> true-存在，false-不存在
     */
    public Mono<Boolean> exists(String key) {
        return template.hasKey(key);
    }

    /**
     * 删除一个或多个键
     *
     * @param keys 要删除的键数组
     * @return Mono<Long> 实际删除的键数量
     */
    public Mono<Long> delete(String... keys) {
        return template.delete(keys);
    }

    /**
     * 删除单个键
     *
     * @param key 要删除的键名
     * @return Mono<Boolean> true-删除成功，false-键不存在
     */
    public Mono<Boolean> delete(String key) {
        return template.delete(key).map(count -> count > 0);
    }

    /**
     * 设置键的过期时间
     *
     * @param key     键名
     * @param timeout 过期时间
     * @return Mono<Boolean> true-设置成功，false-键不存在或设置失败
     */
    public Mono<Boolean> expire(String key, Duration timeout) {
        return template.expire(key, timeout);
    }

    /**
     * 获取键的剩余过期时间
     *
     * @param key 键名
     * @return Mono<Duration> 剩余过期时间，永不过期返回 Duration.ZERO，键不存在返回 Duration.ofMillis(-2)
     */
    public Mono<Duration> ttl(String key) {
        return template.getExpire(key);
    }

    /**
     * 移除键的过期时间，使其永久有效
     *
     * @param key 键名
     * @return Mono<Boolean> true-移除成功，false-键不存在或本来就是永久有效
     */
    public Mono<Boolean> persist(String key) {
        return template.persist(key);
    }

    /**
     * 获取匹配指定模式的所有键
     *
     * @param pattern 键的模式，支持通配符 * ? []
     * @return Flux<String> 匹配的键列表
     */
    public Flux<String> keys(String pattern) {
        return template.keys(pattern);
    }

    /**
     * 扫描匹配指定模式的所有键（推荐在生产环境使用，避免阻塞）
     *
     * @param pattern 键的模式，支持通配符 * ? []
     * @return Flux<String> 匹配的键列表
     */
    public Flux<String> scan(String pattern) {
        return template.scan(ScanOptions.scanOptions().match(pattern).build());
    }

    /**
     * 获取键的数据类型
     *
     * @param key 键名
     * @return Mono<String> 数据类型名称，如 "STRING", "HASH", "LIST" 等
     */
    public Mono<String> type(String key) {
        return template.type(key).map(Enum::name);
    }

    /* ---------------------------- String 操作 ---------------------------- */

    /**
     * 设置键值对
     *
     * @param key   键名
     * @param value 值
     * @return Mono<Boolean> true-设置成功
     */
    public Mono<Boolean> set(String key, Object value) {
        return template.opsForValue().set(key, value);
    }

    /**
     * 设置带过期时间的键值对
     *
     * @param key     键名
     * @param value   值
     * @param timeout 过期时间
     * @return Mono<Boolean> true-设置成功
     */
    public Mono<Boolean> setEx(String key, Object value, Duration timeout) {
        return template.opsForValue().set(key, value, timeout);
    }

    /**
     * 仅当键不存在时设置值
     *
     * @param key   键名
     * @param value 值
     * @return Mono<Boolean> true-设置成功，false-键已存在
     */
    public Mono<Boolean> setIfAbsent(String key, Object value) {
        return template.opsForValue().setIfAbsent(key, value);
    }

    /**
     * 仅当键不存在时设置带过期时间的值（原子操作）
     *
     * @param key     键名
     * @param value   值
     * @param timeout 过期时间
     * @return Mono<Boolean> true-设置成功，false-键已存在
     */
    public Mono<Boolean> setIfAbsentWithExpire(String key, Object value, Duration timeout) {
        return template.opsForValue().setIfAbsent(key, value, timeout);
    }

    /**
     * 仅当键存在时设置值
     *
     * @param key   键名
     * @param value 值
     * @return Mono<Boolean> true-设置成功，false-键不存在
     */
    public Mono<Boolean> setIfPresent(String key, Object value) {
        return template.opsForValue().setIfPresent(key, value);
    }

    /**
     * 获取指定键的值
     *
     * @param key 键名
     * @return Mono<Object> 键对应的值，键不存在返回 Mono.empty()
     */
    public Mono<Object> get(String key) {
        return template.opsForValue().get(key);
    }

    /**
     * 获取并设置新值
     *
     * @param key   键名
     * @param value 新值
     * @return Mono<Object> 旧值，键不存在返回 Mono.empty()
     */
    public Mono<Object> getAndSet(String key, Object value) {
        return template.opsForValue().getAndSet(key, value);
    }

    /**
     * 对整数值进行递增操作
     *
     * @param key 键名
     * @return Mono<Long> 递增后的值
     * @throws org.springframework.data.redis.RedisSystemException 如果值不是整数类型
     */
    public Mono<Long> increment(String key) {
        return template.opsForValue().increment(key);
    }

    /**
     * 对整数值递增指定步长
     *
     * @param key   键名
     * @param delta 递增步长
     * @return Mono<Long> 递增后的值
     * @throws org.springframework.data.redis.RedisSystemException 如果值不是整数类型
     */
    public Mono<Long> incrementBy(String key, long delta) {
        return template.opsForValue().increment(key, delta);
    }

    /**
     * 对整数值进行递减操作
     *
     * @param key 键名
     * @return Mono<Long> 递减后的值
     * @throws org.springframework.data.redis.RedisSystemException 如果值不是整数类型
     */
    public Mono<Long> decrement(String key) {
        return template.opsForValue().decrement(key);
    }

    /**
     * 对整数值递减指定步长
     *
     * @param key   键名
     * @param delta 递减步长
     * @return Mono<Long> 递减后的值
     * @throws org.springframework.data.redis.RedisSystemException 如果值不是整数类型
     */
    public Mono<Long> decrementBy(String key, long delta) {
        return template.opsForValue().decrement(key, delta);
    }

    /**
     * 获取字符串值的长度
     *
     * @param key 键名
     * @return Mono<Long> 字符串长度，键不存在返回 0
     */
    public Mono<Long> strLen(String key) {
        return template.opsForValue().size(key);
    }

    /**
     * 删除一个或多个键（兼容旧方法）
     *
     * @param keys 要删除的键数组
     * @return Mono<Boolean> true-删除成功
     * @deprecated 建议使用 {@link #delete(String...)} 方法
     */
    @Deprecated
    public Mono<Boolean> del(String... keys) {
        return template.delete(keys).thenReturn(true);
    }

    /* ---------------------------- Hash 操作 ---------------------------- */

    /**
     * 设置哈希字段的值
     *
     * @param key   哈希键名
     * @param field 字段名
     * @param value 字段值
     * @return Mono<Boolean> true-字段是新建的，false-字段已存在且值被更新
     */
    public Mono<Boolean> hPut(String key, String field, Object value) {
        return template.opsForHash().put(key, field, value);
    }

    /**
     * 批量设置哈希字段
     *
     * @param key         哈希键名
     * @param fieldValues 字段-值映射表
     * @return Mono<Boolean> true-设置成功
     */
    public Mono<Boolean> hPutAll(String key, Map<String, Object> fieldValues) {
        return template.opsForHash().putAll(key, fieldValues);
    }

    /**
     * 获取哈希字段的值
     *
     * @param key   哈希键名
     * @param field 字段名
     * @return Mono<Object> 字段值，字段不存在返回 Mono.empty()
     */
    public Mono<Object> hGet(String key, String field) {
        return template.opsForHash().get(key, field);
    }

    /**
     * 批量获取哈希字段的值
     *
     * @param key    哈希键名
     * @param fields 字段名列表
     * @return Mono<List < Object>> 字段值列表，不存在的字段对应位置为 null
     */
    public Mono<List<Object>> hMultiGet(String key, List<String> fields) {
        List<Object> objs = fields.stream().map(s -> (Object) s).toList();
        return template.opsForHash().multiGet(key, objs);
    }

    /**
     * 获取哈希的所有字段和值
     *
     * @param key 哈希键名
     * @return Flux<Map.Entry < Object, Object>> 字段-值对的流
     */
    public Flux<Map.Entry<Object, Object>> hEntries(String key) {
        return template.opsForHash().entries(key);
    }

    /**
     * 获取哈希的所有字段名
     *
     * @param key 哈希键名
     * @return Flux<Object> 字段名流
     */
    public Flux<Object> hKeys(String key) {
        return template.opsForHash().keys(key);
    }

    /**
     * 获取哈希的所有字段值
     *
     * @param key 哈希键名
     * @return Flux<Object> 字段值流
     */
    public Flux<Object> hValues(String key) {
        return template.opsForHash().values(key);
    }

    /**
     * 删除哈希中的一个或多个字段
     *
     * @param key    哈希键名
     * @param fields 要删除的字段名
     * @return Mono<Long> 实际删除的字段数量
     */
    public Mono<Long> hDelete(String key, String... fields) {
        return template.opsForHash().remove(key, (Object[]) fields);
    }

    /**
     * 判断哈希中字段是否存在
     *
     * @param key   哈希键名
     * @param field 字段名
     * @return Mono<Boolean> true-字段存在，false-字段不存在
     */
    public Mono<Boolean> hExists(String key, String field) {
        return template.opsForHash().hasKey(key, field);
    }

    /**
     * 获取哈希中字段的数量
     *
     * @param key 哈希键名
     * @return Mono<Long> 字段数量
     */
    public Mono<Long> hSize(String key) {
        return template.opsForHash().size(key);
    }

    /**
     * 对哈希中的整数字段进行递增操作
     *
     * @param key   哈希键名
     * @param field 字段名
     * @param delta 递增步长
     * @return Mono<Long> 递增后的值
     * @throws org.springframework.data.redis.RedisSystemException 如果字段值不是整数类型
     */
    public Mono<Long> hIncrement(String key, String field, long delta) {
        return template.opsForHash().increment(key, field, delta);
    }

    /* ---------------------------- List 操作 ---------------------------- */

    /**
     * 从列表左侧推入一个或多个值
     *
     * @param key    列表键名
     * @param values 要推入的值数组
     * @return Mono<Long> 推入后列表的长度
     */
    public Mono<Long> lPush(String key, Object... values) {
        return template.opsForList().leftPushAll(key, values);
    }

    /**
     * 从列表右侧推入一个或多个值
     *
     * @param key    列表键名
     * @param values 要推入的值数组
     * @return Mono<Long> 推入后列表的长度
     */
    public Mono<Long> rPush(String key, Object... values) {
        return template.opsForList().rightPushAll(key, values);
    }

    /**
     * 从列表左侧弹出一个值
     *
     * @param key 列表键名
     * @return Mono<Object> 弹出的值，列表为空返回 Mono.empty()
     */
    public Mono<Object> lPop(String key) {
        return template.opsForList().leftPop(key);
    }

    /**
     * 从列表右侧弹出一个值
     *
     * @param key 列表键名
     * @return Mono<Object> 弹出的值，列表为空返回 Mono.empty()
     */
    public Mono<Object> rPop(String key) {
        return template.opsForList().rightPop(key);
    }

    /**
     * 获取列表指定范围内的元素
     *
     * @param key   列表键名
     * @param start 起始索引（包含）
     * @param end   结束索引（包含）
     * @return Flux<Object> 元素流
     */
    public Flux<Object> lRange(String key, long start, long end) {
        return template.opsForList().range(key, start, end);
    }

    /**
     * 获取列表长度
     *
     * @param key 列表键名
     * @return Mono<Long> 列表长度
     */
    public Mono<Long> lLen(String key) {
        return template.opsForList().size(key);
    }

    /**
     * 根据索引获取列表元素
     *
     * @param key   列表键名
     * @param index 元素索引
     * @return Mono<Object> 指定索引的元素，索引越界返回 Mono.empty()
     */
    public Mono<Object> lIndex(String key, long index) {
        return template.opsForList().index(key, index);
    }

    /**
     * 设置列表指定索引的元素值
     *
     * @param key   列表键名
     * @param index 元素索引
     * @param value 新值
     * @return Mono<Boolean> true-设置成功，false-索引越界
     */
    public Mono<Boolean> lSet(String key, long index, Object value) {
        return template.opsForList().set(key, index, value);
    }

    /* ---------------------------- Set 操作 ---------------------------- */

    /**
     * 向集合中添加一个或多个成员
     *
     * @param key    集合键名
     * @param values 要添加的成员数组
     * @return Mono<Long> 实际添加的成员数量（已存在的成员不会被重复添加）
     */
    public Mono<Long> sAdd(String key, Object... values) {
        return template.opsForSet().add(key, values);
    }

    /**
     * 获取集合中的所有成员
     *
     * @param key 集合键名
     * @return Flux<Object> 成员流
     */
    public Flux<Object> sMembers(String key) {
        return template.opsForSet().members(key);
    }

    /**
     * 判断指定值是否是集合的成员
     *
     * @param key   集合键名
     * @param value 要判断的值
     * @return Mono<Boolean> true-是成员，false-不是成员
     */
    public Mono<Boolean> sIsMember(String key, Object value) {
        return template.opsForSet().isMember(key, value);
    }

    /**
     * 获取集合的成员数量
     *
     * @param key 集合键名
     * @return Mono<Long> 成员数量
     */
    public Mono<Long> sSize(String key) {
        return template.opsForSet().size(key);
    }

    /**
     * 从集合中移除一个或多个成员
     *
     * @param key    集合键名
     * @param values 要移除的成员数组
     * @return Mono<Long> 实际移除的成员数量
     */
    public Mono<Long> sRemove(String key, Object... values) {
        return template.opsForSet().remove(key, values);
    }

    /**
     * 随机从集合中弹出一个成员
     *
     * @param key 集合键名
     * @return Mono<Object> 弹出的成员，集合为空返回 Mono.empty()
     */
    public Mono<Object> sPop(String key) {
        return template.opsForSet().pop(key);
    }

    /* ---------------------------- ZSet 操作 ---------------------------- */

    /**
     * 向有序集合添加一个成员
     *
     * @param key   有序集合键名
     * @param value 成员值
     * @param score 成员分数
     * @return Mono<Boolean> true-添加成功，false-成员已存在且分数被更新
     */
    public Mono<Boolean> zAdd(String key, Object value, double score) {
        return template.opsForZSet().add(key, value, score);
    }

    /**
     * 批量向有序集合添加成员
     *
     * @param key         有序集合键名
     * @param valueScores 成员-分数映射表
     * @return Mono<Long> 实际添加的成员数量
     */
    public Mono<Long> zAdd(String key, Map<Object, Double> valueScores) {
        return template.opsForZSet().addAll(key, valueScores.entrySet().stream()
                .map(entry -> new org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object>() {
                    @Override
                    public Object getValue() {
                        return entry.getKey();
                    }

                    @Override
                    public Double getScore() {
                        return entry.getValue();
                    }

                    @Override
                    public int compareTo(org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object> o) {
                        return Double.compare(getScore(), o.getScore());
                    }
                }).toList());
    }

    /**
     * 获取有序集合指定索引范围的成员（按分数升序）
     *
     * @param key   有序集合键名
     * @param start 起始索引（包含）
     * @param end   结束索引（包含）
     * @return Flux<Object> 成员流
     */
    public Flux<Object> zRange(String key, long start, long end) {
        return template.opsForZSet().range(key, Range.of(Range.Bound.inclusive(start), Range.Bound.inclusive(end)));
    }

    /**
     * 获取有序集合指定分数范围的成员（按分数升序）
     *
     * @param key 有序集合键名
     * @param min 最小分数（包含）
     * @param max 最大分数（包含）
     * @return Flux<Object> 成员流
     */
    public Flux<Object> zRangeByScore(String key, double min, double max) {
        return template.opsForZSet().rangeByScore(key, Range.of(Range.Bound.inclusive(min), Range.Bound.inclusive(max)));
    }

    /**
     * 获取有序集合中指定成员的分数
     *
     * @param key   有序集合键名
     * @param value 成员值
     * @return Mono<Double> 成员分数，成员不存在返回 Mono.empty()
     */
    public Mono<Double> zScore(String key, Object value) {
        return template.opsForZSet().score(key, value);
    }

    /**
     * 获取有序集合的成员数量
     *
     * @param key 有序集合键名
     * @return Mono<Long> 成员数量
     */
    public Mono<Long> zSize(String key) {
        return template.opsForZSet().size(key);
    }

    /**
     * 获取有序集合中指定成员的排名（按分数升序，0表示第一名）
     *
     * @param key   有序集合键名
     * @param value 成员值
     * @return Mono<Long> 成员排名，成员不存在返回 Mono.empty()
     */
    public Mono<Long> zRank(String key, Object value) {
        return template.opsForZSet().rank(key, value);
    }

    /**
     * 从有序集合中移除一个或多个成员
     *
     * @param key    有序集合键名
     * @param values 要移除的成员数组
     * @return Mono<Long> 实际移除的成员数量
     */
    public Mono<Long> zRemove(String key, Object... values) {
        return template.opsForZSet().remove(key, values);
    }

    /* ---------------------------- 发布订阅操作 ---------------------------- */

    /**
     * 向指定频道发布消息
     *
     * @param channel 频道名称
     * @param message 消息内容
     * @return Mono<Long> 接收到消息的订阅者数量
     */
    public Mono<Long> publish(String channel, Object message) {
        return template.convertAndSend(channel, message);
    }

    /* ---------------------------- Lua 脚本执行 ---------------------------- */

    /**
     * 执行 Lua 脚本字符串
     *
     * @param <T>        返回结果类型
     * @param scriptStr  Lua 脚本字符串
     * @param keys       脚本中使用的键列表
     * @param clazz      返回结果类型 Class
     * @param serializer 结果序列化器
     * @param vals       脚本参数列表
     * @return Flux<T> 脚本执行结果流
     */
    public <T> Flux<T> execute(String scriptStr, List<String> keys, Class<T> clazz,
                               RedisSerializer<T> serializer, List<String> vals) {
        RedisElementWriter<String> writer = RedisElementWriter.from(new StringRedisSerializer());
        RedisElementReader<T> reader = RedisElementReader.from(serializer);
        return template.execute(RedisScript.of(scriptStr, clazz), keys, vals, writer, reader);
    }

    /**
     * 执行 RedisScript 对象
     *
     * @param <T>    返回结果类型
     * @param script RedisScript 对象
     * @param keys   脚本中使用的键列表
     * @param args   脚本参数列表
     * @return Flux<T> 脚本执行结果流
     */
    public <T> Flux<T> execute(RedisScript<T> script, List<String> keys, List<Object> args) {
        return template.execute(script, keys, args);
    }

    /**
     * 执行 Lua 脚本并返回单个结果
     *
     * @param <T>    返回结果类型
     * @param script RedisScript 对象
     * @param keys   脚本中使用的键列表
     * @param args   脚本参数列表
     * @return Mono<T> 脚本执行结果
     */
    public <T> Mono<T> executeForMono(RedisScript<T> script, List<String> keys, List<Object> args) {
        return template.execute(script, keys, args).next();
    }

    /* ---------------------------- 管道操作 ---------------------------- */

    /**
     * 开启管道执行批量操作
     *
     * @param callback ReactiveRedisCallback 回调接口
     * @return Flux<Object> 操作结果流
     */
    public Flux<Object> executePipelined(ReactiveRedisCallback callback) {
        return template.execute(callback);
    }

    /* ---------------------------- 工具方法 ---------------------------- */

    /**
     * 获取原始 ReactiveRedisTemplate 实例
     *
     * @return ReactiveRedisTemplate<String, Object> 原始模板实例
     */
    public ReactiveRedisTemplate<String, Object> getTemplate() {
        return template;
    }

    /**
     * 安全获取操作，如果键不存在返回默认值
     *
     * @param key          键名
     * @param defaultValue 默认值
     * @return Mono<Object> 键值或默认值
     */
    public Mono<Object> getOrDefault(String key, Object defaultValue) {
        return get(key).defaultIfEmpty(defaultValue);
    }

    /**
     * 带重试机制的设置操作
     *
     * @param key        键名
     * @param value      值
     * @param maxRetries 最大重试次数
     * @return Mono<Boolean> true-设置成功，false-设置失败（包括重试后仍然失败）
     */
    public Mono<Boolean> setWithRetry(String key, Object value, int maxRetries) {
        return set(key, value)
                .retry(maxRetries)
                .onErrorReturn(false);
    }





    /* ---------------------------- Stream 基本操作 ---------------------------- */

    /**
     * 向 Stream 添加消息
     *
     * @param key   Stream 键名
     * @param field 字段名
     * @param value 字段值
     * @return Mono<String> 消息 ID
     */
    public Mono<String> xAdd(String key, String field, Object value) {
        MapRecord<String, String, Object> record =  StreamRecords.newRecord()
                .in(key)
                .ofMap(Collections.singletonMap(field, value));

        return template.opsForStream().add(record).map(RecordId::getValue);
    }

    /**
     * 向 Stream 添加包含多个字段的消息
     *
     * @param key  Stream 键名
     * @param body 消息体（字段-值映射）
     * @return Mono<String> 消息 ID
     */
    public Mono<String> xAdd(String key, Map<String, Object> body) {
        MapRecord<String, String, Object> record =  StreamRecords.newRecord()
                .in(key)
                .ofMap(body);
        return template.opsForStream().add(record).map(RecordId::getValue);
    }

    /**
     * 向 Stream 添加消息，指定最大长度
     *
     * @param key       Stream 键名
     * @param body      消息体
     * @param maxLength 最大长度（近似修剪）
     * @return Mono<String> 消息 ID
     */
//    public Mono<String> xAdd(String key, Map<String, Object> body, long maxLength) {
//        MapRecord<String, String, Object> record = StreamRecords.newRecord()
//                .in(key)
//                .ofMap(body);
//        template.opsForStream().add(record)
//        return template.opsForStream()
//                .add(record, StreamAddOptions.empty().maxlen(maxLength).approximateTrimming(true))
//                .map(RecordId::getValue);
//    }

    /**
     * 获取 Stream 长度
     *
     * @param key Stream 键名
     * @return Mono<Long> Stream 中的消息数量
     */
    public Mono<Long> xLen(String key) {
        return template.opsForStream().size(key);
    }

    /**
     * 删除 Stream 中的消息
     *
     * @param key        Stream 键名
     * @param messageIds 要删除的消息 ID 列表
     * @return Mono<Long> 实际删除的消息数量
     */
    public Mono<Long> xDel(String key, String... messageIds) {
        return template.opsForStream().delete(key, messageIds);
    }

    /* ---------------------------- Stream 范围查询 ---------------------------- */

    /**
     * 读取 Stream 中的消息（范围查询）
     *
     * @param key   Stream 键名
     * @param start 起始 ID（包含）
     * @param end   结束 ID（包含）
     * @return Flux<MapRecord < String, Object, Object>> 消息记录流
     */
    public Flux<MapRecord<String, Object, Object>> xRange(String key, String start, String end) {
        return template.opsForStream().range(key, Range.closed(start, end));

    }

    /**
     * 读取 Stream 中的消息（范围查询，限制数量）
     *
     * @param key   Stream 键名
     * @param start 起始 ID（包含）
     * @param end   结束 ID（包含）
     * @param count 最大返回数量
     * @return Flux<MapRecord < String, String, Object>> 消息记录流
     */
    public Flux<MapRecord<String, Object, Object>> xRange(String key, String start, String end, int count) {
        return template.opsForStream().range(key, Range.closed(start, end), Limit.limit().count(count));
    }

    /**
     * 反向读取 Stream 中的消息（范围查询）
     *
     * @param key   Stream 键名
     * @param start 起始 ID（包含）
     * @param end   结束 ID（包含）
     * @return Flux<MapRecord < String, String, Object>> 消息记录流（按 ID 降序）
     */
    public Flux<MapRecord<String, Object, Object>> xRevRange(String key, String start, String end) {
        return template.opsForStream().reverseRange(key, Range.closed(start, end));
    }

    /**
     * 反向读取 Stream 中的消息（范围查询，限制数量）
     *
     * @param key   Stream 键名
     * @param start 起始 ID（包含）
     * @param end   结束 ID（包含）
     * @param count 最大返回数量
     * @return Flux<MapRecord < String, String, Object>> 消息记录流（按 ID 降序）
     */
    public Flux<MapRecord<String, Object, Object>> xRevRange(String key, String start, String end, int count) {
        return template.opsForStream().reverseRange(key, Range.closed(start, end), Limit.limit().count(count));
    }

    /* ---------------------------- Stream 消费者组操作 ---------------------------- */

    /**
     * 创建消费者组
     *
     * @param key       Stream 键名
     * @param groupName 消费者组名称
     * @param startId   起始读取的 ID（"0" 从开始，"$" 从最新）
     * @return Mono<String> 操作结果
     */
    public Mono<String> xGroupCreate(String key, String groupName, String startId) {

        return template.opsForStream().createGroup(key, ReadOffset.from(startId), groupName);
    }

    /**
     * 删除消费者组
     *
     * @param key       Stream 键名
     * @param groupName 消费者组名称
     * @return Mono<String> true-删除成功
     */
    public Mono<String> xGroupDestroy(String key, String groupName) {
        return template.opsForStream().destroyGroup(key, groupName);
    }

    /**
     * 从消费者组读取消息
     *
     * @param consumer     消费者信息
     * @param streamOffset Stream 偏移量配置
     * @return Flux<MapRecord < String, String, Object>> 消息记录流
     */
    public Flux<MapRecord<String, Object, Object>> xReadGroup(Consumer consumer, StreamOffset<String> streamOffset) {
        return template.opsForStream().read(consumer, streamOffset);
    }


    /**
     * 从消费者组读取消息（带数量限制）
     *
     * @param consumer     消费者信息
     * @param streamOffset Stream 偏移量配置
     * @param count        最大读取数量
     * @return Flux<MapRecord < String, String, Object>> 消息记录流
     */
    public Flux<MapRecord<String, Object, Object>> xReadGroup(Consumer consumer, StreamOffset<String> streamOffset, long count) {
        return template.opsForStream().read(consumer, StreamReadOptions.empty().count(count), streamOffset);
    }

    /**
     * 确认消息已被处理
     *
     * @param key        Stream 键名
     * @param groupName  消费者组名称
     * @param messageIds 要确认的消息 ID 列表
     * @return Mono<Long> 实际确认的消息数量
     */
    public Mono<Long> xAck(String key, String groupName, String... messageIds) {
        return template.opsForStream().acknowledge(key, groupName, messageIds);
    }

    /**
     * 查看待处理消息
     *
     * @param key       Stream 键名
     * @param groupName 消费者组名称
     * @return Flux<PendingMessages> 待处理消息信息流
     */
    public Mono<PendingMessagesSummary> xPending(String key, String groupName) {
        return template.opsForStream().pending(key, groupName);
    }

    /**
     * 查看特定消费者的待处理消息
     *
     * @param key       Stream 键名
     * @param groupName 消费者组名称
     * @param consumer  消费者名称
     * @return Flux<PendingMessages> 待处理消息信息流
     */
    public Mono<PendingMessages> xPending(String key, String groupName, String consumer) {
        return template.opsForStream().pending(key, Consumer.from(groupName, consumer));
    }

    /* ---------------------------- Stream 修剪操作 ---------------------------- */

    /**
     * 修剪 Stream，限制最大长度
     *
     * @param key       Stream 键名
     * @param maxLength 最大保留消息数量
     * @return Mono<Long> 实际删除的消息数量
     */
    public Mono<Long> xTrim(String key, long maxLength) {
        return template.opsForStream().trim(key, maxLength);
    }

    /**
     * 修剪 Stream（精确修剪）
     *
     * @param key       Stream 键名
     * @param maxLength 最大保留消息数量
     * @param exactTrim 是否精确修剪
     * @return Mono<Long> 实际删除的消息数量
     */
    public Mono<Long> xTrim(String key, long maxLength, boolean exactTrim) {
        return template.opsForStream().trim(key, maxLength, exactTrim);
    }

    /* ---------------------------- Stream 信息查询 ---------------------------- */

    /**
     * 获取 Stream 信息
     *
     * @param key Stream 键名
     * @return Mono<StreamInfo.XInfoStream> Stream 信息
     */
    public Mono<StreamInfo.XInfoStream> xInfo(String key) {
        return template.opsForStream().info(key);
    }

    /**
     * 获取消费者组信息
     *
     * @param key Stream 键名
     * @return Flux<StreamInfo.XInfoGroup> 消费者组信息流
     */
    public Flux<StreamInfo.XInfoGroup> xInfoGroups(String key) {
        return template.opsForStream().groups(key);
    }

    /**
     * 获取消费者信息
     *
     * @param key       Stream 键名
     * @param groupName 消费者组名称
     * @return Flux<StreamInfo.XInfoConsumer> 消费者信息流
     */
    public Flux<StreamInfo.XInfoConsumer> xInfoConsumers(String key, String groupName) {
        return template.opsForStream().consumers(key, groupName);
    }


    /* ---------------------------- 工具方法 ---------------------------- */

    /**
     * 创建消费者对象
     *
     * @param groupName    消费者组名称
     * @param consumerName 消费者名称
     * @return Consumer 消费者对象
     */
    public Consumer createConsumer(String groupName, String consumerName) {
        return Consumer.from(groupName, consumerName);
    }

    /**
     * 创建 Stream 偏移量配置
     *
     * @param key    Stream 键名
     * @param offset 偏移量（"0" 从开始，"$" 从最新，">" 从消费者组中未消费的消息开始）
     * @return StreamOffset<String> Stream 偏移量配置
     */
    public StreamOffset<String> createStreamOffset(String key, String offset) {
        return StreamOffset.create(key, ReadOffset.from(offset));
    }

    /**
     * 创建 Stream 偏移量配置（从最新消息开始）
     *
     * @param key Stream 键名
     * @return StreamOffset<String> Stream 偏移量配置
     */
    public StreamOffset<String> createStreamOffsetFromLatest(String key) {
        return StreamOffset.create(key, ReadOffset.latest());
    }

    /**
     * 创建 Stream 偏移量配置（从最早消息开始）
     *
     * @param key Stream 键名
     * @return StreamOffset<String> Stream 偏移量配置
     */
    public StreamOffset<String> createStreamOffsetFromEarliest(String key) {
        return StreamOffset.create(key, ReadOffset.from("0"));
    }

    /* ---------------------------- 高级操作 ---------------------------- */

    /**
     * 批量添加消息到 Stream
     *
     * @param key      Stream 键名
     * @param messages 消息列表（每个消息是一个字段-值映射）
     * @return Flux<String> 消息 ID 流
     */
    public Flux<String> xAddBatch(String key, List<Map<String, Object>> messages) {
        return Flux.fromIterable(messages)
                .flatMap(message -> xAdd(key, message));
    }

    /**
     * 读取并确认消息（自动确认模式）
     *
     * @param consumer     消费者信息
     * @param streamOffset Stream 偏移量配置
     * @param groupName    消费者组名称
     * @return Flux<MapRecord < String, String, Object>> 消息记录流（读取后自动确认）
     */
    public Flux<MapRecord<String, Object, Object>> xReadGroupAndAck(Consumer consumer,
                                                                    StreamOffset<String> streamOffset,
                                                                    String groupName) {
        return xReadGroup(consumer, streamOffset)
                .flatMap(record ->
                        xAck(streamOffset.getKey(), groupName, record.getId().getValue())
                                .thenReturn(record)
                );
    }

    /**
     * 转移消息所有权（认领超时消息）
     *
     * @param key         Stream 键名
     * @param groupName   消费者组名称
     * @param consumer    目标消费者
     * @param minIdleTime 最小空闲时间（毫秒）
     * @param messageIds  要转移的消息 ID 列表
     * @return Flux<MapRecord < String, String, Object>> 成功转移的消息记录流
     */
    public Flux<MapRecord<String, Object, Object>> xClaim(String key,
                                                          String groupName,
                                                          Consumer consumer,
                                                          long minIdleTime,
                                                          String... messageIds) {
        RecordId[] recordIds = new RecordId[messageIds.length];
        for (int i = 0; i < messageIds.length; i++) {
            recordIds[i] = RecordId.of(messageIds[i]);
        }

        return template.opsForStream().claim(key, groupName, consumer.getGroup(), Duration.ofMillis(minIdleTime), recordIds);

    }

}