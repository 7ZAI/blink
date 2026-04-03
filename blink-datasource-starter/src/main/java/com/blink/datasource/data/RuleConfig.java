package com.blink.datasource.data;

import cn.hutool.core.bean.BeanUtil;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 规则配置类
 * 用于映射规则配置JSON
 *
 * @author binblink
 */
@Data
public class RuleConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 规则类型
     */
    private String ruleType;

    /**
     * 实体类全限定名（规则所属的实体类）
     */
    private String entityClass;

    /**
     * 字段名（条件过滤使用）
     */
    private String field;

    // ========== FIELD_FILTER ==========

    /**
     * 排除的字段列表
     */
    private List<String> excludeFields;

    /**
     * 只包含的字段（与exclude互斥）
     *
     */
    private List<String> includeFields;

    // ========== CREATOR_FILTER ==========

    /**
     * 匹配类型
     * CREATOR_FILTER: CURRENT_USER/USER_LIST/ROLE_USER
     */
    private String matchType;

    /**
     * 指定用户ID列表（matchType=USER_LIST时使用）
     */
    private List<Integer> userIds;

    /**
     * 指定用户登入名列表（matchType=USER_LIST时使用，用于 create_by/update_by 字段）
     */
    private List<String> loginNames;

    /**
     * 指定角色ID列表（matchType=ROLE_USER时使用）
     */
    private List<Integer> roleIds;

    // ========== DATE_RANGE_FILTER ==========

    /**
     * 范围类型：RELATIVE（相对）/ ABSOLUTE（绝对）
     */
    private String rangeType;

    /**
     * 相对值（负数表示过去）
     */
    private Integer relativeValue;

    /**
     * 单位：DAY/WEEK/MONTH/YEAR
     */
    private String relativeUnit;

    /**
     * 绝对开始时间
     */
    private String startTime;

    /**
     * 绝对结束时间
     */
    private String endTime;

    // ========== CUSTOM_SQL ==========

    /**
     * 自定义SQL片段
     */
    private String sqlFragment;

    // ========== RELATION_FILTER ==========

    /**
     * 关联表名
     */
    private String relationTable;

    /**
     * 当前实体的关联字段
     */
    private String sourceField;

    /**
     * 关联表中关联当前实体的字段
     */
    private String relationSourceField;

    /**
     * 关联表中关联目标实体的字段
     */
    private String relationTargetField;

    /**
     * 匹配类型：CURRENT_USER/CURRENT_DEPT/USER_LIST/DEPT_LIST/ROLE_LIST
     */
    private String relationMatchType;

    /**
     * 匹配值列表（当 matchType 为 *_LIST 时使用）
     */
    private List<Integer> relationMatchValues;

    /**
     * 创建副本
     *
     * @return 副本对象
     */
    public RuleConfig copy() {
        return BeanUtil.copyProperties(this, RuleConfig.class);
    }
}