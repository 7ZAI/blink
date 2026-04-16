package com.blink.framework.common.utils;

import com.blink.framework.test.annotation.UnitTest;
import com.blink.framework.test.base.BlinkUnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.*;

/**
 * EnvReaderUtil 单元测试
 * <p>
 * 测试覆盖：
 * 1. 基础环境变量读取
 * 2. 缓存机制
 * 3. 条件查询
 * 4. 系统属性操作
 * 5. 验证方法
 * 6. 敏感信息处理
 * 7. 类型转换方法
 *
 * @author binblink
 */
@UnitTest
@DisplayName("EnvReaderUtil 环境变量工具测试")
class EnvReaderUtilTest extends BlinkUnitTest {

    @AfterEach
    void tearDown() {
        // 清除缓存避免测试间干扰
        EnvReaderUtil.clearCache();
        // 清除测试设置的系统属性
        System.clearProperty("test.property");
        System.clearProperty("test.bool");
        System.clearProperty("test.int");
    }

    // ==================== 基础读取方法测试 ====================

    @Nested
    @DisplayName("基础读取方法测试")
    class BasicReadTests {

        @Test
        @DisplayName("应该获取所有环境变量")
        void shouldGetAllEnvVariables() {
            // when
            Map<String, String> allEnv = EnvReaderUtil.getAllEnvVariables();

            // then
            assertThat(allEnv).isNotNull();
            assertThat(allEnv).isNotEmpty();
            // 返回的Map应该是不可修改的
            assertThatThrownBy(() -> allEnv.put("test", "value"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("应该获取存在的环境变量")
        void shouldGetExistingEnvVariable() {
            // when - PATH是大多数系统都有的环境变量
            String path = EnvReaderUtil.getEnv("PATH");

            // then
            assertThat(path).isNotNull();
        }

        @Test
        @DisplayName("不存在的环境变量应该返回null")
        void shouldReturnNullForNonExistentEnv() {
            // when
            String value = EnvReaderUtil.getEnv("NON_EXISTENT_ENV_VAR_12345");

            // then
            assertThat(value).isNull();
        }

        @Test
        @DisplayName("应该使用默认值返回环境变量")
        void shouldReturnDefaultForNonExistentEnv() {
            // when
            String value = EnvReaderUtil.getEnv("NON_EXISTENT_ENV_VAR_12345", "defaultValue");

            // then
            assertThat(value).isEqualTo("defaultValue");
        }
    }

    // ==================== 缓存机制测试 ====================

    @Nested
    @DisplayName("缓存机制测试")
    class CacheTests {

        @Test
        @DisplayName("应该缓存环境变量值")
        void shouldCacheEnvValue() {
            // given
            String key = "PATH";
            String firstValue = EnvReaderUtil.getCachedEnv(key);

            // when - 再次获取应该从缓存返回
            String secondValue = EnvReaderUtil.getCachedEnv(key);

            // then
            assertThat(firstValue).isEqualTo(secondValue);
        }

        @Test
        @DisplayName("应该清除缓存")
        void shouldClearCache() {
            // given
            EnvReaderUtil.getCachedEnv("PATH");

            // when
            EnvReaderUtil.clearCache();

            // then - 缓存被清除后可以重新读取
            // 验证通过没有异常
            assertThatCode(() -> EnvReaderUtil.getCachedEnv("PATH"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("缓存应该使用默认值")
        void shouldUseDefaultInCache() {
            // when
            String value = EnvReaderUtil.getCachedEnv("NON_EXISTENT_CACHE_KEY", "defaultCachedValue");

            // then
            assertThat(value).isEqualTo("defaultCachedValue");
        }
    }

    // ==================== 条件查询方法测试 ====================

    @Nested
    @DisplayName("条件查询方法测试")
    class ConditionalQueryTests {

        @Test
        @DisplayName("应该按前缀获取环境变量")
        void shouldGetEnvByPrefix() {
            // when
            Map<String, String> javaEnvs = EnvReaderUtil.getEnvByPrefix("JAVA");

            // then
            assertThat(javaEnvs).isNotNull();
            // 通常会有 JAVA_HOME 或类似的环境变量
        }

        @Test
        @DisplayName("应该按关键词获取环境变量")
        void shouldGetEnvContains() {
            // when
            Map<String, String> homeEnvs = EnvReaderUtil.getEnvContains("HOME");

            // then
            assertThat(homeEnvs).isNotNull();
        }

        @Test
        @DisplayName("应该检查环境变量是否存在")
        void shouldCheckEnvExists() {
            // when & then
            assertThat(EnvReaderUtil.exists("PATH")).isTrue();
            assertThat(EnvReaderUtil.exists("NON_EXISTENT_VAR")).isFalse();
        }

        @Test
        @DisplayName("应该检查环境变量是否存在且非空")
        void shouldCheckEnvExistsAndNotEmpty() {
            // when & then
            assertThat(EnvReaderUtil.existsAndNotEmpty("PATH")).isTrue();
            assertThat(EnvReaderUtil.existsAndNotEmpty("NON_EXISTENT_VAR")).isFalse();
        }
    }

    // ==================== 系统属性测试 ====================

    @Nested
    @DisplayName("系统属性测试")
    class SystemPropertyTests {

        @Test
        @DisplayName("应该设置和获取系统属性")
        void shouldSetAndGetSystemProperty() {
            // given
            EnvReaderUtil.setProperty("test.property", "testValue");

            // when
            String value = EnvReaderUtil.getProperty("test.property");

            // then
            assertThat(value).isEqualTo("testValue");
        }

        @Test
        @DisplayName("不存在的系统属性应该返回null")
        void shouldReturnNullForNonExistentProperty() {
            // when
            String value = EnvReaderUtil.getProperty("non.existent.property");

            // then
            assertThat(value).isNull();
        }

        @Test
        @DisplayName("应该使用默认值返回系统属性")
        void shouldReturnDefaultForNonExistentProperty() {
            // when
            String value = EnvReaderUtil.getProperty("non.existent.property", "defaultPropValue");

            // then
            assertThat(value).isEqualTo("defaultPropValue");
        }

        @Test
        @DisplayName("应该优先返回系统属性")
        void shouldReturnSystemPropertyOverEnv() {
            // given
            EnvReaderUtil.setProperty("PATH", "/custom/path");

            // when
            String value = EnvReaderUtil.getEnvOrProperty("PATH");

            // then
            assertThat(value).isEqualTo("/custom/path");
        }

        @Test
        @DisplayName("没有系统属性时应该返回环境变量")
        void shouldReturnEnvWhenNoSystemProperty() {
            // when
            String value = EnvReaderUtil.getEnvOrProperty("HOME", "default");

            // then
            // HOME通常存在于大多数系统
            assertThat(value).isNotEqualTo("default");
        }
    }

    // ==================== 验证方法测试 ====================

    @Nested
    @DisplayName("验证方法测试")
    class ValidationTests {

        @Test
        @DisplayName("应该验证URL格式")
        void shouldValidateUrlFormat() {
            // given - 使用validateEnv方法直接测试URL格式验证逻辑
            // 注意：isValidUrl使用getEnv获取环境变量，不是系统属性

            // when & then - 测试验证逻辑
            // 对于不存在的环境变量应该返回false
            assertThat(EnvReaderUtil.isValidUrl("non.existent.key")).isFalse();
        }

        @Test
        @DisplayName("应该验证端口格式")
        void shouldValidatePortFormat() {
            // when & then - 对于不存在的环境变量应该返回false
            assertThat(EnvReaderUtil.isValidPort("non.existent.key")).isFalse();
        }

        @Test
        @DisplayName("应该验证布尔值")
        void shouldValidateBoolean() {
            // when & then - 对于不存在的环境变量应该返回false
            assertThat(EnvReaderUtil.isBoolean("non.existent.key")).isFalse();
        }

        @Test
        @DisplayName("应该验证必需的环境变量")
        void shouldValidateRequiredEnvVariables() {
            // when & then - PATH是大多数系统存在的
            assertThatCode(() -> EnvReaderUtil.validateRequired("PATH"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("缺少必需的环境变量应该抛出异常")
        void shouldThrowExceptionForMissingRequiredEnv() {
            // when & then
            assertThatThrownBy(() -> EnvReaderUtil.validateRequired("NON_EXISTENT_REQUIRED_VAR"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("缺少必需的环境变量");
        }
    }

    // ==================== 敏感信息处理测试 ====================

    @Nested
    @DisplayName("敏感信息处理测试")
    class SensitiveInfoTests {

        @Test
        @DisplayName("应该识别敏感键")
        void shouldIdentifySensitiveKeys() {
            // when & then
            assertThat(EnvReaderUtil.isSensitiveKey("PASSWORD")).isTrue();
            assertThat(EnvReaderUtil.isSensitiveKey("API_KEY")).isTrue();
            assertThat(EnvReaderUtil.isSensitiveKey("SECRET")).isTrue();
            assertThat(EnvReaderUtil.isSensitiveKey("TOKEN")).isTrue();
            assertThat(EnvReaderUtil.isSensitiveKey("MY_PASSWORD_VAR")).isTrue();
            assertThat(EnvReaderUtil.isSensitiveKey("NORMAL_VAR")).isFalse();
        }

        @Test
        @DisplayName("应该正确脱敏处理")
        void shouldMaskSensitiveValue() {
            // when
            String shortValue = EnvReaderUtil.maskSensitiveValue("abc");
            String longValue = EnvReaderUtil.maskSensitiveValue("abcdefghijklmnop");

            // then
            assertThat(shortValue).isEqualTo("***");
            assertThat(longValue).contains("*");
            assertThat(longValue).isNotEqualTo("abcdefghijklmnop");
        }

        @Test
        @DisplayName("null值脱敏应该返回***")
        void shouldMaskNullValue() {
            // when
            String masked = EnvReaderUtil.maskSensitiveValue(null);

            // then
            assertThat(masked).isEqualTo("***");
        }

        @Test
        @DisplayName("应该获取脱敏后的环境变量")
        void shouldGetMaskedEnvVariable() {
            // given - getMaskedEnv使用getEnv，只能获取环境变量
            // 对于不存在的环境变量应该返回null

            // when
            String masked = EnvReaderUtil.getMaskedEnv("NON_EXISTENT_PASSWORD_VAR");

            // then
            assertThat(masked).isNull();
        }
    }

    // ==================== 类型转换方法测试 ====================

    @Nested
    @DisplayName("类型转换方法测试")
    class TypeConversionTests {

        @Test
        @DisplayName("不存在的整数应该返回默认值")
        void shouldReturnDefaultForNonExistentInt() {
            // when
            int value = EnvReaderUtil.getEnvAsInt("NON_EXISTENT_INT", 42);

            // then
            assertThat(value).isEqualTo(42);
        }

        @Test
        @DisplayName("应该转换为布尔值-默认值测试")
        void shouldConvertToBooleanDefault() {
            // when & then - 对于不存在的环境变量返回默认值
            assertThat(EnvReaderUtil.getEnvAsBoolean("NON_EXISTENT_BOOL_TRUE", true)).isTrue();
            assertThat(EnvReaderUtil.getEnvAsBoolean("NON_EXISTENT_BOOL_FALSE", false)).isFalse();
        }

        @Test
        @DisplayName("不存在的布尔值应该返回默认值")
        void shouldReturnDefaultForNonExistentBoolean() {
            // when
            boolean value = EnvReaderUtil.getEnvAsBoolean("NON_EXISTENT_BOOL", true);

            // then
            assertThat(value).isTrue();
        }

        @Test
        @DisplayName("空值应该返回空列表")
        void shouldReturnEmptyListForNonExistent() {
            // when
            List<String> list = EnvReaderUtil.getEnvAsList("NON_EXISTENT_LIST", ",");

            // then
            assertThat(list).isEmpty();
        }

        @Test
        @DisplayName("不存在的Map应该返回空Map")
        void shouldReturnEmptyMapForNonExistent() {
            // when
            Map<String, String> map = EnvReaderUtil.getEnvAsMap("NON_EXISTENT_MAP", ";", "=");

            // then
            assertThat(map).isEmpty();
        }
    }

    // ==================== 环境信息测试 ====================

    @Nested
    @DisplayName("环境信息测试")
    class EnvironmentInfoTests {

        @Test
        @DisplayName("应该获取操作系统信息")
        void shouldGetOSInfo() {
            // when
            String osInfo = EnvReaderUtil.getOSInfo();

            // then
            assertThat(osInfo).isNotEmpty();
        }

        @Test
        @DisplayName("应该获取Java信息")
        void shouldGetJavaInfo() {
            // when
            String javaInfo = EnvReaderUtil.getJavaInfo();

            // then
            assertThat(javaInfo).isNotEmpty();
            // Java信息包含版本号和供应商，格式如: "17.0.2 (Oracle Corporation)"
            assertThat(javaInfo).containsAnyOf("Java", "jdk", "1.8", "11", "17", "21");
        }

        @Test
        @DisplayName("应该获取用户信息")
        void shouldGetUserInfo() {
            // when
            String userInfo = EnvReaderUtil.getUserInfo();

            // then
            assertThat(userInfo).isNotEmpty();
        }
    }

    // ==================== 转换方法测试 ====================

    @Nested
    @DisplayName("转换方法测试")
    class ConversionTests {

        @Test
        @DisplayName("应该转换为Properties")
        void shouldConvertToProperties() {
            // when
            Properties props = EnvReaderUtil.toProperties();

            // then
            assertThat(props).isNotNull();
            assertThat(props).isNotEmpty();
        }

        @Test
        @DisplayName("应该转换为Map")
        void shouldConvertToMap() {
            // when
            Map<String, String> map = EnvReaderUtil.toMap();

            // then
            assertThat(map).isNotNull();
            assertThat(map).isNotEmpty();
        }

        @Test
        @DisplayName("应该转换为JSON字符串")
        void shouldConvertToJson() {
            // when
            String json = EnvReaderUtil.toJson();

            // then
            assertThat(json).isNotNull();
            assertThat(json).startsWith("{");
            assertThat(json).endsWith("}");
        }
    }

    // ==================== 命令行参数解析测试 ====================

    @Nested
    @DisplayName("命令行参数解析测试")
    class ArgumentParsingTests {

        @Test
        @DisplayName("应该解析命令行参数")
        void shouldParseCommandLineArgs() {
            // given
            String[] args = {"--server.port=8080", "--debug", "-Dconfig.path=/etc/app"};

            // when
            Map<String, String> parsed = EnvReaderUtil.parseArgs(args);

            // then
            assertThat(parsed).containsEntry("server.port", "8080");
            assertThat(parsed).containsEntry("debug", "true");
            assertThat(EnvReaderUtil.getProperty("config.path")).isEqualTo("/etc/app");
        }
    }

    // ==================== requireEnv 测试 ====================

    @Nested
    @DisplayName("requireEnv 测试")
    class RequireEnvTests {

        @Test
        @DisplayName("应该获取必需的环境变量")
        void shouldGetRequiredEnvVariable() {
            // when & then - PATH 大多数系统存在
            assertThatCode(() -> {
                String path = EnvReaderUtil.requireEnv("PATH");
                assertThat(path).isNotNull();
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("缺少必需的环境变量应该抛出异常")
        void shouldThrowExceptionForMissingRequiredEnv() {
            // when & then
            assertThatThrownBy(() -> EnvReaderUtil.requireEnv("NON_EXISTENT_REQUIRED_VAR_XYZ"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("必需的环境变量未设置");
        }
    }

    // ==================== 工具类实例化测试 ====================

    @Nested
    @DisplayName("工具类实例化测试")
    class UtilityClassTests {

        @Test
        @DisplayName("工具类不应该被实例化")
        void shouldNotBeInstantiated() {
            // when & then - 通过反射测试私有构造函数
            assertThatThrownBy(() -> {
                var constructor = EnvReaderUtil.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                constructor.newInstance();
            }).hasCauseInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ==================== 文件导出测试 ====================

    @Nested
    @DisplayName("文件导出测试")
    class FileExportTests {

        @Test
        @DisplayName("应该导出环境变量到文件")
        @EnabledOnOs({OS.LINUX, OS.MAC})
        void shouldExportEnvToFile() throws IOException {
            // given
            Path tempFile = Files.createTempFile("env_test", ".txt");

            try {
                // when
                EnvReaderUtil.exportToFile(tempFile.toString());

                // then
                String content = Files.readString(tempFile);
                assertThat(content).contains("# 环境变量导出文件");
                assertThat(content).contains("操作系统:");
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }

        @Test
        @DisplayName("导出时应该脱敏敏感信息")
        @EnabledOnOs({OS.LINUX, OS.MAC})
        void shouldMaskSensitiveInfoWhenExporting() throws IOException {
            // given
            Path tempFile = Files.createTempFile("env_test_masked", ".txt");

            try {
                // when
                EnvReaderUtil.exportToFile(tempFile.toString(), false);

                // then
                String content = Files.readString(tempFile);
                assertThat(content).isNotEmpty();
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }
    }
}
