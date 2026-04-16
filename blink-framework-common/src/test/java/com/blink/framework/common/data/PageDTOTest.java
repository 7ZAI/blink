package com.blink.framework.common.data;

import com.blink.framework.test.annotation.UnitTest;
import com.blink.framework.test.base.BlinkUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * PageDTO 单元测试
 * <p>
 * 测试覆盖：
 * 1. 静态工厂方法
 * 2. 分页计算
 * 3. Getter/Setter
 * 4. 继承关系
 *
 * @author binblink
 */
@UnitTest
@DisplayName("PageDTO 分页数据传输对象测试")
class PageDTOTest extends BlinkUnitTest {

    // ==================== 静态工厂方法测试 ====================

    @Nested
    @DisplayName("静态工厂方法测试")
    class StaticFactoryMethodTests {

        @Test
        @DisplayName("应该正确创建PageDTO实例")
        void shouldCreatePageDTOInstance() {
            // given
            List<String> rows = Arrays.asList("item1", "item2", "item3");
            int total = 100;

            // when
            PageDTO<String> pageDTO = PageDTO.of(rows, total, 1, 10);

            // then
            assertThat(pageDTO).isNotNull();
            assertThat(pageDTO.getRows()).isEqualTo(rows);
            assertThat(pageDTO.getTotal()).isEqualTo(total);
            assertThat(pageDTO.getPageNum()).isEqualTo(1);
            assertThat(pageDTO.getPageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("应该正确计算总页数")
        void shouldCalculateTotalPages() {
            // given - 100条记录，每页10条
            List<String> rows = Collections.nCopies(10, "item");

            // when
            PageDTO<String> pageDTO = PageDTO.of(rows, 100, 1, 10);

            // then
            assertThat(pageDTO.getPages()).isEqualTo(10);
        }

        @Test
        @DisplayName("不整除时应该向上取整计算总页数")
        void shouldCeilTotalPagesWhenNotDivisible() {
            // given - 95条记录，每页10条，应该有10页
            List<String> rows = Collections.nCopies(10, "item");

            // when
            PageDTO<String> pageDTO = PageDTO.of(rows, 95, 1, 10);

            // then
            assertThat(pageDTO.getPages()).isEqualTo(10);
        }

        @Test
        @DisplayName("pageNum为null时应该默认为1")
        void shouldDefaultPageNumToOneWhenNull() {
            // when
            PageDTO<String> pageDTO = PageDTO.of(Collections.emptyList(), 0, null, 10);

            // then
            assertThat(pageDTO.getPageNum()).isEqualTo(1);
        }

        @Test
        @DisplayName("pageSize为null时应该默认为10")
        void shouldDefaultPageSizeToTenWhenNull() {
            // when
            PageDTO<String> pageDTO = PageDTO.of(Collections.emptyList(), 0, 1, null);

            // then
            assertThat(pageDTO.getPageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("空列表应该正确处理")
        void shouldHandleEmptyList() {
            // when
            PageDTO<String> pageDTO = PageDTO.of(Collections.emptyList(), 0, 1, 10);

            // then
            assertThat(pageDTO.getRows()).isEmpty();
            assertThat(pageDTO.getTotal()).isEqualTo(0);
            assertThat(pageDTO.getPages()).isEqualTo(0);
        }

        @Test
        @DisplayName("总记录数为0时总页数应该为0")
        void shouldHaveZeroPagesWhenTotalIsZero() {
            // when
            PageDTO<String> pageDTO = PageDTO.of(Collections.emptyList(), 0, 1, 10);

            // then
            assertThat(pageDTO.getPages()).isEqualTo(0);
        }

        @Test
        @DisplayName("pageSize为0时应该正确处理")
        void shouldHandleZeroPageSize() {
            // when
            PageDTO<String> pageDTO = PageDTO.of(Collections.emptyList(), 100, 1, 0);

            // then
            assertThat(pageDTO.getPages()).isEqualTo(0);
        }
    }

    // ==================== Getter/Setter测试 ====================

    @Nested
    @DisplayName("Getter/Setter测试")
    class GetterSetterTests {

        @Test
        @DisplayName("应该正确设置和获取rows")
        void shouldSetAndGetRows() {
            // given
            PageDTO<String> pageDTO = new PageDTO<>();
            List<String> rows = Arrays.asList("a", "b", "c");

            // when
            pageDTO.setRows(rows);

            // then
            assertThat(pageDTO.getRows()).isEqualTo(rows);
        }

        @Test
        @DisplayName("应该正确继承Page的属性")
        void shouldInheritPageProperties() {
            // given
            PageDTO<String> pageDTO = new PageDTO<>();

            // when
            pageDTO.setPageNum(2);
            pageDTO.setPageSize(20);
            pageDTO.setTotal(200);
            pageDTO.setPages(10);
            pageDTO.setOrderBy("createTime desc");

            // then
            assertThat(pageDTO.getPageNum()).isEqualTo(2);
            assertThat(pageDTO.getPageSize()).isEqualTo(20);
            assertThat(pageDTO.getTotal()).isEqualTo(200);
            assertThat(pageDTO.getPages()).isEqualTo(10);
            assertThat(pageDTO.getOrderBy()).isEqualTo("createTime desc");
        }
    }

    // ==================== toString测试 ====================

    @Nested
    @DisplayName("toString测试")
    class ToStringTests {

        @Test
        @DisplayName("toString应该包含所有属性")
        void shouldIncludeAllPropertiesInToString() {
            // given
            List<String> rows = Arrays.asList("item1", "item2");
            PageDTO<String> pageDTO = PageDTO.of(rows, 100, 1, 10);

            // when
            String str = pageDTO.toString();

            // then
            assertThat(str).contains("pageNum=1");
            assertThat(str).contains("pageSize=10");
            assertThat(str).contains("total=100");
            assertThat(str).contains("rows=");
        }
    }

    // ==================== 泛型测试 ====================

    @Nested
    @DisplayName("泛型类型测试")
    class GenericTypeTests {

        @Test
        @DisplayName("应该支持String类型")
        void shouldSupportStringType() {
            // when
            PageDTO<String> pageDTO = PageDTO.of(Arrays.asList("a", "b"), 2, 1, 10);

            // then
            assertThat(pageDTO.getRows()).containsExactly("a", "b");
        }

        @Test
        @DisplayName("应该支持自定义对象类型")
        void shouldSupportCustomObjectType() {
            // given
            @lombok.Data
            @lombok.AllArgsConstructor
            class User {
                private Long id;
                private String name;
            }
            List<User> users = Arrays.asList(
                    new User(1L, "张三"),
                    new User(2L, "李四")
            );

            // when
            PageDTO<User> pageDTO = PageDTO.of(users, 2, 1, 10);

            // then
            assertThat(pageDTO.getRows()).hasSize(2);
            assertThat(pageDTO.getRows().get(0).getName()).isEqualTo("张三");
        }

        @Test
        @DisplayName("应该支持Integer类型")
        void shouldSupportIntegerType() {
            // when
            PageDTO<Integer> pageDTO = PageDTO.of(Arrays.asList(1, 2, 3), 3, 1, 10);

            // then
            assertThat(pageDTO.getRows()).containsExactly(1, 2, 3);
        }
    }

    // ==================== 继承关系测试 ====================

    @Nested
    @DisplayName("继承关系测试")
    class InheritanceTests {

        @Test
        @DisplayName("PageDTO应该是Page的子类")
        void shouldBeSubclassOfPage() {
            // given
            PageDTO<String> pageDTO = new PageDTO<>();

            // when & then
            assertThat(pageDTO).isInstanceOf(Page.class);
        }

        @Test
        @DisplayName("应该可以使用Page引用")
        void canBeReferencedByPageType() {
            // given
            Page page = PageDTO.of(Arrays.asList("a", "b"), 100, 1, 10);

            // when & then
            assertThat(page.getPageNum()).isEqualTo(1);
            assertThat(page.getPageSize()).isEqualTo(10);
            assertThat(page.getTotal()).isEqualTo(100);
        }
    }

    // ==================== 边界条件测试 ====================

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("第一页应该正确计算")
        void shouldCalculateFirstPageCorrectly() {
            // given
            List<String> rows = Collections.nCopies(10, "item");

            // when
            PageDTO<String> pageDTO = PageDTO.of(rows, 100, 1, 10);

            // then
            assertThat(pageDTO.getPageNum()).isEqualTo(1);
        }

        @Test
        @DisplayName("最后一页应该正确计算")
        void shouldCalculateLastPageCorrectly() {
            // given - 第10页是最后一页
            List<String> rows = Collections.nCopies(10, "item");

            // when
            PageDTO<String> pageDTO = PageDTO.of(rows, 100, 10, 10);

            // then
            assertThat(pageDTO.getPageNum()).isEqualTo(10);
            assertThat(pageDTO.getPages()).isEqualTo(10);
        }

        @Test
        @DisplayName("单页数据应该正确处理")
        void shouldHandleSinglePageData() {
            // given - 只有5条记录，一页就够
            List<String> rows = Arrays.asList("a", "b", "c", "d", "e");

            // when
            PageDTO<String> pageDTO = PageDTO.of(rows, 5, 1, 10);

            // then
            assertThat(pageDTO.getPages()).isEqualTo(1);
            assertThat(pageDTO.getRows()).hasSize(5);
        }

        @Test
        @DisplayName("大页码应该正确处理")
        void shouldHandleLargePageNumber() {
            // when
            PageDTO<String> pageDTO = PageDTO.of(Collections.emptyList(), 1000, 100, 10);

            // then
            assertThat(pageDTO.getPageNum()).isEqualTo(100);
        }

        @Test
        @DisplayName("大每页大小应该正确处理")
        void shouldHandleLargePageSize() {
            // when
            PageDTO<String> pageDTO = PageDTO.of(Collections.nCopies(100, "item"), 100, 1, 100);

            // then
            assertThat(pageDTO.getPageSize()).isEqualTo(100);
            assertThat(pageDTO.getPages()).isEqualTo(1);
        }

        @Test
        @DisplayName("rows为null时应该能正常设置")
        void shouldHandleNullRows() {
            // given
            PageDTO<String> pageDTO = new PageDTO<>();

            // when
            pageDTO.setRows(null);

            // then
            assertThat(pageDTO.getRows()).isNull();
        }
    }
}
