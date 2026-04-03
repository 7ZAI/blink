package com.blink.redis;

import com.blink.framework.redis.lock.DistributedLockClient;
import com.blink.framework.redis.config.prop.DistributedLockProperties;
import com.blink.framework.redis.lock.LockAcquisitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test cases for distributed lock functionality.
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DistributedLockTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    private DistributedLockProperties properties;
    private DistributedLockClient lockClient;

    @BeforeEach
    void setUp() {
        properties = new DistributedLockProperties();
        properties.setEnabled(true);
        properties.setDefaultWaitTime(Duration.ofSeconds(3));
        properties.setDefaultLeaseTime(Duration.ofSeconds(30));
        properties.setKeyPrefix("lock:");
        
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        
        lockClient = new DistributedLockClient(redissonClient, properties);
    }

    @Nested
    @DisplayName("TryLock Tests")
    class TryLockTests {
        
        @Test
        @DisplayName("Test tryLock success")
        void testTryLockSuccess() throws InterruptedException {
            when(rLock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);

            boolean acquired = lockClient.tryLock("test-resource");

            assertTrue(acquired);
            verify(redissonClient).getLock("lock:test-resource");
            verify(rLock).tryLock(anyLong(), anyLong(), any());
        }

        @Test
        @DisplayName("Test tryLock failure")
        void testTryLockFailure() throws InterruptedException {
            when(rLock.tryLock(anyLong(), anyLong(), any())).thenReturn(false);

            boolean acquired = lockClient.tryLock("test-resource");

            assertFalse(acquired);
        }

        @Test
        @DisplayName("Test lock with custom wait and lease time")
        void testLockWithCustomTimes() throws InterruptedException {
            when(rLock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);

            boolean acquired = lockClient.tryLock("test-resource", Duration.ofSeconds(5), Duration.ofSeconds(60));

            assertTrue(acquired);
        }

        @Test
        @DisplayName("Test null lock key throws exception")
        void testNullLockKey() {
            assertThrows(IllegalArgumentException.class, () -> lockClient.tryLock(null));
            assertThrows(IllegalArgumentException.class, () -> lockClient.tryLock(""));
        }
    }

    @Nested
    @DisplayName("ExecuteWithLock Tests")
    class ExecuteWithLockTests {

        @Test
        @DisplayName("Test executeWithLock success")
        void testExecuteWithLockSuccess() throws InterruptedException {
            when(rLock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
            when(rLock.isHeldByCurrentThread()).thenReturn(true);

            String result = lockClient.executeWithLock("test-resource", () -> "success");

            assertEquals("success", result);
            verify(rLock).unlock();
        }

        @Test
        @DisplayName("Test executeWithLock failure throws exception")
        void testExecuteWithLockFailure() throws InterruptedException {
            when(rLock.tryLock(anyLong(), anyLong(), any())).thenReturn(false);

            assertThrows(LockAcquisitionException.class, () -> 
                lockClient.executeWithLock("test-resource", () -> "success")
            );
        }

        @Test
        @DisplayName("Test executeWithLock with Runnable")
        void testExecuteWithLockRunnable() throws InterruptedException {
            when(rLock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
            when(rLock.isHeldByCurrentThread()).thenReturn(true);

            AtomicInteger counter = new AtomicInteger(0);
            lockClient.executeWithLock("test-resource", () -> counter.incrementAndGet());

            assertEquals(1, counter.get());
            verify(rLock).unlock();
        }

        @Test
        @DisplayName("Test exception in action still releases lock")
        void testExceptionInActionReleasesLock() throws InterruptedException {
            when(rLock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
            when(rLock.isHeldByCurrentThread()).thenReturn(true);

            assertThrows(RuntimeException.class, () ->
                lockClient.executeWithLock("test-resource", () -> {
                    throw new RuntimeException("Test exception");
                })
            );

            verify(rLock).unlock();
        }

        @Test
        @DisplayName("Test lock acquisition interrupted")
        void testLockAcquisitionInterrupted() throws InterruptedException {
            when(rLock.tryLock(anyLong(), anyLong(), any())).thenThrow(new InterruptedException());

            assertThrows(LockAcquisitionException.class, () ->
                lockClient.executeWithLock("test-resource", () -> "success")
            );
        }
    }

    @Nested
    @DisplayName("Lock Status Tests")
    class LockStatusTests {

        @Test
        @DisplayName("Test unlock releases lock")
        void testUnlock() {
            when(rLock.isHeldByCurrentThread()).thenReturn(true);

            lockClient.unlock("test-resource");

            verify(rLock).unlock();
        }

        @Test
        @DisplayName("Test isLocked")
        void testIsLocked() {
            when(rLock.isLocked()).thenReturn(true);

            boolean locked = lockClient.isLocked("test-resource");

            assertTrue(locked);
        }

        @Test
        @DisplayName("Test isHeldByCurrentThread")
        void testIsHeldByCurrentThread() {
            when(rLock.isHeldByCurrentThread()).thenReturn(true);

            boolean held = lockClient.isHeldByCurrentThread("test-resource");

            assertTrue(held);
        }
    }

    @Nested
    @DisplayName("Lock Type Tests")
    class LockTypeTests {

        @Test
        @DisplayName("Test getFairLock")
        void testGetFairLock() {
            when(redissonClient.getFairLock(anyString())).thenReturn(rLock);

            Object fairLock = lockClient.getFairLock("test-resource");

            assertNotNull(fairLock);
            verify(redissonClient).getFairLock("lock:test-resource");
        }

        @Test
        @DisplayName("Test getReadLock")
        void testGetReadLock() {
            RReadWriteLock readWriteLock = mock(RReadWriteLock.class);
            when(redissonClient.getReadWriteLock(anyString())).thenReturn(readWriteLock);
            when(readWriteLock.readLock()).thenReturn(rLock);

            Object readLock = lockClient.getReadLock("test-resource");

            assertNotNull(readLock);
        }

        @Test
        @DisplayName("Test getWriteLock")
        void testGetWriteLock() {
            RReadWriteLock readWriteLock = mock(RReadWriteLock.class);
            when(redissonClient.getReadWriteLock(anyString())).thenReturn(readWriteLock);
            when(readWriteLock.writeLock()).thenReturn(rLock);

            Object writeLock = lockClient.getWriteLock("test-resource");

            assertNotNull(writeLock);
        }
    }

    @Nested
    @DisplayName("Properties Tests")
    class PropertiesTests {

        @Test
        @DisplayName("Test properties default values")
        void testPropertiesDefaultValues() {
            DistributedLockProperties props = new DistributedLockProperties();
            
            assertFalse(props.isEnabled());
            assertEquals(Duration.ofSeconds(3), props.getDefaultWaitTime());
            assertEquals(Duration.ofSeconds(30), props.getDefaultLeaseTime());
            assertTrue(props.isWatchdogEnabled());
            assertEquals("lock:", props.getKeyPrefix());
            assertEquals(3, props.getRetryCount());
        }

        @Test
        @DisplayName("Test properties setters and getters")
        void testPropertiesSettersAndGetters() {
            DistributedLockProperties props = new DistributedLockProperties();
            
            props.setEnabled(true);
            props.setDefaultWaitTime(Duration.ofSeconds(5));
            props.setDefaultLeaseTime(Duration.ofSeconds(60));
            props.setWatchdogEnabled(false);
            props.setWatchdogTimeout(Duration.ofSeconds(45));
            props.setRetryInterval(Duration.ofMillis(200));
            props.setKeyPrefix("mylock:");
            props.setRetryCount(5);

            assertTrue(props.isEnabled());
            assertEquals(Duration.ofSeconds(5), props.getDefaultWaitTime());
            assertEquals(Duration.ofSeconds(60), props.getDefaultLeaseTime());
            assertFalse(props.isWatchdogEnabled());
            assertEquals(Duration.ofSeconds(45), props.getWatchdogTimeout());
            assertEquals(Duration.ofMillis(200), props.getRetryInterval());
            assertEquals("mylock:", props.getKeyPrefix());
            assertEquals(5, props.getRetryCount());
        }
    }

    @Nested
    @DisplayName("Concurrent Tests")
    class ConcurrentTests {

        @Test
        @DisplayName("Test concurrent lock operations")
        void testConcurrentLockOperations() throws Exception {
            when(rLock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
            when(rLock.isHeldByCurrentThread()).thenReturn(true);

            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        lockClient.executeWithLock("concurrent-test", () -> {
                            successCount.incrementAndGet();
                            return null;
                        });
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            assertEquals(threadCount, successCount.get());
        }
    }
}
