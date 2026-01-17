package com.blink.framework.core.aop;

import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.core.annotation.CacheDoubleDelete;
import com.blink.framework.redis.component.RedisClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 缓存删除 保证数据一致性
 *
 * @author binblink
 */
@Aspect
@Slf4j
@Component
public class RedisCacheUpdateAspect {

    private final RedisClient redisClient;

    public RedisCacheUpdateAspect(RedisClient redisClient){
        this.redisClient = redisClient;
    }
    /**
     * 环绕通知
     */
    @Around("@annotation(cacheDoubleDelete)")
    public Object redisCacheUpdate(ProceedingJoinPoint joinPoint, CacheDoubleDelete cacheDoubleDelete) throws Throwable {

        String keyPrefix = cacheDoubleDelete.keyPrefix();

        long delayTime = cacheDoubleDelete.delayTime();

        String key = keyPrefix;

        if (Objects.isNull(key) || key.trim().length() == 0) {
            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            log.error("@CacheDoubleDelete annotation property keys must not be null! method:{}", methodSignature.getMethod().getName());
            throw new BlinkException("@CacheDoubleDelete annotation property keys must not be null!");
        }
        //参数
        Object[] objs = joinPoint.getArgs();
        //默认注解只配置在实现类中，按照设计只有一个包含一切入参的报文参数 类型固定
        Object body = objs[0];
        if (Objects.nonNull(body)) {

            String filedName = cacheDoubleDelete.fieldName();
            // getXxxx() 通过公共方法访问值
            String methodName = "get" + filedName.substring(0, 1).toUpperCase();
            //防止字段只有一个字符的情况
            if (filedName.length() > 2) {
                methodName = methodName + filedName.substring(1);
            }
            //默认key字段在本类中 不是嵌套的对象内 只有一层嵌套的值
            Method method = body.getClass().getMethod(methodName);
            // key必须为 String类型
            key = key + method.invoke(body);

        }
        redisClient.delete(key);
        // 继续执行被拦截的方法
        Object proceed = joinPoint.proceed();
        //延迟双删
        if (delayTime > 0) {
            Thread.sleep(delayTime);
        }
        redisClient.delete(key);

        return proceed;
    }
}
