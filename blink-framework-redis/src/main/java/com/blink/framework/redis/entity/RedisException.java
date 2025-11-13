package com.blink.framework.redis.entity;

/**
 * 自定义 Redis 异常类
 */
public class RedisException extends RuntimeException {

    public RedisException(String message) {
        super(message);
    }

    public RedisException(String message, Throwable cause) {
        super(message, cause);
    }
}
