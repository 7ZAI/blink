package com.blink.framework.core.resilience;

import com.blink.framework.common.exception.BlinkErrorCodeEnum;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.core.annotation.CircuitBreaker;
import com.blink.framework.core.config.prop.ResilienceProperties;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 熔断切面
 * 
 * 基于 Resilience4j CircuitBreaker 实现
 * 
 * @author binblink
 */
@Aspect
@Slf4j
public class CircuitBreakerAspect {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final ResilienceProperties properties;
    private final ConcurrentHashMap<String, io.github.resilience4j.circuitbreaker.CircuitBreaker> circuitBreakerCache = new ConcurrentHashMap<>();

    public CircuitBreakerAspect(CircuitBreakerRegistry circuitBreakerRegistry, ResilienceProperties properties) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.properties = properties;
    }

    @Around("@annotation(circuitBreaker)")
    public Object around(ProceedingJoinPoint joinPoint, CircuitBreaker circuitBreaker) throws Throwable {
        String name = circuitBreaker.name();
        io.github.resilience4j.circuitbreaker.CircuitBreaker cb = getOrCreateCircuitBreaker(name, circuitBreaker);

        try {
            return io.github.resilience4j.circuitbreaker.CircuitBreaker.decorateSupplier(cb, () -> {
                try {
                    return joinPoint.proceed();
                } catch (Throwable e) {
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    }
                    throw new RuntimeException(e);
                }
            }).get();
        } catch (CallNotPermittedException e) {
            log.warn("熔断器开启，name: {}, state: {}", name, cb.getState());
            
            String fallbackMethod = circuitBreaker.fallbackMethod();
            if (!fallbackMethod.isEmpty()) {
                return executeFallback(joinPoint, fallbackMethod, e);
            }
            
            throw new BlinkException(BlinkErrorCodeEnum.SERVER_NOT_AVAILABLE.getCode());
        }
    }

    private io.github.resilience4j.circuitbreaker.CircuitBreaker getOrCreateCircuitBreaker(
            String name, CircuitBreaker circuitBreaker) {
        return circuitBreakerCache.computeIfAbsent(name, key -> {
            CircuitBreakerConfig config = buildCircuitBreakerConfig(circuitBreaker);
            return circuitBreakerRegistry.circuitBreaker(name, config);
        });
    }

    private CircuitBreakerConfig buildCircuitBreakerConfig(CircuitBreaker circuitBreaker) {
        ResilienceProperties.CircuitBreakerTemplate template = getTemplate(circuitBreaker.configName());

        return CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(template.getSlidingWindowSize())
                .minimumNumberOfCalls(template.getMinimumNumberOfCalls())
                .failureRateThreshold(template.getFailureRateThreshold())
                .slowCallRateThreshold(template.getSlowCallRateThreshold())
                .slowCallDurationThreshold(template.getSlowCallDurationThreshold())
                .waitDurationInOpenState(template.getWaitDurationInOpenState())
                .permittedNumberOfCallsInHalfOpenState(template.getPermittedNumberOfCallsInHalfOpenState())
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(Exception.class)
                .ignoreExceptions(IllegalArgumentException.class)
                .build();
    }

    private ResilienceProperties.CircuitBreakerTemplate getTemplate(String configName) {
        return switch (configName) {
            case "strict" -> properties.getCircuitBreaker().getStrictConfig();
            case "lenient" -> properties.getCircuitBreaker().getLenientConfig();
            default -> properties.getCircuitBreaker().getDefaultConfig();
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
        
        throw new BlinkException(BlinkErrorCodeEnum.SERVER_NOT_AVAILABLE.getCode());
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
