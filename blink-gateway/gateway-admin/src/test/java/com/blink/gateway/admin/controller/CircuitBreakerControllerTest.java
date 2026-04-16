package com.blink.gateway.admin.controller;

import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.rsp.CircuitBreakerConfigRsp;
import com.blink.gateway.admin.dto.rsp.CircuitBreakerOverviewRsp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CircuitBreakerController 单元测试类
 *
 * @author binblink
 * @since 2026-04-15
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CircuitBreakerController 单元测试")
class CircuitBreakerControllerTest {

    @Mock
    private DiscoveryClient discoveryClient;

    @InjectMocks
    private CircuitBreakerController circuitBreakerController;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    @Nested
    @DisplayName("getOverview 测试")
    class GetOverviewTests {

        @Test
        @DisplayName("获取熔断器总览 - 正常场景")
        void testGetOverview_Success() {
            RequestDTO<Void> requestDTO = new RequestDTO<>();

            // Mock 服务实例
            ServiceInstance instance = mock(ServiceInstance.class);
            when(instance.getInstanceId()).thenReturn("instance-001");
            when(instance.getHost()).thenReturn("192.168.1.1");
            when(instance.getPort()).thenReturn(8002);
            when(discoveryClient.getInstances("gateway-app")).thenReturn(List.of(instance));

            ResponseDTO<CircuitBreakerOverviewRsp> response = circuitBreakerController.getOverview(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            assertNotNull(response.getBody().getCircuitBreakers());
            assertEquals(7, response.getBody().getTotalCircuitBreakers()); // 7个预定义熔断器
            assertEquals(1, response.getBody().getTotalInstances());

            verify(discoveryClient, times(1)).getInstances("gateway-app");
        }

        @Test
        @DisplayName("获取熔断器总览 - 多实例场景")
        void testGetOverview_MultipleInstances() {
            RequestDTO<Void> requestDTO = new RequestDTO<>();

            ServiceInstance instance1 = mock(ServiceInstance.class);
            when(instance1.getInstanceId()).thenReturn("instance-001");

            ServiceInstance instance2 = mock(ServiceInstance.class);
            when(instance2.getInstanceId()).thenReturn("instance-002");

            when(discoveryClient.getInstances("gateway-app")).thenReturn(List.of(instance1, instance2));

            ResponseDTO<CircuitBreakerOverviewRsp> response = circuitBreakerController.getOverview(requestDTO);

            assertNotNull(response);
            assertEquals(2, response.getBody().getTotalInstances());

            verify(discoveryClient, times(1)).getInstances("gateway-app");
        }

        @Test
        @DisplayName("获取熔断器总览 - 无实例场景")
        void testGetOverview_NoInstances() {
            RequestDTO<Void> requestDTO = new RequestDTO<>();

            when(discoveryClient.getInstances("gateway-app")).thenReturn(List.of());

            ResponseDTO<CircuitBreakerOverviewRsp> response = circuitBreakerController.getOverview(requestDTO);

            assertNotNull(response);
            assertEquals(0, response.getBody().getTotalInstances());

            verify(discoveryClient, times(1)).getInstances("gateway-app");
        }

        @Test
        @DisplayName("获取熔断器总览 - 异常处理")
        void testGetOverview_Exception() {
            RequestDTO<Void> requestDTO = new RequestDTO<>();

            when(discoveryClient.getInstances("gateway-app"))
                .thenThrow(new RuntimeException("Discovery service error"));

            ResponseDTO<CircuitBreakerOverviewRsp> response = circuitBreakerController.getOverview(requestDTO);

            assertNotNull(response);
            assertNotNull(response.getBody());

            verify(discoveryClient, times(1)).getInstances("gateway-app");
        }
    }

    @Nested
    @DisplayName("getConfig 测试")
    class GetConfigTests {

        @Test
        @DisplayName("获取熔断器配置 - 正常场景")
        void testGetConfig_Success() {
            Map<String, String> body = new HashMap<>();
            body.put("name", "protectedCircuitBreaker");

            RequestDTO<Map<String, String>> requestDTO = new RequestDTO<>();
            requestDTO.setBody(body);

            ServiceInstance instance = mock(ServiceInstance.class);
            when(instance.getInstanceId()).thenReturn("instance-001");
            when(discoveryClient.getInstances("gateway-app")).thenReturn(List.of(instance));

            ResponseDTO<CircuitBreakerConfigRsp> response = circuitBreakerController.getConfig(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            assertEquals("protectedCircuitBreaker", response.getBody().getName());
            assertEquals("default", response.getBody().getBaseConfig());
            assertEquals(55.0, response.getBody().getFailureRateThreshold()); // 覆盖后的阈值

            verify(discoveryClient, times(1)).getInstances("gateway-app");
        }

        @Test
        @DisplayName("获取熔断器配置 - strict 配置")
        void testGetConfig_StrictConfig() {
            Map<String, String> body = new HashMap<>();
            body.put("name", "strictCircuitBreaker");

            RequestDTO<Map<String, String>> requestDTO = new RequestDTO<>();
            requestDTO.setBody(body);

            ServiceInstance instance = mock(ServiceInstance.class);
            when(instance.getInstanceId()).thenReturn("instance-001");
            when(discoveryClient.getInstances("gateway-app")).thenReturn(List.of(instance));

            ResponseDTO<CircuitBreakerConfigRsp> response = circuitBreakerController.getConfig(requestDTO);

            assertNotNull(response);
            assertEquals("strictCircuitBreaker", response.getBody().getName());
            assertEquals("strict", response.getBody().getBaseConfig());
            assertEquals(30.0, response.getBody().getFailureRateThreshold()); // strict 阈值
            assertEquals(120L, response.getBody().getWaitDurationInOpenState()); // 更长等待时间

            verify(discoveryClient, times(1)).getInstances("gateway-app");
        }

        @Test
        @DisplayName("获取熔断器配置 - lenient 配置")
        void testGetConfig_LenientConfig() {
            Map<String, String> body = new HashMap<>();
            body.put("name", "lenientCircuitBreaker");

            RequestDTO<Map<String, String>> requestDTO = new RequestDTO<>();
            requestDTO.setBody(body);

            ServiceInstance instance = mock(ServiceInstance.class);
            when(instance.getInstanceId()).thenReturn("instance-001");
            when(discoveryClient.getInstances("gateway-app")).thenReturn(List.of(instance));

            ResponseDTO<CircuitBreakerConfigRsp> response = circuitBreakerController.getConfig(requestDTO);

            assertNotNull(response);
            assertEquals("lenientCircuitBreaker", response.getBody().getName());
            assertEquals("lenient", response.getBody().getBaseConfig());
            assertEquals(70.0, response.getBody().getFailureRateThreshold()); // lenient 阈值
            assertEquals(30L, response.getBody().getWaitDurationInOpenState()); // 更短等待时间

            verify(discoveryClient, times(1)).getInstances("gateway-app");
        }

        @Test
        @DisplayName("获取熔断器配置 - 名称为空返回默认配置")
        void testGetConfig_EmptyName() {
            Map<String, String> body = new HashMap<>();
            body.put("name", "");

            RequestDTO<Map<String, String>> requestDTO = new RequestDTO<>();
            requestDTO.setBody(body);

            ResponseDTO<CircuitBreakerConfigRsp> response = circuitBreakerController.getConfig(requestDTO);

            assertNotNull(response);
            assertNotNull(response.getBody());
            // 当 name 为空时,直接返回默认配置,不会调用 discoveryClient
        }

        @Test
        @DisplayName("获取熔断器配置 - 不存在的名称返回默认配置")
        void testGetConfig_NonExistentName() {
            Map<String, String> body = new HashMap<>();
            body.put("name", "unknownCircuitBreaker");

            RequestDTO<Map<String, String>> requestDTO = new RequestDTO<>();
            requestDTO.setBody(body);

            ServiceInstance instance = mock(ServiceInstance.class);
            when(instance.getInstanceId()).thenReturn("instance-001");
            when(discoveryClient.getInstances("gateway-app")).thenReturn(List.of(instance));

            ResponseDTO<CircuitBreakerConfigRsp> response = circuitBreakerController.getConfig(requestDTO);

            assertNotNull(response);
            assertNotNull(response.getBody());
            // 应返回 default 配置

            verify(discoveryClient, times(1)).getInstances("gateway-app");
        }

        @Test
        @DisplayName("获取熔断器配置 - 异常处理")
        void testGetConfig_Exception() {
            Map<String, String> body = new HashMap<>();
            body.put("name", "protectedCircuitBreaker");

            RequestDTO<Map<String, String>> requestDTO = new RequestDTO<>();
            requestDTO.setBody(body);

            when(discoveryClient.getInstances("gateway-app"))
                .thenThrow(new RuntimeException("Discovery error"));

            ResponseDTO<CircuitBreakerConfigRsp> response = circuitBreakerController.getConfig(requestDTO);

            assertNotNull(response);
            assertNotNull(response.getBody());

            verify(discoveryClient, times(1)).getInstances("gateway-app");
        }
    }

    @Nested
    @DisplayName("预定义配置验证测试")
    class PredefinedConfigTests {

        @Test
        @DisplayName("验证 default 配置参数")
        void testDefaultConfig() {
            // 通过 getOverview 间接验证配置
            RequestDTO<Void> requestDTO = new RequestDTO<>();
            ServiceInstance instance = mock(ServiceInstance.class);
            when(instance.getInstanceId()).thenReturn("instance-001");
            when(discoveryClient.getInstances("gateway-app")).thenReturn(List.of(instance));

            ResponseDTO<CircuitBreakerOverviewRsp> response = circuitBreakerController.getOverview(requestDTO);

            List<CircuitBreakerConfigRsp> circuitBreakers = response.getBody().getCircuitBreakers();

            // 验证 myCircuitBreaker（继承 default）
            CircuitBreakerConfigRsp myConfig = circuitBreakers.stream()
                .filter(c -> "myCircuitBreaker".equals(c.getName()))
                .findFirst()
                .orElse(null);

            assertNotNull(myConfig);
            assertEquals("default", myConfig.getBaseConfig());
            assertEquals("COUNT_BASED", myConfig.getSlidingWindowType());
            assertEquals(10, myConfig.getSlidingWindowSize());
            assertEquals(5, myConfig.getMinimumNumberOfCalls());
            assertEquals(50.0, myConfig.getFailureRateThreshold());
            assertEquals(60L, myConfig.getWaitDurationInOpenState());
            assertTrue(myConfig.getAutomaticTransitionFromOpenToHalfOpenEnabled());
        }

        @Test
        @DisplayName("验证 imageCircuitBreaker 配置")
        void testImageCircuitBreakerConfig() {
            RequestDTO<Void> requestDTO = new RequestDTO<>();
            ServiceInstance instance = mock(ServiceInstance.class);
            when(instance.getInstanceId()).thenReturn("instance-001");
            when(discoveryClient.getInstances("gateway-app")).thenReturn(List.of(instance));

            ResponseDTO<CircuitBreakerOverviewRsp> response = circuitBreakerController.getOverview(requestDTO);

            CircuitBreakerConfigRsp imageConfig = response.getBody().getCircuitBreakers().stream()
                .filter(c -> "imageCircuitBreaker".equals(c.getName()))
                .findFirst()
                .orElse(null);

            assertNotNull(imageConfig);
            assertEquals("lenient", imageConfig.getBaseConfig());
            assertEquals(80.0, imageConfig.getFailureRateThreshold()); // 覆盖阈值
        }
    }
}