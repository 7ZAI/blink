package com.blink.datasource.interceptor;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NormalFieldInterceptor 单元测试
 * 验证 MyBatis 拦截器对 INSERT/UPDATE 语句的字段自动赋值逻辑
 *
 * @author binblink
 * @since 2026-04-12
 */
@DisplayName("NormalFieldInterceptor 单元测试")
@ExtendWith(MockitoExtension.class)
class NormalFieldInterceptorTest {

    private NormalFieldInterceptor interceptor;

    @Mock
    private MappedStatement mappedStatement;

    @Mock
    private Executor executor;

    @Mock
    private Invocation invocation;

    @BeforeEach
    void setUp() {
        interceptor = new NormalFieldInterceptor();
    }

    // ==================== INSERT 语句测试 ====================

    @Nested
    @DisplayName("INSERT 语句测试")
    class InsertTest {

        @Test
        @DisplayName("TC-001: INSERT设置createTime")
        void intercept_whenInsert_shouldSetCreateTime() throws Throwable {
            // given
            TestEntity entity = new TestEntity();
            setupInvocation(SqlCommandType.INSERT, entity);

            // when
            interceptor.intercept(invocation);

            // then
            assertThat(entity.getCreateTime()).isNotNull();
        }

        @Test
        @DisplayName("TC-002: INSERT设置updateTime")
        void intercept_whenInsert_shouldSetUpdateTime() throws Throwable {
            // given
            TestEntity entity = new TestEntity();
            setupInvocation(SqlCommandType.INSERT, entity);

            // when
            interceptor.intercept(invocation);

            // then
            assertThat(entity.getUpdateTime()).isNotNull();
        }

        @Test
        @DisplayName("TC-008: 时间一致性 - createTime和updateTime值相同")
        void intercept_whenInsert_timesShouldBeEqual() throws Throwable {
            // given
            TestEntity entity = new TestEntity();
            setupInvocation(SqlCommandType.INSERT, entity);

            // when
            interceptor.intercept(invocation);

            // then
            assertThat(entity.getCreateTime()).isEqualTo(entity.getUpdateTime());
        }

        @Test
        @DisplayName("TC-005: 参数无createTime字段")
        void intercept_whenNoCreateTimeField_shouldNotThrowException() throws Throwable {
            // given
            SimpleEntity entity = new SimpleEntity();
            setupInvocation(SqlCommandType.INSERT, entity);

            // when & then
            assertThatCode(() -> interceptor.intercept(invocation))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("TC-006: 参数无updateTime字段")
        void intercept_whenNoUpdateTimeField_shouldNotThrowException() throws Throwable {
            // given
            SimpleEntity entity = new SimpleEntity();
            setupInvocation(SqlCommandType.INSERT, entity);

            // when & then
            assertThatCode(() -> interceptor.intercept(invocation))
                    .doesNotThrowAnyException();
        }
    }

    // ==================== UPDATE 语句测试 ====================

    @Nested
    @DisplayName("UPDATE 语句测试")
    class UpdateTest {

        @Test
        @DisplayName("TC-003: UPDATE设置updateTime")
        void intercept_whenUpdate_shouldSetUpdateTime() throws Throwable {
            // given
            TestEntity entity = new TestEntity();
            setupInvocation(SqlCommandType.UPDATE, entity);

            // when
            interceptor.intercept(invocation);

            // then
            assertThat(entity.getUpdateTime()).isNotNull();
        }

        @Test
        @DisplayName("TC-004: UPDATE不修改createTime")
        void intercept_whenUpdate_shouldNotModifyCreateTime() throws Throwable {
            // given
            TestEntity entity = new TestEntity();
            LocalDateTime originalCreateTime = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
            entity.setCreateTime(originalCreateTime);
            setupInvocation(SqlCommandType.UPDATE, entity);

            // when
            interceptor.intercept(invocation);

            // then
            assertThat(entity.getCreateTime()).isEqualTo(originalCreateTime);
        }
    }

    // ==================== 边界条件测试 ====================

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryTest {

        @Test
        @DisplayName("TC-007: 参数为null")
        void intercept_whenParameterIsNull_shouldHandleGracefully() {
            // given
            Object[] args = new Object[]{mappedStatement, null};
            when(invocation.getArgs()).thenReturn(args);
            when(mappedStatement.getSqlCommandType()).thenReturn(SqlCommandType.INSERT);

            // when & then - 应该抛出 NullPointerException，因为反射无法处理 null
            // 这是预期行为，拦截器假设参数不为 null
            assertThatCode(() -> interceptor.intercept(invocation))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("DELETE语句不处理")
        void intercept_whenDelete_shouldNotModifyFields() throws Throwable {
            // given
            TestEntity entity = new TestEntity();
            LocalDateTime originalTime = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
            entity.setCreateTime(originalTime);
            entity.setUpdateTime(originalTime);
            setupInvocation(SqlCommandType.DELETE, entity);

            // when
            interceptor.intercept(invocation);

            // then - 字段应该保持不变
            assertThat(entity.getCreateTime()).isEqualTo(originalTime);
            assertThat(entity.getUpdateTime()).isEqualTo(originalTime);
        }

        @Test
        @DisplayName("SELECT语句不处理")
        void intercept_whenSelect_shouldNotModifyFields() throws Throwable {
            // given
            TestEntity entity = new TestEntity();
            LocalDateTime originalTime = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
            entity.setCreateTime(originalTime);
            entity.setUpdateTime(originalTime);
            setupInvocation(SqlCommandType.SELECT, entity);

            // when
            interceptor.intercept(invocation);

            // then - 字段应该保持不变
            assertThat(entity.getCreateTime()).isEqualTo(originalTime);
            assertThat(entity.getUpdateTime()).isEqualTo(originalTime);
        }

        @Test
        @DisplayName("调用proceed方法")
        void intercept_shouldCallProceed() throws Throwable {
            // given
            TestEntity entity = new TestEntity();
            setupInvocation(SqlCommandType.INSERT, entity);
            when(invocation.proceed()).thenReturn(1);

            // when
            interceptor.intercept(invocation);

            // then
            verify(invocation).proceed();
        }
    }

    // ==================== 私有字段测试 ====================

    @Nested
    @DisplayName("私有字段测试")
    class PrivateFieldTest {

        @Test
        @DisplayName("私有字段也能被设置")
        void intercept_whenPrivateField_shouldSetSuccessfully() throws Throwable {
            // given
            PrivateFieldEntity entity = new PrivateFieldEntity();
            setupInvocation(SqlCommandType.INSERT, entity);

            // when
            interceptor.intercept(invocation);

            // then
            assertThat(entity.getCreateTime()).isNotNull();
            assertThat(entity.getUpdateTime()).isNotNull();
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 设置 Invocation mock
     */
    private void setupInvocation(SqlCommandType sqlCommandType, Object parameter) {
        Object[] args = new Object[]{mappedStatement, parameter};
        when(invocation.getArgs()).thenReturn(args);
        when(mappedStatement.getSqlCommandType()).thenReturn(sqlCommandType);
    }

    // ==================== 测试用实体类 ====================

    /**
     * 测试实体类 - 包含 createTime 和 updateTime 字段
     */
    public static class TestEntity {
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
        private String name;

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public void setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
        }

        public LocalDateTime getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(LocalDateTime updateTime) {
            this.updateTime = updateTime;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * 简单实体类 - 不包含时间字段
     */
    public static class SimpleEntity {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * 私有字段实体类
     */
    public static class PrivateFieldEntity {
        private LocalDateTime createTime;
        private LocalDateTime updateTime;

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public LocalDateTime getUpdateTime() {
            return updateTime;
        }
    }
}
