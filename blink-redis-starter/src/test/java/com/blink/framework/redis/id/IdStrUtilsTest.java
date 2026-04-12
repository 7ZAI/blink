package com.blink.framework.redis.id;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IdStrUtils 工具类单元测试
 *
 * @author binblink
 */
class IdStrUtilsTest {

    @Nested
    @DisplayName("stringFillAuto 测试")
    class StringFillAutoTests {

        @Test
        @DisplayName("10-01: 左侧补位")
        void testStringFillAuto_PadLeft() {
            // Given
            String str = "123";
            char paddingChar = '0';
            int length = 6;

            // When
            String result = IdStrUtils.stringFillAuto(str, paddingChar, length);

            // Then
            assertEquals("000123", result);
            assertEquals(6, result.length());
        }

        @Test
        @DisplayName("10-02: 无需补位")
        void testStringFillAuto_NoPadding() {
            // Given
            String str = "123456";
            char paddingChar = '0';
            int length = 6;

            // When
            String result = IdStrUtils.stringFillAuto(str, paddingChar, length);

            // Then
            assertEquals("123456", result);
        }

        @Test
        @DisplayName("10-03: 空字符串补位")
        void testStringFillAuto_EmptyString() {
            // Given
            String str = "";
            char paddingChar = '0';
            int length = 5;

            // When
            String result = IdStrUtils.stringFillAuto(str, paddingChar, length);

            // Then
            assertEquals("00000", result);
            assertEquals(5, result.length());
        }
    }

    @Nested
    @DisplayName("getMaxValue 测试")
    class GetMaxValueTests {

        @Test
        @DisplayName("10-04: 计算最大值")
        void testGetMaxValue() {
            // Given
            int length1 = 3;
            int length2 = 5;
            int length3 = 1;

            // When & Then
            assertEquals(999L, IdStrUtils.getMaxValue(length1));
            assertEquals(99999L, IdStrUtils.getMaxValue(length2));
            assertEquals(9L, IdStrUtils.getMaxValue(length3));
        }
    }

    @Nested
    @DisplayName("getMaxIdGenLength 测试")
    class GetMaxIdGenLengthTests {

        @Test
        @DisplayName("10-05: 计算ID生成长度")
        void testGetMaxIdGenLength() {
            // Given
            int validLength = 10;
            int overMaxLength = 30;
            int zeroLength = 0;
            int negativeLength = -1;

            // When & Then
            assertEquals(10, IdStrUtils.getMaxIdGenLength(validLength));
            // 超过最大长度时返回默认值
            assertEquals(IdGeneratorConstant.DEFAULT_MAX_LENGTH, IdStrUtils.getMaxIdGenLength(overMaxLength));
            // 0和负数返回默认值
            assertEquals(IdGeneratorConstant.DEFAULT_MAX_LENGTH, IdStrUtils.getMaxIdGenLength(zeroLength));
            assertEquals(IdGeneratorConstant.DEFAULT_MAX_LENGTH, IdStrUtils.getMaxIdGenLength(negativeLength));
        }
    }

    @Nested
    @DisplayName("getDateTimeString 测试")
    class GetDateTimeStringTests {

        @Test
        @DisplayName("10-06: 获取日期时间字符串")
        void testGetDateTimeString() {
            // Given
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            LocalDateTime now = LocalDateTime.now();

            // When
            String result = IdStrUtils.getDateTimeString();

            // Then
            assertNotNull(result);
            assertEquals(14, result.length());
            // 验证格式正确
            assertDoesNotThrow(() -> LocalDateTime.parse(result, formatter));
            // 验证时间接近当前时间（允许1秒误差）
            LocalDateTime parsedTime = LocalDateTime.parse(result, formatter);
            assertTrue(java.time.Duration.between(now, parsedTime).abs().getSeconds() <= 1);
        }
    }
}
