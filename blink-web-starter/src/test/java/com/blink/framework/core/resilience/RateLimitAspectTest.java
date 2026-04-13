package com.blink.framework.core.resilience;

import com.blink.framework.common.exception.BlinkErrorCodeEnum;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.core.annotation.RateLimit;
import com.blink.framework.core.config.prop.ResilienceProperties;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
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
 * RateLimitAspect 单元测试
 *
 * @author binblink
 */
@DisplayName("RateLimitAspect 单元测试")
@ExtendWith(MockitoExtension.class)
class RateLimitAspectTest {

    private RateLimitAspect aspect;

    @Mock
    private RateLimiterRegistry rateLimiterRegistry;

    @Mock
    private ResilienceProperties properties;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private RateLimit rateLimitAnnotation;

    private ResilienceProperties.RateLimiterConfig rlConfig;
    private ResilienceProperties.RateLimiterTemplate defaultTemplate;

    @BeforeEach
    void setUp() {
        // 设置默认模板
        defaultTemplate = new ResilienceProperties.RateLimiterTemplate();
        defaultTemplate.setLimitForPeriod(100);
        defaultTemplate.setLimitRefreshPeriod(Duration.ofSeconds(1));
        defaultTemplate.setTimeoutDuration(Duration.ZERO);

        rlConfig = new ResilienceProperties.RateLimiterConfig();
        rlConfig.setDefaultConfig(defaultTemplate);

        lenient().when(properties.getRateLimiter()).thenReturn(rlConfig);
        lenient().when(rateLimitAnnotation.name()).thenReturn("test-rate-limiter");
        lenient().when(rateLimitAnnotation.configName()).thenReturn("default");
        lenient().when(rateLimitAnnotation.fallbackMethod()).thenReturn("");
        lenient().when(rateLimitAnnotation.limitForPeriod()).thenReturn(0);
        lenient().when(rateLimitAnnotation.limitRefreshPeriod()).thenReturn(0);
        lenient().when(rateLimitAnnotation.timeoutDuration()).thenReturn(0L);

        aspect = new RateLimitAspect(rateLimiterRegistry, properties);
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

            io.github.resilience4j.ratelimiter.RateLimiter resilience4jRl =
                    io.github.resilience4j.ratelimiter.RateLimiter.of("test", RateLimiterConfig.ofDefaults());
            when(rateLimiterRegistry.rateLimiter(anyString(), any(RateLimiterConfig.class)))
                    .thenReturn(resilience4jRl);

            // Act
            Object result = aspect.around(joinPoint, rateLimitAnnotation);

            // Assert
            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        @DisplayName("限流触发抛出BlinkException")
        void testAround_限流触发抛出异常() {
            // Arrange - 创建一个限流器，限制为每秒1个请求，超时为0
            io.github.resilience4j.ratelimiter.RateLimiter resilience4jRl =
                    io.github.resilience4j.ratelimiter.RateLimiter.of("limited",
                            RateLimiterConfig.custom()
                                    .limitForPeriod(1)
                                    .limitRefreshPeriod(Duration.ofSeconds(1))
                                    .timeoutDuration(Duration.ZERO)
                                    .build());

            when(rateLimiterRegistry.rateLimiter(anyString(), any(RateLimiterConfig.class)))
                    .thenReturn(resilience4jRl);
            when(rateLimitAnnotation.name()).thenReturn("limited");

            // 先消费掉许可
            resilience4jRl.acquirePermission();

            // Act & Assert - 再次调用应该被限流
            assertThatThrownBy(() -> aspect.around(joinPoint, rateLimitAnnotation))
                    .isInstanceOf(BlinkException.class)
                    .hasMessageContaining(BlinkErrorCodeEnum.SYSTEM_BUSY.getCode());
        }

        @Test
        @DisplayName("限流器实例缓存")
        void testAround_限流器实例缓存() throws Throwable {
            // Arrange
            when(joinPoint.proceed()).thenReturn("result");
            when(rateLimitAnnotation.name()).thenReturn("cached-rl");

            io.github.resilience4j.ratelimiter.RateLimiter resilience4jRl =
                    io.github.resilience4j.ratelimiter.RateLimiter.of("cached-rl", RateLimiterConfig.ofDefaults());
            when(rateLimiterRegistry.rateLimiter(anyString(), any(RateLimiterConfig.class)))
                    .thenReturn(resilience4jRl);

            // Act - 调用两次
            aspect.around(joinPoint, rateLimitAnnotation);
            aspect.around(joinPoint, rateLimitAnnotation);

            // Assert - 只创建一次
            verify(rateLimiterRegistry, times(1)).rateLimiter(anyString(), any(RateLimiterConfig.class));
        }

        @Test
        @DisplayName("方法抛出RuntimeException时传播异常")
        void testAround_方法抛出RuntimeException() throws Throwable {
            // Arrange
            RuntimeException expectedException = new RuntimeException("business error");
            lenient().when(joinPoint.proceed()).thenThrow(expectedException);

            io.github.resilience4j.ratelimiter.RateLimiter resilience4jRl =
                    io.github.resilience4j.ratelimiter.RateLimiter.of("test", RateLimiterConfig.ofDefaults());
            when(rateLimiterRegistry.rateLimiter(anyString(), any(RateLimiterConfig.class)))
                    .thenReturn(resilience4jRl);

            // Act & Assert
            assertThatThrownBy(() -> aspect.around(joinPoint, rateLimitAnnotation))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("business error");
        }
    }

    @Nested
    @DisplayName("配置参数测试")
    class ConfigParamTest {

        @Test
        @DisplayName("使用注解参数覆盖模板配置")
        void testBuildRateLimiterConfig_使用注解参数() throws Throwable {
            // Arrange
            when(rateLimitAnnotation.limitForPeriod()).thenReturn(50);
            when(rateLimitAnnotation.limitRefreshPeriod()).thenReturn(2);
            when(rateLimitAnnotation.timeoutDuration()).thenReturn(100L);

            io.github.resilience4j.ratelimiter.RateLimiter resilience4jRl =
                    io.github.resilience4j.ratelimiter.RateLimiter.of("test", RateLimiterConfig.ofDefaults());
            when(rateLimiterRegistry.rateLimiter(anyString(), any(RateLimiterConfig.class)))
                    .thenReturn(resilience4jRl);
            when(joinPoint.proceed()).thenReturn("result");

            // Act
            aspect.around(joinPoint, rateLimitAnnotation);

            // Assert
            verify(rateLimiterRegistry).rateLimiter(anyString(), any(RateLimiterConfig.class));
        }

        @Test
        @DisplayName("注解参数为0时使用模板配置")
        void testBuildRateLimiterConfig_注解参数为0使用模板() throws Throwable {
            // Arrange
            when(rateLimitAnnotation.limitForPeriod()).thenReturn(0);
            when(rateLimitAnnotation.limitRefreshPeriod()).thenReturn(0);
            when(rateLimitAnnotation.timeoutDuration()).thenReturn(0L);

            io.github.resilience4j.ratelimiter.RateLimiter resilience4jRl =
                    io.github.resilience4j.ratelimiter.RateLimiter.of("test", RateLimiterConfig.ofDefaults());
            when(rateLimiterRegistry.rateLimiter(anyString(), any(RateLimiterConfig.class)))
                    .thenReturn(resilience4jRl);
            when(joinPoint.proceed()).thenReturn("result");

            // Act
            aspect.around(joinPoint, rateLimitAnnotation);

            // Assert
            verify(rateLimiterRegistry).rateLimiter(anyString(), any(RateLimiterConfig.class));
        }
    }

    @Nested
    @DisplayName("配置模板测试")
    class ConfigTemplateTest {

        @Test
        @DisplayName("default模板配置")
        void testGetTemplate_default模板() throws Throwable {
            // Arrange
            when(rateLimitAnnotation.configName()).thenReturn("default");

            io.github.resilience4j.ratelimiter.RateLimiter resilience4jRl =
                    io.github.resilience4j.ratelimiter.RateLimiter.of("test", RateLimiterConfig.ofDefaults());
            when(rateLimiterRegistry.rateLimiter(anyString(), any(RateLimiterConfig.class)))
                    .thenReturn(resilience4jRl);
            when(joinPoint.proceed()).thenReturn("result");

            // Act
            aspect.around(joinPoint, rateLimitAnnotation);

            // Assert
            verify(properties).getRateLimiter();
        }

        @Test
        @DisplayName("strict模板配置")
        void testGetTemplate_strict模板() throws Throwable {
            // Arrange
            ResilienceProperties.RateLimiterTemplate strictTemplate = new ResilienceProperties.RateLimiterTemplate();
            strictTemplate.setLimitForPeriod(50);
            strictTemplate.setLimitRefreshPeriod(Duration.ofSeconds(1));
            strictTemplate.setTimeoutDuration(Duration.ZERO);

            rlConfig.setStrictConfig(strictTemplate);
            when(rateLimitAnnotation.configName()).thenReturn("strict");

            io.github.resilience4j.ratelimiter.RateLimiter resilience4jRl =
                    io.github.resilience4j.ratelimiter.RateLimiter.of("test", RateLimiterConfig.ofDefaults());
            when(rateLimiterRegistry.rateLimiter(anyString(), any(RateLimiterConfig.class)))
                    .thenReturn(resilience4jRl);
            when(joinPoint.proceed()).thenReturn("result");

            // Act
            aspect.around(joinPoint, rateLimitAnnotation);

            // Assert
            verify(rateLimiterRegistry).rateLimiter(anyString(), any(RateLimiterConfig.class));
        }

        @Test
        @DisplayName("lenient模板配置")
        void testGetTemplate_lenient模板() throws Throwable {
            // Arrange
            ResilienceProperties.RateLimiterTemplate lenientTemplate = new ResilienceProperties.RateLimiterTemplate();
            lenientTemplate.setLimitForPeriod(200);
            lenientTemplate.setLimitRefreshPeriod(Duration.ofSeconds(1));
            lenientTemplate.setTimeoutDuration(Duration.ofMillis(100));

            rlConfig.setLenientConfig(lenientTemplate);
            when(rateLimitAnnotation.configName()).thenReturn("lenient");

            io.github.resilience4j.ratelimiter.RateLimiter resilience4jRl =
                    io.github.resilience4j.ratelimiter.RateLimiter.of("test", RateLimiterConfig.ofDefaults());
            when(rateLimiterRegistry.rateLimiter(anyString(), any(RateLimiterConfig.class)))
                    .thenReturn(resilience4jRl);
            when(joinPoint.proceed()).thenReturn("result");

            // Act
            aspect.around(joinPoint, rateLimitAnnotation);

            // Assert
            verify(rateLimiterRegistry).rateLimiter(anyString(), any(RateLimiterConfig.class));
        }
    }

    @Nested
    @DisplayName("降级方法测试")
    class FallbackTest {

        @Test
        @DisplayName("限流触发执行降级方法")
        void testAround_限流触发执行降级方法() throws Throwable {
            // Arrange
            when(rateLimitAnnotation.fallbackMethod()).thenReturn("fallbackMethod");
            when(rateLimitAnnotation.name()).thenReturn("fallback-test-rl");

            io.github.resilience4j.ratelimiter.RateLimiter resilience4jRl =
                    io.github.resilience4j.ratelimiter.RateLimiter.of("fallback-test-rl",
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
            Object result = aspect.around(joinPoint, rateLimitAnnotation);

            // Assert
            assertThat(result).isEqualTo("fallback-result");
        }

        @Test
        @DisplayName("降级方法不存在抛出默认异常")
        void testAround_降级方法不存在() {
            // Arrange
            when(rateLimitAnnotation.fallbackMethod()).thenReturn("nonExistMethod");

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
            assertThatThrownBy(() -> aspect.around(joinPoint, rateLimitAnnotation))
                    .isInstanceOf(BlinkException.class);
        }
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
