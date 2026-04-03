package com.blink.framework.core.resilience;

import com.blink.framework.common.context.BlinkRequestContextHolder;
import com.blink.framework.common.exception.BlinkErrorCodeEnum;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.core.annotation.IpRateLimit;
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
 * 基于IP的限流切面
 * 
 * 针对单个IP地址进行限流，适用于公开接口的安全防护
 * 
 * @author blink
 */
@Aspect
@Slf4j
public class IpRateLimitAspect {

    private final RateLimiterRegistry rateLimiterRegistry;
    private final ConcurrentHashMap<String, RateLimiter> rateLimiterCache = new ConcurrentHashMap<>();

    public IpRateLimitAspect(RateLimiterRegistry rateLimiterRegistry) {
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    @Around("@annotation(ipRateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, IpRateLimit ipRateLimit) throws Throwable {
        String clientIp = getClientIp();
        String name = ipRateLimit.name() + ":" + clientIp;
        
        RateLimiter limiter = getOrCreateRateLimiter(name, ipRateLimit);

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
            log.warn("IP限流触发，name: {}, ip: {}, message: {}", ipRateLimit.name(), clientIp, e.getMessage());
            
            String fallbackMethod = ipRateLimit.fallbackMethod();
            if (!fallbackMethod.isEmpty()) {
                return executeFallback(joinPoint, fallbackMethod, e);
            }
            
            throw new BlinkException(BlinkErrorCodeEnum.SYSTEM_BUSY.getCode(), "请求过于频繁，请稍后再试");
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp() {
        String ip = BlinkRequestContextHolder.getClientIp();
        if (ip == null || ip.isEmpty()) {
            ip = "unknown";
        }
        return ip;
    }

    private RateLimiter getOrCreateRateLimiter(String name, IpRateLimit ipRateLimit) {
        return rateLimiterCache.computeIfAbsent(name, key -> {
            RateLimiterConfig config = buildRateLimiterConfig(ipRateLimit);
            return rateLimiterRegistry.rateLimiter(name, config);
        });
    }

    private RateLimiterConfig buildRateLimiterConfig(IpRateLimit ipRateLimit) {
        return RateLimiterConfig.custom()
                .limitForPeriod(ipRateLimit.limitForPeriod())
                .limitRefreshPeriod(Duration.ofSeconds(ipRateLimit.limitRefreshPeriod()))
                .timeoutDuration(Duration.ofMillis(ipRateLimit.timeoutDuration()))
                .build();
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
        
        throw new BlinkException(BlinkErrorCodeEnum.SYSTEM_BUSY.getCode(), "请求过于频繁，请稍后再试");
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
