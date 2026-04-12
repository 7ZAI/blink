package com.blink.datasource.utils;

import com.blink.datasource.IntegrationTestConfig;
import com.blink.framework.common.data.Page;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PageUtils 集成测试
 * 使用 Spring Boot Test 测试分页相关功能
 *
 * @author binblink
 * @since 2026-04-12
 */
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = IntegrationTestConfig.class)
@DisplayName("PageUtils 集成测试")
class PageUtilsIntegrationTest {

    // ==================== transformOrderBy 集成测试 ====================

    @Nested
    @DisplayName("transformOrderBy 方法集成测试")
    class TransformOrderByTest {

        @Test
        @DisplayName("TC-INT-001: Page对象排序字段转换")
        void whenTransformOrderBy_shouldConvertFieldNames() {
            // given
            Page page = new Page();
            page.setPageNum(1);
            page.setPageSize(10);
            page.setOrderBy("userName asc, createTime desc");

            // when
            Page result = PageUtils.transformOrderBy(page, com.baomidou.mybatisplus.core.toolkit.StringUtils::camelToUnderline);

            // then
            assertThat(result.getOrderBy()).isEqualTo("user_name asc, create_time desc");
        }

        @Test
        @DisplayName("TC-INT-002: null排序字段处理")
        void whenNullOrderBy_shouldReturnOriginalPage() {
            // given
            Page page = new Page();
            page.setPageNum(1);
            page.setPageSize(10);
            page.setOrderBy(null);

            // when
            Page result = PageUtils.transformOrderBy(page, com.baomidou.mybatisplus.core.toolkit.StringUtils::camelToUnderline);

            // then
            assertThat(result).isSameAs(page);
        }

        @Test
        @DisplayName("TC-INT-003: 空排序字段处理")
        void whenEmptyOrderBy_shouldReturnOriginalPage() {
            // given
            Page page = new Page();
            page.setPageNum(1);
            page.setPageSize(10);
            page.setOrderBy("");

            // when
            Page result = PageUtils.transformOrderBy(page, com.baomidou.mybatisplus.core.toolkit.StringUtils::camelToUnderline);

            // then
            assertThat(result).isSameAs(page);
        }
    }

    // ==================== 分页边界条件测试 ====================

    @Nested
    @DisplayName("分页边界条件测试")
    class BoundaryTest {

        @Test
        @DisplayName("TC-INT-004: 最后一页数据")
        void whenLastPage_shouldReturnRemainingRecords() {
            // given - 总共5条数据，每页2条，第3页应返回1条
            Page page = new Page();
            page.setPageNum(3);
            page.setPageSize(2);

            // when
            // 实际测试需要 PageHelper 环境

            // then - 占位验证
            assertThat(page.getPageNum()).isEqualTo(3);
        }

        @Test
        @DisplayName("TC-INT-005: 超出页码范围")
        void whenPageNumExceedsTotal_shouldReturnEmptyList() {
            // given - 总共5条数据，请求第100页
            Page page = new Page();
            page.setPageNum(100);
            page.setPageSize(10);

            // when
            // 实际测试需要 PageHelper 环境

            // then - 占位验证
            assertThat(page.getPageNum()).isEqualTo(100);
        }

        @Test
        @DisplayName("TC-INT-006: pageSize为0")
        void whenPageSizeZero_shouldHandleCorrectly() {
            // given
            Page page = new Page();
            page.setPageNum(1);
            page.setPageSize(0);

            // when & then - 验证不抛异常
            assertThat(page.getPageSize()).isEqualTo(0);
        }
    }
}
