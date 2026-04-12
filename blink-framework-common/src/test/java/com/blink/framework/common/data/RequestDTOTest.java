package com.blink.framework.common.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * RequestDTO 单元测试
 * <p>
 * 测试覆盖：
 * 1. 静态工厂方法
 * 2. Getter/Setter
 * 3. 所有属性
 * 4. 泛型支持
 *
 * @author binblink
 */
@DisplayName("RequestDTO 通用请求数据传输对象测试")
class RequestDTOTest {

    // ==================== 静态工厂方法测试 ====================

    @Nested
    @DisplayName("静态工厂方法测试")
    class StaticFactoryMethodTests {

        @Test
        @DisplayName("newInstance(T)应该创建带body的请求")
        void shouldCreateRequestWithBody() {
            // given
            String body = "请求体数据";

            // when
            RequestDTO<String> request = RequestDTO.newInstance(body);

            // then
            assertThat(request).isNotNull();
            assertThat(request.getBody()).isEqualTo(body);
        }

        @Test
        @DisplayName("newInstance()应该创建空body的请求")
        void shouldCreateRequestWithEmptyBody() {
            // when
            RequestDTO<EmptyBody> request = RequestDTO.newInstance();

            // then
            assertThat(request).isNotNull();
            assertThat(request.getBody()).isNull();
        }

        @Test
        @DisplayName("null body应该能正确处理")
        void shouldHandleNullBody() {
            // when
            RequestDTO<String> request = RequestDTO.newInstance(null);

            // then
            assertThat(request.getBody()).isNull();
        }
    }

    // ==================== 基础属性Getter/Setter测试 ====================

    @Nested
    @DisplayName("基础属性Getter/Setter测试")
    class BasicPropertyTests {

        @Test
        @DisplayName("应该正确设置和获取requestId")
        void shouldSetAndGetRequestId() {
            // given
            RequestDTO<String> request = new RequestDTO<>();

            // when
            request.setRequestId("req-12345");

            // then
            assertThat(request.getRequestId()).isEqualTo("req-12345");
        }

        @Test
        @DisplayName("应该正确设置和获取traceId")
        void shouldSetAndGetTraceId() {
            // given
            RequestDTO<String> request = new RequestDTO<>();

            // when
            request.setTraceId("trace-67890");

            // then
            assertThat(request.getTraceId()).isEqualTo("trace-67890");
        }

        @Test
        @DisplayName("应该正确设置和获取spanId")
        void shouldSetAndGetSpanId() {
            // given
            RequestDTO<String> request = new RequestDTO<>();

            // when
            request.setSpanId("span-111");

            // then
            assertThat(request.getSpanId()).isEqualTo("span-111");
        }

        @Test
        @DisplayName("应该正确设置和获取parentSpanId")
        void shouldSetAndGetParentSpanId() {
            // given
            RequestDTO<String> request = new RequestDTO<>();

            // when
            request.setParentSpanId("parent-span-222");

            // then
            assertThat(request.getParentSpanId()).isEqualTo("parent-span-222");
        }

        @Test
        @DisplayName("应该正确设置和获取version")
        void shouldSetAndGetVersion() {
            // given
            RequestDTO<String> request = new RequestDTO<>();

            // when
            request.setVersion("1.0.0");

            // then
            assertThat(request.getVersion()).isEqualTo("1.0.0");
        }
    }

    // ==================== 用户相关属性测试 ====================

    @Nested
    @DisplayName("用户相关属性测试")
    class UserRelatedPropertyTests {

        @Test
        @DisplayName("应该正确设置和获取userId")
        void shouldSetAndGetUserId() {
            // given
            RequestDTO<String> request = new RequestDTO<>();

            // when
            request.setUserId("user-001");

            // then
            assertThat(request.getUserId()).isEqualTo("user-001");
        }

        @Test
        @DisplayName("应该正确设置和获取loginName")
        void shouldSetAndGetLoginName() {
            // given
            RequestDTO<String> request = new RequestDTO<>();

            // when
            request.setLoginName("admin");

            // then
            assertThat(request.getLoginName()).isEqualTo("admin");
        }

        @Test
        @DisplayName("应该正确设置和获取token")
        void shouldSetAndGetToken() {
            // given
            RequestDTO<String> request = new RequestDTO<>();

            // when
            request.setToken("jwt-token-xxx");

            // then
            assertThat(request.getToken()).isEqualTo("jwt-token-xxx");
        }

        @Test
        @DisplayName("应该正确设置和获取clientIp")
        void shouldSetAndGetClientIp() {
            // given
            RequestDTO<String> request = new RequestDTO<>();

            // when
            request.setClientIp("192.168.1.100");

            // then
            assertThat(request.getClientIp()).isEqualTo("192.168.1.100");
        }
    }

    // ==================== 请求来源属性测试 ====================

    @Nested
    @DisplayName("请求来源属性测试")
    class SourcePropertyTests {

        @Test
        @DisplayName("应该正确设置和获取source")
        void shouldSetAndGetSource() {
            // given
            RequestDTO<String> request = new RequestDTO<>();

            // when
            request.setSource("web");

            // then
            assertThat(request.getSource()).isEqualTo("web");
        }

        @Test
        @DisplayName("应该正确设置和获取channel")
        void shouldSetAndGetChannel() {
            // given
            RequestDTO<String> request = new RequestDTO<>();

            // when
            request.setChannel("mobile-app");

            // then
            assertThat(request.getChannel()).isEqualTo("mobile-app");
        }

        @Test
        @DisplayName("应该正确设置和获取uri")
        void shouldSetAndGetUri() {
            // given
            RequestDTO<String> request = new RequestDTO<>();

            // when
            request.setUri("/api/user/list");

            // then
            assertThat(request.getUri()).isEqualTo("/api/user/list");
        }

        @Test
        @DisplayName("应该正确设置和获取timeout")
        void shouldSetAndGetTimeout() {
            // given
            RequestDTO<String> request = new RequestDTO<>();

            // when
            request.setTimeout(5000);

            // then
            assertThat(request.getTimeout()).isEqualTo(5000);
        }
    }

    // ==================== 时间属性测试 ====================

    @Nested
    @DisplayName("时间属性测试")
    class TimePropertyTests {

        @Test
        @DisplayName("应该正确设置和获取reqDate")
        void shouldSetAndGetReqDate() {
            // given
            RequestDTO<String> request = new RequestDTO<>();
            LocalDate date = LocalDate.of(2024, 1, 15);

            // when
            request.setReqDate(date);

            // then
            assertThat(request.getReqDate()).isEqualTo(date);
        }

        @Test
        @DisplayName("应该正确设置和获取startDateTime")
        void shouldSetAndGetStartDateTime() {
            // given
            RequestDTO<String> request = new RequestDTO<>();
            LocalDateTime dateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

            // when
            request.setStartDateTime(dateTime);

            // then
            assertThat(request.getStartDateTime()).isEqualTo(dateTime);
        }

        @Test
        @DisplayName("应该正确设置和获取endDateTime")
        void shouldSetAndGetEndDateTime() {
            // given
            RequestDTO<String> request = new RequestDTO<>();
            LocalDateTime dateTime = LocalDateTime.of(2024, 1, 15, 11, 30, 0);

            // when
            request.setEndDateTime(dateTime);

            // then
            assertThat(request.getEndDateTime()).isEqualTo(dateTime);
        }
    }

    // ==================== 扩展字段测试 ====================

    @Nested
    @DisplayName("扩展字段测试")
    class ExtensionsPropertyTests {

        @Test
        @DisplayName("应该正确设置和获取extensions")
        void shouldSetAndGetExtensions() {
            // given
            RequestDTO<String> request = new RequestDTO<>();
            Map<String, Object> extensions = new HashMap<>();
            extensions.put("key1", "value1");
            extensions.put("key2", 123);

            // when
            request.setExtensions(extensions);

            // then
            assertThat(request.getExtensions()).isEqualTo(extensions);
            assertThat(request.getExtensions()).hasSize(2);
        }

        @Test
        @DisplayName("extensions应该支持复杂对象")
        void shouldSupportComplexObjectInExtensions() {
            // given
            RequestDTO<String> request = new RequestDTO<>();
            Map<String, Object> extensions = new HashMap<>();
            extensions.put("userInfo", Map.of("id", 1, "name", "张三"));

            // when
            request.setExtensions(extensions);

            // then
            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = (Map<String, Object>) request.getExtensions().get("userInfo");
            assertThat(userInfo.get("name")).isEqualTo("张三");
        }
    }

    // ==================== Body测试 ====================

    @Nested
    @DisplayName("Body测试")
    class BodyTests {

        @Test
        @DisplayName("应该正确设置和获取String类型的body")
        void shouldSetAndGetStringBody() {
            // given
            RequestDTO<String> request = new RequestDTO<>();

            // when
            request.setBody("测试body");

            // then
            assertThat(request.getBody()).isEqualTo("测试body");
        }

        @Test
        @DisplayName("应该正确设置和获取复杂对象类型的body")
        void shouldSetAndGetComplexObjectBody() {
            // given
            @lombok.Data
            @lombok.AllArgsConstructor
            class QueryReq {
                private String keyword;
                private int pageNum;
                private int pageSize;
            }
            RequestDTO<QueryReq> request = new RequestDTO<>();
            QueryReq queryReq = new QueryReq("关键词", 1, 10);

            // when
            request.setBody(queryReq);

            // then
            assertThat(request.getBody()).isNotNull();
            assertThat(request.getBody().getKeyword()).isEqualTo("关键词");
            assertThat(request.getBody().getPageNum()).isEqualTo(1);
        }
    }

    // ==================== toString测试 ====================

    @Nested
    @DisplayName("toString测试")
    class ToStringTests {

        @Test
        @DisplayName("toString应该包含主要属性")
        void shouldIncludeMainPropertiesInToString() {
            // given
            RequestDTO<String> request = RequestDTO.newInstance("测试数据");
            request.setRequestId("req-001");
            request.setTraceId("trace-001");
            request.setLoginName("admin");

            // when
            String str = request.toString();

            // then
            assertThat(str).contains("requestId='req-001'");
            assertThat(str).contains("traceId='trace-001'");
            assertThat(str).contains("loginName='admin'");
            assertThat(str).contains("body=测试数据");
        }
    }

    // ==================== 泛型类型测试 ====================

    @Nested
    @DisplayName("泛型类型测试")
    class GenericTypeTests {

        @Test
        @DisplayName("应该支持String类型body")
        void shouldSupportStringTypeBody() {
            // when
            RequestDTO<String> request = RequestDTO.newInstance("字符串数据");

            // then
            assertThat(request.getBody()).isEqualTo("字符串数据");
        }

        @Test
        @DisplayName("应该支持Integer类型body")
        void shouldSupportIntegerTypeBody() {
            // when
            RequestDTO<Integer> request = RequestDTO.newInstance(123);

            // then
            assertThat(request.getBody()).isEqualTo(123);
        }

        @Test
        @DisplayName("应该支持自定义对象类型body")
        void shouldSupportCustomObjectTypeBody() {
            // given
            @lombok.Data
            @lombok.AllArgsConstructor
            class AddUserReq {
                private String name;
                private Integer age;
            }
            AddUserReq addUserReq = new AddUserReq("张三", 25);

            // when
            RequestDTO<AddUserReq> request = RequestDTO.newInstance(addUserReq);

            // then
            assertThat(request.getBody().getName()).isEqualTo("张三");
            assertThat(request.getBody().getAge()).isEqualTo(25);
        }

        @Test
        @DisplayName("应该支持EmptyBody类型")
        void shouldSupportEmptyBodyType() {
            // when
            RequestDTO<EmptyBody> request = RequestDTO.newInstance();

            // then
            assertThat(request.getBody()).isNull();
        }
    }

    // ==================== 实际使用场景测试 ====================

    @Nested
    @DisplayName("实际使用场景测试")
    class UsageScenarioTests {

        @Test
        @DisplayName("完整的Controller请求场景")
        void shouldWorkInFullRequestScenario() {
            // given - 模拟前端请求
            @lombok.Data
            @lombok.AllArgsConstructor
            class QueryUserReq {
                private String keyword;
                private int pageNum;
                private int pageSize;
            }

            // when
            RequestDTO<QueryUserReq> request = RequestDTO.newInstance(
                    new QueryUserReq("张", 1, 10));
            request.setRequestId("req-" + System.currentTimeMillis());
            request.setTraceId("trace-" + java.util.UUID.randomUUID());
            request.setLoginName("admin");
            request.setUserId("1");
            request.setClientIp("192.168.1.100");
            request.setSource("web");
            request.setChannel("pc");
            request.setToken("jwt-token-xxx");
            request.setUri("/api/user/list");

            // then
            assertThat(request.getBody().getKeyword()).isEqualTo("张");
            assertThat(request.getLoginName()).isEqualTo("admin");
            assertThat(request.getClientIp()).isEqualTo("192.168.1.100");
        }

        @Test
        @DisplayName("分布式追踪场景")
        void shouldWorkInDistributedTracingScenario() {
            // given
            RequestDTO<String> request = new RequestDTO<>();

            // when - 设置追踪信息
            request.setTraceId("trace-parent-001");
            request.setSpanId("span-001");
            request.setParentSpanId("span-parent");

            // then
            assertThat(request.getTraceId()).isEqualTo("trace-parent-001");
            assertThat(request.getSpanId()).isEqualTo("span-001");
            assertThat(request.getParentSpanId()).isEqualTo("span-parent");
        }

        @Test
        @DisplayName("多渠道请求场景")
        void shouldWorkInMultiChannelScenario() {
            // given
            RequestDTO<String> request = new RequestDTO<>();

            // when - 移动端渠道
            request.setSource("app");
            request.setChannel("android");
            request.setVersion("2.0.0");

            // then
            assertThat(request.getSource()).isEqualTo("app");
            assertThat(request.getChannel()).isEqualTo("android");
            assertThat(request.getVersion()).isEqualTo("2.0.0");
        }
    }

    // ==================== 边界条件测试 ====================

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("所有属性为null时toString应该正常工作")
        void shouldHandleToStringWithAllNullProperties() {
            // given
            RequestDTO<String> request = new RequestDTO<>();

            // when
            String str = request.toString();

            // then
            assertThat(str).isNotNull();
            assertThat(str).contains("RequestDTO");
        }

        @Test
        @DisplayName("空extensions应该能正确处理")
        void shouldHandleEmptyExtensions() {
            // given
            RequestDTO<String> request = new RequestDTO<>();

            // when
            request.setExtensions(new HashMap<>());

            // then
            assertThat(request.getExtensions()).isEmpty();
        }

        @Test
        @DisplayName("超长requestId应该能正确处理")
        void shouldHandleLongRequestId() {
            // given
            RequestDTO<String> request = new RequestDTO<>();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                sb.append("a");
            }
            String longId = sb.toString();

            // when
            request.setRequestId(longId);

            // then
            assertThat(request.getRequestId()).hasSize(100);
        }
    }
}
