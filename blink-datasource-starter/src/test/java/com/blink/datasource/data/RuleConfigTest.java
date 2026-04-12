package com.blink.datasource.data;

import cn.hutool.core.bean.BeanUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RuleConfig 单元测试
 * 验证规则配置数据类的 copy 方法和序列化
 *
 * @author binblink
 * @since 2026-04-12
 */
@DisplayName("RuleConfig 单元测试")
class RuleConfigTest {

    // ==================== copy 方法测试 ====================

    @Nested
    @DisplayName("copy 方法测试")
    class CopyTest {

        @Test
        @DisplayName("TC-001: copy方法-完整属性")
        void copy_whenFullProperties_shouldCopyAllProperties() {
            // given
            RuleConfig original = new RuleConfig();
            original.setRuleType("FIELD_FILTER");
            original.setEntityClass("com.blink.entity.SysUser");
            original.setField("createBy");
            original.setMatchType("CURRENT_USER");
            original.setExcludeFields(Arrays.asList("password", "salt"));
            original.setIncludeFields(Arrays.asList("id", "name"));
            original.setUserIds(Arrays.asList(1, 2, 3));
            original.setLoginNames(Arrays.asList("admin", "test"));
            original.setRoleIds(Arrays.asList(10, 20));
            original.setRangeType("RELATIVE");
            original.setRelativeValue(-30);
            original.setRelativeUnit("DAY");
            original.setStartTime("2024-01-01");
            original.setEndTime("2024-12-31");
            original.setSqlFragment("status = 1");
            original.setRelationTable("sys_user_dept");
            original.setSourceField("userId");
            original.setRelationSourceField("user_id");
            original.setRelationTargetField("dept_id");
            original.setRelationMatchType("CURRENT_DEPT");
            original.setRelationMatchValues(Arrays.asList(100, 200));

            // when
            RuleConfig copy = original.copy();

            // then
            assertThat(copy).isNotSameAs(original);
            assertThat(copy.getRuleType()).isEqualTo("FIELD_FILTER");
            assertThat(copy.getEntityClass()).isEqualTo("com.blink.entity.SysUser");
            assertThat(copy.getField()).isEqualTo("createBy");
            assertThat(copy.getMatchType()).isEqualTo("CURRENT_USER");
            assertThat(copy.getExcludeFields()).containsExactly("password", "salt");
            assertThat(copy.getIncludeFields()).containsExactly("id", "name");
            assertThat(copy.getUserIds()).containsExactly(1, 2, 3);
            assertThat(copy.getLoginNames()).containsExactly("admin", "test");
            assertThat(copy.getRoleIds()).containsExactly(10, 20);
            assertThat(copy.getRangeType()).isEqualTo("RELATIVE");
            assertThat(copy.getRelativeValue()).isEqualTo(-30);
            assertThat(copy.getRelativeUnit()).isEqualTo("DAY");
            assertThat(copy.getStartTime()).isEqualTo("2024-01-01");
            assertThat(copy.getEndTime()).isEqualTo("2024-12-31");
            assertThat(copy.getSqlFragment()).isEqualTo("status = 1");
            assertThat(copy.getRelationTable()).isEqualTo("sys_user_dept");
            assertThat(copy.getSourceField()).isEqualTo("userId");
            assertThat(copy.getRelationSourceField()).isEqualTo("user_id");
            assertThat(copy.getRelationTargetField()).isEqualTo("dept_id");
            assertThat(copy.getRelationMatchType()).isEqualTo("CURRENT_DEPT");
            assertThat(copy.getRelationMatchValues()).containsExactly(100, 200);
        }

        @Test
        @DisplayName("TC-002: copy方法-对象独立性")
        void copy_shouldCreateIndependentObject() {
            // given
            RuleConfig original = new RuleConfig();
            original.setRuleType("FIELD_FILTER");
            original.setEntityClass("com.blink.entity.SysUser");

            // when
            RuleConfig copy = original.copy();

            // 修改原始对象的基本属性
            original.setRuleType("CREATOR_FILTER");
            original.setEntityClass("com.blink.entity.Other");

            // then - 副本不应受影响
            assertThat(copy.getRuleType()).isEqualTo("FIELD_FILTER");
            assertThat(copy.getEntityClass()).isEqualTo("com.blink.entity.SysUser");
        }

        @Test
        @DisplayName("TC-003: copy方法-excludeFields")
        void copy_whenExcludeFields_shouldCopyList() {
            // given
            RuleConfig original = new RuleConfig();
            original.setExcludeFields(Arrays.asList("password", "salt", "token"));

            // when
            RuleConfig copy = original.copy();

            // then
            assertThat(copy.getExcludeFields())
                    .isNotNull()
                    .containsExactly("password", "salt", "token");
        }

        @Test
        @DisplayName("TC-004: copy方法-userIds")
        void copy_whenUserIds_shouldCopyList() {
            // given
            RuleConfig original = new RuleConfig();
            original.setUserIds(Arrays.asList(1, 2, 3, 4, 5));

            // when
            RuleConfig copy = original.copy();

            // then
            assertThat(copy.getUserIds())
                    .isNotNull()
                    .containsExactly(1, 2, 3, 4, 5);
        }

        @Test
        @DisplayName("TC-005: copy方法-空属性")
        void copy_whenNullProperties_shouldReturnNull() {
            // given
            RuleConfig original = new RuleConfig();

            // when
            RuleConfig copy = original.copy();

            // then
            assertThat(copy.getRuleType()).isNull();
            assertThat(copy.getEntityClass()).isNull();
            assertThat(copy.getExcludeFields()).isNull();
            assertThat(copy.getUserIds()).isNull();
        }
    }

    // ==================== Lombok Getter/Setter 测试 ====================

    @Nested
    @DisplayName("Getter/Setter 测试")
    class GetterSetterTest {

        @Test
        @DisplayName("TC-006: 所有字段Getter/Setter正常工作")
        void getterSetter_shouldWorkCorrectly() {
            // given
            RuleConfig config = new RuleConfig();

            // when
            config.setRuleType("TEST_TYPE");
            config.setEntityClass("com.test.Entity");
            config.setField("testField");

            // then
            assertThat(config.getRuleType()).isEqualTo("TEST_TYPE");
            assertThat(config.getEntityClass()).isEqualTo("com.test.Entity");
            assertThat(config.getField()).isEqualTo("testField");
        }
    }

    // ==================== 序列化测试 ====================

    @Nested
    @DisplayName("序列化测试")
    class SerializationTest {

        @Test
        @DisplayName("TC-007: 实现Serializable接口")
        void shouldImplementSerializable() {
            // 验证 RuleConfig 实现了 Serializable 接口
            assertThat(java.io.Serializable.class).isAssignableFrom(RuleConfig.class);
        }
    }

    // ==================== 边界条件测试 ====================

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryTest {

        @Test
        @DisplayName("TC-008: 空列表属性")
        void whenEmptyList_shouldHandleCorrectly() {
            // given
            RuleConfig original = new RuleConfig();
            original.setExcludeFields(List.of());
            original.setUserIds(List.of());

            // when
            RuleConfig copy = original.copy();

            // then
            assertThat(copy.getExcludeFields()).isEmpty();
            assertThat(copy.getUserIds()).isEmpty();
        }

        @Test
        @DisplayName("TC-009: 单元素列表")
        void whenSingleElementList_shouldCopyCorrectly() {
            // given
            RuleConfig original = new RuleConfig();
            original.setExcludeFields(List.of("password"));
            original.setUserIds(List.of(1));

            // when
            RuleConfig copy = original.copy();

            // then
            assertThat(copy.getExcludeFields()).containsExactly("password");
            assertThat(copy.getUserIds()).containsExactly(1);
        }
    }
}
