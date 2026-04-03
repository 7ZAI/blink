package com.blink.framework.core.resilience;

import com.blink.framework.common.exception.BlinkErrorCodeEnum;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.core.annotation.Retry;
import com.blink.framework.core.config.prop.ResilienceProperties;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * 重试切面
 * 
 * 基于 Resilience4j Retry 实现
 * 
 * @author binblink
 */
@Aspect
@Slf4j
public class RetryAspect {

    private final RetryRegistry retryRegistry;
    private final ResilienceProperties properties;
    private final ConcurrentHashMap<String, io.github.resilience4j.retry.Retry> retryCache = new ConcurrentHashMap<>();

    public RetryAspect(RetryRegistry retryRegistry, ResilienceProperties properties) {
        this.retryRegistry = retryRegistry;
        this.properties = properties;
    }

    @Around("@annotation(retry)")
    public Object around(ProceedingJoinPoint joinPoint, Retry retry) throws Throwable {
        String name = retry.name();
        io.github.resilience4j.retry.Retry retryer = getOrCreateRetry(name, retry);

        try {
            return io.github.resilience4j.retry.Retry.decorateSupplier(retryer, () -> {
                try {
                    return joinPoint.proceed();
                } catch (Throwable e) {
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    }
                    throw new RuntimeException(e);
                }
            }).get();
        } catch (Exception e) {
            log.error("重试失败，name: {}, message: {}", name, e.getMessage());
            
            String fallbackMethod = retry.fallbackMethod();
            if (!fallbackMethod.isEmpty()) {
                return executeFallback(joinPoint, fallbackMethod, e);
            }
            
            if (e.getCause() != null) {
                throw e.getCause();
            }
            throw new BlinkException(BlinkErrorCodeEnum.SYSTEM_BUSY.getCode());
        }
    }

    private io.github.resilience4j.retry.Retry getOrCreateRetry(String name, Retry retry) {
        return retryCache.computeIfAbsent(name, key -> {
            RetryConfig config = buildRetryConfig(retry);
            return retryRegistry.retry(name, config);
        });
    }

    private RetryConfig buildRetryConfig(Retry retry) {
        ResilienceProperties.RetryTemplate template = getTemplate(retry.configName());

        int maxAttempts = retry.maxAttempts() > 0 ? retry.maxAttempts() : template.getMaxAttempts();
        long waitDuration = retry.waitDuration() > 0 ? retry.waitDuration() : template.getWaitDuration().toMillis();

        return RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .waitDuration(java.time.Duration.ofMillis(waitDuration))
                .retryExceptions(IOException.class, TimeoutException.class)
                .ignoreExceptions(IllegalArgumentException.class, BlinkException.class)
                .build();
    }

    private ResilienceProperties.RetryTemplate getTemplate(String configName) {
        return switch (configName) {
            case "quick" -> properties.getRetry().getQuickConfig();
            case "slow" -> properties.getRetry().getSlowConfig();
            default -> properties.getRetry().getDefaultConfig();
        };
    }

    private Object executeFallback(ProceedingJoinPoint joinPoint, String fallbackMethodName, Throwable e) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method fallbackMethod = findFallbackMethod(joinPoint, fallbackMethodName, signature.getParameterTypes());
            
            if (fallbackMethod != null) {
                Object[] args = joinPoint.getArgs();
                Object[] fallbackArgs = new Object[args.length + 1];
                System.arraycopy(args, 0, fallbackArgs, 0, args.length);
                fallbackArgs[args.length] = e;
                
                fallbackMethod.setAccessible(true);
                return fallbackMethod.invoke(joinPoint.getTarget(), fallbackArgs);
            }
        } catch (Exception ex) {
            log.error("执行降级方法失败: {}", ex.getMessage(), ex);
        }
        
        throw new BlinkException(BlinkErrorCodeEnum.SYSTEM_BUSY.getCode());
    }

    private Method findFallbackMethod(ProceedingJoinPoint joinPoint, String methodName, Class<?>[] paramTypes) {
        Class<?> targetClass = joinPoint.getTarget().getClass();
        
        try {
            return targetClass.getDeclaredMethod(methodName, appendThrowable(paramTypes));
        } catch (NoSuchMethodException e) {
            log.warn("未找到降级方法: {}", methodName);
            return null;
        }
    }

    private Class<?>[] appendThrowable(Class<?>[] paramTypes) {
        Class<?>[] newTypes = new Class<?>[paramTypes.length + 1];
        System.arraycopy(paramTypes, 0, newTypes, 0, paramTypes.length);
        newTypes[paramTypes.length] = Throwable.class;
        return newTypes;
    }
}
