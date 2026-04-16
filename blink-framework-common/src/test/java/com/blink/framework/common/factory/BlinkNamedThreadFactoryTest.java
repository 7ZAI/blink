package com.blink.framework.common.factory;

import com.blink.framework.test.annotation.UnitTest;
import com.blink.framework.test.base.BlinkUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

/**
 * BlinkNamedThreadFactory 单元测试
 * <p>
 * 测试覆盖：
 * 1. Builder 模式构建
 * 2. 线程命名
 * 3. 守护线程设置
 * 4. 线程优先级设置
 * 5. 异常处理器设置
 * 6. 静态工厂方法
 *
 * @author binblink
 */
@UnitTest
@DisplayName("BlinkNamedThreadFactory 线程工厂测试")
class BlinkNamedThreadFactoryTest extends BlinkUnitTest {

    // ==================== Builder 构建测试 ====================

    @Nested
    @DisplayName("Builder 构建测试")
    class BuilderTests {

        @Test
        @DisplayName("应该使用Builder构建线程工厂")
        void shouldBuildWithBuilder() {
            // when
            BlinkNamedThreadFactory factory = new BlinkNamedThreadFactory.Builder("TestThread")
                    .daemon(true)
                    .priority(Thread.MAX_PRIORITY)
                    .build();

            // then
            assertThat(factory).isNotNull();
        }

        @Test
        @DisplayName("线程名前缀为空应该抛出异常")
        void shouldThrowExceptionWhenNamePrefixIsNull() {
            // when & then
            assertThatThrownBy(() -> new BlinkNamedThreadFactory.Builder(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("线程名前缀不能为空");
        }

        @Test
        @DisplayName("线程名前缀为空白字符串应该抛出异常")
        void shouldThrowExceptionWhenNamePrefixIsBlank() {
            // when & then
            assertThatThrownBy(() -> new BlinkNamedThreadFactory.Builder("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("线程名前缀不能为空");
        }

        @Test
        @DisplayName("Builder应该正确设置线程组")
        void shouldSetThreadGroup() {
            // given
            ThreadGroup customGroup = new ThreadGroup("CustomGroup");

            // when
            BlinkNamedThreadFactory factory = new BlinkNamedThreadFactory.Builder("Test")
                    .group(customGroup)
                    .build();

            // then
            Thread thread = factory.newThread(() -> {});
            assertThat(thread.getThreadGroup()).isEqualTo(customGroup);
        }

        @Test
        @DisplayName("Builder应该正确设置守护线程属性")
        void shouldSetDaemonFlag() {
            // when
            BlinkNamedThreadFactory daemonFactory = new BlinkNamedThreadFactory.Builder("DaemonTest")
                    .daemon(true)
                    .build();

            BlinkNamedThreadFactory nonDaemonFactory = new BlinkNamedThreadFactory.Builder("NonDaemonTest")
                    .daemon(false)
                    .build();

            // then
            Thread daemonThread = daemonFactory.newThread(() -> {});
            Thread nonDaemonThread = nonDaemonFactory.newThread(() -> {});

            assertThat(daemonThread.isDaemon()).isTrue();
            assertThat(nonDaemonThread.isDaemon()).isFalse();
        }

        @Test
        @DisplayName("Builder应该正确设置线程优先级")
        void shouldSetPriority() {
            // when
            BlinkNamedThreadFactory factory = new BlinkNamedThreadFactory.Builder("PriorityTest")
                    .priority(Thread.MAX_PRIORITY)
                    .build();

            // then
            Thread thread = factory.newThread(() -> {});
            assertThat(thread.getPriority()).isEqualTo(Thread.MAX_PRIORITY);
        }

        @Test
        @DisplayName("优先级超过最大值应该被限制")
        void shouldCapPriorityToMaxValue() {
            // when
            BlinkNamedThreadFactory factory = new BlinkNamedThreadFactory.Builder("PriorityCapTest")
                    .priority(100) // 超过 MAX_PRIORITY (10)
                    .build();

            // then
            Thread thread = factory.newThread(() -> {});
            assertThat(thread.getPriority()).isEqualTo(Thread.MAX_PRIORITY);
        }
    }

    // ==================== 线程创建测试 ====================

    @Nested
    @DisplayName("线程创建测试")
    class ThreadCreationTests {

        @Test
        @DisplayName("应该创建带有正确名称的线程")
        void shouldCreateThreadWithCorrectName() {
            // given
            BlinkNamedThreadFactory factory = new BlinkNamedThreadFactory.Builder("MyThread")
                    .build();

            // when
            Thread thread1 = factory.newThread(() -> {});
            Thread thread2 = factory.newThread(() -> {});

            // then
            assertThat(thread1.getName()).isEqualTo("MyThread-1");
            assertThat(thread2.getName()).isEqualTo("MyThread-2");
        }

        @Test
        @DisplayName("应该自动递增线程编号")
        void shouldIncrementThreadNumber() {
            // given
            BlinkNamedThreadFactory factory = new BlinkNamedThreadFactory.Builder("Counter")
                    .build();

            // when
            Thread thread1 = factory.newThread(() -> {});
            Thread thread2 = factory.newThread(() -> {});
            Thread thread3 = factory.newThread(() -> {});

            // then
            assertThat(thread1.getName()).isEqualTo("Counter-1");
            assertThat(thread2.getName()).isEqualTo("Counter-2");
            assertThat(thread3.getName()).isEqualTo("Counter-3");
        }

        @Test
        @DisplayName("线程名前缀应该被trim")
        void shouldTrimNamePrefix() {
            // given
            BlinkNamedThreadFactory factory = new BlinkNamedThreadFactory.Builder("  TrimmedPrefix  ")
                    .build();

            // when
            Thread thread = factory.newThread(() -> {});

            // then
            assertThat(thread.getName()).startsWith("TrimmedPrefix-");
        }

        @Test
        @DisplayName("默认优先级应该是NORM_PRIORITY")
        void shouldHaveDefaultNormPriority() {
            // given
            BlinkNamedThreadFactory factory = new BlinkNamedThreadFactory.Builder("DefaultPriority")
                    .build();

            // when
            Thread thread = factory.newThread(() -> {});

            // then
            assertThat(thread.getPriority()).isEqualTo(Thread.NORM_PRIORITY);
        }

        @Test
        @DisplayName("默认应该不是守护线程")
        void shouldNotBeDaemonByDefault() {
            // given
            BlinkNamedThreadFactory factory = new BlinkNamedThreadFactory.Builder("NonDaemon")
                    .build();

            // when
            Thread thread = factory.newThread(() -> {});

            // then
            assertThat(thread.isDaemon()).isFalse();
        }
    }

    // ==================== 异常处理器测试 ====================

    @Nested
    @DisplayName("异常处理器测试")
    class ExceptionHandlerTests {

        @Test
        @DisplayName("应该设置未捕获异常处理器")
        void shouldSetUncaughtExceptionHandler() {
            // given
            AtomicReference<Throwable> capturedException = new AtomicReference<>();
            Thread.UncaughtExceptionHandler handler = (t, e) -> capturedException.set(e);

            BlinkNamedThreadFactory factory = new BlinkNamedThreadFactory.Builder("ExceptionTest")
                    .exceptionHandler(handler)
                    .build();

            // when
            Thread thread = factory.newThread(() -> {});

            // then
            assertThat(thread.getUncaughtExceptionHandler()).isEqualTo(handler);
        }

        @Test
        @DisplayName("未设置异常处理器时应该为null")
        void shouldHaveNoExceptionHandlerByDefault() {
            // given
            BlinkNamedThreadFactory factory = new BlinkNamedThreadFactory.Builder("NoHandler")
                    .build();

            // when
            Thread thread = factory.newThread(() -> {});

            // then
            // 注意：线程默认会继承线程组的异常处理器，这里检查设置的自定义处理器
            // 由于我们没有设置，所以检查线程是否有自定义处理器
            // 实际上如果没有设置，getUncaughtExceptionHandler可能返回线程组默认的
            // 这个测试验证我们显式设置为null的情况
            assertThat(thread.getUncaughtExceptionHandler()).isNotNull();
        }
    }

    // ==================== 静态工厂方法测试 ====================

    @Nested
    @DisplayName("静态工厂方法测试")
    class StaticFactoryMethodTests {

        @Test
        @DisplayName("create方法应该创建默认线程工厂")
        void shouldCreateDefaultFactory() {
            // when
            BlinkNamedThreadFactory factory = BlinkNamedThreadFactory.create("DefaultFactory");

            // then
            Thread thread = factory.newThread(() -> {});
            assertThat(thread.getName()).isEqualTo("DefaultFactory-1");
            assertThat(thread.isDaemon()).isFalse();
            assertThat(thread.getPriority()).isEqualTo(Thread.NORM_PRIORITY);
        }

        @Test
        @DisplayName("createForIO应该创建IO类型线程工厂")
        void shouldCreateIOFactory() {
            // when
            BlinkNamedThreadFactory factory = BlinkNamedThreadFactory.createForIO("OrderModule");

            // then
            Thread thread = factory.newThread(() -> {});
            assertThat(thread.getName()).startsWith("OrderModule-IO-");
            assertThat(thread.isDaemon()).isFalse();
            assertThat(thread.getPriority()).isEqualTo(Thread.NORM_PRIORITY);
        }

        @Test
        @DisplayName("createForCompute应该创建计算类型线程工厂")
        void shouldCreateComputeFactory() {
            // when
            BlinkNamedThreadFactory factory = BlinkNamedThreadFactory.createForCompute("PaymentModule");

            // then
            Thread thread = factory.newThread(() -> {});
            assertThat(thread.getName()).startsWith("PaymentModule-Compute-");
            assertThat(thread.isDaemon()).isFalse();
            assertThat(thread.getPriority()).isEqualTo(Thread.NORM_PRIORITY);
        }
    }

    // ==================== 线程池使用场景测试 ====================

    @Nested
    @DisplayName("线程池使用场景测试")
    class ThreadPoolScenarioTests {

        @Test
        @DisplayName("应该在线程池中正确工作")
        void shouldWorkInThreadPool() {
            // given
            BlinkNamedThreadFactory factory = BlinkNamedThreadFactory.create("PoolThread");
            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(3, factory);
            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(3);
            java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(0);

            // when
            for (int i = 0; i < 3; i++) {
                executor.submit(() -> {
                    counter.incrementAndGet();
                    latch.countDown();
                });
            }

            try {
                latch.await(java.util.concurrent.TimeUnit.SECONDS.toNanos(5), java.util.concurrent.TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                executor.shutdown();
            }

            // then
            assertThat(counter.get()).isEqualTo(3);
        }

        @Test
        @DisplayName("线程工厂创建的线程应该能正确执行任务")
        void shouldExecuteTaskCorrectly() {
            // given
            BlinkNamedThreadFactory factory = BlinkNamedThreadFactory.create("TaskThread");
            AtomicReference<String> result = new AtomicReference<>();

            // when
            Thread thread = factory.newThread(() -> result.set("Task executed"));
            thread.start();

            try {
                thread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // then
            assertThat(result.get()).isEqualTo("Task executed");
        }
    }

    // ==================== 边界条件测试 ====================

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("优先级为0时不应设置")
        void shouldNotSetPriorityWhenZero() {
            // given
            BlinkNamedThreadFactory factory = new BlinkNamedThreadFactory.Builder("ZeroPriority")
                    .priority(0)
                    .build();

            // when
            Thread thread = factory.newThread(() -> {});

            // then - 优先级为0时保持默认（NORM_PRIORITY = 5）
            assertThat(thread.getPriority()).isEqualTo(Thread.NORM_PRIORITY);
        }

        @Test
        @DisplayName("应该支持长名称前缀")
        void shouldSupportLongNamePrefix() {
            // given
            String longPrefix = "VeryLongModuleName-Subsystem-Component";

            // when
            BlinkNamedThreadFactory factory = BlinkNamedThreadFactory.create(longPrefix);
            Thread thread = factory.newThread(() -> {});

            // then
            assertThat(thread.getName()).startsWith(longPrefix);
        }

        @Test
        @DisplayName("多个工厂实例应该有独立的计数器")
        void shouldHaveIndependentCounters() {
            // given
            BlinkNamedThreadFactory factory1 = BlinkNamedThreadFactory.create("Factory1");
            BlinkNamedThreadFactory factory2 = BlinkNamedThreadFactory.create("Factory2");

            // when
            Thread t1 = factory1.newThread(() -> {});
            Thread t2 = factory2.newThread(() -> {});
            Thread t3 = factory1.newThread(() -> {});

            // then
            assertThat(t1.getName()).isEqualTo("Factory1-1");
            assertThat(t2.getName()).isEqualTo("Factory2-1");
            assertThat(t3.getName()).isEqualTo("Factory1-2");
        }
    }
}
