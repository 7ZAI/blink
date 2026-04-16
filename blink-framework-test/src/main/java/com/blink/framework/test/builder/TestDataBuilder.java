package com.blink.framework.test.builder;

import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.Page;

import java.util.UUID;

/**
 * 通用测试数据构建器
 * 支持 Builder 模式快速构建测试对象
 *
 * @author binblink
 * @since 2026-04-16
 */
public class TestDataBuilder {

    /**
     * 创建 RequestDTO 测试数据
     * 自动生成 requestId 和 traceId
     *
     * @param body 业务数据
     * @return RequestDTO 对象
     */
    public static <T> RequestDTO<T> requestDTO(T body) {
        RequestDTO<T> dto = new RequestDTO<>();
        dto.setBody(body);
        dto.setRequestId(UUID.randomUUID().toString());
        dto.setTraceId(UUID.randomUUID().toString());
        return dto;
    }

    /**
     * 创建空的 RequestDTO
     *
     * @return 空的 RequestDTO
     */
    public static RequestDTO<Void> emptyRequestDTO() {
        return requestDTO(null);
    }

    /**
     * 创建带指定 requestId 的 RequestDTO
     *
     * @param body      业务数据
     * @param requestId 指定的 requestId
     * @return RequestDTO 对象
     */
    public static <T> RequestDTO<T> requestDTOWithId(T body, String requestId) {
        RequestDTO<T> dto = new RequestDTO<>();
        dto.setBody(body);
        dto.setRequestId(requestId);
        dto.setTraceId(UUID.randomUUID().toString());
        return dto;
    }

    /**
     * 创建分页请求对象
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return Page 对象
     */
    public static Page page(int pageNum, int pageSize) {
        Page page = new Page();
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        return page;
    }

    /**
     * 创建默认分页请求（第一页，10条）
     *
     * @return 默认 Page 对象
     */
    public static Page defaultPage() {
        return page(1, 10);
    }

    /**
     * 创建大分页请求（第一页，100条）
     *
     * @return 大 Page 对象
     */
    public static Page largePage() {
        return page(1, 100);
    }

    /**
     * 创建随机字符串
     *
     * @param length 字符串长度
     * @return 随机字符串
     */
    public static String randomString(int length) {
        return UUID.randomUUID().toString().substring(0, Math.min(length, 32));
    }

    /**
     * 创建随机登录名（用于测试）
     *
     * @return 随机登录名
     */
    public static String randomLoginName() {
        return "test_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 创建随机邮箱
     *
     * @return 随机邮箱
     */
    public static String randomEmail() {
        return "test_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    /**
     * 创建随机手机号
     *
     * @return 随机手机号
     */
    public static String randomPhone() {
        return "138" + String.format("%08d", (int) (Math.random() * 100000000));
    }
}