package com.blink.datasource.handler;

import com.blink.datasource.IntegrationTestConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MyMetaObjectHandler 集成测试
 * 测试 MyBatis-Plus 字段自动填充功能
 *
 * @author binblink
 * @since 2026-04-12
 */
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = IntegrationTestConfig.class)
@DisplayName("MyMetaObjectHandler 集成测试")
class MyMetaObjectHandlerIntegrationTest {

    @Autowired
    private MyMetaObjectHandler myMetaObjectHandler;

    // ==================== MetaObjectHandler 接口测试 ====================

    @Nested
    @DisplayName("MetaObjectHandler 接口测试")
    class InterfaceTest {

        @Test
        @DisplayName("TC-INT-001: 实现MetaObjectHandler接口")
        void shouldImplementMetaObjectHandler() {
            // when & then
            assertThat(myMetaObjectHandler).isInstanceOf(com.baomidou.mybatisplus.core.handlers.MetaObjectHandler.class);
        }
    }
}
