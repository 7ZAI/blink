package com.blink.framework.core.resilience;

import com.blink.framework.common.exception.BlinkErrorCodeEnum;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.core.annotation.RateLimit;
import com.blink.framework.core.config.prop.ResilienceProperties;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流切面
 * 
 * 基于 Resilience4j RateLimiter 实现
 * 
 * @author binblink
 */
@Aspect
@Slf4j
public class RateLimitAspect {

    private final RateLimiterRegistry rateLimiterRegistry;
    private final ResilienceProperties properties;
    private final ConcurrentHashMap<String, RateLimiter> rateLimiterCache = new ConcurrentHashMap<>();

    public RateLimitAspect(RateLimiterRegistry rateLimiterRegistry, ResilienceProperties properties) {
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.properties = properties;
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String name = rateLimit.name();
        RateLimiter limiter = getOrCreateRateLimiter(name, rateLimit);

        try {
            return RateLimiter.decorateSupplier(limiter, () -> {
                try {
                    return joinPoint.proceed();
                } catch (Throwable e) {
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    }
                    throw new RuntimeException(e);
                }
            }).get();
        } catch (RequestNotPermitted e) {
            log.warn("限流触发，name: {}, message: {}", name, e.getMessage());
            
            String fallbackMethod = rateLimit.fallbackMethod();
            if (!fallbackMethod.isEmpty()) {
                return executeFallback(joinPoint, fallbackMethod, e);
            }
            
            throw new BlinkException(BlinkErrorCodeEnum.SYSTEM_BUSY.getCode());
        }
    }

    private RateLimiter getOrCreateRateLimiter(String name, RateLimit rateLimit) {
        return rateLimiterCache.computeIfAbsent(name, key -> {
            RateLimiterConfig config = buildRateLimiterConfig(rateLimit);
            return rateLimiterRegistry.rateLimiter(name, config);
        });
    }

    private RateLimiterConfig buildRateLimiterConfig(RateLimit rateLimit) {
        ResilienceProperties.RateLimiterTemplate template = getTemplate(rateLimit.configName());

        return RateLimiterConfig.custom()
                .limitForPeriod(rateLimit.limitForPeriod() > 0 ? rateLimit.limitForPeriod() : template.getLimitForPeriod())
                .limitRefreshPeriod(rateLimit.limitRefreshPeriod() > 0 
                        ? Duration.ofSeconds(rateLimit.limitRefreshPeriod()) 
                        : template.getLimitRefreshPeriod())
                .timeoutDuration(rateLimit.timeoutDuration() > 0 
                        ? Duration.ofMillis(rateLimit.timeoutDuration()) 
                        : template.getTimeoutDuration())
                .build();
    }

    private ResilienceProperties.RateLimiterTemplate getTemplate(String configName) {
        return switch (configName) {
            case "strict" -> properties.getRateLimiter().getStrictConfig();
            case "lenient" -> properties.getRateLimiter().getLenientConfig();
            default -> properties.getRateLimiter().getDefaultConfig();
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
