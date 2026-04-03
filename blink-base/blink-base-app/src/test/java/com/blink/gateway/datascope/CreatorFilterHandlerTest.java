package com.blink.gateway.datascope;

import com.blink.base.datascope.handler.CreatorFilterHandler;
import com.blink.base.mapper.SysUserRoleRelaMapper;
import com.blink.datasource.data.DataScopeParseResult;
import com.blink.datasource.data.RuleConfig;
import com.blink.datasource.data.UserDataScopeInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * CreatorFilterHandler 单元测试类
 * 测试创建人过滤规则处理器的各项功能
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreatorFilterHandler 单元测试")
class CreatorFilterHandlerTest {

    @Mock
    private SysUserRoleRelaMapper sysUserRoleRelaMapper;

    private CreatorFilterHandler creatorFilterHandler;

    @BeforeEach
    void setUp() {
        creatorFilterHandler = new CreatorFilterHandler(sysUserRoleRelaMapper);
    }

    @Test
    @DisplayName("测试 CURRENT_USER 类型 - 生成正确的SQL条件")
    void testCurrentUserMatch() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setField("create_by");
        config.setMatchType("CURRENT_USER");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");

        // 执行测试
        creatorFilterHandler.apply(sql, config, context);

        // 验证结果
        assertTrue(sql.toString().contains("create_by = 100"));
    }

    @Test
    @DisplayName("测试 CURRENT_USER 类型 - 带表别名")
    void testCurrentUserMatchWithTableAlias() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setField("create_by");
        config.setMatchType("CURRENT_USER");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, "t");

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user t WHERE 1=1");

        // 执行测试
        creatorFilterHandler.apply(sql, config, context);

        // 验证结果
        assertTrue(sql.toString().contains("t.create_by = 100"));
    }

    @Test
    @DisplayName("测试 USER_LIST 类型 - 生成正确的IN条件")
    void testUserListMatch() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setField("create_by");
        config.setMatchType("USER_LIST");
        config.setUserIds(Arrays.asList(1, 2, 3));

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");

        // 执行测试
        creatorFilterHandler.apply(sql, config, context);

        // 验证结果
        assertTrue(sql.toString().contains("create_by IN (1, 2, 3)"));
    }

    @Test
    @DisplayName("测试 USER_LIST 类型 - 空用户列表不生成条件")
    void testUserListMatchEmptyList() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setField("create_by");
        config.setMatchType("USER_LIST");
        config.setUserIds(Collections.emptyList());

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");
        String originalSql = sql.toString();

        // 执行测试
        creatorFilterHandler.apply(sql, config, context);

        // 验证结果 - SQL不应改变
        assertEquals(originalSql, sql.toString());
    }

    @Test
    @DisplayName("测试 USER_LIST 类型 - null用户列表不生成条件")
    void testUserListMatchNullList() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setField("create_by");
        config.setMatchType("USER_LIST");
        config.setUserIds(null);

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");
        String originalSql = sql.toString();

        // 执行测试
        creatorFilterHandler.apply(sql, config, context);

        // 验证结果 - SQL不应改变
        assertEquals(originalSql, sql.toString());
    }

    @Test
    @DisplayName("测试 ROLE_USER 类型 - 生成正确的IN条件")
    void testRoleUserMatch() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setField("create_by");
        config.setMatchType("ROLE_USER");
        config.setRoleIds(Arrays.asList(10, 20));

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        // Mock Mapper 返回用户ID列表
        when(sysUserRoleRelaMapper.selectUserIdsByRoleIds(anyList()))
                .thenReturn(Arrays.asList(1, 2, 3));

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");

        // 执行测试
        creatorFilterHandler.apply(sql, config, context);

        // 验证结果
        assertTrue(sql.toString().contains("create_by IN (1, 2, 3)"));
        verify(sysUserRoleRelaMapper, times(1)).selectUserIdsByRoleIds(Arrays.asList(10, 20));
    }

    @Test
    @DisplayName("测试 ROLE_USER 类型 - 空角色列表不生成条件")
    void testRoleUserMatchEmptyRoleList() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setField("create_by");
        config.setMatchType("ROLE_USER");
        config.setRoleIds(Collections.emptyList());

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");
        String originalSql = sql.toString();

        // 执行测试
        creatorFilterHandler.apply(sql, config, context);

        // 验证结果 - SQL不应改变
        assertEquals(originalSql, sql.toString());
        // 不应调用 Mapper
        verify(sysUserRoleRelaMapper, never()).selectUserIdsByRoleIds(anyList());
    }

    @Test
    @DisplayName("测试 ROLE_USER 类型 - 角色下无用户时不生成条件")
    void testRoleUserMatchNoUsersInRole() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setField("create_by");
        config.setMatchType("ROLE_USER");
        config.setRoleIds(Arrays.asList(10, 20));

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        // Mock Mapper 返回空列表
        when(sysUserRoleRelaMapper.selectUserIdsByRoleIds(anyList()))
                .thenReturn(Collections.emptyList());

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");
        String originalSql = sql.toString();

        // 执行测试
        creatorFilterHandler.apply(sql, config, context);

        // 验证结果 - SQL不应改变
        assertEquals(originalSql, sql.toString());
        verify(sysUserRoleRelaMapper, times(1)).selectUserIdsByRoleIds(Arrays.asList(10, 20));
    }

    @Test
    @DisplayName("测试空字段名 - 不生成条件")
    void testEmptyFieldName() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setField("");
        config.setMatchType("CURRENT_USER");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");
        String originalSql = sql.toString();

        // 执行测试
        creatorFilterHandler.apply(sql, config, context);

        // 验证结果 - SQL不应改变
        assertEquals(originalSql, sql.toString());
    }

    @Test
    @DisplayName("测试空匹配类型 - 不生成条件")
    void testEmptyMatchType() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setField("create_by");
        config.setMatchType("");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");
        String originalSql = sql.toString();

        // 执行测试
        creatorFilterHandler.apply(sql, config, context);

        // 验证结果 - SQL不应改变
        assertEquals(originalSql, sql.toString());
    }

    @Test
    @DisplayName("测试未知匹配类型 - 不生成条件")
    void testUnknownMatchType() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setField("create_by");
        config.setMatchType("UNKNOWN_TYPE");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");
        String originalSql = sql.toString();

        // 执行测试
        creatorFilterHandler.apply(sql, config, context);

        // 验证结果 - SQL不应改变
        assertEquals(originalSql, sql.toString());
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