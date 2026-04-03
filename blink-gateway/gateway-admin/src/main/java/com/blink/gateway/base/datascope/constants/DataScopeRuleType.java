package com.blink.gateway.base.datascope.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据范围规则类型枚举
 * 定义具体的过滤规则类型
 *
 * @author binblink
 */
@Getter
@AllArgsConstructor
public enum DataScopeRuleType {

    /**
     * 字段过滤
     */
    FIELD_FILTER("字段过滤"),

    /**
     * 用户过滤（可匹配 create_by、update_by 等用户登入名字段）
     */
    CREATOR_FILTER("用户过滤"),

    /**
     * 时间范围过滤
     */
    DATE_RANGE_FILTER("时间范围过滤"),

    /**
     * 自定义SQL
     */
    CUSTOM_SQL("自定义SQL"),

    /**
     * 关联过滤（通过关联表过滤数据）
     */
    RELATION_FILTER("关联过滤");

    private final String description;
}