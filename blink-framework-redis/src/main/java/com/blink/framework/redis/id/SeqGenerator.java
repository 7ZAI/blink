package com.blink.framework.redis.id;

import cn.hutool.core.util.StrUtil;
import com.blink.framework.redis.component.RedisClient;
import com.blink.framework.redis.serializer.LongRedisSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;


/**
 * 顺序号生成器
 *
 * @author binblink
 */
@Slf4j
public class SeqGenerator {


    /**
     * 本地缓存
     */
    private static final Map<String, IdCache> ID_STORE_MAP = new ConcurrentHashMap<>();

    /**
     * 缓存锁 key 与 锁 对应
     */
    private static final Map<String, ReentrantLock> LOCK_MAP = new ConcurrentHashMap<>();


    private final RedisClient redisClient;

    private final BlinkRedisProperties properties;

    public SeqGenerator(RedisClient redisClient, BlinkRedisProperties properties) {
        this.redisClient = redisClient;
        this.properties = properties;
    }

    /**
     * 生成序列号
     *
     * @param key  redis中的key值
     * @param maxValue 最大值
     * @return 数字
     */
    public Long generateId(String key, Long maxValue) {

        if (StrUtil.isBlank(key)) {
            throw new RuntimeException("key 不能为空");
        }

        Integer delta = this.properties.getIdGenerator().getKeySteps(key);

        //delta 为1 表示 不需要本地缓存 每次都访问redis获取 不建议
        if (delta == 1) {
            return getIncrSeqFromRedis(key, maxValue, delta);
        }

        //delta 大于1 表示 从本地缓存中获取值
        return getIncrSeqFromLocalCache(key, maxValue, delta);
    }

    private Long getIncrSeqFromLocalCache(String key, Long maxValue, Integer delta) {
        //不同key对应不同的锁对象 这个锁只有在同key 第一次创建缓存或者缓存序列用完才会锁
        ReentrantLock lock = LOCK_MAP.get(key);
        // 第一次创建时 防止同个key创建两次
        if (Objects.isNull(lock)) {
            synchronized (this) {
                lock = LOCK_MAP.get(key);
                if (Objects.isNull(lock)) {
                    lock = new ReentrantLock();
                    LOCK_MAP.put(key, lock);
                }
            }
        }

        IdCache idCache = ID_STORE_MAP.get(key);
        Long value = null;
        if (Objects.isNull(idCache)) {
            //相同key情况下 只有需要去redis获取时才要保证只有一个线程去获取，否则后续线程会覆盖掉前一个获取没使用的id段
            lock.lock();
            try {
                //二次判断 防止竞争线程 进入 因为另一个竞争成功获得执行权的线程已经创建新的缓存idStore实例
                if (Objects.isNull(ID_STORE_MAP.get(key))) {

                    Long localMaxSeq = getIncrSeqFromRedis(key, maxValue, delta);
                    Long currentId = localMaxSeq - delta + 1;
                    idCache = new IdCache(currentId, localMaxSeq, maxValue);
                    ID_STORE_MAP.put(key, idCache);
                    return currentId;
                } else {
                    idCache = ID_STORE_MAP.get(key);
                }
            } finally {
                lock.unlock();
            }
        }

        value = idCache.nextValue();

        // 超过了最大值 本地段号用完了
        if (value < 0) {
            //只有需要去redis获取时才要保证只有一个线程去获取，否则后续线程会覆盖掉前一个获取没使用的id
            lock.lock();
            try {
                //二次判断 防止竞争线程 进入 因为另一个竞争成功获得执行权的线程已经创建新的缓存idStore实例
                value = idCache.nextValue();
                if (value < 0) {
                    Long localMaxSeq = getIncrSeqFromRedis(key, maxValue, delta);
                    Long currentId = localMaxSeq - delta + 1;
                    idCache = new IdCache(currentId, localMaxSeq, maxValue);
                    value = currentId;
                    ID_STORE_MAP.put(key, idCache);
                }
                if (log.isDebugEnabled()) {
                    log.debug("acquire id \"{}\" with key \"{}\".", value, key);
                }
            } finally {
                lock.unlock();
            }
        }

        return value;
    }

    /**
     * 从 redis获取自增数字
     * @param key key
     * @param maxValue 最大值
     * @param step 步进值 即增量值 本地缓存数量
     * @return
     */
    private Long getIncrSeqFromRedis(String key, Long maxValue, Integer step) {

        log.info("使用Redis lua脚本生成序列号 with key: \"{}\" maxValue: \"{}\" step: \"{}\".", key, maxValue, step);

        List<String> keys = new ArrayList<>();
        keys.add(key);

        RedisScript<Long> redisScript = RedisScript.of(this.properties.getIdGenerator().getLuaScript(), Long.class);
        Long seq = redisClient.execute(redisScript, new LongRedisSerializer(), keys, String.valueOf(maxValue), String.valueOf(step));

        log.info("Redis生成的序列号 起始值 seq: \"{}\" ", seq);
        return seq;
    }

}
