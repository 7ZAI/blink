package com.blink.gateway.datascope;

import com.blink.base.datascope.merge.RuleMergeStrategy;
import com.blink.datasource.data.RuleConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RuleMergeStrategy 单元测试类
 * 测试规则合并策略的各项功能，包括 RELATION_FILTER 合并
 *
 * @author binblink
 */
@DisplayName("RuleMergeStrategy 单元测试")
class RuleMergeStrategyTest {

    private RuleMergeStrategy mergeStrategy;

    @BeforeEach
    void setUp() {
        mergeStrategy = new RuleMergeStrategy();
    }

    // ==================== RELATION_FILTER 合并测试 ====================

    @Test
    @DisplayName("测试 RELATION_FILTER - 相同关联表相同匹配类型合并值")
    void testMergeRelationFilterSameTableSameType() {
        // 第一个规则
        RuleConfig config1 = createRelationFilterConfig("sys_user_role_rela", "ROLE_LIST", Arrays.asList(1, 2));

        // 第二个规则
        RuleConfig config2 = createRelationFilterConfig("sys_user_role_rela", "ROLE_LIST", Arrays.asList(3, 4));

        List<RuleConfig> rules = Arrays.asList(config1, config2);

        // 执行合并
        RuleConfig merged = mergeStrategy.merge(rules);

        // 验证结果
        assertNotNull(merged);
        assertEquals("sys_user_role_rela", merged.getRelationTable());
        assertEquals("ROLE_LIST", merged.getRelationMatchType());
        assertEquals(4, merged.getRelationMatchValues().size());
        assertTrue(merged.getRelationMatchValues().containsAll(Arrays.asList(1, 2, 3, 4)));
    }

    @Test
    @DisplayName("测试 RELATION_FILTER - 相同关联表不同列表类型合并值")
    void testMergeRelationFilterSameTableDifferentListType() {
        // 第一个规则
        RuleConfig config1 = createRelationFilterConfig("sys_user_role_rela", "USER_LIST", Arrays.asList(1, 2));

        // 第二个规则
        RuleConfig config2 = createRelationFilterConfig("sys_user_role_rela", "ROLE_LIST", Arrays.asList(3, 4));

        List<RuleConfig> rules = Arrays.asList(config1, config2);

        // 执行合并
        RuleConfig merged = mergeStrategy.merge(rules);

        // 验证结果 - 不同类型但都是列表，合并值
        assertNotNull(merged);
        assertEquals(4, merged.getRelationMatchValues().size());
        assertTrue(merged.getRelationMatchValues().containsAll(Arrays.asList(1, 2, 3, 4)));
    }

    @Test
    @DisplayName("测试 RELATION_FILTER - 不同关联表保留第一个规则")
    void testMergeRelationFilterDifferentTable() {
        // 第一个规则
        RuleConfig config1 = createRelationFilterConfig("sys_user_role_rela", "ROLE_LIST", Arrays.asList(1, 2));

        // 第二个规则 - 不同关联表
        RuleConfig config2 = createRelationFilterConfig("sys_user_group_rela", "DEPT_LIST", Arrays.asList(3, 4));

        List<RuleConfig> rules = Arrays.asList(config1, config2);

        // 执行合并
        RuleConfig merged = mergeStrategy.merge(rules);

        // 验证结果 - 保留第一个规则
        assertNotNull(merged);
        assertEquals("sys_user_role_rela", merged.getRelationTable());
        assertEquals("ROLE_LIST", merged.getRelationMatchType());
        assertEquals(2, merged.getRelationMatchValues().size());
        assertTrue(merged.getRelationMatchValues().containsAll(Arrays.asList(1, 2)));
    }

    @Test
    @DisplayName("测试 RELATION_FILTER - CURRENT_USER类型保持不变")
    void testMergeRelationFilterCurrentUser() {
        // 第一个规则
        RuleConfig config1 = createRelationFilterConfig("sys_user_role_rela", "CURRENT_USER", null);

        // 第二个规则
        RuleConfig config2 = createRelationFilterConfig("sys_user_role_rela", "CURRENT_USER", null);

        List<RuleConfig> rules = Arrays.asList(config1, config2);

        // 执行合并
        RuleConfig merged = mergeStrategy.merge(rules);

        // 验证结果 - CURRENT_USER 保持不变
        assertNotNull(merged);
        assertEquals("CURRENT_USER", merged.getRelationMatchType());
    }

    @Test
    @DisplayName("测试 RELATION_FILTER - CURRENT_DEPT类型保持不变")
    void testMergeRelationFilterCurrentDept() {
        // 第一个规则
        RuleConfig config1 = createRelationFilterConfig("sys_user_group_rela", "CURRENT_DEPT", null);

        // 第二个规则
        RuleConfig config2 = createRelationFilterConfig("sys_user_group_rela", "CURRENT_DEPT", null);

        List<RuleConfig> rules = Arrays.asList(config1, config2);

        // 执行合并
        RuleConfig merged = mergeStrategy.merge(rules);

        // 验证结果 - CURRENT_DEPT 保持不变
        assertNotNull(merged);
        assertEquals("CURRENT_DEPT", merged.getRelationMatchType());
    }

    @Test
    @DisplayName("测试 RELATION_FILTER - 空规则列表返回null")
    void testMergeRelationFilterEmptyList() {
        List<RuleConfig> rules = Collections.emptyList();

        RuleConfig merged = mergeStrategy.merge(rules);

        assertNull(merged);
    }

    @Test
    @DisplayName("测试 RELATION_FILTER - 单个规则直接返回")
    void testMergeRelationFilterSingleRule() {
        RuleConfig config = createRelationFilterConfig("sys_user_role_rela", "ROLE_LIST", Arrays.asList(1, 2));

        List<RuleConfig> rules = Collections.singletonList(config);

        RuleConfig merged = mergeStrategy.merge(rules);

        assertNotNull(merged);
        assertEquals("sys_user_role_rela", merged.getRelationTable());
        assertEquals("ROLE_LIST", merged.getRelationMatchType());
    }

    @Test
    @DisplayName("测试 RELATION_FILTER - 多个规则合并去重")
    void testMergeRelationFilterDeduplication() {
        // 第一个规则
        RuleConfig config1 = createRelationFilterConfig("sys_user_role_rela", "ROLE_LIST", Arrays.asList(1, 2, 3));

        // 第二个规则 - 有重复值
        RuleConfig config2 = createRelationFilterConfig("sys_user_role_rela", "ROLE_LIST", Arrays.asList(2, 3, 4));

        // 第三个规则
        RuleConfig config3 = createRelationFilterConfig("sys_user_role_rela", "ROLE_LIST", Arrays.asList(4, 5));

        List<RuleConfig> rules = Arrays.asList(config1, config2, config3);

        // 执行合并
        RuleConfig merged = mergeStrategy.merge(rules);

        // 验证结果 - 去重后应该有5个值
        assertNotNull(merged);
        assertEquals(5, merged.getRelationMatchValues().size());
        assertTrue(merged.getRelationMatchValues().containsAll(Arrays.asList(1, 2, 3, 4, 5)));
    }

    // ==================== 其他规则类型合并测试 ====================

    @Test
    @DisplayName("测试 CREATOR_FILTER - USER_LIST合并")
    void testMergeCreatorFilterUserList() {
        RuleConfig config1 = new RuleConfig();
        config1.setRuleType("CREATOR_FILTER");
        config1.setField("create_by");
        config1.setMatchType("USER_LIST");
        config1.setUserIds(Arrays.asList(1, 2));

        RuleConfig config2 = new RuleConfig();
        config2.setRuleType("CREATOR_FILTER");
        config2.setField("create_by");
        config2.setMatchType("USER_LIST");
        config2.setUserIds(Arrays.asList(3, 4));

        List<RuleConfig> rules = Arrays.asList(config1, config2);

        RuleConfig merged = mergeStrategy.merge(rules);

        assertNotNull(merged);
        assertEquals(4, merged.getUserIds().size());
        assertTrue(merged.getUserIds().containsAll(Arrays.asList(1, 2, 3, 4)));
    }

    @Test
    @DisplayName("测试 CUSTOM_SQL - OR连接")
    void testMergeCustomSql() {
        RuleConfig config1 = new RuleConfig();
        config1.setRuleType("CUSTOM_SQL");
        config1.setSqlFragment("status = 1");

        RuleConfig config2 = new RuleConfig();
        config2.setRuleType("CUSTOM_SQL");
        config2.setSqlFragment("deleted = 0");

        List<RuleConfig> rules = Arrays.asList(config1, config2);

        RuleConfig merged = mergeStrategy.merge(rules);

        assertNotNull(merged);
        assertTrue(merged.getSqlFragment().contains("OR"));
        assertTrue(merged.getSqlFragment().contains("status = 1"));
        assertTrue(merged.getSqlFragment().contains("deleted = 0"));
    }

    @Test
    @DisplayName("测试 FIELD_FILTER - 排除字段取交集")
    void testMergeFieldFilter() {
        RuleConfig config1 = new RuleConfig();
        config1.setRuleType("FIELD_FILTER");
        config1.setExcludeFields(Arrays.asList("password", "salt", "email"));

        RuleConfig config2 = new RuleConfig();
        config2.setRuleType("FIELD_FILTER");
        config2.setExcludeFields(Arrays.asList("password", "salt", "phone"));

        List<RuleConfig> rules = Arrays.asList(config1, config2);

        RuleConfig merged = mergeStrategy.merge(rules);

        assertNotNull(merged);
        // 取交集：只排除两个角色都需要排除的字段
        assertEquals(2, merged.getExcludeFields().size());
        assertTrue(merged.getExcludeFields().containsAll(Arrays.asList("password", "salt")));
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建 RELATION_FILTER 配置对象
     */
    private RuleConfig createRelationFilterConfig(String relationTable, String matchType, List<Integer> matchValues) {
        RuleConfig config = new RuleConfig();
        config.setRuleType("RELATION_FILTER");
        config.setRelationTable(relationTable);
        config.setSourceField("user_id");
        config.setRelationSourceField("user_id");
        config.setRelationTargetField("role_id");
        config.setRelationMatchType(matchType);
        config.setRelationMatchValues(matchValues);
        return config;
    }
}