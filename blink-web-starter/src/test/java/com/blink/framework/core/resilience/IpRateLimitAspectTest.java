package com.blink.framework.core.resilience;

import com.blink.framework.common.context.BlinkRequestContext;
import com.blink.framework.common.context.BlinkRequestContextHolder;
import com.blink.framework.common.exception.BlinkErrorCodeEnum;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.core.annotation.IpRateLimit;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * IpRateLimitAspect 单元测试
 *
 * @author binblink
 */
@DisplayName("IpRateLimitAspect 单元测试")
@ExtendWith(MockitoExtension.class)
class IpRateLimitAspectTest {

    private IpRateLimitAspect aspect;

    @Mock
    private RateLimiterRegistry rateLimiterRegistry;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private IpRateLimit ipRateLimitAnnotation;

    @BeforeEach
    void setUp() {
        lenient().when(ipRateLimitAnnotation.name()).thenReturn("test-ip-rate-limiter");
        lenient().when(ipRateLimitAnnotation.fallbackMethod()).thenReturn("");
        lenient().when(ipRateLimitAnnotation.limitForPeriod()).thenReturn(10);
        lenient().when(ipRateLimitAnnotation.limitRefreshPeriod()).thenReturn(1);
        lenient().when(ipRateLimitAnnotation.timeoutDuration()).thenReturn(0L);

        aspect = new IpRateLimitAspect(rateLimiterRegistry);
        BlinkRequestContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        BlinkRequestContextHolder.clearContext();
    }

    @Nested
    @DisplayName("around 方法测试")
    class AroundTest {

        @Test
        @DisplayName("正常调用返回结果")
        void testAround_正常调用返回结果() throws Throwable {
            // Arrange
            Object expectedResult = "success";
            when(joinPoint.proceed()).thenReturn(expectedResult);

            setClientIp("192.168.1.1");

            io.github.resilience4j.ratelimiter.RateLimiter resilience4jRl =
                    io.github.resilience4j.ratelimiter.RateLimiter.of("test", RateLimiterConfig.ofDefaults());
            when(rateLimiterRegistry.rateLimiter(anyString(), any(RateLimiterConfig.class)))
                    .thenReturn(resilience4jRl);

            // Act
            Object result = aspect.around(joinPoint, ipRateLimitAnnotation);

            // Assert
            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        @DisplayName("从Context获取IP")
        void testGetClientIp_从Context获取() throws Throwable {
            // Arrange
            String expectedIp = "10.0.0.100";
            setClientIp(expectedIp);
            when(joinPoint.proceed()).thenReturn("result");

            io.github.resilience4j.ratelimiter.RateLimiter resilience4jRl =
                    io.github.resilience4j.ratelimiter.RateLimiter.of("test", RateLimiterConfig.ofDefaults());
            when(rateLimiterRegistry.rateLimiter(anyString(), any(RateLimiterConfig.class)))
                    .thenReturn(resilience4jRl);

            // Act
            aspect.around(joinPoint, ipRateLimitAnnotation);

            // Assert - 验证限流器名称包含IP
            verify(rateLimiterRegistry).rateLimiter(contains(expectedIp), any(RateLimiterConfig.class));
        }

        @Test
        @DisplayName("Context为空返回unknown")
        void testGetClientIp_Context为空返回unknown() throws Throwable {
            // Arrange - 不设置IP
            BlinkRequestContextHolder.clearContext();
            when(joinPoint.proceed()).thenReturn("result");

            io.github.resilience4j.ratelimiter.RateLimiter resilience4jRl =
                    io.github.resilience4j.ratelimiter.RateLimiter.of("test", RateLimiterConfig.ofDefaults());
            when(rateLimiterRegistry.rateLimiter(anyString(), any(RateLimiterConfig.class)))
                    .thenReturn(resilience4jRl);

            // Act
            aspect.around(joinPoint, ipRateLimitAnnotation);

            // Assert - 验证限流器名称包含unknown
            verify(rateLimiterRegistry).rateLimiter(contains("unknown"), any(RateLimiterConfig.class));
        }

        @Test
        @DisplayName("按IP限流触发")
        void testAround_按IP限流触发() {
            // Arrange
            String clientIp = "192.168.1.50";
            setClientIp(clientIp);

            io.github.resilience4j.ratelimiter.RateLimiter resilience4jRl =
                    io.github.resilience4j.ratelimiter.RateLimiter.of("limited-ip",
                            RateLimiterConfig.custom()
                                    .limitForPeriod(1)
                                    .limitRefreshPeriod(Duration.ofSeconds(1))
                                    .timeoutDuration(Duration.ZERO)
                                    .build());

            when(rateLimiterRegistry.rateLimiter(anyString(), any(RateLimiterConfig.class)))
                    .thenReturn(resilience4jRl);
            when(ipRateLimitAnnotation.name()).thenReturn("limited-ip");

            // 先消费掉许可
            resilience4jRl.acquirePermission();

            // Act & Assert - 再次调用应该被限流
            assertThatThrownBy(() -> aspect.around(joinPoint, ipRateLimitAnnotation))
                    .isInstanceOf(BlinkException.class)
                    .hasMessageContaining(BlinkErrorCodeEnum.SYSTEM_BUSY.getCode());
        }

        @Test
        @DisplayName("不同IP独立限流")
        void testAround_不同IP独立限流() throws Throwable {
            // Arrange
            when(joinPoint.proceed()).thenReturn("result");

            io.github.resilience4j.ratelimiter.RateLimiter resilience4jRl =
                    io.github.resilience4j.ratelimiter.RateLimiter.of("test", RateLimiterConfig.ofDefaults());
            when(rateLimiterRegistry.rateLimiter(anyString(), any(RateLimiterConfig.class)))
                    .thenReturn(resilience4jRl);

            // Act - 使用不同IP调用
            setClientIp("192.168.1.1");
            aspect.around(joinPoint, ipRateLimitAnnotation);

            setClientIp("192.168.1.2");
            aspect.around(joinPoint, ipRateLimitAnnotation);

            // Assert - 两个不同IP创建不同的限流器
            verify(rateLimiterRegistry).rateLimiter(contains("192.168.1.1"), any(RateLimiterConfig.class));
            verify(rateLimiterRegistry).rateLimiter(contains("192.168.1.2"), any(RateLimiterConfig.class));
        }
    }

    @Nested
    @DisplayName("降级方法测试")
    class FallbackTest {

        @Test
        @DisplayName("限流触发执行降级方法")
        void testAround_限流触发执行降级方法() throws Throwable {
            // Arrange
            when(ipRateLimitAnnotation.fallbackMethod()).thenReturn("fallbackMethod");
            when(ipRateLimitAnnotation.name()).thenReturn("fallback-ip-rl");

            setClientIp("192.168.1.100");

            io.github.resilience4j.ratelimiter.RateLimiter resilience4jRl =
                    io.github.resilience4j.ratelimiter.RateLimiter.of("fallback-ip-rl",
                            RateLimiterConfig.custom()
                                    .limitForPeriod(1)
                                    .limitRefreshPeriod(Duration.ofSeconds(1))
                                    .timeoutDuration(Duration.ZERO)
                                    .build());

            // 先消费掉许可
            resilience4jRl.acquirePermission();

            when(rateLimiterRegistry.rateLimiter(anyString(), any(RateLimiterConfig.class)))
                    .thenReturn(resilience4jRl);

            TestService testService = new TestService();
            when(joinPoint.getTarget()).thenReturn(testService);
            lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{});
            when(methodSignature.getParameterTypes()).thenReturn(new Class<?>[0]);
            when(joinPoint.getSignature()).thenReturn(methodSignature);

            // Act
            Object result = aspect.around(joinPoint, ipRateLimitAnnotation);

            // Assert
            assertThat(result).isEqualTo("fallback-result");
        }

        @Test
        @DisplayName("降级方法不存在抛出默认异常")
        void testAround_降级方法不存在() {
            // Arrange
            when(ipRateLimitAnnotation.fallbackMethod()).thenReturn("nonExistMethod");

            setClientIp("192.168.1.101");

            io.github.resilience4j.ratelimiter.RateLimiter resilience4jRl =
                    io.github.resilience4j.ratelimiter.RateLimiter.of("test",
                            RateLimiterConfig.custom()
                                    .limitForPeriod(1)
                                    .limitRefreshPeriod(Duration.ofSeconds(1))
                                    .timeoutDuration(Duration.ZERO)
                                    .build());

            // 先消费掉许可
            resilience4jRl.acquirePermission();

            when(rateLimiterRegistry.rateLimiter(anyString(), any(RateLimiterConfig.class)))
                    .thenReturn(resilience4jRl);

            TestService testService = new TestService();
            when(joinPoint.getTarget()).thenReturn(testService);
            lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{});
            when(methodSignature.getParameterTypes()).thenReturn(new Class<?>[0]);
            when(joinPoint.getSignature()).thenReturn(methodSignature);

            // Act & Assert
            assertThatThrownBy(() -> aspect.around(joinPoint, ipRateLimitAnnotation))
                    .isInstanceOf(BlinkException.class);
        }
    }

    /**
     * 设置客户端IP到Context
     */
    private void setClientIp(String ip) {
        BlinkRequestContext context = new BlinkRequestContext();
        context.setClientIp(ip);
        BlinkRequestContextHolder.setContext(context);
    }

    /**
     * 测试服务类 - 用于降级方法测试
     */
    @SuppressWarnings("unused")
    static class TestService {
        public String fallbackMethod(Throwable e) {
            return "fallback-result";
        }
    }
}
