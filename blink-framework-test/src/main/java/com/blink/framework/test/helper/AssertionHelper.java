package com.blink.framework.test.helper;

import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.data.PageDTO;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.ObjectAssert;

import java.util.List;
import java.util.function.Consumer;

/**
 * Blink 专用断言辅助工具
 * 提供 ResponseDTO、PageDTO、RequestDTO 等断言方法
 *
 * 使用方式：
 * <pre>
 * // 断言 ResponseDTO 成功
 * AssertionHelper.assertThatSuccess(response);
 *
 * // 断言 ResponseDTO 失败
 * AssertionHelper.assertThatError(response, "BUSS0001");
 *
 * // 断言 PageDTO 分页数据
 * AssertionHelper.assertThatPage(pageRsp, 10, 100);
 *
 * // 软断言（收集所有错误）
 * AssertionHelper.assertSoftly(soft -> {
 *     soft.assertThat(response.getMsgCode()).isEqualTo("BLINK0000");
 *     soft.assertThat(response.getBody()).isNotNull();
 * });
 * </pre>
 *
 * @author binblink
 * @since 2026-04-16
 */
public class AssertionHelper {

    // ========== ResponseDTO 断言 ==========

    /**
     * 断言 ResponseDTO 成功（msgCode = "BLINK0000")
     *
     * @param response ResponseDTO 对象
     * @return ObjectAssert 用于进一步断言
     */
    public static ObjectAssert<ResponseDTO<?>> assertThatSuccess(ResponseDTO<?> response) {
        Assertions.assertThat(response)
                .isNotNull()
                .extracting("msgCode")
                .isEqualTo("BLINK0000");
        return Assertions.assertThat(response);
    }

    /**
     * 断言 ResponseDTO 成功并验证 body 存在
     *
     * @param response ResponseDTO 对象
     * @return ObjectAssert 用于进一步断言
     */
    public static ObjectAssert<ResponseDTO<?>> assertThatSuccessWithBody(ResponseDTO<?> response) {
        Assertions.assertThat(response)
                .isNotNull()
                .extracting("msgCode", "body")
                .containsExactly("BLINK0000", Assertions.assertThat(response.getBody()).isNotNull());
        return Assertions.assertThat(response);
    }

    /**
     * 断言 ResponseDTO 失败（指定错误码）
     *
     * @param response  ResponseDTO 对象
     * @param errorCode 期望的错误码
     * @return ObjectAssert 用于进一步断言
     */
    public static ObjectAssert<ResponseDTO<?>> assertThatError(ResponseDTO<?> response, String errorCode) {
        Assertions.assertThat(response)
                .isNotNull()
                .extracting("msgCode")
                .isEqualTo(errorCode);
        return Assertions.assertThat(response);
    }

    /**
     * 断言 ResponseDTO 失败（错误码匹配前缀）
     *
     * @param response     ResponseDTO 对象
     * @param errorPrefix  错误码前缀（如 "GATE"）
     * @return ObjectAssert 用于进一步断言
     */
    public static ObjectAssert<ResponseDTO<?>> assertThatErrorPrefix(ResponseDTO<?> response, String errorPrefix) {
        Assertions.assertThat(response)
                .isNotNull()
                .extracting("msgCode")
                .asString()
                .startsWith(errorPrefix);
        return Assertions.assertThat(response);
    }

    /**
     * 断言 ResponseDTO 消息内容
     *
     * @param response ResponseDTO 对象
     * @param message  期望的消息内容
     */
    public static void assertThatMessage(ResponseDTO<?> response, String message) {
        Assertions.assertThat(response)
                .isNotNull()
                .extracting("msg")
                .isEqualTo(message);
    }

    /**
     * 断言 ResponseDTO body 为空
     *
     * @param response ResponseDTO 对象
     */
    public static void assertThatEmptyBody(ResponseDTO<?> response) {
        Assertions.assertThat(response.getBody()).isNull();
    }

    // ========== PageDTO 断言 ==========

    /**
     * 断言 PageDTO 分页数据
     *
     * @param pageDTO   PageDTO 对象
     * @param pageSize  期望的每页大小
     * @param total     期望的总记录数
     */
    public static void assertThatPage(PageDTO<?> pageDTO, int pageSize, long total) {
        Assertions.assertThat(pageDTO)
                .isNotNull()
                .extracting("pageSize", "total")
                .containsExactly(pageSize, total);
    }

    /**
     * 断言 PageDTO 数据列表不为空
     *
     * @param pageDTO PageDTO 对象
     */
    public static void assertThatPageNotEmpty(PageDTO<?> pageDTO) {
        Assertions.assertThat(pageDTO)
                .isNotNull()
                .extracting("rows")
                .asList()
                .isNotEmpty();
    }

    /**
     * 断言 PageDTO 数据列表为空
     *
     * @param pageDTO PageDTO 对象
     */
    public static void assertThatPageEmpty(PageDTO<?> pageDTO) {
        Assertions.assertThat(pageDTO)
                .isNotNull()
                .extracting("rows")
                .asList()
                .isEmpty();
    }

    /**
     * 断言 PageDTO 数据列表大小
     *
     * @param pageDTO PageDTO 对象
     * @param size    期望的列表大小
     */
    public static void assertThatPageSize(PageDTO<?> pageDTO, int size) {
        Assertions.assertThat(pageDTO)
                .isNotNull()
                .extracting("rows")
                .asList()
                .hasSize(size);
    }

    /**
     * 断言 PageDTO 第一页
     *
     * @param pageDTO PageDTO 对象
     */
    public static void assertThatFirstPage(PageDTO<?> pageDTO) {
        Assertions.assertThat(pageDTO)
                .isNotNull()
                .extracting("pageNum")
                .isEqualTo(1);
    }

    // ========== RequestDTO 断言 ==========

    /**
     * 断言 RequestDTO 包含 body
     *
     * @param requestDTO RequestDTO 对象
     */
    public static void assertThatHasBody(RequestDTO<?> requestDTO) {
        Assertions.assertThat(requestDTO)
                .isNotNull()
                .extracting("body")
                .isNotNull();
    }

    /**
     * 断言 RequestDTO requestId 存在
     *
     * @param requestDTO RequestDTO 对象
     */
    public static void assertThatHasRequestId(RequestDTO<?> requestDTO) {
        Assertions.assertThat(requestDTO)
                .isNotNull()
                .extracting("requestId")
                .isNotNull()
                .asString()
                .isNotEmpty();
    }

    // ========== 软断言 ==========

    /**
     * 执行软断言（收集所有错误，不会在第一个失败时停止）
     *
     * @param softAssertionsConsumer 软断言消费者
     */
    public static void assertSoftly(Consumer<SoftAssertions> softAssertionsConsumer) {
        SoftAssertions softAssertions = new SoftAssertions();
        softAssertionsConsumer.accept(softAssertions);
        softAssertions.assertAll();
    }

    /**
     * 创建软断言对象（手动控制 assertAll）
     *
     * @return SoftAssertions 对象
     */
    public static SoftAssertions createSoftAssertions() {
        return new SoftAssertions();
    }

    // ========== 集合断言 ==========

    /**
     * 断言列表包含指定元素（按属性）
     *
     * @param list      列表
     * @param property  属性名
     * @param values    期望的属性值
     */
    public static void assertThatListContains(List<?> list, String property, Object... values) {
        Assertions.assertThat(list)
                .extracting(property)
                .contains(values);
    }

    /**
     * 断言列表大小
     *
     * @param list 列表
     * @param size 期望大小
     */
    public static void assertThatListSize(List<?> list, int size) {
        Assertions.assertThat(list).hasSize(size);
    }

    /**
     * 断言列表为空
     *
     * @param list 列表
     */
    public static void assertThatListEmpty(List<?> list) {
        Assertions.assertThat(list).isEmpty();
    }

    /**
     * 断言列表不为空
     *
     * @param list 列表
     */
    public static void assertThatListNotEmpty(List<?> list) {
        Assertions.assertThat(list).isNotEmpty();
    }

    // ========== 条件断言 ==========

    /**
     * 断言对象满足条件
     *
     * @param object    对象
     * @param condition 条件描述
     * @param predicate 条件判断
     */
    public static <T> void assertThatCondition(T object, String condition, java.util.function.Predicate<T> predicate) {
        Assertions.assertThat(object)
                .as(condition)
                .matches(predicate);
    }
}