package com.blink.framework.common.context;

import com.blink.framework.test.annotation.UnitTest;
import com.blink.framework.test.base.BlinkUnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * BlinkRequestContext 和 BlinkRequestContextHolder 单元测试
 * <p>
 * 测试覆盖：
 * 1. BlinkRequestContext 属性存取
 * 2. BlinkRequestContextHolder ThreadLocal 管理
 * 3. 上下文生命周期
 * 4. 静态便捷方法
 *
 * @author binblink
 */
@UnitTest
@DisplayName("BlinkRequestContext 请求上下文测试")
class BlinkRequestContextTest extends BlinkUnitTest {

    @AfterEach
    void tearDown() {
        // 每个测试后清理上下文，避免测试间干扰
        BlinkRequestContextHolder.clearContext();
    }

    // ==================== BlinkRequestContext 测试 ====================

    @Nested
    @DisplayName("BlinkRequestContext 属性测试")
    class BlinkRequestContextPropertyTests {

        @Test
        @DisplayName("应该正确设置和获取所有属性")
        void shouldSetAndGetAllProperties() {
            // given
            BlinkRequestContext context = new BlinkRequestContext();

            // when
            context.setRequestId("req-12345");
            context.setTraceId("trace-67890");
            context.setSpanId("span-abc");
            context.setUserId("user-001");
            context.setLoginName("admin");
            context.setClientIp("192.168.1.100");
            context.setLanguage("zh-CN");
            context.setAppName("blink-base");
            context.setRequestDate(LocalDate.of(2024, 1, 15));
            context.setChannel("web");

            // then
            assertThat(context.getRequestId()).isEqualTo("req-12345");
            assertThat(context.getTraceId()).isEqualTo("trace-67890");
            assertThat(context.getSpanId()).isEqualTo("span-abc");
            assertThat(context.getUserId()).isEqualTo("user-001");
            assertThat(context.getLoginName()).isEqualTo("admin");
            assertThat(context.getClientIp()).isEqualTo("192.168.1.100");
            assertThat(context.getLanguage()).isEqualTo("zh-CN");
            assertThat(context.getAppName()).isEqualTo("blink-base");
            assertThat(context.getRequestDate()).isEqualTo(LocalDate.of(2024, 1, 15));
            assertThat(context.getChannel()).isEqualTo("web");
        }

        @Test
        @DisplayName("新创建的上下文属性应该为null")
        void shouldHaveNullPropertiesForNewContext() {
            // when
            BlinkRequestContext context = new BlinkRequestContext();

            // then
            assertThat(context.getRequestId()).isNull();
            assertThat(context.getTraceId()).isNull();
            assertThat(context.getSpanId()).isNull();
            assertThat(context.getUserId()).isNull();
            assertThat(context.getLoginName()).isNull();
            assertThat(context.getClientIp()).isNull();
            assertThat(context.getLanguage()).isNull();
            assertThat(context.getAppName()).isNull();
            assertThat(context.getRequestDate()).isNull();
            assertThat(context.getChannel()).isNull();
        }
    }

    // ==================== BlinkRequestContextHolder 测试 ====================

    @Nested
    @DisplayName("BlinkRequestContextHolder 上下文管理测试")
    class BlinkRequestContextHolderTests {

        @Test
        @DisplayName("应该正确设置和获取上下文")
        void shouldSetAndGetContext() {
            // given
            BlinkRequestContext context = new BlinkRequestContext();
            context.setRequestId("req-001");
            context.setLoginName("testuser");

            // when
            BlinkRequestContextHolder.setContext(context);
            BlinkRequestContext retrieved = BlinkRequestContextHolder.getContext();

            // then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getRequestId()).isEqualTo("req-001");
            assertThat(retrieved.getLoginName()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("没有设置上下文时应该返回空上下文")
        void shouldReturnEmptyContextWhenNotSet() {
            // when
            BlinkRequestContext context = BlinkRequestContextHolder.getContext();

            // then
            assertThat(context).isNotNull();
            assertThat(context.getRequestId()).isNull();
        }

        @Test
        @DisplayName("应该正确清除上下文")
        void shouldClearContext() {
            // given
            BlinkRequestContext context = new BlinkRequestContext();
            context.setRequestId("req-002");
            BlinkRequestContextHolder.setContext(context);

            // when
            BlinkRequestContextHolder.clearContext();
            BlinkRequestContext afterClear = BlinkRequestContextHolder.getContext();

            // then - 清除后获取的是新的空上下文
            assertThat(afterClear.getRequestId()).isNull();
        }

        @Test
        @DisplayName("设置null上下文应该抛出异常")
        void shouldThrowExceptionWhenSetNullContext() {
            // when & then
            assertThatThrownBy(() -> BlinkRequestContextHolder.setContext(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Only non-null RequestContext instances are permitted");
        }

        @Test
        @DisplayName("多次获取上下文应该返回同一个实例")
        void shouldReturnSameContextInstance() {
            // given
            BlinkRequestContext context = new BlinkRequestContext();
            context.setRequestId("req-003");
            BlinkRequestContextHolder.setContext(context);

            // when
            BlinkRequestContext first = BlinkRequestContextHolder.getContext();
            BlinkRequestContext second = BlinkRequestContextHolder.getContext();

            // then
            assertThat(first).isSameAs(second);
        }
    }

    // ==================== 静态便捷方法测试 ====================

    @Nested
    @DisplayName("静态便捷方法测试")
    class StaticConvenienceMethodTests {

        @Test
        @DisplayName("应该通过静态方法获取RequestId")
        void shouldGetRequestIdStatically() {
            // given
            BlinkRequestContext context = new BlinkRequestContext();
            context.setRequestId("static-req-001");
            BlinkRequestContextHolder.setContext(context);

            // when & then
            assertThat(BlinkRequestContextHolder.getRequestId()).isEqualTo("static-req-001");
        }

        @Test
        @DisplayName("应该通过静态方法获取TraceId")
        void shouldGetTraceIdStatically() {
            // given
            BlinkRequestContext context = new BlinkRequestContext();
            context.setTraceId("trace-001");
            BlinkRequestContextHolder.setContext(context);

            // when & then
            assertThat(BlinkRequestContextHolder.getTraceId()).isEqualTo("trace-001");
        }

        @Test
        @DisplayName("应该通过静态方法获取UserId")
        void shouldGetUserIdStatically() {
            // given
            BlinkRequestContext context = new BlinkRequestContext();
            context.setUserId("user-123");
            BlinkRequestContextHolder.setContext(context);

            // when & then
            assertThat(BlinkRequestContextHolder.getUserId()).isEqualTo("user-123");
        }

        @Test
        @DisplayName("应该通过静态方法获取LoginName")
        void shouldGetLoginNameStatically() {
            // given
            BlinkRequestContext context = new BlinkRequestContext();
            context.setLoginName("admin");
            BlinkRequestContextHolder.setContext(context);

            // when & then
            assertThat(BlinkRequestContextHolder.getLoginName()).isEqualTo("admin");
        }

        @Test
        @DisplayName("应该通过静态方法获取ClientIp")
        void shouldGetClientIpStatically() {
            // given
            BlinkRequestContext context = new BlinkRequestContext();
            context.setClientIp("10.0.0.1");
            BlinkRequestContextHolder.setContext(context);

            // when & then
            assertThat(BlinkRequestContextHolder.getClientIp()).isEqualTo("10.0.0.1");
        }

        @Test
        @DisplayName("应该通过静态方法获取Language")
        void shouldGetLanguageStatically() {
            // given
            BlinkRequestContext context = new BlinkRequestContext();
            context.setLanguage("en-US");
            BlinkRequestContextHolder.setContext(context);

            // when & then
            assertThat(BlinkRequestContextHolder.getLanguage()).isEqualTo("en-US");
        }

        @Test
        @DisplayName("应该通过静态方法获取AppName")
        void shouldGetAppNameStatically() {
            // given
            BlinkRequestContext context = new BlinkRequestContext();
            context.setAppName("blink-gateway");
            BlinkRequestContextHolder.setContext(context);

            // when & then
            assertThat(BlinkRequestContextHolder.getAppName()).isEqualTo("blink-gateway");
        }

        @Test
        @DisplayName("应该通过静态方法获取RequestDate")
        void shouldGetRequestDateStatically() {
            // given
            LocalDate date = LocalDate.of(2024, 6, 15);
            BlinkRequestContext context = new BlinkRequestContext();
            context.setRequestDate(date);
            BlinkRequestContextHolder.setContext(context);

            // when & then
            assertThat(BlinkRequestContextHolder.getRequestDate()).isEqualTo(date);
        }

        @Test
        @DisplayName("应该通过静态方法获取Channel")
        void shouldGetChannelStatically() {
            // given
            BlinkRequestContext context = new BlinkRequestContext();
            context.setChannel("mobile");
            BlinkRequestContextHolder.setContext(context);

            // when & then
            assertThat(BlinkRequestContextHolder.getChannel()).isEqualTo("mobile");
        }

        @Test
        @DisplayName("应该通过静态方法获取SpanId")
        void shouldGetSpanIdStatically() {
            // given
            BlinkRequestContext context = new BlinkRequestContext();
            context.setSpanId("span-xyz");
            BlinkRequestContextHolder.setContext(context);

            // when & then
            assertThat(BlinkRequestContextHolder.getSpanId()).isEqualTo("span-xyz");
        }
    }

    // ==================== 多线程隔离测试 ====================

    @Nested
    @DisplayName("多线程隔离测试")
    class ThreadIsolationTests {

        @Test
        @DisplayName("不同线程应该有独立的上下文")
        void shouldHaveIndependentContextInDifferentThreads() throws InterruptedException {
            // given
            BlinkRequestContext mainContext = new BlinkRequestContext();
            mainContext.setRequestId("main-thread-req");
            BlinkRequestContextHolder.setContext(mainContext);

            // when - 在另一个线程中设置不同的上下文
            Thread otherThread = new Thread(() -> {
                BlinkRequestContext threadContext = new BlinkRequestContext();
                threadContext.setRequestId("other-thread-req");
                BlinkRequestContextHolder.setContext(threadContext);

                // 在子线程中验证
                assertThat(BlinkRequestContextHolder.getRequestId()).isEqualTo("other-thread-req");
            });

            otherThread.start();
            otherThread.join();

            // then - 主线程的上下文不受影响
            assertThat(BlinkRequestContextHolder.getRequestId()).isEqualTo("main-thread-req");
        }

        @Test
        @DisplayName("子线程清除上下文不应影响主线程")
        void clearingContextInChildThreadShouldNotAffectMainThread() throws InterruptedException {
            // given
            BlinkRequestContext mainContext = new BlinkRequestContext();
            mainContext.setRequestId("main-req");
            BlinkRequestContextHolder.setContext(mainContext);

            // when - 在子线程中清除上下文
            Thread otherThread = new Thread(() -> {
                BlinkRequestContextHolder.clearContext();
            });

            otherThread.start();
            otherThread.join();

            // then - 主线程的上下文仍然存在
            assertThat(BlinkRequestContextHolder.getRequestId()).isEqualTo("main-req");
        }
    }
}
