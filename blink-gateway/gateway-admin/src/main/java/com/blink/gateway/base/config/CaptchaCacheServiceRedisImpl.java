package com.blink.gateway.base.config;

import com.anji.captcha.service.CaptchaCacheService;
import com.blink.framework.redis.component.RedisClient;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 验证码 Redis 缓存服务
 * <p>
 * 用于在微服务多实例环境下共享验证码数据
 * </p>
 *
 * @author binblink
 */
@Service
public class CaptchaCacheServiceRedisImpl implements CaptchaCacheService {

    @Resource
    private RedisClient redisClient;

    private static final String CAPTCHA_KEY_PREFIX = "captcha:";

    @Override
    public void set(String key, String value, long expiresInSeconds) {
        String redisKey = CAPTCHA_KEY_PREFIX + key;
        redisClient.setEx(redisKey, value, expiresInSeconds);
    }

    @Override
    public boolean exists(String key) {
        String redisKey = CAPTCHA_KEY_PREFIX + key;
        return redisClient.exists(redisKey);
    }

    @Override
    public void delete(String key) {
        String redisKey = CAPTCHA_KEY_PREFIX + key;
        redisClient.delete(redisKey);
    }

    @Override
    public String get(String key) {
        String redisKey = CAPTCHA_KEY_PREFIX + key;
        Object value = redisClient.get(redisKey);
        return value != null ? value.toString() : null;
    }

    @Override
    public Long increment(String key, long val) {
        String redisKey = CAPTCHA_KEY_PREFIX + key;
        return redisClient.incrementBy(redisKey, val);
    }

    /**
     * 返回缓存类型标识
     * 必须返回 "redis" 才能被 anji-captcha 识别使用
     */
    @Override
    public String type() {
        return "redis";
    }
}
