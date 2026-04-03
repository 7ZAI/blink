package com.blink.redis;

import com.blink.framework.redis.component.CacheComponent;
import com.blink.framework.redis.component.RedisClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CacheComponentTest {

    @Mock
    private RedisClient redisClient;

    @Mock
    private TaskExecutor taskExecutor;

    @Test
    void cacheMissShouldRefreshThroughTaskExecutor() {
        CacheComponent cacheComponent = new CacheComponent(false, taskExecutor);
        ReflectionTestUtils.setField(cacheComponent, "redisClient", redisClient);
        when(redisClient.get("k")).thenReturn(null);

        Object value = cacheComponent.getFromCacheOrDB("k", () -> "v");

        assertEquals("v", value);

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(taskExecutor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        verify(redisClient).delete("k");
        verify(redisClient).set("k", "v");
    }
}
