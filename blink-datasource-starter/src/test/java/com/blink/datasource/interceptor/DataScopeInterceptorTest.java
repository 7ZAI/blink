package com.blink.datasource.interceptor;

import com.blink.datasource.annotation.DataScope;
import com.blink.datasource.component.DataScopeEntityScanner;
import com.blink.datasource.constants.DataSourceConstant;
import com.blink.datasource.data.DataScopeParseResult;
import com.blink.datasource.data.RuleConfig;
import com.blink.datasource.data.RuleMerge;
import com.blink.datasource.data.UserDataScopeInfo;
import com.blink.datasource.handler.RuleHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.reflection.MetaObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DataScopeInterceptor 单元测试
 * 验证数据范围权限拦截器的核心拦截逻辑
 *
 * <p><b>测试限制说明：</b></p>
 * <p>DataScopeInterceptor 深度依赖 MyBatis 运行时环境，包括：</p>
 * <ul>
 *   <li>StatementHandler 及其内部结构</li>
 *   <li>BoundSql 的 sql 字段修改（通过反射）</li>
 *   <li>MappedStatement 的获取和解析</li>
 *   <li>DataScopeEntityScanner 的静态缓存</li>
 * </ul>
 * <p>核心拦截方法 intercept() 和 shouldBeIntercepted() 建议通过集成测试验证。</p>
 * <p>本测试类重点测试可独立测试的 getMergedRules() 和 getHandler() 方法。</p>
 *
 * @author binblink
 * @since 2026-04-12
 */
@DisplayName("DataScopeInterceptor 单元测试")
@ExtendWith(MockitoExtension.class)
class DataScopeInterceptorTest {

    @Mock
    private RuleHandler ruleHandler;

    @Mock
    private RuleMerge ruleMerge;

    @Mock
    private Supplier<UserDataScopeInfo> userInfoSupplier;

    private DataScopeInterceptor interceptor;
    private List<RuleHandler> ruleHandlers;

    @BeforeEach
    void setUp() {
        ruleHandlers = new ArrayList<>();
        interceptor = new DataScopeInterceptor(ruleHandlers, userInfoSupplier, ruleMerge);
    }

    // ==================== getHandler 方法测试 ====================

    @Nested
    @DisplayName("getHandler 方法测试")
    class GetHandlerTest {

        @Test
        @DisplayName("TC-019: 找到处理器")
        void getHandler_whenHandlerExists_shouldReturnHandler() throws Exception {
            // given
            when(ruleHandler.getRuleType()).thenReturn("FIELD_FILTER");
            ruleHandlers.add(ruleHandler);

            // when
            RuleHandler result = invokeGetHandler(interceptor, "FIELD_FILTER");

            // then
            assertThat(result).isEqualTo(ruleHandler);
        }

        @Test
        @DisplayName("TC-020: 未找到处理器")
        void getHandler_whenHandlerNotExists_shouldReturnNull() throws Exception {
            // given - 空的处理器列表

            // when
            RuleHandler result = invokeGetHandler(interceptor, "UNKNOWN_TYPE");

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("TC-021: null规则类型")
        void getHandler_whenRuleTypeIsNull_shouldReturnNull() throws Exception {
            // when
            RuleHandler result = invokeGetHandler(interceptor, null);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("TC-022: handlers为null")
        void getHandler_whenHandlersIsNull_shouldReturnNull() throws Exception {
            // given - 创建一个没有handlers的拦截器
            DataScopeInterceptor interceptorWithNullHandlers = new DataScopeInterceptor(null, userInfoSupplier, ruleMerge);

            // when
            RuleHandler result = invokeGetHandler(interceptorWithNullHandlers, "FIELD_FILTER");

            // then
            assertThat(result).isNull();
        }
    }

    // ==================== getMergedRules 方法测试 ====================

    @Nested
    @DisplayName("getMergedRules 方法测试")
    class GetMergedRulesTest {

        @Test
        @DisplayName("TC-014: 无匹配规则")
        void getMergedRules_whenNoMatch_shouldReturnEmptyList() throws Exception {
            // given
            List<RuleConfig> roleRules = new ArrayList<>();
            RuleConfig rule = new RuleConfig();
            rule.setEntityClass("com.blink.entity.OtherEntity");
            rule.setRuleType("FIELD_FILTER");
            roleRules.add(rule);

            // when
            List<RuleConfig> result = invokeGetMergedRules(interceptor, roleRules, "com.blink.entity.TargetEntity");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("TC-015: 单个规则")
        void getMergedRules_whenSingleRule_shouldReturnSingleRule() throws Exception {
            // given
            List<RuleConfig> roleRules = new ArrayList<>();
            RuleConfig rule = new RuleConfig();
            rule.setEntityClass("com.blink.entity.TargetEntity");
            rule.setRuleType("FIELD_FILTER");
            roleRules.add(rule);

            // when
            List<RuleConfig> result = invokeGetMergedRules(interceptor, roleRules, "com.blink.entity.TargetEntity");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(rule);
        }

        @Test
        @DisplayName("TC-016: 同类型多规则")
        void getMergedRules_whenMultipleSameTypeRules_shouldMergeRules() throws Exception {
            // given
            List<RuleConfig> roleRules = new ArrayList<>();
            RuleConfig rule1 = new RuleConfig();
            rule1.setEntityClass("com.blink.entity.TargetEntity");
            rule1.setRuleType("FIELD_FILTER");
            rule1.setExcludeFields(List.of("password"));

            RuleConfig rule2 = new RuleConfig();
            rule2.setEntityClass("com.blink.entity.TargetEntity");
            rule2.setRuleType("FIELD_FILTER");
            rule2.setExcludeFields(List.of("salt"));

            roleRules.add(rule1);
            roleRules.add(rule2);

            RuleConfig mergedRule = new RuleConfig();
            mergedRule.setRuleType("FIELD_FILTER");
            mergedRule.setExcludeFields(List.of("password", "salt"));
            when(ruleMerge.merge(any())).thenReturn(mergedRule);

            // when
            List<RuleConfig> result = invokeGetMergedRules(interceptor, roleRules, "com.blink.entity.TargetEntity");

            // then
            assertThat(result).hasSize(1);
            verify(ruleMerge).merge(any());
        }

        @Test
        @DisplayName("TC-017: 不同类型多规则")
        void getMergedRules_whenMultipleDifferentTypeRules_shouldReturnSeparately() throws Exception {
            // given
            List<RuleConfig> roleRules = new ArrayList<>();
            RuleConfig rule1 = new RuleConfig();
            rule1.setEntityClass("com.blink.entity.TargetEntity");
            rule1.setRuleType("FIELD_FILTER");

            RuleConfig rule2 = new RuleConfig();
            rule2.setEntityClass("com.blink.entity.TargetEntity");
            rule2.setRuleType("CREATOR_FILTER");

            roleRules.add(rule1);
            roleRules.add(rule2);

            // when
            List<RuleConfig> result = invokeGetMergedRules(interceptor, roleRules, "com.blink.entity.TargetEntity");

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("TC-018: 规则类型分组")
        void getMergedRules_whenMixedTypes_shouldGroupCorrectly() throws Exception {
            // given - 2个FIELD_FILTER + 1个CREATOR_FILTER
            List<RuleConfig> roleRules = new ArrayList<>();

            RuleConfig rule1 = new RuleConfig();
            rule1.setEntityClass("com.blink.entity.TargetEntity");
            rule1.setRuleType("FIELD_FILTER");

            RuleConfig rule2 = new RuleConfig();
            rule2.setEntityClass("com.blink.entity.TargetEntity");
            rule2.setRuleType("FIELD_FILTER");

            RuleConfig rule3 = new RuleConfig();
            rule3.setEntityClass("com.blink.entity.TargetEntity");
            rule3.setRuleType("CREATOR_FILTER");

            roleRules.add(rule1);
            roleRules.add(rule2);
            roleRules.add(rule3);

            RuleConfig mergedFieldFilter = new RuleConfig();
            mergedFieldFilter.setRuleType("FIELD_FILTER");
            when(ruleMerge.merge(any())).thenReturn(mergedFieldFilter);

            // when
            List<RuleConfig> result = invokeGetMergedRules(interceptor, roleRules, "com.blink.entity.TargetEntity");

            // then
            assertThat(result).hasSize(2);
        }
    }

    // ==================== intercept 方法测试 ====================
    // 注意：以下测试需要深度 Mock MyBatis 内部结构

    @Nested
    @DisplayName("intercept 方法测试")
    class InterceptTest {

        @Test
        @DisplayName("TC-023: 不需要拦截-无用户上下文")
        void intercept_whenNoUserContext_shouldProceedDirectly() throws Throwable {
            // given
            when(userInfoSupplier.get()).thenReturn(null);

            Invocation invocation = mock(Invocation.class);
            when(invocation.proceed()).thenReturn("result");

            // when
            Object result = interceptor.intercept(invocation);

            // then
            assertThat(result).isEqualTo("result");
            verify(invocation).proceed();
        }

        @Test
        @DisplayName("TC-024: 不需要拦截-用户无过滤权限")
        void intercept_whenNoRuleConfigs_shouldProceedDirectly() throws Throwable {
            // given
            UserDataScopeInfo userInfo = new UserDataScopeInfo();
            userInfo.setRuleConfigs(null);
            when(userInfoSupplier.get()).thenReturn(userInfo);

            Invocation invocation = mock(Invocation.class);
            when(invocation.proceed()).thenReturn("result");

            // when
            Object result = interceptor.intercept(invocation);

            // then
            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("TC-025: 不需要拦截-用户无过滤权限（空列表）")
        void intercept_whenEmptyRuleConfigs_shouldProceedDirectly() throws Throwable {
            // given
            UserDataScopeInfo userInfo = new UserDataScopeInfo();
            userInfo.setRuleConfigs(Collections.emptyList());
            when(userInfoSupplier.get()).thenReturn(userInfo);

            Invocation invocation = mock(Invocation.class);
            when(invocation.proceed()).thenReturn("result");

            // when
            Object result = interceptor.intercept(invocation);

            // then
            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("TC-026: 不需要拦截-超级管理员")
        void intercept_whenSuperAdmin_shouldProceedDirectly() throws Throwable {
            // given
            UserDataScopeInfo userInfo = new UserDataScopeInfo();
            userInfo.setSuperFlag(DataSourceConstant.SUPER_ADMIN_YES);
            userInfo.setRuleConfigs(List.of(new RuleConfig()));
            when(userInfoSupplier.get()).thenReturn(userInfo);

            Invocation invocation = mock(Invocation.class);
            when(invocation.proceed()).thenReturn("result");

            // when
            Object result = interceptor.intercept(invocation);

            // then
            assertThat(result).isEqualTo("result");
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 通过反射调用 getHandler 方法
     */
    private RuleHandler invokeGetHandler(DataScopeInterceptor interceptor, String ruleType) throws Exception {
        Method method = DataScopeInterceptor.class.getDeclaredMethod("getHandler", String.class);
        method.setAccessible(true);
        return (RuleHandler) method.invoke(interceptor, ruleType);
    }

    /**
     * 通过反射调用 getMergedRules 方法
     */
    @SuppressWarnings("unchecked")
    private List<RuleConfig> invokeGetMergedRules(DataScopeInterceptor interceptor, List<RuleConfig> roleRules, String entityClass) throws Exception {
        Method method = DataScopeInterceptor.class.getDeclaredMethod("getMergedRules", List.class, String.class);
        method.setAccessible(true);
        return (List<RuleConfig>) method.invoke(interceptor, roleRules, entityClass);
    }
}
