package com.blink.framework.core.resilience;

import com.blink.framework.common.exception.BlinkErrorCodeEnum;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.core.annotation.CircuitBreaker;
import com.blink.framework.core.config.prop.ResilienceProperties;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * CircuitBreakerAspect 单元测试
 *
 * @author binblink
 */
@DisplayName("CircuitBreakerAspect 单元测试")
@ExtendWith(MockitoExtension.class)
class CircuitBreakerAspectTest {

    private CircuitBreakerAspect aspect;

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Mock
    private ResilienceProperties properties;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private CircuitBreaker circuitBreakerAnnotation;

    private ResilienceProperties.CircuitBreakerConfig cbConfig;
    private ResilienceProperties.CircuitBreakerTemplate defaultTemplate;

    @BeforeEach
    void setUp() {
        // 设置默认模板
        defaultTemplate = new ResilienceProperties.CircuitBreakerTemplate();
        defaultTemplate.setSlidingWindowSize(10);
        defaultTemplate.setMinimumNumberOfCalls(5);
        defaultTemplate.setFailureRateThreshold(50);
        defaultTemplate.setSlowCallRateThreshold(100);
        defaultTemplate.setSlowCallDurationThreshold(Duration.ofSeconds(8));
        defaultTemplate.setWaitDurationInOpenState(Duration.ofSeconds(60));
        defaultTemplate.setPermittedNumberOfCallsInHalfOpenState(3);

        cbConfig = new ResilienceProperties.CircuitBreakerConfig();
        cbConfig.setDefaultConfig(defaultTemplate);

        lenient().when(properties.getCircuitBreaker()).thenReturn(cbConfig);
        lenient().when(circuitBreakerAnnotation.name()).thenReturn("test-circuit-breaker");
        lenient().when(circuitBreakerAnnotation.configName()).thenReturn("default");
        lenient().when(circuitBreakerAnnotation.fallbackMethod()).thenReturn("");

        aspect = new CircuitBreakerAspect(circuitBreakerRegistry, properties);
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

            io.github.resilience4j.circuitbreaker.CircuitBreaker resilience4jCb =
                    io.github.resilience4j.circuitbreaker.CircuitBreaker.of("test", CircuitBreakerConfig.ofDefaults());
            when(circuitBreakerRegistry.circuitBreaker(anyString(), any(CircuitBreakerConfig.class)))
                    .thenReturn(resilience4jCb);

            // Act
            Object result = aspect.around(joinPoint, circuitBreakerAnnotation);

            // Assert
            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        @DisplayName("熔断器触发抛出BlinkException")
        void testAround_熔断器触发抛出异常() {
            // Arrange
            io.github.resilience4j.circuitbreaker.CircuitBreaker resilience4jCb =
                    io.github.resilience4j.circuitbreaker.CircuitBreaker.of("test",
                            CircuitBreakerConfig.custom()
                                    .slidingWindowSize(5)
                                    .minimumNumberOfCalls(1)
                                    .failureRateThreshold(1)
                                    .build());

            // 强制打开熔断器
            resilience4jCb.transitionToOpenState();

            when(circuitBreakerRegistry.circuitBreaker(anyString(), any(CircuitBreakerConfig.class)))
                    .thenReturn(resilience4jCb);

            // Act & Assert
            assertThatThrownBy(() -> aspect.around(joinPoint, circuitBreakerAnnotation))
                    .isInstanceOf(BlinkException.class)
                    .hasMessageContaining(BlinkErrorCodeEnum.SERVER_NOT_AVAILABLE.getCode());
        }

        @Test
        @DisplayName("熔断器实例缓存")
        void testAround_熔断器实例缓存() throws Throwable {
            // Arrange
            when(joinPoint.proceed()).thenReturn("result");
            when(circuitBreakerAnnotation.name()).thenReturn("cached-cb");

            io.github.resilience4j.circuitbreaker.CircuitBreaker resilience4jCb =
                    io.github.resilience4j.circuitbreaker.CircuitBreaker.of("cached-cb", CircuitBreakerConfig.ofDefaults());
            when(circuitBreakerRegistry.circuitBreaker(anyString(), any(CircuitBreakerConfig.class)))
                    .thenReturn(resilience4jCb);

            // Act - 调用两次
            aspect.around(joinPoint, circuitBreakerAnnotation);
            aspect.around(joinPoint, circuitBreakerAnnotation);

            // Assert - 只创建一次
            verify(circuitBreakerRegistry, times(1)).circuitBreaker(anyString(), any(CircuitBreakerConfig.class));
        }

        @Test
        @DisplayName("方法抛出RuntimeException时传播异常")
        void testAround_方法抛出RuntimeException() throws Throwable {
            // Arrange
            RuntimeException expectedException = new RuntimeException("business error");
            lenient().when(joinPoint.proceed()).thenThrow(expectedException);

            io.github.resilience4j.circuitbreaker.CircuitBreaker resilience4jCb =
                    io.github.resilience4j.circuitbreaker.CircuitBreaker.of("test", CircuitBreakerConfig.ofDefaults());
            when(circuitBreakerRegistry.circuitBreaker(anyString(), any(CircuitBreakerConfig.class)))
                    .thenReturn(resilience4jCb);

            // Act & Assert
            assertThatThrownBy(() -> aspect.around(joinPoint, circuitBreakerAnnotation))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("business error");
        }
    }

    @Nested
    @DisplayName("配置模板测试")
    class ConfigTemplateTest {

        @Test
        @DisplayName("default模板配置正确")
        void testBuildCircuitBreakerConfig_default模板() throws Throwable {
            // Arrange
            when(joinPoint.proceed()).thenReturn("result");
            when(circuitBreakerAnnotation.configName()).thenReturn("default");

            io.github.resilience4j.circuitbreaker.CircuitBreaker resilience4jCb =
                    io.github.resilience4j.circuitbreaker.CircuitBreaker.of("test", CircuitBreakerConfig.ofDefaults());
            when(circuitBreakerRegistry.circuitBreaker(anyString(), any(CircuitBreakerConfig.class)))
                    .thenReturn(resilience4jCb);

            // Act
            aspect.around(joinPoint, circuitBreakerAnnotation);

            // Assert
            verify(circuitBreakerRegistry).circuitBreaker(anyString(), any(CircuitBreakerConfig.class));
        }

        @Test
        @DisplayName("strict模板配置正确")
        void testBuildCircuitBreakerConfig_strict模板() throws Throwable {
            // Arrange
            ResilienceProperties.CircuitBreakerTemplate strictTemplate = new ResilienceProperties.CircuitBreakerTemplate();
            strictTemplate.setSlidingWindowSize(10);
            strictTemplate.setMinimumNumberOfCalls(5);
            strictTemplate.setFailureRateThreshold(30);
            strictTemplate.setSlowCallRateThreshold(50);
            strictTemplate.setSlowCallDurationThreshold(Duration.ofSeconds(3));
            strictTemplate.setWaitDurationInOpenState(Duration.ofSeconds(120));
            strictTemplate.setPermittedNumberOfCallsInHalfOpenState(2);

            cbConfig.setStrictConfig(strictTemplate);
            when(circuitBreakerAnnotation.configName()).thenReturn("strict");

            io.github.resilience4j.circuitbreaker.CircuitBreaker resilience4jCb =
                    io.github.resilience4j.circuitbreaker.CircuitBreaker.of("test", CircuitBreakerConfig.ofDefaults());
            when(circuitBreakerRegistry.circuitBreaker(anyString(), any(CircuitBreakerConfig.class)))
                    .thenReturn(resilience4jCb);
            when(joinPoint.proceed()).thenReturn("result");

            // Act
            aspect.around(joinPoint, circuitBreakerAnnotation);

            // Assert
            verify(circuitBreakerRegistry).circuitBreaker(anyString(), any(CircuitBreakerConfig.class));
        }

        @Test
        @DisplayName("lenient模板配置正确")
        void testBuildCircuitBreakerConfig_lenient模板() throws Throwable {
            // Arrange
            ResilienceProperties.CircuitBreakerTemplate lenientTemplate = new ResilienceProperties.CircuitBreakerTemplate();
            lenientTemplate.setSlidingWindowSize(20);
            lenientTemplate.setMinimumNumberOfCalls(10);
            lenientTemplate.setFailureRateThreshold(70);
            lenientTemplate.setSlowCallRateThreshold(100);
            lenientTemplate.setSlowCallDurationThreshold(Duration.ofSeconds(15));
            lenientTemplate.setWaitDurationInOpenState(Duration.ofSeconds(30));
            lenientTemplate.setPermittedNumberOfCallsInHalfOpenState(5);

            cbConfig.setLenientConfig(lenientTemplate);
            when(circuitBreakerAnnotation.configName()).thenReturn("lenient");

            io.github.resilience4j.circuitbreaker.CircuitBreaker resilience4jCb =
                    io.github.resilience4j.circuitbreaker.CircuitBreaker.of("test", CircuitBreakerConfig.ofDefaults());
            when(circuitBreakerRegistry.circuitBreaker(anyString(), any(CircuitBreakerConfig.class)))
                    .thenReturn(resilience4jCb);
            when(joinPoint.proceed()).thenReturn("result");

            // Act
            aspect.around(joinPoint, circuitBreakerAnnotation);

            // Assert
            verify(circuitBreakerRegistry).circuitBreaker(anyString(), any(CircuitBreakerConfig.class));
        }
    }

    @Nested
    @DisplayName("降级方法测试")
    class FallbackTest {

        @Test
        @DisplayName("熔断触发执行降级方法")
        void testAround_熔断触发执行降级方法() throws Throwable {
            // Arrange
            when(circuitBreakerAnnotation.fallbackMethod()).thenReturn("fallbackMethod");
            when(circuitBreakerAnnotation.name()).thenReturn("fallback-test");

            io.github.resilience4j.circuitbreaker.CircuitBreaker resilience4jCb =
                    io.github.resilience4j.circuitbreaker.CircuitBreaker.of("fallback-test",
                            CircuitBreakerConfig.custom()
                                    .slidingWindowSize(5)
                                    .minimumNumberOfCalls(1)
                                    .failureRateThreshold(1)
                                    .build());
            resilience4jCb.transitionToOpenState();

            when(circuitBreakerRegistry.circuitBreaker(anyString(), any(CircuitBreakerConfig.class)))
                    .thenReturn(resilience4jCb);

            // 创建一个测试目标类
            TestService testService = new TestService();
            when(joinPoint.getTarget()).thenReturn(testService);
            when(joinPoint.getArgs()).thenReturn(new Object[]{});
            when(methodSignature.getParameterTypes()).thenReturn(new Class<?>[0]);
            when(joinPoint.getSignature()).thenReturn(methodSignature);

            // Act
            Object result = aspect.around(joinPoint, circuitBreakerAnnotation);

            // Assert
            assertThat(result).isEqualTo("fallback-result");
        }

        @Test
        @DisplayName("降级方法不存在抛出默认异常")
        void testAround_降级方法不存在() {
            // Arrange
            when(circuitBreakerAnnotation.fallbackMethod()).thenReturn("nonExistMethod");

            io.github.resilience4j.circuitbreaker.CircuitBreaker resilience4jCb =
                    io.github.resilience4j.circuitbreaker.CircuitBreaker.of("test",
                            CircuitBreakerConfig.custom()
                                    .slidingWindowSize(5)
                                    .minimumNumberOfCalls(1)
                                    .failureRateThreshold(1)
                                    .build());
            resilience4jCb.transitionToOpenState();

            when(circuitBreakerRegistry.circuitBreaker(anyString(), any(CircuitBreakerConfig.class)))
                    .thenReturn(resilience4jCb);

            TestService testService = new TestService();
            when(joinPoint.getTarget()).thenReturn(testService);
            lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{});
            when(methodSignature.getParameterTypes()).thenReturn(new Class<?>[0]);
            when(joinPoint.getSignature()).thenReturn(methodSignature);

            // Act & Assert
            assertThatThrownBy(() -> aspect.around(joinPoint, circuitBreakerAnnotation))
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
