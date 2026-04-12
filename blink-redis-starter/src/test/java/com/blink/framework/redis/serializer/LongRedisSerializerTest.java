package com.blink.framework.redis.serializer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LongRedisSerializer 序列化器单元测试
 *
 * @author binblink
 */
class LongRedisSerializerTest {

    @Nested
    @DisplayName("序列化测试")
    class SerializeTests {

        @Test
        @DisplayName("10-07: 序列化Long")
        void testSerialize() {
            // Given
            LongRedisSerializer serializer = new LongRedisSerializer();
            Long value = 12345L;

            // When
            byte[] result = serializer.serialize(value);

            // Then
            assertNotNull(result);
            assertArrayEquals("12345".getBytes(StandardCharsets.UTF_8), result);
        }

        @Test
        @DisplayName("10-09: 序列化0")
        void testSerialize_Zero() {
            // Given
            LongRedisSerializer serializer = new LongRedisSerializer();
            Long value = 0L;

            // When
            byte[] result = serializer.serialize(value);

            // Then
            assertNotNull(result);
            assertArrayEquals("0".getBytes(StandardCharsets.UTF_8), result);
        }

        @Test
        @DisplayName("10-11: 序列化Long.MAX_VALUE")
        void testSerialize_MaxValue() {
            // Given
            LongRedisSerializer serializer = new LongRedisSerializer();
            Long value = Long.MAX_VALUE;

            // When
            byte[] result = serializer.serialize(value);

            // Then
            assertNotNull(result);
            assertEquals(String.valueOf(Long.MAX_VALUE), new String(result, StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("10-13: 序列化Long.MIN_VALUE")
        void testSerialize_MinValue() {
            // Given
            LongRedisSerializer serializer = new LongRedisSerializer();
            Long value = Long.MIN_VALUE;

            // When
            byte[] result = serializer.serialize(value);

            // Then
            assertNotNull(result);
            assertEquals(String.valueOf(Long.MIN_VALUE), new String(result, StandardCharsets.UTF_8));
        }
    }

    @Nested
    @DisplayName("反序列化测试")
    class DeserializeTests {

        @Test
        @DisplayName("10-08: 反序列化字节数组")
        void testDeserialize() {
            // Given
            LongRedisSerializer serializer = new LongRedisSerializer();
            byte[] bytes = "12345".getBytes(StandardCharsets.UTF_8);

            // When
            Long result = serializer.deserialize(bytes);

            // Then
            assertEquals(12345L, result);
        }

        @Test
        @DisplayName("10-10: 反序列化\"0\"")
        void testDeserialize_Zero() {
            // Given
            LongRedisSerializer serializer = new LongRedisSerializer();
            byte[] bytes = "0".getBytes(StandardCharsets.UTF_8);

            // When
            Long result = serializer.deserialize(bytes);

            // Then
            assertEquals(0L, result);
        }

        @Test
        @DisplayName("10-12: 反序列化最大值")
        void testDeserialize_MaxValue() {
            // Given
            LongRedisSerializer serializer = new LongRedisSerializer();
            byte[] bytes = String.valueOf(Long.MAX_VALUE).getBytes(StandardCharsets.UTF_8);

            // When
            Long result = serializer.deserialize(bytes);

            // Then
            assertEquals(Long.MAX_VALUE, result);
        }

        @Test
        @DisplayName("10-14: 反序列化最小值")
        void testDeserialize_MinValue() {
            // Given
            LongRedisSerializer serializer = new LongRedisSerializer();
            byte[] bytes = String.valueOf(Long.MIN_VALUE).getBytes(StandardCharsets.UTF_8);

            // When
            Long result = serializer.deserialize(bytes);

            // Then
            assertEquals(Long.MIN_VALUE, result);
        }
    }

    @Nested
    @DisplayName("往返测试")
    class RoundTripTests {

        @Test
        @DisplayName("10-15: 序列化后反序列化")
        void testRoundTrip() {
            // Given
            LongRedisSerializer serializer = new LongRedisSerializer();
            Long originalValue = 9876543210L;

            // When
            byte[] serialized = serializer.serialize(originalValue);
            Long deserialized = serializer.deserialize(serialized);

            // Then
            assertEquals(originalValue, deserialized);
        }

        @Test
        @DisplayName("10-16: 使用指定字符集构造")
        void testSerialize_WithCharset() {
            // Given
            LongRedisSerializer serializer = new LongRedisSerializer(StandardCharsets.ISO_8859_1);
            Long value = 12345L;

            // When
            byte[] result = serializer.serialize(value);

            // Then
            assertNotNull(result);
            assertArrayEquals("12345".getBytes(StandardCharsets.ISO_8859_1), result);
        }

        @Test
        @DisplayName("10-17: 反序列化无效数据抛出异常")
        void testDeserialize_InvalidData() {
            // Given
            LongRedisSerializer serializer = new LongRedisSerializer();
            byte[] invalidBytes = "not-a-number".getBytes(StandardCharsets.UTF_8);

            // When & Then
            assertThrows(NumberFormatException.class, () -> serializer.deserialize(invalidBytes));
        }
    }
}
