package com.blink.framework.redis.aop;

import com.blink.framework.redis.config.prop.DistributedLockProperties;
import com.blink.framework.redis.lock.DistributedLockClient;
import com.blink.framework.redis.lock.LockAcquisitionException;
import com.blink.framework.redis.lock.LockFailureStrategy;
import com.blink.framework.redis.annotation.DistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁切面，处理 @DistributedLock 注解。
 * <p>
 * 该切面拦截带有 @DistributedLock 注解的方法，在方法执行前获取分布式锁，执行完成后释放锁。
 * </p>
 *
 * <p>功能特性：</p>
 * <ul>
 *   <li>支持 SpEL 表达式动态生成锁键</li>
 *   <li>支持公平锁</li>
 *   <li>支持多种获取锁失败策略</li>
 *   <li>支持重试机制</li>
 * </ul>
 *
 * @author binblink
 * @see DistributedLock
 * @see DistributedLockClient
 */
@Aspect
@Slf4j
public class DistributedLockAspect {

    /**
     * SpEL 变量引用正则模式，匹配 #varName 或 #varName.property 格式
     */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("#[\\w]+(?:\\.[\\w]+)*");

    private final DistributedLockClient lockClient;
    private final DistributedLockProperties properties;
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * 构造分布式锁切面实例。
     *
     * @param lockClient 分布式锁客户端
     * @param properties 分布式锁配置属性
     */
    public DistributedLockAspect(DistributedLockClient lockClient, DistributedLockProperties properties) {
        this.lockClient = lockClient;
        this.properties = properties;
    }

    /**
     * 环绕通知，处理带有 @DistributedLock 注解的方法。
     * <p>
     * 执行流程：
     * <ol>
     *   <li>解析锁键（支持 SpEL 表达式）</li>
     *   <li>获取锁实例（公平锁需要持有一个实例到 finally 块）</li>
     *   <li>尝试获取锁</li>
     *   <li>执行业务方法</li>
     *   <li>释放锁</li>
     * </ol>
     * </p>
     *
     * @param joinPoint 切点
     * @param lock      分布式锁注解
     * @return 方法执行结果
     * @throws Throwable 方法执行异常时抛出
     */
    @Around("@annotation(lock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock lock) throws Throwable {
        // 解析锁键
        String lockKey = resolveLockKey(joinPoint, lock);

        // 解析等待时间和持有时间
        Duration waitTime = resolveWaitTime(lock);
        Duration leaseTime = resolveLeaseTime(lock);

        // 公平锁必须持有同一个锁实例到 finally，否则解锁时会取到另一把普通锁
        RLock lockInstance = lock.fairLock() ? lockClient.getFairLock(lockKey) : null;
        boolean acquired = tryAcquire(lock, lockKey, lockInstance, waitTime, leaseTime);

        if (!acquired) {
            return handleLockFailure(joinPoint, lock, lockKey, waitTime, leaseTime);
        }

        try {
            log.debug("[分布式锁] 获取成功 | key: {}", lockKey);
            return joinPoint.proceed();
        } finally {
            if (lockInstance != null) {
                lockClient.unlock(lockInstance, lockKey);
            } else {
                lockClient.unlock(lockKey);
            }
            log.debug("[分布式锁] 释放成功 | key: {}", lockKey);
        }
    }

    /**
     * 解析锁键。
     * <p>
     * 支持以下格式：
     * <ul>
     *   <li>静态键："my-resource"</li>
     *   <li>模板键："user:#userId"</li>
     *   <li>SpEL 表达式："'user:' + #user.id"</li>
     * </ul>
     * </p>
     *
     * @param joinPoint 切点
     * @param lock      注解
     * @return 解析后的锁键
     */
    private String resolveLockKey(ProceedingJoinPoint joinPoint, DistributedLock lock) {
        String keyExpression = StringUtils.hasText(lock.key()) ? lock.key() : lock.value();

        // 未指定键时，使用 类名:方法名 作为默认键
        if (!StringUtils.hasText(keyExpression)) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String className = signature.getDeclaringType().getSimpleName();
            String methodName = signature.getName();
            return className + ":" + methodName;
        }

        // 包含变量引用时，解析 SpEL 表达式
        if (containsVariableReference(keyExpression)) {
            return evaluateKeyExpression(joinPoint, keyExpression);
        }

        return keyExpression;
    }

    /**
     * 检查表达式是否包含变量引用。
     *
     * @param expression 表达式
     * @return 包含变量引用返回 true，否则返回 false
     */
    private boolean containsVariableReference(String expression) {
        return expression.contains("#");
    }

    /**
     * 计算锁键表达式。
     * <p>
     * 根据表达式格式选择不同的解析策略：
     * <ul>
     *   <li>原生 SpEL 表达式（包含 + 或 '）：直接解析</li>
     *   <li>模板格式（如 "order:#orderId"）：插值替换</li>
     * </ul>
     * </p>
     *
     * @param joinPoint  切点
     * @param expression 表达式
     * @return 解析后的锁键
     */
    private String evaluateKeyExpression(ProceedingJoinPoint joinPoint, String expression) {
        if (looksLikeRawSpel(expression)) {
            return evaluateSpelExpression(joinPoint, expression);
        }
        // 兼容 "order:#orderId" 这类模板写法，只替换变量片段，避免整串按 SpEL 解析失败
        return interpolateTemplate(joinPoint, expression);
    }

    /**
     * 执行原生 SpEL 表达式解析。
     *
     * @param joinPoint  切点
     * @param expression SpEL 表达式
     * @return 解析后的值
     */
    private String evaluateSpelExpression(ProceedingJoinPoint joinPoint, String expression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);

        // 构建 SpEL 上下文，设置方法参数为变量
        EvaluationContext context = new StandardEvaluationContext();
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        Expression exp = parser.parseExpression(expression);
        return exp.getValue(context, String.class);
    }

    /**
     * 模板插值，将 #varName 格式的变量替换为实际值。
     *
     * @param joinPoint  切点
     * @param expression 模板表达式
     * @return 插值后的字符串
     */
    private String interpolateTemplate(ProceedingJoinPoint joinPoint, String expression) {
        StringBuilder builder = new StringBuilder();
        Matcher matcher = VARIABLE_PATTERN.matcher(expression);
        int lastIndex = 0;
        while (matcher.find()) {
            builder.append(expression, lastIndex, matcher.start());
            String value = evaluateSpelExpression(joinPoint, matcher.group());
            builder.append(value == null ? "null" : value);
            lastIndex = matcher.end();
        }
        builder.append(expression.substring(lastIndex));
        return builder.toString();
    }

    /**
     * 判断表达式是否为原生 SpEL 格式。
     * <p>
     * 原生 SpEL 特征：包含字符串拼接符 + 或字符串引号 '，或以 # 开头
     * </p>
     *
     * @param expression 表达式
     * @return 是原生 SpEL 返回 true，否则返回 false
     */
    private boolean looksLikeRawSpel(String expression) {
        return expression.contains("'") || expression.contains("+") || expression.startsWith("#");
    }

    /**
     * 解析等待时间。
     *
     * @param lock 注解
     * @return 等待时间
     */
    private Duration resolveWaitTime(DistributedLock lock) {
        if (lock.waitTime() > 0) {
            return Duration.of(lock.waitTime(), toChronoUnit(lock.timeUnit()));
        }
        return properties.getDefaultWaitTime();
    }

    /**
     * 解析持有时间。
     * <p>
     * 返回 Duration.ofMillis(-1) 表示启用看门狗自动续期机制。
     * </p>
     *
     * @param lock 注解
     * @return 持有时间
     */
    private Duration resolveLeaseTime(DistributedLock lock) {
        if (lock.leaseTime() > 0) {
            return Duration.of(lock.leaseTime(), toChronoUnit(lock.timeUnit()));
        }
        // 返回 -1 表示启用看门狗机制
        return Duration.ofMillis(-1);
    }

    /**
     * 将 TimeUnit 转换为 ChronoUnit。
     *
     * @param timeUnit 时间单位
     * @return 对应的 ChronoUnit
     */
    private java.time.temporal.ChronoUnit toChronoUnit(TimeUnit timeUnit) {
        return switch (timeUnit) {
            case NANOSECONDS -> java.time.temporal.ChronoUnit.NANOS;
            case MICROSECONDS -> java.time.temporal.ChronoUnit.MICROS;
            case MILLISECONDS -> java.time.temporal.ChronoUnit.MILLIS;
            case SECONDS -> java.time.temporal.ChronoUnit.SECONDS;
            case MINUTES -> java.time.temporal.ChronoUnit.MINUTES;
            case HOURS -> java.time.temporal.ChronoUnit.HOURS;
            case DAYS -> java.time.temporal.ChronoUnit.DAYS;
        };
    }

    /**
     * 处理获取锁失败的情况。
     *
     * @param joinPoint 切点
     * @param lock      注解
     * @param lockKey   锁键
     * @param waitTime  等待时间
     * @param leaseTime 持有时间
     * @return 根据失败策略返回的结果
     * @throws Throwable 执行异常时抛出
     */
    private Object handleLockFailure(ProceedingJoinPoint joinPoint,
                                     DistributedLock lock,
                                     String lockKey,
                                     Duration waitTime,
                                     Duration leaseTime) throws Throwable {
        LockFailureStrategy strategy = lock.failureStrategy();
        String message = lock.errorMessage();

        log.warn("[分布式锁] 获取失败 | key: {}, 策略: {}", lockKey, strategy);

        return switch (strategy) {
            case THROW_EXCEPTION -> throw new LockAcquisitionException(
                    String.format("%s [key: %s]", message, lockKey));
            case RETURN_NULL -> null;
            case EXECUTE_WITHOUT_LOCK -> {
                // 显式降级为"继续执行业务但不加锁"，而不是直接返回 null
                log.warn("[分布式锁] 降级执行（无锁）| key: {}", lockKey);
                yield joinPoint.proceed();
            }
            case RETRY -> retryAcquire(joinPoint, lock, lockKey, waitTime, leaseTime, message);
        };
    }

    /**
     * 重试获取锁。
     * <p>
     * 首次尝试已经失败，此方法负责按配置做补充重试。
     * </p>
     *
     * @param joinPoint 切点
     * @param lock      注解
     * @param lockKey   锁键
     * @param waitTime  等待时间
     * @param leaseTime 持有时间
     * @param message   错误消息
     * @return 方法执行结果
     * @throws Throwable 获取锁失败或执行异常时抛出
     */
    private Object retryAcquire(ProceedingJoinPoint joinPoint,
                                DistributedLock lock,
                                String lockKey,
                                Duration waitTime,
                                Duration leaseTime,
                                String message) throws Throwable {
        // 公平锁实例在重试过程中保持不变，避免重复获取
        RLock lockInstance = lock.fairLock() ? lockClient.getFairLock(lockKey) : null;

        for (int attempt = 0; attempt < properties.getRetryCount(); attempt++) {
            // 重试间隔等待
            if (attempt > 0) {
                try {
                    Thread.sleep(properties.getRetryInterval().toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new LockAcquisitionException("重试被中断 [key: " + lockKey + "]", e);
                }
            }

            if (tryAcquire(lock, lockKey, lockInstance, waitTime, leaseTime)) {
                try {
                    return joinPoint.proceed();
                } finally {
                    if (lockInstance != null) {
                        lockClient.unlock(lockInstance, lockKey);
                    } else {
                        lockClient.unlock(lockKey);
                    }
                }
            }
        }

        throw new LockAcquisitionException(String.format("%s [key: %s]", message, lockKey));
    }

    /**
     * 尝试获取锁。
     *
     * @param lock        注解
     * @param lockKey     锁键
     * @param lockInstance 锁实例（公平锁场景）
     * @param waitTime    等待时间
     * @param leaseTime   持有时间
     * @return 获取成功返回 true，否则返回 false
     */
    private boolean tryAcquire(DistributedLock lock,
                               String lockKey,
                               RLock lockInstance,
                               Duration waitTime,
                               Duration leaseTime) {
        if (lock.fairLock()) {
            return lockClient.tryLock(lockInstance, lockKey, waitTime, leaseTime);
        }
        return lockClient.tryLock(lockKey, waitTime, leaseTime);
    }
}