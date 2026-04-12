package com.blink.log.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LogType 枚举类测试
 *
 * @author binblink
 */
@DisplayName("LogType 枚举类测试")
class LogTypeTest {

    @Test
    @DisplayName("枚举值验证")
    void enumValues_shouldBeCorrect() {
        // then
        assertThat(LogType.LOGIN.getCode()).isEqualTo("LOGIN");
        assertThat(LogType.LOGIN.getDescription()).isEqualTo("登入日志");

        assertThat(LogType.SYSTEM.getCode()).isEqualTo("SYSTEM");
        assertThat(LogType.SYSTEM.getDescription()).isEqualTo("系统日志");

        assertThat(LogType.OPERATION.getCode()).isEqualTo("OPERATION");
        assertThat(LogType.OPERATION.getDescription()).isEqualTo("操作日志");
    }

    @Test
    @DisplayName("枚举数量验证")
    void enumCount_shouldBeThree() {
        // when
        LogType[] values = LogType.values();

        // then
        assertThat(values).hasSize(3);
        assertThat(values).containsExactly(LogType.LOGIN, LogType.SYSTEM, LogType.OPERATION);
    }

    @ParameterizedTest
    @CsvSource({
            "LOGIN, LOGIN",
            "SYSTEM, SYSTEM",
            "OPERATION, OPERATION",
            "INVALID, OPERATION",  // 无效code返回默认值OPERATION
            "'', OPERATION",       // 空字符串返回默认值
            "login, OPERATION"     // 大小写敏感，返回默认值
    })
    @DisplayName("getByCode - 根据编码获取枚举")
    void getByCode_shouldReturnCorrectEnum(String code, String expectedCode) {
        // when
        LogType result = LogType.getByCode(code);

        // then
        assertThat(result.getCode()).isEqualTo(expectedCode);
    }

    @Test
    @DisplayName("getByCode - null返回默认值")
    void getByCode_null_shouldReturnDefault() {
        // when
        LogType result = LogType.getByCode(null);

        // then
        assertThat(result).isEqualTo(LogType.OPERATION);
    }
}
