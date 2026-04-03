package com.blink.framework.redis.aop;

import com.blink.framework.common.exception.BlinkErrorCodeEnum;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.factory.BlinkNamedThreadFactory;
import com.blink.framework.redis.annotation.CacheDoubleDelete;
import com.blink.framework.redis.component.RedisClient;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;


import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 缓存删除 保证数据一致性
 * 适用于明确 key 值的情况 不适用动态拼接的key
 *
 * @author binblink
 */
@Aspect
@Slf4j
public class RedisCacheUpdateAspect {

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(
            new BlinkNamedThreadFactory.Builder("cache-double-delete").build());

    @Resource
    private RedisClient redisClient;

    @PreDestroy
    public void destroy() {
        executorService.shutdownNow();
    }

    /**
     * 环绕通知
     */
    @Around("@annotation(cacheDoubleDelete)")
    public Object redisCacheUpdate(ProceedingJoinPoint joinPoint, CacheDoubleDelete cacheDoubleDelete) throws Throwable {

        String keyPrefix = cacheDoubleDelete.keyPrefix();

        long delayTime = cacheDoubleDelete.delayTime();

        String key = keyPrefix;

        if (Objects.isNull(key) || key.trim().isEmpty()) {
            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            log.error("@CacheDoubleDelete annotation property keys must not be null! method:{}", methodSignature.getMethod().getName());
            throw new BlinkException(BlinkErrorCodeEnum.ILLEGAL_PARAMETER.getCode());
        }
        //参数
        Object[] objs = joinPoint.getArgs();
        if (!cacheDoubleDelete.fieldName().isEmpty() && objs.length == 0) {
            // 配了 fieldName 就必须能从入参里解析实际 key。
            throw new BlinkException(BlinkErrorCodeEnum.ILLEGAL_PARAMETER.getCode());
        }
        //默认注解只配置在实现类中，按照设计只有一个包含一切入参的报文参数 类型固定
        Object body = objs.length > 0 ? objs[0] : null;
        if (Objects.nonNull(body)) {

            String filedName = cacheDoubleDelete.fieldName();
            if(!filedName.isEmpty()){
                // getXxxx() 通过公共方法访问值
                String methodName = "get" + filedName.substring(0, 1).toUpperCase();
                //防止字段只有一个字符的情况
                if (filedName.length() > 1) {
                    methodName = methodName + filedName.substring(1);
                }
                //默认key字段在本类中 不是嵌套的对象内 只有一层嵌套的值
                Method method = body.getClass().getMethod(methodName);
                // key必须为 String类型
                key = key + method.invoke(body);
            }
        }
        redisClient.delete(key);
        // 继续执行被拦截的方法
        Object proceed = joinPoint.proceed();
        //延迟双删
        if (delayTime > 0) {
            final String cacheKey = key;
            // 第二次删除异步调度，避免业务线程直接 sleep 阻塞。
            executorService.schedule(() -> redisClient.delete(cacheKey), delayTime, TimeUnit.MILLISECONDS);
        } else {
            redisClient.delete(key);
        }

        return proceed;
    }
}
