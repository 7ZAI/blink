package com.blink.datasource.function;

/**
 * 排序字段转换器
 * 用于将前端传递的排序字段名转换为数据库字段名
 *
 * <p>使用示例：
 * <pre>
 * // 简单转换
 * OrderFieldConverter converter = fieldName -> {
 *     Map<String, String> mapping = Map.of(
 *         "userName", "user_name",
 *         "createTime", "create_time"
 *     );
 *     return mapping.getOrDefault(fieldName, fieldName);
 * };
 *
 * // 使用 Lambda 表达式
 * String dbOrderBy = PageUtils.transformOrderBy("userName asc, createTime desc",
 *     field -> CaseUtil.toUnderlineCase(field));
 * </pre>
 *
 * @author binblink
 */
@FunctionalInterface
public interface OrderFieldConverter {

    /**
     * 转换字段名
     *
     * @param frontendField 前端传递的字段名
     * @return 数据库字段名，如果不需要转换可返回原值或 null
     */
    String convert(String frontendField);
}