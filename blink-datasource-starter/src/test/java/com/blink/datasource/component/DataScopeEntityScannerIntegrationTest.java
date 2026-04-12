package com.blink.datasource.component;

import com.blink.datasource.IntegrationTestConfig;
import com.blink.datasource.TestEntities;
import com.blink.datasource.data.RelationInfoVO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataScopeEntityScanner 集成测试
 * 测试 Spring 容器启动时的实体扫描功能
 *
 * @author binblink
 * @since 2026-04-12
 */
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = IntegrationTestConfig.class)
@DisplayName("DataScopeEntityScanner 集成测试")
class DataScopeEntityScannerIntegrationTest {

    @Autowired
    private DataScopeEntityScanner dataScopeEntityScanner;

    // ==================== run 方法集成测试 ====================

    @Nested
    @DisplayName("run 方法集成测试")
    class RunMethodTest {

        @Test
        @DisplayName("TC-INT-001: 扫描@DataScopeEntity注解类")
        void whenScan_shouldFindDataScopeEntities() {
            // given - Spring 容器启动时已执行 run() 方法

            // when - 检查表名是否已注册
            boolean isRegistered = DataScopeEntityScanner.isRegistered("test_user");

            // then
            assertThat(isRegistered).isTrue();
        }

        @Test
        @DisplayName("TC-INT-002: 建立表名映射")
        void whenScan_shouldBuildTableMapping() {
            // when
            Class<?> entityClass = DataScopeEntityScanner.getEntityClass("test_user");

            // then
            assertThat(entityClass).isNotNull();
            assertThat(entityClass).isEqualTo(TestEntities.TestUser.class);
        }

        @Test
        @DisplayName("TC-INT-003: 建立部门表映射")
        void whenScan_shouldBuildDeptTableMapping() {
            // when
            Class<?> entityClass = DataScopeEntityScanner.getEntityClass("test_dept");

            // then
            assertThat(entityClass).isNotNull();
            assertThat(entityClass).isEqualTo(TestEntities.TestDept.class);
        }

        @Test
        @DisplayName("TC-INT-004: 获取表名")
        void whenGetTableName_shouldReturnCorrectName() {
            // when
            String tableName = DataScopeEntityScanner.getTableName(TestEntities.TestUser.class.getName());

            // then
            assertThat(tableName).isEqualTo("test_user");
        }

        @Test
        @DisplayName("TC-INT-005: 检查实体是否有注解")
        void whenCheckDataScopeEntity_shouldReturnTrue() {
            // when
            boolean hasAnnotation = DataScopeEntityScanner.isDataScopeEntity(TestEntities.TestUser.class);

            // then
            assertThat(hasAnnotation).isTrue();
        }

        @Test
        @DisplayName("TC-INT-006: 无注解实体返回false")
        void whenCheckNonDataScopeEntity_shouldReturnFalse() {
            // when
            boolean hasAnnotation = DataScopeEntityScanner.isDataScopeEntity(TestEntities.TestOther.class);

            // then
            assertThat(hasAnnotation).isFalse();
        }

        @Test
        @DisplayName("TC-INT-007: 获取所有已注册表名")
        void whenGetAllTableNames_shouldReturnAllTables() {
            // when
            java.util.Set<String> tableNames = DataScopeEntityScanner.getAllTableNames();

            // then
            assertThat(tableNames).contains("test_user", "test_dept");
        }

        @Test
        @DisplayName("TC-INT-008: 检查关联关系")
        void whenCheckRelation_shouldReturnCorrectResult() {
            // when
            boolean hasRelation = DataScopeEntityScanner.hasRelation("test_user");

            // then - test_user 表应该有关联关系（通过 test_user_dept）
            assertThat(hasRelation).isTrue();
        }

        @Test
        @DisplayName("TC-INT-009: 获取关联关系")
        void whenGetRelations_shouldReturnRelations() {
            // when
            java.util.List<RelationInfoVO> relations = DataScopeEntityScanner.getRelations("test_user");

            // then
            assertThat(relations).isNotEmpty();
        }
    }

    // ==================== 边界条件测试 ====================

    @Nested
    @DisplayName("边界条件集成测试")
    class BoundaryTest {

        @Test
        @DisplayName("TC-INT-010: 未注册表名返回null")
        void whenUnregisteredTable_shouldReturnNull() {
            // when
            Class<?> entityClass = DataScopeEntityScanner.getEntityClass("unknown_table");

            // then
            assertThat(entityClass).isNull();
        }

        @Test
        @DisplayName("TC-INT-011: null参数安全处理")
        void whenNullParameter_shouldHandleSafely() {
            // when & then
            assertThat(DataScopeEntityScanner.getEntityClass(null)).isNull();
            assertThat(DataScopeEntityScanner.getTableName((String) null)).isNull();
            assertThat(DataScopeEntityScanner.isDataScopeEntity(null)).isFalse();
        }
    }
}
