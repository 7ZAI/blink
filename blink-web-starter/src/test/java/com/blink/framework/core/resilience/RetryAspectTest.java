package com.blink.framework.core.resilience;

import com.blink.framework.common.exception.BlinkErrorCodeEnum;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.core.annotation.Retry;
import com.blink.framework.core.config.prop.ResilienceProperties;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * RetryAspect 单元测试
 *
 * @author binblink
 */
@DisplayName("RetryAspect 单元测试")
@ExtendWith(MockitoExtension.class)
class RetryAspectTest {

    private RetryAspect aspect;

    @Mock
    private RetryRegistry retryRegistry;

    @Mock
    private ResilienceProperties properties;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private Retry retryAnnotation;

    private ResilienceProperties.RetryConfig retryConfig;
    private ResilienceProperties.RetryTemplate defaultTemplate;

    @BeforeEach
    void setUp() {
        // 设置默认模板
        defaultTemplate = new ResilienceProperties.RetryTemplate();
        defaultTemplate.setMaxAttempts(3);
        defaultTemplate.setWaitDuration(Duration.ofMillis(500));

        retryConfig = new ResilienceProperties.RetryConfig();
        retryConfig.setDefaultConfig(defaultTemplate);

        lenient().when(properties.getRetry()).thenReturn(retryConfig);
        lenient().when(retryAnnotation.name()).thenReturn("test-retry");
        lenient().when(retryAnnotation.configName()).thenReturn("default");
        lenient().when(retryAnnotation.fallbackMethod()).thenReturn("");
        lenient().when(retryAnnotation.maxAttempts()).thenReturn(0);
        lenient().when(retryAnnotation.waitDuration()).thenReturn(0L);

        aspect = new RetryAspect(retryRegistry, properties);
    }

    @Nested
    @DisplayName("around 方法测试")
    class AroundTest {

        @Test
        @DisplayName("正常调用直接返回")
        void testAround_正常调用直接返回() throws Throwable {
            // Arrange
            Object expectedResult = "success";
            when(joinPoint.proceed()).thenReturn(expectedResult);

            io.github.resilience4j.retry.Retry resilience4jRetry =
                    io.github.resilience4j.retry.Retry.of("test", RetryConfig.ofDefaults());
            when(retryRegistry.retry(anyString(), any(RetryConfig.class)))
                    .thenReturn(resilience4jRetry);

            // Act
            Object result = aspect.around(joinPoint, retryAnnotation);

            // Assert
            assertThat(result).isEqualTo(expectedResult);
            verify(joinPoint, times(1)).proceed();
        }

        @Test
        @DisplayName("重试器实例缓存")
        void testAround_重试器实例缓存() throws Throwable {
            // Arrange
            when(joinPoint.proceed()).thenReturn("result");
            when(retryAnnotation.name()).thenReturn("cached-retry");

            io.github.resilience4j.retry.Retry resilience4jRetry =
                    io.github.resilience4j.retry.Retry.of("cached-retry", RetryConfig.ofDefaults());
            when(retryRegistry.retry(anyString(), any(RetryConfig.class)))
                    .thenReturn(resilience4jRetry);

            // Act - 调用两次
            aspect.around(joinPoint, retryAnnotation);
            aspect.around(joinPoint, retryAnnotation);

            // Assert - 只创建一次
            verify(retryRegistry, times(1)).retry(anyString(), any(RetryConfig.class));
        }

        @Test
        @DisplayName("方法抛出RuntimeException时传播异常")
        void testAround_方法抛出RuntimeException() throws Throwable {
            // Arrange
            RuntimeException expectedException = new RuntimeException("business error");
            lenient().when(joinPoint.proceed()).thenThrow(expectedException);

            io.github.resilience4j.retry.Retry resilience4jRetry =
                    io.github.resilience4j.retry.Retry.of("test", RetryConfig.ofDefaults());
            when(retryRegistry.retry(anyString(), any(RetryConfig.class)))
                    .thenReturn(resilience4jRetry);

            // Act & Assert - RuntimeException 被包装为 BlinkException
            assertThatThrownBy(() -> aspect.around(joinPoint, retryAnnotation))
                    .isInstanceOf(BlinkException.class);
        }

        @Test
        @DisplayName("重试失败执行降级方法")
        void testAround_重试失败执行降级方法() throws Throwable {
            // Arrange
            when(retryAnnotation.fallbackMethod()).thenReturn("fallbackMethod");
            when(joinPoint.proceed()).thenThrow(new IOException("Connection timeout"));

            io.github.resilience4j.retry.Retry resilience4jRetry =
                    io.github.resilience4j.retry.Retry.of("fallback-test",
                            RetryConfig.custom()
                                    .maxAttempts(2)
                                    .waitDuration(Duration.ofMillis(10))
                                    .retryExceptions(IOException.class, TimeoutException.class)
                                    .ignoreExceptions(IllegalArgumentException.class, BlinkException.class)
                                    .build());
            when(retryRegistry.retry(anyString(), any(RetryConfig.class)))
                    .thenReturn(resilience4jRetry);

            TestService testService = new TestService();
            when(joinPoint.getTarget()).thenReturn(testService);
            lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{});
            when(methodSignature.getParameterTypes()).thenReturn(new Class<?>[0]);
            when(joinPoint.getSignature()).thenReturn(methodSignature);

            // Act
            Object result = aspect.around(joinPoint, retryAnnotation);

            // Assert
            assertThat(result).isEqualTo("fallback-result");
        }
    }

    @Nested
    @DisplayName("配置参数测试")
    class ConfigParamTest {

        @Test
        @DisplayName("使用注解参数覆盖模板配置")
        void testBuildRetryConfig_使用注解参数() throws Throwable {
            // Arrange
            when(retryAnnotation.maxAttempts()).thenReturn(5);
            when(retryAnnotation.waitDuration()).thenReturn(1000L);
            when(joinPoint.proceed()).thenReturn("result");

            io.github.resilience4j.retry.Retry resilience4jRetry =
                    io.github.resilience4j.retry.Retry.of("test", RetryConfig.ofDefaults());
            when(retryRegistry.retry(anyString(), any(RetryConfig.class)))
                    .thenReturn(resilience4jRetry);

            // Act
            aspect.around(joinPoint, retryAnnotation);

            // Assert
            verify(retryRegistry).retry(anyString(), any(RetryConfig.class));
        }

        @Test
        @DisplayName("注解参数为0时使用模板配置")
        void testBuildRetryConfig_注解参数为0使用模板() throws Throwable {
            // Arrange
            when(retryAnnotation.maxAttempts()).thenReturn(0);
            when(retryAnnotation.waitDuration()).thenReturn(0L);
            when(joinPoint.proceed()).thenReturn("result");

            io.github.resilience4j.retry.Retry resilience4jRetry =
                    io.github.resilience4j.retry.Retry.of("test", RetryConfig.ofDefaults());
            when(retryRegistry.retry(anyString(), any(RetryConfig.class)))
                    .thenReturn(resilience4jRetry);

            // Act
            aspect.around(joinPoint, retryAnnotation);

            // Assert
            verify(retryRegistry).retry(anyString(), any(RetryConfig.class));
        }
    }

    @Nested
    @DisplayName("配置模板测试")
    class ConfigTemplateTest {

        @Test
        @DisplayName("quick模板配置")
        void testGetTemplate_quick模板() throws Throwable {
            // Arrange
            ResilienceProperties.RetryTemplate quickTemplate = new ResilienceProperties.RetryTemplate();
            quickTemplate.setMaxAttempts(2);
            quickTemplate.setWaitDuration(Duration.ofMillis(200));

            retryConfig.setQuickConfig(quickTemplate);
            when(retryAnnotation.configName()).thenReturn("quick");

            io.github.resilience4j.retry.Retry resilience4jRetry =
                    io.github.resilience4j.retry.Retry.of("test", RetryConfig.ofDefaults());
            when(retryRegistry.retry(anyString(), any(RetryConfig.class)))
                    .thenReturn(resilience4jRetry);
            when(joinPoint.proceed()).thenReturn("result");

            // Act
            aspect.around(joinPoint, retryAnnotation);

            // Assert
            verify(retryRegistry).retry(anyString(), any(RetryConfig.class));
        }

        @Test
        @DisplayName("slow模板配置")
        void testGetTemplate_slow模板() throws Throwable {
            // Arrange
            ResilienceProperties.RetryTemplate slowTemplate = new ResilienceProperties.RetryTemplate();
            slowTemplate.setMaxAttempts(5);
            slowTemplate.setWaitDuration(Duration.ofMillis(1000));

            retryConfig.setSlowConfig(slowTemplate);
            when(retryAnnotation.configName()).thenReturn("slow");

            io.github.resilience4j.retry.Retry resilience4jRetry =
                    io.github.resilience4j.retry.Retry.of("test", RetryConfig.ofDefaults());
            when(retryRegistry.retry(anyString(), any(RetryConfig.class)))
                    .thenReturn(resilience4jRetry);
            when(joinPoint.proceed()).thenReturn("result");

            // Act
            aspect.around(joinPoint, retryAnnotation);

            // Assert
            verify(retryRegistry).retry(anyString(), any(RetryConfig.class));
        }
    }

    @Nested
    @DisplayName("降级方法测试")
    class FallbackTest {

        @Test
        @DisplayName("降级方法不存在抛出默认异常")
        void testAround_降级方法不存在() throws Throwable {
            // Arrange
            when(retryAnnotation.fallbackMethod()).thenReturn("nonExistMethod");
            when(joinPoint.proceed()).thenThrow(new IOException("Error"));

            io.github.resilience4j.retry.Retry resilience4jRetry =
                    io.github.resilience4j.retry.Retry.of("test",
                            RetryConfig.custom()
                                    .maxAttempts(2)
                                    .waitDuration(Duration.ofMillis(10))
                                    .retryExceptions(IOException.class)
                                    .build());
            when(retryRegistry.retry(anyString(), any(RetryConfig.class)))
                    .thenReturn(resilience4jRetry);

            TestService testService = new TestService();
            when(joinPoint.getTarget()).thenReturn(testService);
            lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{});
            when(methodSignature.getParameterTypes()).thenReturn(new Class<?>[0]);
            when(joinPoint.getSignature()).thenReturn(methodSignature);

            // Act & Assert
            assertThatThrownBy(() -> aspect.around(joinPoint, retryAnnotation))
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
