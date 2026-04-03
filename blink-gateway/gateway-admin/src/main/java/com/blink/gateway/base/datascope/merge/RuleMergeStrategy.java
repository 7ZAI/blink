package com.blink.gateway.base.datascope.merge;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;

import cn.hutool.core.util.StrUtil;
import com.blink.gateway.base.datascope.constants.DataScopeRuleType;
import com.blink.datasource.data.RuleConfig;
import com.blink.datasource.data.RuleMerge;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 规则合并策略
 * 处理多角色场景下的规则合并逻辑，采用并集（最宽松）策略
 *
 * @author binblink
 */
@Slf4j
public class RuleMergeStrategy implements RuleMerge {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 合并同一类型的多个规则配置
     * 多角色对同一实体有多个相同类型的规则时，取并集（最宽松）
     *
     * @param sameTypeRules 同类型规则列表
     * @return 合并后的规则配置
     */
    @Override
    public RuleConfig merge(List<RuleConfig> sameTypeRules) {
        if (CollUtil.isEmpty(sameTypeRules)) {
            return null;
        }

        if (sameTypeRules.size() == 1) {
            return sameTypeRules.get(0);
        }

        // 以第一个规则为基础进行合并
        RuleConfig merged = BeanUtil.copyProperties(sameTypeRules.get(0), RuleConfig.class);
        String ruleType = merged.getRuleType();

        for (int i = 1; i < sameTypeRules.size(); i++) {
            RuleConfig source = sameTypeRules.get(i);
            mergeSameTypeRule(merged, source);
        }

        return merged;
    }

    /**
     * 合并同类型规则（并集）
     *
     * @param target 目标规则配置（合并结果）
     * @param source 源规则配置
     */
    private void mergeSameTypeRule(RuleConfig target, RuleConfig source) {
        DataScopeRuleType ruleType = DataScopeRuleType.valueOf(target.getRuleType());

        switch (ruleType) {
            case FIELD_FILTER:
                mergeFieldFilter(target, source);
                break;
            case CREATOR_FILTER:
                mergeCreatorFilter(target, source);
                break;
            case DATE_RANGE_FILTER:
                mergeDateRangeFilter(target, source);
                break;
            case CUSTOM_SQL:
                mergeCustomSql(target, source);
                break;
            case RELATION_FILTER:
                mergeRelationFilter(target, source);
                break;
            default:
                break;
        }
    }

    /**
     * 合并字段过滤规则
     * 取排除字段的交集（更宽松：只排除共同需要排除的字段）
     *
     * @param target 目标规则配置
     * @param source 源规则配置
     */
    private void mergeFieldFilter(RuleConfig target, RuleConfig source) {
        List<String> targetExclude = target.getExcludeFields();
        List<String> sourceExclude = source.getExcludeFields();

        if (targetExclude != null && sourceExclude != null) {
            // 取交集：只排除两个角色都需要排除的字段
            Set<String> mergedExclude = new HashSet<>(targetExclude);
            mergedExclude.retainAll(sourceExclude);
            target.setExcludeFields(new ArrayList<>(mergedExclude));
        } else if (targetExclude == null) {
            // 如果目标没有排除字段，则使用源的
            target.setExcludeFields(sourceExclude);
        }
        // 如果源没有排除字段，保持目标不变（更宽松）
    }

    /**
     * 合并创建人过滤规则
     * 合并用户列表（并集）
     *
     * @param target 目标规则配置
     * @param source 源规则配置
     */
    private void mergeCreatorFilter(RuleConfig target, RuleConfig source) {
        String targetMatchType = target.getMatchType();
        String sourceMatchType = source.getMatchType();

        // 如果 matchType 不同，转换为 USER_LIST 模式合并
        if (targetMatchType == null || !targetMatchType.equals(sourceMatchType)) {
            target.setMatchType("USER_LIST");
            Set<Integer> mergedUserIds = new HashSet<>();

            if (target.getUserIds() != null) {
                mergedUserIds.addAll(target.getUserIds());
            }
            if (source.getUserIds() != null) {
                mergedUserIds.addAll(source.getUserIds());
            }

            target.setUserIds(new ArrayList<>(mergedUserIds));
        } else if ("USER_LIST".equals(targetMatchType)) {
            // 同为 USER_LIST，直接合并用户列表
            Set<Integer> mergedUserIds = new HashSet<>();
            if (target.getUserIds() != null) {
                mergedUserIds.addAll(target.getUserIds());
            }
            if (source.getUserIds() != null) {
                mergedUserIds.addAll(source.getUserIds());
            }
            target.setUserIds(new ArrayList<>(mergedUserIds));
        } else if ("ROLE_USER".equals(targetMatchType)) {
            // 同为 ROLE_USER，合并角色列表
            Set<Integer> mergedRoleIds = new HashSet<>();
            if (target.getRoleIds() != null) {
                mergedRoleIds.addAll(target.getRoleIds());
            }
            if (source.getRoleIds() != null) {
                mergedRoleIds.addAll(source.getRoleIds());
            }
            target.setRoleIds(new ArrayList<>(mergedRoleIds));
        }
        // CURRENT_USER 类型不需要合并，保持原样
    }

    /**
     * 合并时间范围过滤规则
     * 取更大的时间范围（更宽松）
     *
     * @param target 目标规则配置
     * @param source 源规则配置
     */
    private void mergeDateRangeFilter(RuleConfig target, RuleConfig source) {
        String targetRangeType = target.getRangeType();
        String sourceRangeType = source.getRangeType();

        // 相对时间范围
        if ("RELATIVE".equals(targetRangeType) && "RELATIVE".equals(sourceRangeType)) {
            String targetUnit = target.getRelativeUnit();
            String sourceUnit = source.getRelativeUnit();

            if (targetUnit != null && targetUnit.equals(sourceUnit)) {
                // 相同单位时取较大的值
                int targetValue = target.getRelativeValue() != null ? target.getRelativeValue() : 0;
                int sourceValue = source.getRelativeValue() != null ? source.getRelativeValue() : 0;
                target.setRelativeValue(Math.max(targetValue, sourceValue));
            } else {
                // 不同单位时转换为天数比较
                int targetDays = convertToDays(
                        target.getRelativeValue() != null ? target.getRelativeValue() : 0,
                        targetUnit
                );
                int sourceDays = convertToDays(
                        source.getRelativeValue() != null ? source.getRelativeValue() : 0,
                        sourceUnit
                );

                if (sourceDays > targetDays) {
                    target.setRelativeValue(source.getRelativeValue());
                    target.setRelativeUnit(source.getRelativeUnit());
                }
            }
        } else {
            // 绝对时间范围或有混合类型，合并日期范围
            LocalDateTime targetStart = parseDateTime(target.getStartTime());
            LocalDateTime targetEnd = parseDateTime(target.getEndTime());
            LocalDateTime sourceStart = parseDateTime(source.getStartTime());
            LocalDateTime sourceEnd = parseDateTime(source.getEndTime());

            if (sourceStart != null && (targetStart == null || sourceStart.isBefore(targetStart))) {
                target.setStartTime(source.getStartTime());
            }
            if (sourceEnd != null && (targetEnd == null || sourceEnd.isAfter(targetEnd))) {
                target.setEndTime(source.getEndTime());
            }
        }
    }

    /**
     * 合并自定义SQL规则
     * 用 OR 连接多个SQL片段
     *
     * @param target 目标规则配置
     * @param source 源规则配置
     */
    private void mergeCustomSql(RuleConfig target, RuleConfig source) {
        String targetFragment = target.getSqlFragment();
        String sourceFragment = source.getSqlFragment();

        if (targetFragment != null && sourceFragment != null) {
            // 用 OR 连接，实现并集
            target.setSqlFragment("(" + targetFragment + " OR " + sourceFragment + ")");
        } else if (targetFragment == null) {
            target.setSqlFragment(sourceFragment);
        }
    }

    /**
     * 合并关联过滤规则
     * 相同关联表时合并匹配值（并集），不同关联表时用 OR 连接 EXISTS 子查询
     *
     * @param target 目标规则配置
     * @param source 源规则配置
     */
    private void mergeRelationFilter(RuleConfig target, RuleConfig source) {
        String targetRelationTable = target.getRelationTable();
        String sourceRelationTable = source.getRelationTable();

        // 如果关联表不同，无法直接合并，保留第一个规则
        // 原因：单个 RuleConfig 无法表示多个不同的关联条件
        if (!Objects.equals(targetRelationTable, sourceRelationTable)) {
            logDifferentRelationTables(targetRelationTable, sourceRelationTable);
            return;
        }

        String targetMatchType = target.getRelationMatchType();
        String sourceMatchType = source.getRelationMatchType();

        // CURRENT_USER 和 CURRENT_DEPT 类型无需合并值，保持原样
        if ("CURRENT_USER".equals(targetMatchType) || "CURRENT_DEPT".equals(targetMatchType)) {
            return;
        }

        // 对于 *_LIST 类型，合并匹配值
        if (isListMatchType(targetMatchType) && isListMatchType(sourceMatchType)) {
            // 如果匹配类型相同，直接合并值
            if (targetMatchType.equals(sourceMatchType)) {
                Set<Integer> mergedValues = new HashSet<>();
                if (target.getRelationMatchValues() != null) {
                    mergedValues.addAll(target.getRelationMatchValues());
                }
                if (source.getRelationMatchValues() != null) {
                    mergedValues.addAll(source.getRelationMatchValues());
                }
                target.setRelationMatchValues(new ArrayList<>(mergedValues));
            } else {
                // 匹配类型不同但都是列表类型，合并值（语义上可能有差异，但取并集更宽松）
                Set<Integer> mergedValues = new HashSet<>();
                if (target.getRelationMatchValues() != null) {
                    mergedValues.addAll(target.getRelationMatchValues());
                }
                if (source.getRelationMatchValues() != null) {
                    mergedValues.addAll(source.getRelationMatchValues());
                }
                target.setRelationMatchValues(new ArrayList<>(mergedValues));
                logDifferentMatchTypes(targetMatchType, sourceMatchType);
            }
        }
    }

    /**
     * 判断是否为列表类型的匹配类型
     *
     * @param matchType 匹配类型
     * @return true=列表类型
     */
    private boolean isListMatchType(String matchType) {
        return "USER_LIST".equals(matchType) || "DEPT_LIST".equals(matchType) || "ROLE_LIST".equals(matchType);
    }

    /**
     * 记录不同关联表的日志
     */
    private void logDifferentRelationTables(String targetTable, String sourceTable) {
        log.debug("[RuleMerge] 关联表不同，保留第一个规则 | target: {}, source: {}", targetTable, sourceTable);
    }

    /**
     * 记录不同匹配类型的日志
     */
    private void logDifferentMatchTypes(String targetType, String sourceType) {
        log.debug("[RuleMerge] 匹配类型不同，已合并值 | target: {}, source: {}", targetType, sourceType);
    }

    /**
     * 将相对时间转换为天数
     *
     * @param value 相对值
     * @param unit  单位（DAY/WEEK/MONTH/YEAR）
     * @return 天数
     */
    private int convertToDays(int value, String unit) {
        if (unit == null) {
            return value;
        }

        return switch (unit.toUpperCase()) {
            case "DAY" -> value;
            case "WEEK" -> value * 7;
            case "MONTH" -> value * 30;
            case "YEAR" -> value * 365;
            default -> value;
        };
    }

    /**
     * 解析日期时间字符串
     *
     * @param dateTimeStr 日期时间字符串
     * @return LocalDateTime 对象，解析失败返回null
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (StrUtil.isBlank(dateTimeStr)) {
            return null;
        }

        try {
            return LocalDateTime.parse(dateTimeStr, DATE_FORMATTER);
        } catch (Exception e) {
            // 尝试只解析日期
            try {
                return LocalDateTime.parse(dateTimeStr + " 00:00:00", DATE_FORMATTER);
            } catch (Exception ex) {
                return null;
            }
        }
    }
}