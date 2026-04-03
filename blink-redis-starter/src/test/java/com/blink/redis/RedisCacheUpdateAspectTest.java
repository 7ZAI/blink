package com.blink.redis;

import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.redis.annotation.CacheDoubleDelete;
import com.blink.framework.redis.aop.RedisCacheUpdateAspect;
import com.blink.framework.redis.component.RedisClient;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisCacheUpdateAspectTest {

    @Mock
    private RedisClient redisClient;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Test
    void delayedDeleteShouldNotBlockCaller() throws Throwable {
        RedisCacheUpdateAspect aspect = new RedisCacheUpdateAspect();
        ReflectionTestUtils.setField(aspect, "redisClient", redisClient);

        Method method = SampleService.class.getMethod("deleteLater", RequestBody.class);
        CacheDoubleDelete annotation = method.getAnnotation(CacheDoubleDelete.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{new RequestBody("123")});
        when(joinPoint.proceed()).thenReturn("ok");

        long start = System.currentTimeMillis();
        Object result = aspect.redisCacheUpdate(joinPoint, annotation);
        long elapsed = System.currentTimeMillis() - start;

        assertEquals("ok", result);
        verify(redisClient, timeout(50).atLeastOnce()).delete("user:123");
        verify(redisClient, timeout(500).times(2)).delete("user:123");
        org.junit.jupiter.api.Assertions.assertTrue(elapsed < 150, "delay delete should not block the caller");
        aspect.destroy();
    }

    @Test
    void missingArgsForFieldNameShouldThrowBusinessException() throws Throwable {
        RedisCacheUpdateAspect aspect = new RedisCacheUpdateAspect();
        ReflectionTestUtils.setField(aspect, "redisClient", redisClient);

        Method method = SampleService.class.getMethod("invalidNoArgs");
        CacheDoubleDelete annotation = method.getAnnotation(CacheDoubleDelete.class);

        when(joinPoint.getArgs()).thenReturn(new Object[0]);

        assertThrows(BlinkException.class, () -> aspect.redisCacheUpdate(joinPoint, annotation));
        aspect.destroy();
    }

    static class SampleService {

        @CacheDoubleDelete(keyPrefix = "user:", fieldName = "id", delayTime = 250)
        public String deleteLater(RequestBody body) {
            return "ok";
        }

        @CacheDoubleDelete(keyPrefix = "user:", fieldName = "id")
        public void invalidNoArgs() {
        }
    }

    public static class RequestBody {
        private final String id;

        RequestBody(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }
}
