package com.blink.datasource.interceptor;

import com.blink.datasource.IntegrationTestConfig;
import com.blink.datasource.data.RuleConfig;
import com.blink.datasource.data.UserDataScopeInfo;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataScopeInterceptor 集成测试
 * 使用 Spring Boot Test 测试拦截器相关功能
 *
 * @author binblink
 * @since 2026-04-12
 */
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = IntegrationTestConfig.class)
@DisplayName("DataScopeInterceptor 集成测试")
class DataScopeInterceptorIntegrationTest {

    @Autowired
    private DataScopeInterceptor dataScopeInterceptor;

    @Autowired
    private Supplier<UserDataScopeInfo> userInfoSupplier;

    // ==================== 拦截器初始化测试 ====================

    @Nested
    @DisplayName("拦截器初始化测试")
    class InitializationTest {

        @Test
        @DisplayName("TC-INT-001: 拦截器正确初始化")
        void shouldInitializeCorrectly() {
            // when & then
            assertThat(dataScopeInterceptor).isNotNull();
        }
    }

    // ==================== 用户上下文测试 ====================

    @Nested
    @DisplayName("用户上下文测试")
    class UserContextTest {

        @Test
        @DisplayName("TC-INT-002: 获取用户信息")
        void whenGetUserInfo_shouldReturnTestUser() {
            // when
            UserDataScopeInfo userInfo = userInfoSupplier.get();

            // then
            assertThat(userInfo).isNotNull();
            assertThat(userInfo.getUserId()).isEqualTo(1);
            assertThat(userInfo.getLoginName()).isEqualTo("testUser");
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建测试用规则配置
     */
    private List<RuleConfig> createTestRuleConfigs() {
        RuleConfig config = new RuleConfig();
        config.setRuleType("CREATOR_FILTER");
        config.setEntityClass("com.blink.datasource.TestEntities$TestUser");
        config.setField("createBy");
        config.setMatchType("CURRENT_USER");

        return new ArrayList<>(List.of(config));
    }
}
