package com.blink.framework.common.utils;

import com.blink.framework.test.annotation.UnitTest;
import com.blink.framework.test.base.BlinkUnitTest;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * JacksonUtil 单元测试
 * <p>
 * 测试覆盖：
 * 1. 基础序列化/反序列化
 * 2. 泛型集合处理
 * 3. Map转换
 * 4. 日期时间处理
 * 5. 安全转换
 * 6. JSON验证
 * 7. 节点操作
 *
 * @author binblink
 */
@UnitTest
@DisplayName("JacksonUtil JSON工具类测试")
class JacksonUtilTest extends BlinkUnitTest {

    // ==================== 测试数据类 ====================

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    static class TestUser {
        private Long id;
        private String name;
        private Integer age;
        private LocalDateTime createTime;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    static class TestComplexObject {
        private String name;
        private List<String> tags;
        private Map<String, Object> metadata;
    }

    // ==================== 基础序列化测试 ====================

    @Nested
    @DisplayName("基础序列化测试")
    class BasicSerializationTests {

        @Test
        @DisplayName("应该正确序列化简单对象")
        void shouldSerializeSimpleObject() {
            // given
            TestUser user = new TestUser(1L, "张三", 25, null);

            // when
            String json = JacksonUtil.toJson(user);

            // then
            assertThat(json).isNotEmpty();
            assertThat(json).contains("\"id\":\"1\""); // Long转String
            assertThat(json).contains("\"name\":\"张三\"");
            assertThat(json).contains("\"age\":25");
        }

        @Test
        @DisplayName("应该正确序列化null对象")
        void shouldSerializeNullObject() {
            // when
            String json = JacksonUtil.toJson(null);

            // then
            assertThat(json).isNull();
        }

        @Test
        @DisplayName("应该忽略null字段")
        void shouldIgnoreNullFields() {
            // given
            TestUser user = new TestUser(1L, null, 25, null);

            // when
            String json = JacksonUtil.toJson(user);

            // then
            assertThat(json).doesNotContain("\"name\"");
            assertThat(json).doesNotContain("\"createTime\"");
        }

        @Test
        @DisplayName("应该正确序列化List")
        void shouldSerializeList() {
            // given
            List<String> list = Arrays.asList("a", "b", "c");

            // when
            String json = JacksonUtil.toJson(list);

            // then
            assertThat(json).isEqualTo("[\"a\",\"b\",\"c\"]");
        }

        @Test
        @DisplayName("应该正确序列化Map")
        void shouldSerializeMap() {
            // given
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", "test");
            map.put("value", 123);

            // when
            String json = JacksonUtil.toJson(map);

            // then
            assertThat(json).contains("\"name\":\"test\"");
            assertThat(json).contains("\"value\":123");
        }

        @Test
        @DisplayName("应该生成美化格式的JSON")
        void shouldGeneratePrettyJson() {
            // given
            TestUser user = new TestUser(1L, "张三", 25, null);

            // when
            String prettyJson = JacksonUtil.toPrettyJson(user);

            // then
            assertThat(prettyJson).contains("\n");
            assertThat(prettyJson).contains("  ");
        }
    }

    // ==================== 基础反序列化测试 ====================

    @Nested
    @DisplayName("基础反序列化测试")
    class BasicDeserializationTests {

        @Test
        @DisplayName("应该正确反序列化JSON字符串")
        void shouldDeserializeJsonString() {
            // given
            String json = "{\"id\":\"1\",\"name\":\"张三\",\"age\":25}";

            // when
            TestUser user = JacksonUtil.fromJson(json, TestUser.class);

            // then
            assertThat(user).isNotNull();
            assertThat(user.getId()).isEqualTo(1L);
            assertThat(user.getName()).isEqualTo("张三");
            assertThat(user.getAge()).isEqualTo(25);
        }

        @Test
        @DisplayName("空字符串应该返回null")
        void shouldReturnNullForEmptyString() {
            // when
            TestUser user = JacksonUtil.fromJson("", TestUser.class);

            // then
            assertThat(user).isNull();
        }

        @Test
        @DisplayName("null字符串应该返回null")
        void shouldReturnNullForNullString() {
            // when
            TestUser user = JacksonUtil.fromJson(null, TestUser.class);

            // then
            assertThat(user).isNull();
        }

        @Test
        @DisplayName("空白字符串应该返回null")
        void shouldReturnNullForBlankString() {
            // when
            TestUser user = JacksonUtil.fromJson("   ", TestUser.class);

            // then
            assertThat(user).isNull();
        }

        @Test
        @DisplayName("无效JSON应该抛出异常")
        void shouldThrowExceptionForInvalidJson() {
            // given
            String invalidJson = "not a valid json";

            // when & then
            assertThatThrownBy(() -> JacksonUtil.fromJson(invalidJson, TestUser.class))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("反序列化失败");
        }
    }

    // ==================== 泛型集合测试 ====================

    @Nested
    @DisplayName("泛型集合测试")
    class GenericCollectionTests {

        @Test
        @DisplayName("应该正确反序列化为List")
        void shouldDeserializeToList() {
            // given
            String json = "[{\"id\":\"1\",\"name\":\"张三\"},{\"id\":\"2\",\"name\":\"李四\"}]";

            // when
            List<TestUser> users = JacksonUtil.fromJsonToList(json, TestUser.class);

            // then
            assertThat(users).hasSize(2);
            assertThat(users.get(0).getName()).isEqualTo("张三");
            assertThat(users.get(1).getName()).isEqualTo("李四");
        }

        @Test
        @DisplayName("空数组应该返回空List")
        void shouldReturnEmptyListForEmptyArray() {
            // when
            List<TestUser> users = JacksonUtil.fromJsonToList("[]", TestUser.class);

            // then
            assertThat(users).isEmpty();
        }

        @Test
        @DisplayName("null/空字符串应该返回空List")
        void shouldReturnEmptyListForNullOrEmptyString() {
            assertThat(JacksonUtil.fromJsonToList(null, TestUser.class)).isEmpty();
            assertThat(JacksonUtil.fromJsonToList("", TestUser.class)).isEmpty();
        }

        @Test
        @DisplayName("应该正确处理复杂泛型")
        void shouldHandleComplexGeneric() {
            // given
            String json = "[\"a\",\"b\",\"c\"]";

            // when
            List<String> list = JacksonUtil.fromJson(json, new TypeReference<List<String>>() {});

            // then
            assertThat(list).containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("应该正确处理嵌套泛型")
        void shouldHandleNestedGeneric() {
            // given
            String json = "{\"key1\":[1,2,3],\"key2\":[4,5,6]}";

            // when
            Map<String, List<Integer>> map = JacksonUtil.fromJson(json,
                    new TypeReference<Map<String, List<Integer>>>() {});

            // then
            assertThat(map.get("key1")).containsExactly(1, 2, 3);
            assertThat(map.get("key2")).containsExactly(4, 5, 6);
        }
    }

    // ==================== Map转换测试 ====================

    @Nested
    @DisplayName("Map转换测试")
    class MapConversionTests {

        @Test
        @DisplayName("应该正确将对象转换为Map")
        void shouldConvertObjectToMap() {
            // given
            TestUser user = new TestUser(1L, "张三", 25, null);

            // when
            Map<String, Object> map = JacksonUtil.toMap(user);

            // then
            assertThat(map.get("id")).isEqualTo("1"); // Long转String
            assertThat(map.get("name")).isEqualTo("张三");
            assertThat(map.get("age")).isEqualTo(25);
        }

        @Test
        @DisplayName("null对象应该返回空Map")
        void shouldReturnEmptyMapForNullObject() {
            // when
            Map<String, Object> map = JacksonUtil.toMap(null);

            // then
            assertThat(map).isEmpty();
        }

        @Test
        @DisplayName("应该正确将Map转换为对象")
        void shouldConvertMapToObject() {
            // given
            Map<String, Object> map = new HashMap<>();
            map.put("id", 1L);
            map.put("name", "张三");
            map.put("age", 25);

            // when
            TestUser user = JacksonUtil.fromMap(map, TestUser.class);

            // then
            assertThat(user.getId()).isEqualTo(1L);
            assertThat(user.getName()).isEqualTo("张三");
            assertThat(user.getAge()).isEqualTo(25);
        }

        @Test
        @DisplayName("null/空Map应该返回null")
        void shouldReturnNullForNullOrEmptyMap() {
            assertThat(JacksonUtil.fromMap(null, TestUser.class)).isNull();
            assertThat(JacksonUtil.fromMap(Collections.emptyMap(), TestUser.class)).isNull();
        }

        @Test
        @DisplayName("应该正确反序列化JSON为Map")
        void shouldDeserializeJsonToMap() {
            // given
            String json = "{\"name\":\"test\",\"value\":123}";

            // when
            Map<String, Object> map = JacksonUtil.fromJsonToMap(json, String.class, Object.class);

            // then
            assertThat(map.get("name")).isEqualTo("test");
            assertThat(map.get("value")).isEqualTo(123);
        }

        @Test
        @DisplayName("应该深度转换Map为对象")
        void shouldDeepConvertMapToObject() {
            // given
            Map<String, Object> map = new HashMap<>();
            map.put("name", "test");
            List<String> tags = Arrays.asList("tag1", "tag2");
            map.put("tags", tags);

            // when
            TestComplexObject obj = JacksonUtil.deepConvert(map, TestComplexObject.class);

            // then
            assertThat(obj.getName()).isEqualTo("test");
            assertThat(obj.getTags()).containsExactly("tag1", "tag2");
        }
    }

    // ==================== 日期时间测试 ====================

    @Nested
    @DisplayName("日期时间处理测试")
    class DateTimeTests {

        @Test
        @DisplayName("应该正确序列化LocalDateTime")
        void shouldSerializeLocalDateTime() {
            // given
            LocalDateTime dateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 45);
            Map<String, Object> data = new HashMap<>();
            data.put("time", dateTime);

            // when
            String json = JacksonUtil.toJson(data);

            // then
            assertThat(json).contains("\"time\":\"2024-01-15 10:30:45\"");
        }

        @Test
        @DisplayName("应该正确反序列化LocalDateTime")
        void shouldDeserializeLocalDateTime() {
            // given
            String json = "{\"time\":\"2024-01-15 10:30:45\"}";

            // when
            Map<String, Object> map = JacksonUtil.fromJsonToMap(json, String.class, Object.class);

            // then
            assertThat(map.get("time")).isEqualTo("2024-01-15 10:30:45");
        }

        @Test
        @DisplayName("应该正确序列化LocalDate")
        void shouldSerializeLocalDate() {
            // given
            LocalDate date = LocalDate.of(2024, 1, 15);
            Map<String, Object> data = new HashMap<>();
            data.put("date", date);

            // when
            String json = JacksonUtil.toJson(data);

            // then
            assertThat(json).contains("\"date\":\"2024-01-15\"");
        }

        @Test
        @DisplayName("应该正确序列化LocalTime")
        void shouldSerializeLocalTime() {
            // given
            LocalTime time = LocalTime.of(10, 30, 45);
            Map<String, Object> data = new HashMap<>();
            data.put("time", time);

            // when
            String json = JacksonUtil.toJson(data);

            // then
            assertThat(json).contains("\"time\":\"10:30:45\"");
        }
    }

    // ==================== 安全转换测试 ====================

    @Nested
    @DisplayName("安全转换测试")
    class SafeConversionTests {

        @Test
        @DisplayName("安全转换应该不抛异常")
        void shouldNotThrowExceptionForSafeConversion() {
            // when
            TestUser user = JacksonUtil.safeFromJson("invalid json", TestUser.class);

            // then
            assertThat(user).isNull();
        }

        @Test
        @DisplayName("安全转换失败应该返回默认值")
        void shouldReturnDefaultValueForSafeConversion() {
            // given
            TestUser defaultUser = new TestUser(0L, "default", 0, null);

            // when
            TestUser user = JacksonUtil.safeFromJson("invalid json", TestUser.class, defaultUser);

            // then
            assertThat(user).isEqualTo(defaultUser);
        }

        @Test
        @DisplayName("安全转换成功应该返回正确值")
        void shouldReturnCorrectValueForSuccessfulConversion() {
            // given
            String json = "{\"id\":\"1\",\"name\":\"张三\"}";

            // when
            TestUser user = JacksonUtil.safeFromJson(json, TestUser.class);

            // then
            assertThat(user).isNotNull();
            assertThat(user.getName()).isEqualTo("张三");
        }
    }

    // ==================== JSON验证测试 ====================

    @Nested
    @DisplayName("JSON验证测试")
    class JsonValidationTests {

        @Test
        @DisplayName("有效JSON字符串应该返回true")
        void shouldReturnTrueForValidJson() {
            assertThat(JacksonUtil.isValidJson("{\"name\":\"test\"}")).isTrue();
            assertThat(JacksonUtil.isValidJson("[1,2,3]")).isTrue();
            assertThat(JacksonUtil.isValidJson("\"string\"")).isTrue();
            assertThat(JacksonUtil.isValidJson("123")).isTrue();
            assertThat(JacksonUtil.isValidJson("true")).isTrue();
            assertThat(JacksonUtil.isValidJson("null")).isTrue();
        }

        @Test
        @DisplayName("无效JSON字符串应该返回false")
        void shouldReturnFalseForInvalidJson() {
            assertThat(JacksonUtil.isValidJson("not json")).isFalse();
            assertThat(JacksonUtil.isValidJson("{invalid}")).isFalse();
            assertThat(JacksonUtil.isValidJson("")).isFalse();
            assertThat(JacksonUtil.isValidJson(null)).isFalse();
        }

        @Test
        @DisplayName("应该正确判断JSON对象")
        void shouldCorrectlyIdentifyJsonObject() {
            assertThat(JacksonUtil.isJsonObject("{\"name\":\"test\"}")).isTrue();
            assertThat(JacksonUtil.isJsonObject("[1,2,3]")).isFalse();
            assertThat(JacksonUtil.isJsonObject("\"string\"")).isFalse();
        }

        @Test
        @DisplayName("应该正确判断JSON数组")
        void shouldCorrectlyIdentifyJsonArray() {
            assertThat(JacksonUtil.isJsonArray("[1,2,3]")).isTrue();
            assertThat(JacksonUtil.isJsonArray("{\"name\":\"test\"}")).isFalse();
            assertThat(JacksonUtil.isJsonArray("\"string\"")).isFalse();
        }
    }

    // ==================== 转义处理测试 ====================

    @Nested
    @DisplayName("转义处理测试")
    class EscapeHandlingTests {

        @Test
        @DisplayName("应该正确反转义JSON字符串")
        void shouldUnescapeJsonString() {
            // given
            String escaped = "\"{\\\"name\\\":\\\"张三\\\"}\"";

            // when
            String unescaped = JacksonUtil.unescapeJson(escaped);

            // then
            assertThat(unescaped).isEqualTo("{\"name\":\"张三\"}");
        }

        @Test
        @DisplayName("应该处理混乱的JSON")
        void shouldParseMessyJson() {
            // given
            String messyJson = "\"{\\\"id\\\":\\\"1\\\",\\\"name\\\":\\\"test\\\"}\"";

            // when
            TestUser user = JacksonUtil.parseMessyJson(messyJson, TestUser.class);

            // then
            assertThat(user).isNotNull();
            assertThat(user.getId()).isEqualTo(1L);
            assertThat(user.getName()).isEqualTo("test");
        }

        @Test
        @DisplayName("应该规范化JSON字符串")
        void shouldNormalizeJson() {
            // given
            String json = "{\"name\":\"test\",\"age\":25}";

            // when
            String normalized = JacksonUtil.normalizeJson(json);

            // then
            assertThat(JacksonUtil.isValidJson(normalized)).isTrue();
        }

        @Test
        @DisplayName("fromJson应正确保留嵌套JSON字符串的转义")
        void shouldPreserveNestedJsonEscapingWithFromJson() {
            // given - 模拟 ruleConfig 字段包含嵌套 JSON 的场景
            String nestedJson = "{\"relationTable\":\"sys_user_group_rela\",\"sourceField\":\"group_id\"}";
            String escapedNestedJson = nestedJson.replace("\"", "\\\"");
            String requestBody = "{\"body\":{\"dataFilterId\":10,\"ruleConfig\":\"" + escapedNestedJson + "\"}}";

            // when - 使用 fromJson 解析（正确方式）
            Map<String, Object> result = JacksonUtil.fromJson(requestBody, Map.class);

            // then - ruleConfig 字段应保留为字符串，内部转义正确
            assertThat(result).isNotNull();
            Map<String, Object> body = (Map<String, Object>) result.get("body");
            String ruleConfig = (String) body.get("ruleConfig");
            assertThat(ruleConfig).isEqualTo(nestedJson);
        }

        @Test
        @DisplayName("parseMessyJson会破坏正常嵌套JSON字符串")
        void parseMessyJsonShouldBreakNormalNestedJson() {
            // given - 模拟 ruleConfig 字段包含嵌套 JSON 的场景（与上例相同）
            String nestedJson = "{\"relationTable\":\"sys_user_group_rela\",\"sourceField\":\"group_id\"}";
            String escapedNestedJson = nestedJson.replace("\"", "\\\"");
            String requestBody = "{\"body\":{\"dataFilterId\":10,\"ruleConfig\":\"" + escapedNestedJson + "\"}}";

            // when & then - parseMessyJson 会破坏嵌套 JSON 结构，抛出异常
            assertThatThrownBy(() -> JacksonUtil.parseMessyJson(requestBody, Map.class))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("反序列化失败");
        }
    }

    // ==================== 对象操作测试 ====================

    @Nested
    @DisplayName("对象操作测试")
    class ObjectOperationTests {

        @Test
        @DisplayName("应该正确进行对象转换")
        void shouldConvertObject() {
            // given
            Map<String, Object> map = new HashMap<>();
            map.put("id", 1L);
            map.put("name", "张三");
            map.put("age", 25);

            // when
            TestUser user = JacksonUtil.convert(map, TestUser.class);

            // then
            assertThat(user.getId()).isEqualTo(1L);
            assertThat(user.getName()).isEqualTo("张三");
        }

        @Test
        @DisplayName("应该正确进行深度拷贝")
        void shouldDeepCopyObject() {
            // given
            TestUser original = new TestUser(1L, "张三", 25, null);

            // when
            TestUser copy = JacksonUtil.deepCopy(original, TestUser.class);

            // then
            assertThat(copy).isNotSameAs(original);
            assertThat(copy.getId()).isEqualTo(original.getId());
            assertThat(copy.getName()).isEqualTo(original.getName());
        }

        @Test
        @DisplayName("应该正确合并对象")
        void shouldMergeObjects() {
            // given
            TestUser source = new TestUser(1L, "张三", 25, null);
            TestUser target = new TestUser(null, "李四", null, null);

            // when
            TestUser merged = JacksonUtil.merge(source, target, TestUser.class);

            // then
            assertThat(merged.getId()).isEqualTo(1L); // 来自source
            assertThat(merged.getName()).isEqualTo("李四"); // target覆盖
            assertThat(merged.getAge()).isEqualTo(25); // 来自source
        }

        @Test
        @DisplayName("source为null应该返回target")
        void shouldReturnTargetWhenSourceIsNull() {
            // given
            TestUser target = new TestUser(1L, "李四", 30, null);

            // when
            TestUser merged = JacksonUtil.merge(null, target, TestUser.class);

            // then
            assertThat(merged).isEqualTo(target);
        }

        @Test
        @DisplayName("target为null应该返回source")
        void shouldReturnSourceWhenTargetIsNull() {
            // given
            TestUser source = new TestUser(1L, "张三", 25, null);

            // when
            TestUser merged = JacksonUtil.merge(source, null, TestUser.class);

            // then
            assertThat(merged).isEqualTo(source);
        }
    }

    // ==================== 批量操作测试 ====================

    @Nested
    @DisplayName("批量操作测试")
    class BatchOperationTests {

        @Test
        @DisplayName("应该正确批量转换为JSON")
        void shouldBatchToJson() {
            // given
            List<TestUser> users = Arrays.asList(
                    new TestUser(1L, "张三", 25, null),
                    new TestUser(2L, "李四", 30, null)
            );

            // when
            String json = JacksonUtil.batchToJson(users);

            // then
            assertThat(json).startsWith("[").endsWith("]");
            assertThat(json).contains("张三");
            assertThat(json).contains("李四");
        }

        @Test
        @DisplayName("空List应该返回空数组")
        void shouldReturnEmptyArrayForEmptyList() {
            // when
            String json = JacksonUtil.batchToJson(Collections.emptyList());

            // then
            assertThat(json).isEqualTo("[]");
        }

        @Test
        @DisplayName("null应该返回空数组")
        void shouldReturnEmptyArrayForNull() {
            // when
            String json = JacksonUtil.batchToJson(null);

            // then
            assertThat(json).isEqualTo("[]");
        }

        @Test
        @DisplayName("应该批量将Map列表转换为对象列表")
        void shouldBatchMapToObjectList() {
            // given
            List<Map<String, Object>> mapList = Arrays.asList(
                    new HashMap<String, Object>() {{ put("id", 1L); put("name", "张三"); }},
                    new HashMap<String, Object>() {{ put("id", 2L); put("name", "李四"); }}
            );

            // when
            List<TestUser> users = JacksonUtil.batchMapToObject(mapList, TestUser.class);

            // then
            assertThat(users).hasSize(2);
            assertThat(users.get(0).getName()).isEqualTo("张三");
            assertThat(users.get(1).getName()).isEqualTo("李四");
        }
    }

    // ==================== Long类型处理测试 ====================

    @Nested
    @DisplayName("Long类型处理测试")
    class LongTypeTests {

        @Test
        @DisplayName("Long类型应该转换为字符串防止精度丢失")
        void shouldConvertLongToString() {
            // given
            Long bigLong = 1234567890123456789L;
            Map<String, Object> data = new HashMap<>();
            data.put("id", bigLong);

            // when
            String json = JacksonUtil.toJson(data);

            // then
            assertThat(json).contains("\"id\":\"1234567890123456789\"");
        }

        @Test
        @DisplayName("超过16位的Long应该正确序列化")
        void shouldCorrectlySerializeLongOver16Digits() {
            // given
            Long bigLong = 9007199254740993L; // 超过JS安全整数范围
            Map<String, Object> data = new HashMap<>();
            data.put("id", bigLong);

            // when
            String json = JacksonUtil.toJson(data);

            // then
            assertThat(json).contains("\"id\":\"9007199254740993\"");
        }
    }
}
