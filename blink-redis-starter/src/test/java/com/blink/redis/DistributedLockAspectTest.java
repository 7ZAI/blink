package com.blink.redis;

import com.blink.framework.redis.annotation.DistributedLock;
import com.blink.framework.redis.aop.DistributedLockAspect;
import com.blink.framework.redis.config.prop.DistributedLockProperties;
import com.blink.framework.redis.lock.DistributedLockClient;
import com.blink.framework.redis.lock.LockAcquisitionException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RLock;

import java.lang.reflect.Method;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DistributedLockAspectTest {

    @Mock
    private DistributedLockClient lockClient;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private DistributedLockAspect aspect;

    @BeforeEach
    void setUp() {
        DistributedLockProperties properties = new DistributedLockProperties();
        properties.setRetryCount(2);
        properties.setRetryInterval(Duration.ofMillis(10));
        aspect = new DistributedLockAspect(lockClient, properties);
    }

    @Test
    void executeWithoutLockStrategyShouldProceed() throws Throwable {
        Method method = SampleService.class.getMethod("executeWithoutLock", String.class);
        DistributedLock annotation = method.getAnnotation(DistributedLock.class);

        prepareJoinPoint(method, new Object[]{"42"});
        when(lockClient.tryLock(eq("order:42"), any(), any())).thenReturn(false);
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.around(joinPoint, annotation);

        assertEquals("ok", result);
        verify(joinPoint).proceed();
    }

    @Test
    void fairLockShouldUnlockTheSameLockInstance() throws Throwable {
        Method method = SampleService.class.getMethod("fairLockMethod");
        DistributedLock annotation = method.getAnnotation(DistributedLock.class);
        RLock fairLock = mock(RLock.class);

        prepareJoinPoint(method, new Object[0]);
        when(lockClient.getFairLock("fair-resource")).thenReturn(fairLock);
        when(lockClient.tryLock(eq(fairLock), eq("fair-resource"), any(), any())).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.around(joinPoint, annotation);

        assertEquals("ok", result);
        verify(lockClient).unlock(fairLock, "fair-resource");
        verify(lockClient, never()).unlock("fair-resource");
    }

    @Test
    void templateKeyShouldResolveWithMethodArguments() throws Throwable {
        Method method = SampleService.class.getMethod("templateKey", String.class);
        DistributedLock annotation = method.getAnnotation(DistributedLock.class);

        prepareJoinPoint(method, new Object[]{"42"});
        when(lockClient.tryLock(eq("order:42"), any(), any())).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.around(joinPoint, annotation);

        assertEquals("ok", result);
        verify(lockClient).tryLock(eq("order:42"), any(), any());
    }

    @Test
    void retryStrategyShouldRetryAndEventuallyProceed() throws Throwable {
        Method method = SampleService.class.getMethod("retryMethod");
        DistributedLock annotation = method.getAnnotation(DistributedLock.class);

        prepareJoinPoint(method, new Object[0]);
        when(lockClient.tryLock(eq("retry-resource"), any(), any())).thenReturn(false, true);
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.around(joinPoint, annotation);

        assertEquals("ok", result);
        verify(lockClient, times(2)).tryLock(eq("retry-resource"), any(), any());
        verify(joinPoint).proceed();
    }

    @Test
    void retryStrategyShouldThrowAfterRetriesExhausted() throws Throwable {
        Method method = SampleService.class.getMethod("retryMethod");
        DistributedLock annotation = method.getAnnotation(DistributedLock.class);

        prepareJoinPoint(method, new Object[0]);
        when(lockClient.tryLock(eq("retry-resource"), any(), any())).thenReturn(false);

        assertThrows(LockAcquisitionException.class, () -> aspect.around(joinPoint, annotation));
        verify(lockClient, times(3)).tryLock(eq("retry-resource"), any(), any());
    }

    private void prepareJoinPoint(Method method, Object[] args) {
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(args);
    }

    static class SampleService {

        @DistributedLock(key = "order:#orderId", failureStrategy = com.blink.framework.redis.lock.LockFailureStrategy.EXECUTE_WITHOUT_LOCK)
        public String executeWithoutLock(String orderId) {
            return "ok";
        }

        @DistributedLock("order:#orderId")
        public String templateKey(String orderId) {
            return orderId;
        }

        @DistributedLock(key = "fair-resource", fairLock = true)
        public String fairLockMethod() {
            return "ok";
        }

        @DistributedLock(key = "retry-resource", failureStrategy = com.blink.framework.redis.lock.LockFailureStrategy.RETRY)
        public String retryMethod() {
            return "ok";
        }
    }
}
