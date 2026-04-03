package com.blink.gateway.datascope;

import com.blink.base.datascope.handler.RelationFilterHandler;
import com.blink.datasource.data.DataScopeParseResult;
import com.blink.datasource.data.RuleConfig;
import com.blink.datasource.data.UserDataScopeInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RelationFilterHandler 单元测试类
 * 测试关联过滤规则处理器的各项功能
 *
 * @author binblink
 */
@DisplayName("RelationFilterHandler 单元测试")
class RelationFilterHandlerTest {

    private RelationFilterHandler relationFilterHandler;

    @BeforeEach
    void setUp() {
        relationFilterHandler = new RelationFilterHandler();
    }

    // ==================== SQL生成测试 ====================

    @Test
    @DisplayName("测试 CURRENT_USER 类型 - 生成正确的EXISTS子查询")
    void testCurrentUserMatch() {
        // 准备测试数据
        RuleConfig config = createValidConfig();
        config.setRelationMatchType("CURRENT_USER");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, "t");

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user t WHERE 1=1");

        // 执行测试
        relationFilterHandler.apply(sql, config, context);

        // 验证结果
        String result = sql.toString();
        assertTrue(result.contains("EXISTS"));
        assertTrue(result.contains("sys_user_role_rela"));
        assertTrue(result.contains("r.user_id = t.user_id"));
        assertTrue(result.contains("r.role_id = 100"));
    }

    @Test
    @DisplayName("测试 CURRENT_DEPT 类型 - 生成正确的EXISTS子查询")
    void testCurrentDeptMatch() {
        // 准备测试数据
        RuleConfig config = createValidConfig();
        config.setRelationMatchType("CURRENT_DEPT");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setDeptId(50);

        DataScopeParseResult context = createMockContext(userInfo, "t");

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user t WHERE 1=1");

        // 执行测试
        relationFilterHandler.apply(sql, config, context);

        // 验证结果
        String result = sql.toString();
        assertTrue(result.contains("EXISTS"));
        assertTrue(result.contains("r.role_id = 50"));
    }

    @Test
    @DisplayName("测试 USER_LIST 类型 - 生成正确的IN条件")
    void testUserListMatch() {
        // 准备测试数据
        RuleConfig config = createValidConfig();
        config.setRelationMatchType("USER_LIST");
        config.setRelationMatchValues(Arrays.asList(1, 2, 3));

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, "t");

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user t WHERE 1=1");

        // 执行测试
        relationFilterHandler.apply(sql, config, context);

        // 验证结果
        String result = sql.toString();
        assertTrue(result.contains("EXISTS"));
        assertTrue(result.contains("r.role_id IN (1, 2, 3)"));
    }

    @Test
    @DisplayName("测试 ROLE_LIST 类型 - 生成正确的IN条件")
    void testRoleListMatch() {
        // 准备测试数据
        RuleConfig config = createValidConfig();
        config.setRelationMatchType("ROLE_LIST");
        config.setRelationMatchValues(Arrays.asList(10, 20, 30));

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, "t");

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user t WHERE 1=1");

        // 执行测试
        relationFilterHandler.apply(sql, config, context);

        // 验证结果
        String result = sql.toString();
        assertTrue(result.contains("EXISTS"));
        assertTrue(result.contains("r.role_id IN (10, 20, 30)"));
    }

    @Test
    @DisplayName("测试无表别名 - 使用默认别名t")
    void testDefaultTableAlias() {
        // 准备测试数据
        RuleConfig config = createValidConfig();
        config.setRelationMatchType("CURRENT_USER");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");

        // 执行测试
        relationFilterHandler.apply(sql, config, context);

        // 验证结果
        String result = sql.toString();
        assertTrue(result.contains("r.user_id = t.user_id"));
    }

    // ==================== 配置校验测试 ====================

    @Test
    @DisplayName("测试空关联表名 - 不生成条件")
    void testEmptyRelationTable() {
        RuleConfig config = createValidConfig();
        config.setRelationTable("");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, "t");

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user t WHERE 1=1");
        String originalSql = sql.toString();

        relationFilterHandler.apply(sql, config, context);

        assertEquals(originalSql, sql.toString());
    }

    @Test
    @DisplayName("测试空源字段名 - 不生成条件")
    void testEmptySourceField() {
        RuleConfig config = createValidConfig();
        config.setSourceField("");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, "t");

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user t WHERE 1=1");
        String originalSql = sql.toString();

        relationFilterHandler.apply(sql, config, context);

        assertEquals(originalSql, sql.toString());
    }

    @Test
    @DisplayName("测试空匹配类型 - 不生成条件")
    void testEmptyMatchType() {
        RuleConfig config = createValidConfig();
        config.setRelationMatchType("");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, "t");

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user t WHERE 1=1");
        String originalSql = sql.toString();

        relationFilterHandler.apply(sql, config, context);

        assertEquals(originalSql, sql.toString());
    }

    // ==================== SQL安全校验测试 ====================

    @Test
    @DisplayName("测试SQL注入 - 非法表名应被拒绝")
    void testSqlInjectionInvalidTableName() {
        RuleConfig config = createValidConfig();
        config.setRelationTable("sys_user; DROP TABLE sys_user;--");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, "t");

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user t WHERE 1=1");
        String originalSql = sql.toString();

        relationFilterHandler.apply(sql, config, context);

        // 非法表名应该被拒绝，SQL不变
        assertEquals(originalSql, sql.toString());
    }

    @Test
    @DisplayName("测试SQL注入 - 非法字段名应被拒绝")
    void testSqlInjectionInvalidFieldName() {
        RuleConfig config = createValidConfig();
        config.setSourceField("user_id; DELETE FROM sys_user");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, "t");

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user t WHERE 1=1");
        String originalSql = sql.toString();

        relationFilterHandler.apply(sql, config, context);

        // 非法字段名应该被拒绝
        assertEquals(originalSql, sql.toString());
    }

    @Test
    @DisplayName("测试SQL注入 - 非法表别名应被拒绝")
    void testSqlInjectionInvalidTableAlias() {
        RuleConfig config = createValidConfig();
        config.setRelationMatchType("CURRENT_USER");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        // 使用非法别名
        DataScopeParseResult context = createMockContext(userInfo, "t; DROP TABLE");

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user t WHERE 1=1");
        String originalSql = sql.toString();

        relationFilterHandler.apply(sql, config, context);

        // 非法别名应该被拒绝
        assertEquals(originalSql, sql.toString());
    }

    @Test
    @DisplayName("测试合法的表名字段名 - 包含下划线和数字")
    void testValidIdentifierWithUnderscoreAndNumbers() {
        RuleConfig config = new RuleConfig();
        config.setRelationTable("sys_user_role_rela");
        config.setSourceField("user_id_2");
        config.setRelationSourceField("user_id");
        config.setRelationTargetField("role_id");
        config.setRelationMatchType("CURRENT_USER");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, "t1");

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user t1 WHERE 1=1");

        relationFilterHandler.apply(sql, config, context);

        // 应该成功生成SQL
        assertTrue(sql.toString().contains("EXISTS"));
    }

    // ==================== 空值处理测试 ====================

    @Test
    @DisplayName("测试 CURRENT_USER - 用户ID为空不生成条件")
    void testCurrentUserWithNullUserId() {
        RuleConfig config = createValidConfig();
        config.setRelationMatchType("CURRENT_USER");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(null);

        DataScopeParseResult context = createMockContext(userInfo, "t");

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user t WHERE 1=1");
        String originalSql = sql.toString();

        relationFilterHandler.apply(sql, config, context);

        assertEquals(originalSql, sql.toString());
    }

    @Test
    @DisplayName("测试 CURRENT_DEPT - 部门ID为空不生成条件")
    void testCurrentDeptWithNullDeptId() {
        RuleConfig config = createValidConfig();
        config.setRelationMatchType("CURRENT_DEPT");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setDeptId(null);

        DataScopeParseResult context = createMockContext(userInfo, "t");

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user t WHERE 1=1");
        String originalSql = sql.toString();

        relationFilterHandler.apply(sql, config, context);

        assertEquals(originalSql, sql.toString());
    }

    @Test
    @DisplayName("测试 USER_LIST - 空列表不生成条件")
    void testUserListWithEmptyList() {
        RuleConfig config = createValidConfig();
        config.setRelationMatchType("USER_LIST");
        config.setRelationMatchValues(Collections.emptyList());

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, "t");

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user t WHERE 1=1");
        String originalSql = sql.toString();

        relationFilterHandler.apply(sql, config, context);

        assertEquals(originalSql, sql.toString());
    }

    @Test
    @DisplayName("测试 USER_LIST - null列表不生成条件")
    void testUserListWithNullList() {
        RuleConfig config = createValidConfig();
        config.setRelationMatchType("USER_LIST");
        config.setRelationMatchValues(null);

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, "t");

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user t WHERE 1=1");
        String originalSql = sql.toString();

        relationFilterHandler.apply(sql, config, context);

        assertEquals(originalSql, sql.toString());
    }

    // ==================== 未知匹配类型测试 ====================

    @Test
    @DisplayName("测试未知匹配类型 - 不生成条件")
    void testUnknownMatchType() {
        RuleConfig config = createValidConfig();
        config.setRelationMatchType("UNKNOWN_TYPE");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, "t");

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user t WHERE 1=1");
        String originalSql = sql.toString();

        relationFilterHandler.apply(sql, config, context);

        assertEquals(originalSql, sql.toString());
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建有效的配置对象
     */
    private RuleConfig createValidConfig() {
        RuleConfig config = new RuleConfig();
        config.setRelationTable("sys_user_role_rela");
        config.setSourceField("user_id");
        config.setRelationSourceField("user_id");
        config.setRelationTargetField("role_id");
        config.setRelationMatchType("CURRENT_USER");
        return config;
    }

    /**
     * 创建模拟的 DataScopeParseResult
     *
     * @param userInfo   用户信息
     * @param tableAlias 表别名
     * @return 模拟的上下文对象
     */
    private DataScopeParseResult createMockContext(UserDataScopeInfo userInfo, String tableAlias) {
        DataScopeParseResult context = new DataScopeParseResult();
        try {
            // 使用反射设置私有字段
            var userInfoField = DataScopeParseResult.class.getDeclaredField("userInfo");
            userInfoField.setAccessible(true);
            userInfoField.set(context, userInfo);

            var tableAliasField = DataScopeParseResult.class.getDeclaredField("tableAlias");
            tableAliasField.setAccessible(true);
            tableAliasField.set(context, tableAlias);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create mock context", e);
        }
        return context;
    }
}