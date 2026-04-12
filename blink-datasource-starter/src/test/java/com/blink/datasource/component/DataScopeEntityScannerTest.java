package com.blink.datasource.component;

import com.blink.datasource.annotation.DataScopeEntity;
import com.blink.datasource.data.RegisteredEntityVO;
import com.blink.datasource.data.RelationInfoVO;
import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataScopeEntityScanner 单元测试
 * 验证数据范围实体扫描器的静态方法逻辑
 *
 * <p><b>测试限制说明：</b></p>
 * <p>run() 方法需要 Spring 上下文和类扫描功能，需要通过集成测试来验证。
 * 本测试类仅测试静态方法，通过反射预先设置静态缓存来模拟已注册状态。</p>
 *
 * @author binblink
 * @since 2026-04-12
 */
@DisplayName("DataScopeEntityScanner 单元测试")
class DataScopeEntityScannerTest {

    // 反射获取静态 Map 字段
    private static final String TABLE_ENTITY_MAP_FIELD = "TABLE_ENTITY_MAP";
    private static final String REGISTERED_ENTITIES_FIELD = "REGISTERED_ENTITIES";
    private static final String TABLE_RELATIONS_MAP_FIELD = "TABLE_RELATIONS_MAP";

    @AfterEach
    void tearDown() throws Exception {
        // 清空静态缓存
        clearStaticCaches();
    }

    // ==================== getEntityClass 方法测试 ====================

    @Nested
    @DisplayName("getEntityClass 方法测试")
    class GetEntityClassTest {

        @Test
        @DisplayName("TC-001: getEntityClass-已注册表名")
        void getEntityClass_whenTableRegistered_shouldReturnEntityClass() throws Exception {
            // given
            addToTableEntityMap("sys_user", TestUserEntity.class);

            // when
            Class<?> result = DataScopeEntityScanner.getEntityClass("sys_user");

            // then
            assertThat(result).isEqualTo(TestUserEntity.class);
        }

        @Test
        @DisplayName("TC-002: getEntityClass-未注册表名")
        void getEntityClass_whenTableNotRegistered_shouldReturnNull() {
            // when
            Class<?> result = DataScopeEntityScanner.getEntityClass("unknown_table");

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("TC-003: getEntityClass-null参数")
        void getEntityClass_whenNull_shouldReturnNull() {
            // when
            Class<?> result = DataScopeEntityScanner.getEntityClass(null);

            // then
            assertThat(result).isNull();
        }
    }

    // ==================== isDataScopeEntity 方法测试 ====================

    @Nested
    @DisplayName("isDataScopeEntity 方法测试")
    class IsDataScopeEntityTest {

        @Test
        @DisplayName("TC-004: isDataScopeEntity-有注解")
        void isDataScopeEntity_whenHasAnnotation_shouldReturnTrue() {
            // when
            boolean result = DataScopeEntityScanner.isDataScopeEntity(TestUserEntity.class);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("TC-005: isDataScopeEntity-无注解")
        void isDataScopeEntity_whenNoAnnotation_shouldReturnFalse() {
            // when
            boolean result = DataScopeEntityScanner.isDataScopeEntity(NoAnnotationEntity.class);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("TC-006: isDataScopeEntity-null参数")
        void isDataScopeEntity_whenNull_shouldReturnFalse() {
            // when
            boolean result = DataScopeEntityScanner.isDataScopeEntity(null);

            // then
            assertThat(result).isFalse();
        }
    }

    // ==================== isRegistered 方法测试 ====================

    @Nested
    @DisplayName("isRegistered 方法测试")
    class IsRegisteredTest {

        @Test
        @DisplayName("TC-007: isRegistered-已注册")
        void isRegistered_whenTableRegistered_shouldReturnTrue() throws Exception {
            // given
            addToTableEntityMap("sys_user", TestUserEntity.class);

            // when
            boolean result = DataScopeEntityScanner.isRegistered("sys_user");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("TC-008: isRegistered-未注册")
        void isRegistered_whenTableNotRegistered_shouldReturnFalse() {
            // when
            boolean result = DataScopeEntityScanner.isRegistered("unknown_table");

            // then
            assertThat(result).isFalse();
        }
    }

    // ==================== getTableName 方法测试 ====================

    @Nested
    @DisplayName("getTableName 方法测试")
    class GetTableNameTest {

        @Test
        @DisplayName("TC-009: getTableName-已注册实体")
        void getTableName_whenEntityRegistered_shouldReturnTableName() throws Exception {
            // given
            addToTableEntityMap("sys_user", TestUserEntity.class);

            // when
            String result = DataScopeEntityScanner.getTableName(TestUserEntity.class.getName());

            // then
            assertThat(result).isEqualTo("sys_user");
        }

        @Test
        @DisplayName("TC-010: getTableName-未注册实体")
        void getTableName_whenEntityNotRegistered_shouldReturnNull() {
            // when
            String result = DataScopeEntityScanner.getTableName("com.unknown.Entity");

            // then
            assertThat(result).isNull();
        }
    }

    // ==================== getRelations 方法测试 ====================

    @Nested
    @DisplayName("getRelations 方法测试")
    class GetRelationsTest {

        @Test
        @DisplayName("TC-011: getRelations-有关联")
        void getRelations_whenHasRelation_shouldReturnRelationList() throws Exception {
            // given
            RelationInfoVO relation = new RelationInfoVO();
            relation.setName("部门关联");
            addToTableRelationsMap("sys_user", relation);

            // when
            List<RelationInfoVO> result = DataScopeEntityScanner.getRelations("sys_user");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("部门关联");
        }

        @Test
        @DisplayName("TC-012: getRelations-无关联")
        void getRelations_whenNoRelation_shouldReturnEmptyList() {
            // when
            List<RelationInfoVO> result = DataScopeEntityScanner.getRelations("unknown_table");

            // then
            assertThat(result).isEmpty();
        }
    }

    // ==================== hasRelation 方法测试 ====================

    @Nested
    @DisplayName("hasRelation 方法测试")
    class HasRelationTest {

        @Test
        @DisplayName("TC-013: hasRelation-有关联")
        void hasRelation_whenHasRelation_shouldReturnTrue() throws Exception {
            // given
            RelationInfoVO relation = new RelationInfoVO();
            addToTableRelationsMap("sys_user", relation);

            // when
            boolean result = DataScopeEntityScanner.hasRelation("sys_user");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("TC-014: hasRelation-无关联")
        void hasRelation_whenNoRelation_shouldReturnFalse() {
            // when
            boolean result = DataScopeEntityScanner.hasRelation("unknown_table");

            // then
            assertThat(result).isFalse();
        }
    }

    // ==================== getAllTableNames 方法测试 ====================

    @Nested
    @DisplayName("getAllTableNames 方法测试")
    class GetAllTableNamesTest {

        @Test
        @DisplayName("TC-015: getAllTableNames")
        void getAllTableNames_shouldReturnAllTableNames() throws Exception {
            // given
            addToTableEntityMap("sys_user", TestUserEntity.class);
            addToTableEntityMap("sys_dept", TestDeptEntity.class);

            // when
            Set<String> result = DataScopeEntityScanner.getAllTableNames();

            // then
            assertThat(result).containsExactlyInAnyOrder("sys_user", "sys_dept");
        }
    }

    // ==================== getRegisteredEntities 方法测试 ====================

    @Nested
    @DisplayName("getRegisteredEntities 方法测试")
    class GetRegisteredEntitiesTest {

        @Test
        @DisplayName("TC-016: getRegisteredEntities-返回不可修改列表")
        void getRegisteredEntities_shouldReturnUnmodifiableList() throws Exception {
            // given
            RegisteredEntityVO vo = new RegisteredEntityVO();
            vo.setEntityClass(TestUserEntity.class.getName());
            vo.setTableName("sys_user");
            addToRegisteredEntities(vo);

            // when
            List<RegisteredEntityVO> result = DataScopeEntityScanner.getRegisteredEntities();

            // then
            assertThat(result).isUnmodifiable();
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 通过反射向 TABLE_ENTITY_MAP 添加数据
     */
    private void addToTableEntityMap(String tableName, Class<?> entityClass) throws Exception {
        java.lang.reflect.Field field = DataScopeEntityScanner.class.getDeclaredField(TABLE_ENTITY_MAP_FIELD);
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Class<?>> map = (java.util.Map<String, Class<?>>) field.get(null);
        map.put(tableName, entityClass);
    }

    /**
     * 通过反射向 REGISTERED_ENTITIES 添加数据
     */
    private void addToRegisteredEntities(RegisteredEntityVO vo) throws Exception {
        java.lang.reflect.Field field = DataScopeEntityScanner.class.getDeclaredField(REGISTERED_ENTITIES_FIELD);
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<RegisteredEntityVO> list = (List<RegisteredEntityVO>) field.get(null);
        list.add(vo);
    }

    /**
     * 通过反射向 TABLE_RELATIONS_MAP 添加数据
     */
    private void addToTableRelationsMap(String tableName, RelationInfoVO relation) throws Exception {
        java.lang.reflect.Field field = DataScopeEntityScanner.class.getDeclaredField(TABLE_RELATIONS_MAP_FIELD);
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, List<RelationInfoVO>> map = (java.util.Map<String, List<RelationInfoVO>>) field.get(null);
        map.computeIfAbsent(tableName, k -> new java.util.ArrayList<>()).add(relation);
    }

    /**
     * 清空静态缓存
     */
    private void clearStaticCaches() throws Exception {
        java.lang.reflect.Field tableEntityMapField = DataScopeEntityScanner.class.getDeclaredField(TABLE_ENTITY_MAP_FIELD);
        tableEntityMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Class<?>> tableEntityMap = (java.util.Map<String, Class<?>>) tableEntityMapField.get(null);
        tableEntityMap.clear();

        java.lang.reflect.Field registeredEntitiesField = DataScopeEntityScanner.class.getDeclaredField(REGISTERED_ENTITIES_FIELD);
        registeredEntitiesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<RegisteredEntityVO> registeredEntities = (List<RegisteredEntityVO>) registeredEntitiesField.get(null);
        registeredEntities.clear();

        java.lang.reflect.Field tableRelationsMapField = DataScopeEntityScanner.class.getDeclaredField(TABLE_RELATIONS_MAP_FIELD);
        tableRelationsMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, List<RelationInfoVO>> tableRelationsMap = (java.util.Map<String, List<RelationInfoVO>>) tableRelationsMapField.get(null);
        tableRelationsMap.clear();
    }

    // ==================== 测试用实体类 ====================

    @DataScopeEntity(name = "用户", enName = "User")
    @TableName("sys_user")
    public static class TestUserEntity {
        private Long userId;
        private String userName;
    }

    @DataScopeEntity(name = "部门", enName = "Dept")
    @TableName("sys_dept")
    public static class TestDeptEntity {
        private Long deptId;
        private String deptName;
    }

    /**
     * 无注解实体类
     */
    public static class NoAnnotationEntity {
        private Long id;
        private String name;
    }
}
