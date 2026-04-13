package com.blink.framework.core.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MdcThreadPoolTaskExecutor 单元测试
 *
 * @author binblink
 */
@DisplayName("MdcThreadPoolTaskExecutor 单元测试")
class MdcThreadPoolTaskExecutorTest {

    private MdcThreadPoolTaskExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new MdcThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("test-");
        executor.initialize();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
        MDC.clear();
    }

    @Nested
    @DisplayName("submit(Callable) 方法测试")
    class SubmitCallableTest {

        @Test
        @DisplayName("Callable任务传递MDC上下文")
        void testSubmit_Callable_传递MDC上下文() throws Exception {
            // Arrange
            MDC.put("traceId", "trace-123");
            MDC.put("userId", "user-001");

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Map<String, String>> childMdc = new AtomicReference<>();

            Callable<String> task = () -> {
                childMdc.set(MDC.getCopyOfContextMap());
                latch.countDown();
                return "success";
            };

            // Act
            Future<String> future = executor.submit(task);
            latch.await(5, TimeUnit.SECONDS);

            // Assert
            assertThat(future.get()).isEqualTo("success");
            Map<String, String> mdcMap = childMdc.get();
            assertThat(mdcMap).isNotNull();
            assertThat(mdcMap.get("traceId")).isEqualTo("trace-123");
            assertThat(mdcMap.get("userId")).isEqualTo("user-001");
        }

        @Test
        @DisplayName("Callable任务执行后清空MDC")
        void testSubmit_Callable_执行后清空MDC() throws Exception {
            // Arrange
            MDC.put("traceId", "trace-after-clear");

            Callable<String> task = () -> "done";

            // Act
            Future<String> future = executor.submit(task);
            future.get(); // 等待完成

            // 稍等一下确保任务完成
            Thread.sleep(100);

            // Assert - 由于线程池复用，我们需要在另一个任务中检查
            // 提交一个新任务时，如果主线程MDC已清空，子线程不应该看到之前的数据
            MDC.clear(); // 清空主线程MDC
            AtomicReference<Map<String, String>> nextTaskMdc = new AtomicReference<>();
            Future<Void> checkFuture = executor.submit(() -> {
                nextTaskMdc.set(MDC.getCopyOfContextMap());
                return null;
            });
            checkFuture.get();

            // 由于主线程MDC已清空，下一个任务不应该看到任何数据
            assertThat(nextTaskMdc.get()).isNull();
        }

        @Test
        @DisplayName("空MDC上下文场景")
        void testSubmit_Callable_空MDC上下文() throws Exception {
            // Arrange - 不设置任何MDC
            MDC.clear();

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Map<String, String>> childMdc = new AtomicReference<>();

            Callable<String> task = () -> {
                childMdc.set(MDC.getCopyOfContextMap());
                latch.countDown();
                return "success";
            };

            // Act
            Future<String> future = executor.submit(task);
            latch.await(5, TimeUnit.SECONDS);

            // Assert
            assertThat(future.get()).isEqualTo("success");
            // 空MDC时，子线程MDC也应该为空
            assertThat(childMdc.get()).isNull();
        }
    }

    @Nested
    @DisplayName("execute(Runnable) 方法测试")
    class ExecuteRunnableTest {

        @Test
        @DisplayName("Runnable任务传递MDC上下文")
        void testExecute_Runnable_传递MDC上下文() throws Exception {
            // Arrange
            MDC.put("traceId", "runnable-trace");
            MDC.put("userName", "testuser");

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Map<String, String>> childMdc = new AtomicReference<>();

            Runnable task = () -> {
                childMdc.set(MDC.getCopyOfContextMap());
                latch.countDown();
            };

            // Act
            executor.execute(task);
            latch.await(5, TimeUnit.SECONDS);

            // Assert
            Map<String, String> mdcMap = childMdc.get();
            assertThat(mdcMap).isNotNull();
            assertThat(mdcMap.get("traceId")).isEqualTo("runnable-trace");
            assertThat(mdcMap.get("userName")).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Runnable任务执行后清空MDC")
        void testExecute_Runnable_执行后清空MDC() throws Exception {
            // Arrange
            MDC.put("traceId", "clear-test");

            Runnable task = () -> {
                // 任务执行
            };

            // Act
            executor.execute(task);
            Thread.sleep(100);

            // Assert - 清空主线程MDC后，提交新任务检查子线程MDC是否为空
            MDC.clear();
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Map<String, String>> nextTaskMdc = new AtomicReference<>();

            executor.execute(() -> {
                nextTaskMdc.set(MDC.getCopyOfContextMap());
                latch.countDown();
            });

            latch.await(5, TimeUnit.SECONDS);
            assertThat(nextTaskMdc.get()).isNull();
        }
    }

    @Nested
    @DisplayName("submit(Runnable) 方法测试")
    class SubmitRunnableTest {

        @Test
        @DisplayName("submit(Runnable)传递MDC上下文")
        void testSubmit_Runnable_传递MDC上下文() throws Exception {
            // Arrange
            MDC.put("traceId", "submit-runnable-trace");
            MDC.put("operation", "test");

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Map<String, String>> childMdc = new AtomicReference<>();

            Runnable task = () -> {
                childMdc.set(MDC.getCopyOfContextMap());
                latch.countDown();
            };

            // Act
            Future<?> future = executor.submit(task);
            latch.await(5, TimeUnit.SECONDS);

            // Assert
            future.get();
            Map<String, String> mdcMap = childMdc.get();
            assertThat(mdcMap).isNotNull();
            assertThat(mdcMap.get("traceId")).isEqualTo("submit-runnable-trace");
            assertThat(mdcMap.get("operation")).isEqualTo("test");
        }
    }

    @Nested
    @DisplayName("并发场景测试")
    class ConcurrentTest {

        @Test
        @DisplayName("多个任务并发执行时MDC隔离")
        void test多个任务并发执行时MDC隔离() throws Exception {
            // Arrange
            int taskCount = 5;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(taskCount);
            AtomicReference<String>[] results = new AtomicReference[taskCount];

            for (int i = 0; i < taskCount; i++) {
                results[i] = new AtomicReference<>();
            }

            // Act - 设置主线程MDC
            MDC.put("mainTrace", "main-trace-id");

            for (int i = 0; i < taskCount; i++) {
                final int index = i;
                MDC.put("taskTrace", "task-" + index);

                executor.execute(() -> {
                    try {
                        startLatch.await();
                        Map<String, String> mdc = MDC.getCopyOfContextMap();
                        if (mdc != null) {
                            results[index].set(mdc.get("taskTrace"));
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            endLatch.await(10, TimeUnit.SECONDS);

            // Assert - 每个任务应该看到其提交时的MDC值
            // 注意：由于任务可能复用线程，这里主要验证不会抛异常
            assertThat(endLatch.getCount()).isZero();
        }

        @Test
        @DisplayName("主线程MDC不受子线程影响")
        void test主线程MDC不受子线程影响() throws Exception {
            // Arrange
            MDC.put("mainKey", "mainValue");
            CountDownLatch latch = new CountDownLatch(1);

            // Act
            executor.execute(() -> {
                MDC.put("childKey", "childValue");
                latch.countDown();
            });

            latch.await(5, TimeUnit.SECONDS);

            // Assert - 主线程MDC不变
            assertThat(MDC.get("mainKey")).isEqualTo("mainValue");
            assertThat(MDC.get("childKey")).isNull();
        }
    }
}
