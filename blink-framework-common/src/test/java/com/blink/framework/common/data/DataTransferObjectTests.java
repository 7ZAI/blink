package com.blink.framework.common.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * 数据传输对象单元测试
 * <p>
 * 测试覆盖：
 * 1. Page 分页属性
 * 2. EmptyBody 空响应体
 *
 * @author binblink
 */
@DisplayName("数据传输对象测试")
class DataTransferObjectTests {

    // ==================== Page 测试 ====================

    @Nested
    @DisplayName("Page 分页对象测试")
    class PageTests {

        @Test
        @DisplayName("应该正确创建Page实例")
        void shouldCreatePageInstance() {
            // when
            Page page = new Page();

            // then
            assertThat(page).isNotNull();
        }

        @Test
        @DisplayName("应该有默认分页参数")
        void shouldHaveDefaultPaginationParameters() {
            // when
            Page page = new Page();

            // then
            assertThat(page.getPageNum()).isEqualTo(1);
            assertThat(page.getPageSize()).isEqualTo(10);
            assertThat(page.getTotal()).isEqualTo(0);
            assertThat(page.getPages()).isEqualTo(1);
        }

        @Test
        @DisplayName("应该正确设置分页属性")
        void shouldSetPageProperties() {
            // given
            Page page = new Page();

            // when
            page.setPageNum(5);
            page.setPageSize(20);
            page.setTotal(100);
            page.setPages(5);
            page.setOrderBy("createTime desc");

            // then
            assertThat(page.getPageNum()).isEqualTo(5);
            assertThat(page.getPageSize()).isEqualTo(20);
            assertThat(page.getTotal()).isEqualTo(100);
            assertThat(page.getPages()).isEqualTo(5);
            assertThat(page.getOrderBy()).isEqualTo("createTime desc");
        }

        @Test
        @DisplayName("orderBy默认应该为null")
        void shouldHaveNullOrderByDefault() {
            // when
            Page page = new Page();

            // then
            assertThat(page.getOrderBy()).isNull();
        }

        @Test
        @DisplayName("toString应该包含所有属性")
        void shouldIncludeAllPropertiesInToString() {
            // given
            Page page = new Page();
            page.setPageNum(2);
            page.setPageSize(50);

            // when
            String str = page.toString();

            // then
            assertThat(str).contains("pageNum=2");
            assertThat(str).contains("pageSize=50");
        }

        @Test
        @DisplayName("total可以设置为-1表示不查询总数")
        void shouldAllowMinusOneForTotal() {
            // given
            Page page = new Page();

            // when
            page.setTotal(-1);

            // then
            assertThat(page.getTotal()).isEqualTo(-1);
        }
    }

    // ==================== EmptyBody 测试 ====================

    @Nested
    @DisplayName("EmptyBody 空响应体测试")
    class EmptyBodyTests {

        @Test
        @DisplayName("应该成功创建EmptyBody实例")
        void shouldCreateEmptyBodyInstance() {
            // when
            EmptyBody emptyBody = new EmptyBody();

            // then
            assertThat(emptyBody).isNotNull();
        }

        @Test
        @DisplayName("EmptyBody应该是可序列化的")
        void shouldBeSerializable() {
            // when
            EmptyBody emptyBody = new EmptyBody();

            // then
            assertThat(emptyBody).isInstanceOf(java.io.Serializable.class);
        }

        @Test
        @DisplayName("EmptyBody应该有serialVersionUID")
        void shouldHaveSerialVersionUID() {
            // given
            EmptyBody emptyBody = new EmptyBody();

            // when & then
            // 验证可以正常创建和序列化
            assertThatCode(() -> {
                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(bos);
                oos.writeObject(emptyBody);
                oos.close();
            }).doesNotThrowAnyException();
        }
    }
}
