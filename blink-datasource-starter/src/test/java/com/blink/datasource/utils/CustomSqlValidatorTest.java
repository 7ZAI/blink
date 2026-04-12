package com.blink.datasource.utils;

import com.blink.datasource.constants.DataSourceConstant;
import com.blink.framework.common.exception.BlinkException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CustomSqlValidator 单元测试
 * 验证 SQL 片段安全性校验逻辑，防止 SQL 注入攻击
 *
 * @author binblink
 * @since 2026-04-12
 */
@DisplayName("CustomSqlValidator 单元测试")
class CustomSqlValidatorTest {

    // ==================== 边界条件测试 ====================

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryConditionsTest {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("TC-001~TC-003: null或空白字符串应该直接返回不抛异常")
        void validate_whenInputIsNullOrBlank_shouldNotThrowException(String input) {
            // when & then
            assertThatCode(() -> CustomSqlValidator.validate(input))
                    .doesNotThrowAnyException();
        }
    }

    // ==================== 安全字符测试 ====================

    @Nested
    @DisplayName("安全字符测试")
    class SafeCharactersTest {

        @Test
        @DisplayName("TC-004: 安全字符-字母数字下划线")
        void validate_whenContainsSafeCharacters_shouldNotThrowException() {
            // given
            String sqlFragment = "user_id = 123";

            // when & then
            assertThatCode(() -> CustomSqlValidator.validate(sqlFragment))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("TC-005: 安全字符-比较运算符")
        void validate_whenContainsComparisonOperators_shouldNotThrowException() {
            // given
            String sqlFragment = "age > 18 AND age <= 60";

            // when & then
            assertThatCode(() -> CustomSqlValidator.validate(sqlFragment))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("TC-006: 安全字符-括号引号")
        void validate_whenContainsParenthesesAndQuotes_shouldNotThrowException() {
            // given - 使用ASCII字符，中文字符不在白名单中
            String sqlFragment = "name = 'admin'";

            // when & then
            assertThatCode(() -> CustomSqlValidator.validate(sqlFragment))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("安全字符-算术运算符")
        void validate_whenContainsArithmeticOperators_shouldNotThrowException() {
            // given
            String sqlFragment = "price * 1.1 + 10";

            // when & then
            assertThatCode(() -> CustomSqlValidator.validate(sqlFragment))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("安全字符-复杂条件表达式")
        void validate_whenContainsComplexExpression_shouldNotThrowException() {
            // given
            String sqlFragment = "(status = 1 OR status = 2) AND create_time >= '2024-01-01'";

            // when & then
            assertThatCode(() -> CustomSqlValidator.validate(sqlFragment))
                    .doesNotThrowAnyException();
        }
    }

    // ==================== 危险关键字测试 ====================

    @Nested
    @DisplayName("危险关键字测试")
    class DangerousKeywordsTest {

        @ParameterizedTest
        @ValueSource(strings = {
                "SELECT * FROM users",
                "select * from users",
                "Select * From Users",
                "1 = 1 SELECT 1",
                "id IN (SELECT id FROM other)"
        })
        @DisplayName("TC-007: 危险关键字-SELECT（含大小写混合）")
        void validate_whenContainsSelectKeyword_shouldThrowBlinkException(String sqlFragment) {
            // when & then
            assertThatThrownBy(() -> CustomSqlValidator.validate(sqlFragment))
                    .isInstanceOf(BlinkException.class)
                    .hasFieldOrPropertyWithValue("code", DataSourceConstant.DATA_SCOPE_SQL_FRAGMENT_INVALID);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "INSERT INTO users VALUES(1)",
                "insert into users",
                "1; INSERT INTO users"
        })
        @DisplayName("TC-008: 危险关键字-INSERT")
        void validate_whenContainsInsertKeyword_shouldThrowBlinkException(String sqlFragment) {
            // when & then
            assertThatThrownBy(() -> CustomSqlValidator.validate(sqlFragment))
                    .isInstanceOf(BlinkException.class)
                    .hasFieldOrPropertyWithValue("code", DataSourceConstant.DATA_SCOPE_SQL_FRAGMENT_INVALID);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "UPDATE users SET name='hacked'",
                "update users set",
                "1; UPDATE users"
        })
        @DisplayName("TC-009: 危险关键字-UPDATE")
        void validate_whenContainsUpdateKeyword_shouldThrowBlinkException(String sqlFragment) {
            // when & then
            assertThatThrownBy(() -> CustomSqlValidator.validate(sqlFragment))
                    .isInstanceOf(BlinkException.class)
                    .hasFieldOrPropertyWithValue("code", DataSourceConstant.DATA_SCOPE_SQL_FRAGMENT_INVALID);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "DELETE FROM users",
                "delete from users",
                "1; DELETE FROM users"
        })
        @DisplayName("TC-010: 危险关键字-DELETE")
        void validate_whenContainsDeleteKeyword_shouldThrowBlinkException(String sqlFragment) {
            // when & then
            assertThatThrownBy(() -> CustomSqlValidator.validate(sqlFragment))
                    .isInstanceOf(BlinkException.class)
                    .hasFieldOrPropertyWithValue("code", DataSourceConstant.DATA_SCOPE_SQL_FRAGMENT_INVALID);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "DROP TABLE users",
                "drop table users",
                "DROP DATABASE test"
        })
        @DisplayName("TC-011: 危险关键字-DROP")
        void validate_whenContainsDropKeyword_shouldThrowBlinkException(String sqlFragment) {
            // when & then
            assertThatThrownBy(() -> CustomSqlValidator.validate(sqlFragment))
                    .isInstanceOf(BlinkException.class)
                    .hasFieldOrPropertyWithValue("code", DataSourceConstant.DATA_SCOPE_SQL_FRAGMENT_INVALID);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "1 UNION SELECT * FROM users",
                "1 union select password from users",
                "1 UNION ALL SELECT 1"
        })
        @DisplayName("TC-012: 危险关键字-UNION注入")
        void validate_whenContainsUnionKeyword_shouldThrowBlinkException(String sqlFragment) {
            // when & then
            assertThatThrownBy(() -> CustomSqlValidator.validate(sqlFragment))
                    .isInstanceOf(BlinkException.class)
                    .hasFieldOrPropertyWithValue("code", DataSourceConstant.DATA_SCOPE_SQL_FRAGMENT_INVALID);
        }
    }

    // ==================== SQL注释测试 ====================

    @Nested
    @DisplayName("SQL注释测试")
    class SqlCommentTest {

        @ParameterizedTest
        @ValueSource(strings = {
                "1 -- comment",
                "1--comment",
                "id = 1 --",
                "-- 单行注释"
        })
        @DisplayName("TC-013: SQL注释-单行注释")
        void validate_whenContainsSingleLineComment_shouldThrowBlinkException(String sqlFragment) {
            // when & then
            assertThatThrownBy(() -> CustomSqlValidator.validate(sqlFragment))
                    .isInstanceOf(BlinkException.class)
                    .hasFieldOrPropertyWithValue("code", DataSourceConstant.DATA_SCOPE_SQL_FRAGMENT_INVALID);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "1 /* comment */",
                "/* 多行注释 */",
                "id = 1/*注入*/"
        })
        @DisplayName("TC-014: SQL注释-多行注释")
        void validate_whenContainsMultiLineComment_shouldThrowBlinkException(String sqlFragment) {
            // when & then
            assertThatThrownBy(() -> CustomSqlValidator.validate(sqlFragment))
                    .isInstanceOf(BlinkException.class)
                    .hasFieldOrPropertyWithValue("code", DataSourceConstant.DATA_SCOPE_SQL_FRAGMENT_INVALID);
        }
    }

    // ==================== 存储过程和系统函数测试 ====================

    @Nested
    @DisplayName("存储过程和系统函数测试")
    class ProcedureAndFunctionTest {

        @ParameterizedTest
        @ValueSource(strings = {
                "EXEC xp_cmdshell 'dir'",
                "exec sp_who",
                "EXECUTE sp_help"
        })
        @DisplayName("TC-015: 存储过程调用")
        void validate_whenContainsExecKeyword_shouldThrowBlinkException(String sqlFragment) {
            // when & then
            assertThatThrownBy(() -> CustomSqlValidator.validate(sqlFragment))
                    .isInstanceOf(BlinkException.class)
                    .hasFieldOrPropertyWithValue("code", DataSourceConstant.DATA_SCOPE_SQL_FRAGMENT_INVALID);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "SLEEP(5)",
                "sleep(10)",
                "1 AND SLEEP(5)"
        })
        @DisplayName("TC-016: 时间盲注-SLEEP")
        void validate_whenContainsSleepKeyword_shouldThrowBlinkException(String sqlFragment) {
            // when & then
            assertThatThrownBy(() -> CustomSqlValidator.validate(sqlFragment))
                    .isInstanceOf(BlinkException.class)
                    .hasFieldOrPropertyWithValue("code", DataSourceConstant.DATA_SCOPE_SQL_FRAGMENT_INVALID);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "BENCHMARK(10000000,SHA1('test'))",
                "benchmark(1000,1)"
        })
        @DisplayName("TC-017: 时间盲注-BENCHMARK")
        void validate_whenContainsBenchmarkKeyword_shouldThrowBlinkException(String sqlFragment) {
            // when & then
            assertThatThrownBy(() -> CustomSqlValidator.validate(sqlFragment))
                    .isInstanceOf(BlinkException.class)
                    .hasFieldOrPropertyWithValue("code", DataSourceConstant.DATA_SCOPE_SQL_FRAGMENT_INVALID);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "xp_cmdshell 'dir'",
                "xp_loginconfig",
                "sp_who"
        })
        @DisplayName("系统存储过程前缀")
        void validate_whenContainsXpSpPrefix_shouldThrowBlinkException(String sqlFragment) {
            // when & then
            assertThatThrownBy(() -> CustomSqlValidator.validate(sqlFragment))
                    .isInstanceOf(BlinkException.class)
                    .hasFieldOrPropertyWithValue("code", DataSourceConstant.DATA_SCOPE_SQL_FRAGMENT_INVALID);
        }
    }

    // ==================== 文件操作测试 ====================

    @Nested
    @DisplayName("文件操作测试")
    class FileOperationTest {

        @ParameterizedTest
        @ValueSource(strings = {
                "INTO OUTFILE '/tmp/file.txt'",
                "into outfile '/var/www/shell.php'",
                "SELECT 1 INTO OUTFILE 'test'"
        })
        @DisplayName("TC-018: 文件操作-INTO OUTFILE")
        void validate_whenContainsIntoOutfile_shouldThrowBlinkException(String sqlFragment) {
            // when & then
            assertThatThrownBy(() -> CustomSqlValidator.validate(sqlFragment))
                    .isInstanceOf(BlinkException.class)
                    .hasFieldOrPropertyWithValue("code", DataSourceConstant.DATA_SCOPE_SQL_FRAGMENT_INVALID);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "FROM INFORMATION_SCHEMA.TABLES",
                "from information_schema.columns",
                "INFORMATION_SCHEMA.SCHEMATA"
        })
        @DisplayName("TC-019: 信息泄露-INFORMATION_SCHEMA")
        void validate_whenContainsInformationSchema_shouldThrowBlinkException(String sqlFragment) {
            // when & then
            assertThatThrownBy(() -> CustomSqlValidator.validate(sqlFragment))
                    .isInstanceOf(BlinkException.class)
                    .hasFieldOrPropertyWithValue("code", DataSourceConstant.DATA_SCOPE_SQL_FRAGMENT_INVALID);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "LOAD_FILE('/etc/passwd')",
                "load_file('c:\\\\windows\\\\system32\\\\config\\\\sam')"
        })
        @DisplayName("文件读取-LOAD_FILE")
        void validate_whenContainsLoadFile_shouldThrowBlinkException(String sqlFragment) {
            // when & then
            assertThatThrownBy(() -> CustomSqlValidator.validate(sqlFragment))
                    .isInstanceOf(BlinkException.class)
                    .hasFieldOrPropertyWithValue("code", DataSourceConstant.DATA_SCOPE_SQL_FRAGMENT_INVALID);
        }
    }

    // ==================== 其他注入测试 ====================

    @Nested
    @DisplayName("其他注入测试")
    class OtherInjectionTest {

        @Test
        @DisplayName("TC-021: 非白名单特殊字符")
        void validate_whenContainsNonWhitelistCharacters_shouldThrowBlinkException() {
            // given - 中文字符不在白名单范围内
            String sqlFragment = "name = '中文测试'";

            // when & then
            assertThatThrownBy(() -> CustomSqlValidator.validate(sqlFragment))
                    .isInstanceOf(BlinkException.class)
                    .hasFieldOrPropertyWithValue("code", DataSourceConstant.DATA_SCOPE_SQL_FRAGMENT_INVALID);
        }

        @Test
        @DisplayName("语句分隔符-分号")
        void validate_whenContainsSemicolon_shouldThrowBlinkException() {
            // given
            String sqlFragment = "1; DROP TABLE users";

            // when & then
            assertThatThrownBy(() -> CustomSqlValidator.validate(sqlFragment))
                    .isInstanceOf(BlinkException.class)
                    .hasFieldOrPropertyWithValue("code", DataSourceConstant.DATA_SCOPE_SQL_FRAGMENT_INVALID);
        }

        @Test
        @DisplayName("WAITFOR延时注入")
        void validate_whenContainsWaitfor_shouldThrowBlinkException() {
            // given
            String sqlFragment = "WAITFOR DELAY '0:0:5'";

            // when & then
            assertThatThrownBy(() -> CustomSqlValidator.validate(sqlFragment))
                    .isInstanceOf(BlinkException.class)
                    .hasFieldOrPropertyWithValue("code", DataSourceConstant.DATA_SCOPE_SQL_FRAGMENT_INVALID);
        }
    }

    // ==================== 综合场景测试 ====================

    @Nested
    @DisplayName("综合场景测试")
    class ComprehensiveTest {

        @Test
        @DisplayName("合法的复杂条件表达式")
        void validate_whenContainsLegalComplexExpression_shouldNotThrowException() {
            // given - 使用ASCII字符，避免冒号等非白名单字符
            String sqlFragment = "(dept_id IN (1,2,3) OR create_by = 'admin') AND status = 1 AND create_time >= '2024-01-01'";

            // when & then
            assertThatCode(() -> CustomSqlValidator.validate(sqlFragment))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("合法的IN条件")
        void validate_whenContainsInCondition_shouldNotThrowException() {
            // given
            String sqlFragment = "id IN (1, 2, 3, 4, 5)";

            // when & then
            assertThatCode(() -> CustomSqlValidator.validate(sqlFragment))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("合法的BETWEEN条件")
        void validate_whenContainsBetweenCondition_shouldNotThrowException() {
            // given
            String sqlFragment = "age BETWEEN 18 AND 60";

            // when & then
            assertThatCode(() -> CustomSqlValidator.validate(sqlFragment))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("合法的LIKE条件")
        void validate_whenContainsLikeCondition_shouldNotThrowException() {
            // given - 注意LIKE不是关键字，%在白名单中
            String sqlFragment = "name LIKE '%test%'";

            // when & then
            assertThatCode(() -> CustomSqlValidator.validate(sqlFragment))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("合法的IS NULL条件")
        void validate_whenContainsIsNullCondition_shouldNotThrowException() {
            // given
            String sqlFragment = "deleted_at IS NULL AND status IS NOT NULL";

            // when & then
            assertThatCode(() -> CustomSqlValidator.validate(sqlFragment))
                    .doesNotThrowAnyException();
        }
    }
}
