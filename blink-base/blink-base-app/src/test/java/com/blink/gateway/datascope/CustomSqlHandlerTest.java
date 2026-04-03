package com.blink.gateway.datascope;

import com.blink.base.datascope.handler.CustomSqlHandler;
import com.blink.datasource.data.DataScopeParseResult;
import com.blink.datasource.data.RuleConfig;
import com.blink.datasource.data.UserDataScopeInfo;
import com.blink.framework.common.exception.BlinkException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CustomSqlHandler 单元测试类
 * 测试自定义SQL规则处理器的各项功能
 *
 * @author binblink
 */
@DisplayName("CustomSqlHandler 单元测试")
class CustomSqlHandlerTest {

    private CustomSqlHandler customSqlHandler;

    @BeforeEach
    void setUp() {
        customSqlHandler = new CustomSqlHandler();
    }

    @Test
    @DisplayName("测试简单SQL片段 - 状态过滤")
    void testSimpleSqlFragment() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setSqlFragment("status = 0");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");

        // 执行测试
        customSqlHandler.apply(sql, config, context);

        // 验证结果
        assertTrue(sql.toString().contains("status = 0"));
    }

    @Test
    @DisplayName("测试SQL片段 - 带当前用户ID占位符")
    void testSqlFragmentWithCurrentUserId() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setSqlFragment("create_by = #{currentUserId}");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");

        // 执行测试
        customSqlHandler.apply(sql, config, context);

        // 验证结果 - 占位符应被替换为实际用户ID
        String result = sql.toString();
        assertTrue(result.contains("create_by = 100"));
        assertFalse(result.contains("#{currentUserId}"));
    }

    @Test
    @DisplayName("测试SQL片段 - 带登录名占位符")
    void testSqlFragmentWithLoginName() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setSqlFragment("creator_name = #{loginName}");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);
        userInfo.setLoginName("admin");

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");

        // 执行测试
        customSqlHandler.apply(sql, config, context);

        // 验证结果 - 占位符应被替换为带引号的登录名
        String result = sql.toString();
        assertTrue(result.contains("creator_name = 'admin'"));
        assertFalse(result.contains("#{loginName}"));
    }

    @Test
    @DisplayName("测试SQL片段 - 带部门ID占位符")
    void testSqlFragmentWithDeptId() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setSqlFragment("dept_id = #{currentDeptId}");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);
        userInfo.setDeptId(5);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");

        // 执行测试
        customSqlHandler.apply(sql, config, context);

        // 验证结果
        String result = sql.toString();
        assertTrue(result.contains("dept_id = 5"));
        assertFalse(result.contains("#{currentDeptId}"));
    }

    @Test
    @DisplayName("测试SQL片段 - 多个占位符组合")
    void testSqlFragmentWithMultiplePlaceholders() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setSqlFragment("(create_by = #{currentUserId} OR dept_id = #{currentDeptId})");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);
        userInfo.setDeptId(5);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");

        // 执行测试
        customSqlHandler.apply(sql, config, context);

        // 验证结果
        String result = sql.toString();
        assertTrue(result.contains("create_by = 100"));
        assertTrue(result.contains("dept_id = 5"));
    }

    @Test
    @DisplayName("测试SQL片段 - 时间函数")
    void testSqlFragmentWithDateFunction() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setSqlFragment("create_time > DATE_SUB(NOW(), INTERVAL 7 DAY)");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");

        // 执行测试
        customSqlHandler.apply(sql, config, context);

        // 验证结果
        assertTrue(sql.toString().contains("create_time > DATE_SUB(NOW(), INTERVAL 7 DAY)"));
    }

    @Test
    @DisplayName("测试SQL片段 - DATE_FORMAT函数")
    void testSqlFragmentWithDateFormat() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setSqlFragment("DATE_FORMAT(create_time, '%Y-%m') = DATE_FORMAT(NOW(), '%Y-%m')");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");

        // 执行测试
        customSqlHandler.apply(sql, config, context);

        // 验证结果
        assertTrue(sql.toString().contains("DATE_FORMAT"));
    }

    @Test
    @DisplayName("测试空SQL片段 - 不生成条件")
    void testEmptySqlFragment() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setSqlFragment("");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");
        String originalSql = sql.toString();

        // 执行测试
        customSqlHandler.apply(sql, config, context);

        // 验证结果 - SQL不应改变
        assertEquals(originalSql, sql.toString());
    }

    @Test
    @DisplayName("测试null SQL片段 - 不生成条件")
    void testNullSqlFragment() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setSqlFragment(null);

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");
        String originalSql = sql.toString();

        // 执行测试
        customSqlHandler.apply(sql, config, context);

        // 验证结果 - SQL不应改变
        assertEquals(originalSql, sql.toString());
    }

    @Test
    @DisplayName("测试SQL注入防护 - 禁止SELECT关键字")
    void testSqlInjectionPrevention_Select() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setSqlFragment("status = 0; SELECT * FROM sys_user");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");

        // 执行测试并验证异常
        assertThrows(BlinkException.class, () -> {
            customSqlHandler.apply(sql, config, context);
        });
    }

    @Test
    @DisplayName("测试SQL注入防护 - 禁止UNION关键字")
    void testSqlInjectionPrevention_Union() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setSqlFragment("status = 0 UNION SELECT * FROM sys_user");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");

        // 执行测试并验证异常
        assertThrows(BlinkException.class, () -> {
            customSqlHandler.apply(sql, config, context);
        });
    }

    @Test
    @DisplayName("测试SQL注入防护 - 禁止分号")
    void testSqlInjectionPrevention_Semicolon() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setSqlFragment("status = 0; DELETE FROM sys_user");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");

        // 执行测试并验证异常
        assertThrows(BlinkException.class, () -> {
            customSqlHandler.apply(sql, config, context);
        });
    }

    @Test
    @DisplayName("测试SQL注入防护 - 禁止DELETE关键字")
    void testSqlInjectionPrevention_Delete() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setSqlFragment("1=1 OR 1=1; DELETE FROM sys_user WHERE 1=1");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");

        // 执行测试并验证异常
        assertThrows(BlinkException.class, () -> {
            customSqlHandler.apply(sql, config, context);
        });
    }

    @Test
    @DisplayName("测试SQL注入防护 - 禁止注释")
    void testSqlInjectionPrevention_Comment() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setSqlFragment("status = 0 -- comment");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");

        // 执行测试并验证异常
        assertThrows(BlinkException.class, () -> {
            customSqlHandler.apply(sql, config, context);
        });
    }

    @Test
    @DisplayName("测试登录名SQL注入防护 - 单引号转义")
    void testLoginNameSqlInjectionPrevention() {
        // 准备测试数据 - 包含单引号的登录名（SQL注入尝试）
        RuleConfig config = new RuleConfig();
        config.setSqlFragment("creator_name = #{loginName}");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);
        userInfo.setLoginName("admin' OR '1'='1");

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");

        // 执行测试
        customSqlHandler.apply(sql, config, context);

        // 验证结果 - 单引号应被转义为两个单引号
        String result = sql.toString();
        assertTrue(result.contains("admin'' OR ''1''=''1"));
        // 确保原始注入尝试不会生效
        assertFalse(result.contains("admin' OR '1'='1"));
    }

    @Test
    @DisplayName("测试复杂条件组合")
    void testComplexCondition() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setSqlFragment("(status = 0 AND create_time > '2026-01-01') OR (dept_id = #{currentDeptId} AND is_public = 1)");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);
        userInfo.setDeptId(5);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");

        // 执行测试
        customSqlHandler.apply(sql, config, context);

        // 验证结果
        String result = sql.toString();
        assertTrue(result.contains("status = 0"));
        assertTrue(result.contains("dept_id = 5"));
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