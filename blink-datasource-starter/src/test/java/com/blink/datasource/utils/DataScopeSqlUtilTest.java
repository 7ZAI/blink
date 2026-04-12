package com.blink.datasource.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataScopeSqlUtil 单元测试
 * 验证 SQL 解析工具类的各项功能
 *
 * @author binblink
 * @since 2026-04-12
 */
@DisplayName("DataScopeSqlUtil 单元测试")
class DataScopeSqlUtilTest {

    // ==================== extractTableNames 测试 ====================

    @Nested
    @DisplayName("extractTableNames 方法测试")
    class ExtractTableNamesTest {

        @Test
        @DisplayName("TC-001: 单表查询")
        void extractTableNames_whenSingleTable_shouldReturnOneTableName() {
            // given
            String sql = "SELECT * FROM sys_user";

            // when
            Set<String> tableNames = DataScopeSqlUtil.extractTableNames(sql);

            // then
            assertThat(tableNames).containsExactly("sys_user");
        }

        @Test
        @DisplayName("TC-002: 带WHERE条件")
        void extractTableNames_whenWithWhereClause_shouldReturnOneTableName() {
            // given
            String sql = "SELECT * FROM sys_user WHERE id = 1";

            // when
            Set<String> tableNames = DataScopeSqlUtil.extractTableNames(sql);

            // then
            assertThat(tableNames).containsExactly("sys_user");
        }

        @Test
        @DisplayName("TC-003: 两表JOIN")
        void extractTableNames_whenTwoTableJoin_shouldReturnTwoTableNames() {
            // given
            String sql = "SELECT * FROM sys_user u JOIN sys_dept d ON u.dept_id = d.id";

            // when
            Set<String> tableNames = DataScopeSqlUtil.extractTableNames(sql);

            // then
            assertThat(tableNames).containsExactlyInAnyOrder("sys_user", "sys_dept");
        }

        @Test
        @DisplayName("TC-004: 三表JOIN")
        void extractTableNames_whenThreeTableJoin_shouldReturnThreeTableNames() {
            // given
            String sql = "SELECT * FROM a JOIN b ON a.id = b.id JOIN c ON b.id = c.id";

            // when
            Set<String> tableNames = DataScopeSqlUtil.extractTableNames(sql);

            // then
            assertThat(tableNames).containsExactlyInAnyOrder("a", "b", "c");
        }

        @Test
        @DisplayName("TC-005: LEFT JOIN")
        void extractTableNames_whenLeftJoin_shouldReturnAllTableNames() {
            // given
            String sql = "SELECT * FROM sys_user u LEFT JOIN sys_dept d ON u.dept_id = d.id";

            // when
            Set<String> tableNames = DataScopeSqlUtil.extractTableNames(sql);

            // then
            assertThat(tableNames).containsExactlyInAnyOrder("sys_user", "sys_dept");
        }

        @Test
        @DisplayName("TC-006: 子查询")
        void extractTableNames_whenSubquery_shouldReturnInnerTableName() {
            // given
            String sql = "SELECT * FROM (SELECT * FROM sys_user) t";

            // when
            Set<String> tableNames = DataScopeSqlUtil.extractTableNames(sql);

            // then
            assertThat(tableNames).contains("sys_user");
        }

        @ParameterizedTest
        @ValueSource(strings = {"INVALID SQL", "", "SELECT 1"})
        @DisplayName("TC-007: 无效SQL")
        void extractTableNames_whenInvalidSql_shouldReturnEmptySet(String sql) {
            // when
            Set<String> tableNames = DataScopeSqlUtil.extractTableNames(sql);

            // then
            assertThat(tableNames).isEmpty();
        }
    }

    // ==================== extractSelectPart 测试 ====================

    @Nested
    @DisplayName("extractSelectPart 方法测试")
    class ExtractSelectPartTest {

        @Test
        @DisplayName("TC-008: 指定字段")
        void extractSelectPart_whenSpecificFields_shouldReturnFieldsString() {
            // given
            String sql = "SELECT id, name FROM sys_user";

            // when
            String selectPart = DataScopeSqlUtil.extractSelectPart(sql);

            // then
            assertThat(selectPart).isEqualTo("id, name");
        }

        @Test
        @DisplayName("TC-009: SELECT *")
        void extractSelectPart_whenSelectAll_shouldReturnAsterisk() {
            // given
            String sql = "SELECT * FROM sys_user";

            // when
            String selectPart = DataScopeSqlUtil.extractSelectPart(sql);

            // then
            assertThat(selectPart).isEqualTo("*");
        }

        @Test
        @DisplayName("TC-010: 带别名")
        void extractSelectPart_whenWithAlias_shouldReturnFieldsWithAlias() {
            // given
            String sql = "SELECT u.id AS userId, u.name FROM sys_user u";

            // when
            String selectPart = DataScopeSqlUtil.extractSelectPart(sql);

            // then
            assertThat(selectPart).isEqualTo("u.id AS userId, u.name");
        }

        @Test
        @DisplayName("TC-011: 带函数")
        void extractSelectPart_whenWithFunction_shouldReturnFunctionExpression() {
            // given
            String sql = "SELECT COUNT(*), MAX(id) FROM sys_user";

            // when
            String selectPart = DataScopeSqlUtil.extractSelectPart(sql);

            // then
            assertThat(selectPart).isEqualTo("COUNT(*), MAX(id)");
        }

        @Test
        @DisplayName("TC-012: 无效SQL")
        void extractSelectPart_whenInvalidSql_shouldReturnAsterisk() {
            // given
            String sql = "INVALID SQL";

            // when
            String selectPart = DataScopeSqlUtil.extractSelectPart(sql);

            // then
            assertThat(selectPart).isEqualTo("*");
        }
    }

    // ==================== extractFromPart 测试 ====================

    @Nested
    @DisplayName("extractFromPart 方法测试")
    class ExtractFromPartTest {

        @Test
        @DisplayName("TC-013: 标准FROM")
        void extractFromPart_whenStandardFrom_shouldReturnFromPart() {
            // given
            String sql = "SELECT id FROM sys_user WHERE id = 1";

            // when
            String fromPart = DataScopeSqlUtil.extractFromPart(sql);

            // then
            assertThat(fromPart).isEqualTo(" FROM sys_user WHERE id = 1");
        }

        @Test
        @DisplayName("TC-014: 无FROM")
        void extractFromPart_whenNoFrom_shouldReturnEmptyString() {
            // given
            String sql = "SELECT 1";

            // when
            String fromPart = DataScopeSqlUtil.extractFromPart(sql);

            // then
            assertThat(fromPart).isEmpty();
        }

        @Test
        @DisplayName("TC-015: 小写from")
        void extractFromPart_whenLowercaseFrom_shouldReturnFromPart() {
            // given
            String sql = "select id from sys_user";

            // when
            String fromPart = DataScopeSqlUtil.extractFromPart(sql);

            // then
            assertThat(fromPart).isEqualTo(" from sys_user");
        }
    }

    // ==================== parseSelectFields 测试 ====================

    @Nested
    @DisplayName("parseSelectFields 方法测试")
    class ParseSelectFieldsTest {

        @Test
        @DisplayName("TC-016: 多字段")
        void parseSelectFields_whenMultipleFields_shouldReturnFieldList() {
            // given
            String selectPart = "id, name, age";

            // when
            List<String> fields = DataScopeSqlUtil.parseSelectFields(selectPart);

            // then
            assertThat(fields).containsExactly("id", "name", "age");
        }

        @Test
        @DisplayName("TC-017: 带空格")
        void parseSelectFields_whenWithSpaces_shouldTrimFields() {
            // given
            String selectPart = "id,  name , age";

            // when
            List<String> fields = DataScopeSqlUtil.parseSelectFields(selectPart);

            // then
            assertThat(fields).containsExactly("id", "name", "age");
        }

        @Test
        @DisplayName("TC-018: 星号")
        void parseSelectFields_whenAsterisk_shouldReturnAsteriskList() {
            // given
            String selectPart = "*";

            // when
            List<String> fields = DataScopeSqlUtil.parseSelectFields(selectPart);

            // then
            assertThat(fields).containsExactly("*");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("TC-019&TC-020: 空字符串或null")
        void parseSelectFields_whenNullOrEmpty_shouldReturnAsteriskList(String selectPart) {
            // when
            List<String> fields = DataScopeSqlUtil.parseSelectFields(selectPart);

            // then
            assertThat(fields).containsExactly("*");
        }
    }

    // ==================== extractFieldName 测试 ====================

    @Nested
    @DisplayName("extractFieldName 方法测试")
    class ExtractFieldNameTest {

        @Test
        @DisplayName("TC-021: 普通字段")
        void extractFieldName_whenPlainField_shouldReturnFieldName() {
            // given
            String fieldExpression = "user_id";

            // when
            String fieldName = DataScopeSqlUtil.extractFieldName(fieldExpression);

            // then
            assertThat(fieldName).isEqualTo("user_id");
        }

        @Test
        @DisplayName("TC-022: 带表别名")
        void extractFieldName_whenWithTableAlias_shouldReturnFieldName() {
            // given
            String fieldExpression = "u.user_id";

            // when
            String fieldName = DataScopeSqlUtil.extractFieldName(fieldExpression);

            // then
            assertThat(fieldName).isEqualTo("user_id");
        }

        @Test
        @DisplayName("TC-023: 带AS别名")
        void extractFieldName_whenWithAsAlias_shouldReturnOriginalFieldName() {
            // given
            String fieldExpression = "user_id AS uid";

            // when
            String fieldName = DataScopeSqlUtil.extractFieldName(fieldExpression);

            // then
            assertThat(fieldName).isEqualTo("user_id");
        }

        @Test
        @DisplayName("TC-024: 表别名+AS别名")
        void extractFieldName_whenWithTableAndAsAlias_shouldReturnFieldName() {
            // given
            String fieldExpression = "u.user_id AS uid";

            // when
            String fieldName = DataScopeSqlUtil.extractFieldName(fieldExpression);

            // then
            assertThat(fieldName).isEqualTo("user_id");
        }

        @Test
        @DisplayName("TC-025: 小写as")
        void extractFieldName_whenLowercaseAs_shouldReturnOriginalFieldName() {
            // given
            String fieldExpression = "user_id as uid";

            // when
            String fieldName = DataScopeSqlUtil.extractFieldName(fieldExpression);

            // then
            assertThat(fieldName).isEqualTo("user_id");
        }

        @Test
        @DisplayName("TC-026: 带空格")
        void extractFieldName_whenWithSpaces_shouldTrimAndReturnFieldName() {
            // given
            String fieldExpression = "  user_id  ";

            // when
            String fieldName = DataScopeSqlUtil.extractFieldName(fieldExpression);

            // then
            assertThat(fieldName).isEqualTo("user_id");
        }
    }

    // ==================== filterFields 测试 ====================

    @Nested
    @DisplayName("filterFields 方法测试")
    class FilterFieldsTest {

        @Test
        @DisplayName("TC-027: 无排除字段")
        void filterFields_whenNoExcludeFields_shouldReturnOriginalFields() {
            // given
            String selectPart = "id, name";
            List<String> excludeFields = Collections.emptyList();

            // when
            String result = DataScopeSqlUtil.filterFields(selectPart, excludeFields, null);

            // then
            assertThat(result).isEqualTo("id, name");
        }

        @Test
        @DisplayName("TC-028: 排除单个字段")
        void filterFields_whenExcludeSingleField_shouldReturnFilteredFields() {
            // given
            String selectPart = "id, name, password";
            List<String> excludeFields = Collections.singletonList("password");

            // when
            String result = DataScopeSqlUtil.filterFields(selectPart, excludeFields, null);

            // then
            assertThat(result).isEqualTo("id, name");
        }

        @Test
        @DisplayName("TC-029: 排除多个字段")
        void filterFields_whenExcludeMultipleFields_shouldReturnFilteredFields() {
            // given
            String selectPart = "id, name, password, salt";
            List<String> excludeFields = Arrays.asList("password", "salt");

            // when
            String result = DataScopeSqlUtil.filterFields(selectPart, excludeFields, null);

            // then
            assertThat(result).isEqualTo("id, name");
        }

        @Test
        @DisplayName("TC-030: 排除带表别名")
        void filterFields_whenExcludeWithTableAlias_shouldFilterCorrectly() {
            // given
            String selectPart = "u.id, u.password";
            List<String> excludeFields = Collections.singletonList("password");
            String tableAlias = "u";

            // when
            String result = DataScopeSqlUtil.filterFields(selectPart, excludeFields, tableAlias);

            // then
            assertThat(result).isEqualTo("u.id");
        }

        @Test
        @DisplayName("TC-031: 排除所有字段")
        void filterFields_whenExcludeAllFields_shouldReturnEmptyString() {
            // given
            String selectPart = "id, name";
            List<String> excludeFields = Arrays.asList("id", "name");

            // when
            String result = DataScopeSqlUtil.filterFields(selectPart, excludeFields, null);

            // then
            assertThat(result).isEmpty();
        }
    }

    // ==================== appendWhereCondition 测试 ====================

    @Nested
    @DisplayName("appendWhereCondition 方法测试")
    class AppendWhereConditionTest {

        @Test
        @DisplayName("TC-032: 无WHERE")
        void appendWhereCondition_whenNoWhere_shouldAddWhereClause() {
            // given
            StringBuilder sql = new StringBuilder("SELECT * FROM sys_user");
            String condition = "id = 1";

            // when
            DataScopeSqlUtil.appendWhereCondition(sql, condition);

            // then
            assertThat(sql.toString()).isEqualTo("SELECT * FROM sys_user WHERE id = 1");
        }

        @Test
        @DisplayName("TC-033: 已有WHERE")
        void appendWhereCondition_whenExistingWhere_shouldAddAndClause() {
            // given
            StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE status = 1");
            String condition = "id = 1";

            // when
            DataScopeSqlUtil.appendWhereCondition(sql, condition);

            // then
            assertThat(sql.toString()).contains("AND id = 1");
        }

        @Test
        @DisplayName("TC-034: 有GROUP BY")
        void appendWhereCondition_whenWithGroupBy_shouldInsertBeforeGroupBy() {
            // given
            StringBuilder sql = new StringBuilder("SELECT * FROM sys_user GROUP BY dept_id");
            String condition = "id = 1";

            // when
            DataScopeSqlUtil.appendWhereCondition(sql, condition);

            // then
            assertThat(sql.toString()).contains(" WHERE id = 1 GROUP BY");
        }

        @Test
        @DisplayName("TC-035: 有ORDER BY")
        void appendWhereCondition_whenWithOrderBy_shouldInsertBeforeOrderBy() {
            // given
            StringBuilder sql = new StringBuilder("SELECT * FROM sys_user ORDER BY id");
            String condition = "id = 1";

            // when
            DataScopeSqlUtil.appendWhereCondition(sql, condition);

            // then
            assertThat(sql.toString()).contains(" WHERE id = 1 ORDER BY");
        }

        @Test
        @DisplayName("TC-036: 有LIMIT")
        void appendWhereCondition_whenWithLimit_shouldInsertBeforeLimit() {
            // given
            StringBuilder sql = new StringBuilder("SELECT * FROM sys_user LIMIT 10");
            String condition = "id = 1";

            // when
            DataScopeSqlUtil.appendWhereCondition(sql, condition);

            // then
            assertThat(sql.toString()).contains(" WHERE id = 1 LIMIT");
        }

        @Test
        @DisplayName("TC-037: 有HAVING")
        void appendWhereCondition_whenWithHaving_shouldInsertBeforeHaving() {
            // given - HAVING通常与GROUP BY一起使用
            StringBuilder sql = new StringBuilder("SELECT dept_id, COUNT(*) FROM sys_user GROUP BY dept_id HAVING count > 1");
            String condition = "id = 1";

            // when
            DataScopeSqlUtil.appendWhereCondition(sql, condition);

            // then - WHERE应该在GROUP BY之前插入
            assertThat(sql.toString()).contains(" WHERE id = 1 GROUP BY");
        }

        @Test
        @DisplayName("TC-038: 多个子句")
        void appendWhereCondition_whenMultipleClauses_shouldInsertAtCorrectPosition() {
            // given
            StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE status = 1 ORDER BY id LIMIT 10");
            String condition = "dept_id = 1";

            // when
            DataScopeSqlUtil.appendWhereCondition(sql, condition);

            // then - 验证AND被正确添加
            assertThat(sql.toString()).contains("status = 1 AND dept_id = 1");
            assertThat(sql.toString()).contains("ORDER BY id LIMIT 10");
        }

        @Test
        @DisplayName("复杂场景-WHERE + GROUP BY + ORDER BY")
        void appendWhereCondition_whenComplexScenario_shouldInsertCorrectly() {
            // given
            StringBuilder sql = new StringBuilder("SELECT dept_id, COUNT(*) FROM sys_user WHERE status = 1 GROUP BY dept_id ORDER BY dept_id");
            String condition = "del_flag = 0";

            // when
            DataScopeSqlUtil.appendWhereCondition(sql, condition);

            // then
            assertThat(sql.toString()).contains("status = 1 AND del_flag = 0 GROUP BY");
        }
    }
}
