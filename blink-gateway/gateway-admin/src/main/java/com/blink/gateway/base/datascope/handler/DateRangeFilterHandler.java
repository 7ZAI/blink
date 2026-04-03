package com.blink.gateway.base.datascope.handler;

import cn.hutool.core.util.StrUtil;
import com.blink.gateway.base.datascope.constants.DataScopeRuleType;
import com.blink.datasource.data.DataScopeParseResult;
import com.blink.datasource.data.RuleConfig;
import com.blink.datasource.handler.RuleHandler;
import com.blink.datasource.utils.DataScopeSqlUtil;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 时间范围过滤规则处理器
 * 根据时间字段过滤数据
 *
 * @author binblink
 */
@Slf4j
public class DateRangeFilterHandler implements RuleHandler {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void apply(StringBuilder sql, RuleConfig config, DataScopeParseResult context) {
        String field = config.getField();
        String rangeType = config.getRangeType();

        if (StrUtil.isBlank(field) || StrUtil.isBlank(rangeType)) {
            return;
        }

        // 构建字段名（带别名）
        String fieldName = buildFieldName(field, context.getTableAlias());

        // 根据范围类型构建条件
        String condition = buildRangeCondition(fieldName, config);
        if (StrUtil.isNotBlank(condition)) {
            DataScopeSqlUtil.appendWhereCondition(sql, condition);
        }
    }

    /**
     * 构建字段名（带别名）
     *
     * @param field      字段名
     * @param tableAlias 表别名
     * @return 完整字段名
     */
    private String buildFieldName(String field, String tableAlias) {
        if (StrUtil.isNotBlank(tableAlias)) {
            return tableAlias + "." + field;
        }
        return field;
    }

    /**
     * 构建时间范围条件
     *
     * @param fieldName 字段名
     * @param config    规则配置
     * @return SQL条件
     */
    private String buildRangeCondition(String fieldName, RuleConfig config) {
        String rangeType = config.getRangeType();

        if ("RELATIVE".equals(rangeType)) {
            return buildRelativeCondition(fieldName, config);
        } else if ("ABSOLUTE".equals(rangeType)) {
            return buildAbsoluteCondition(fieldName, config);
        }

        return null;
    }

    /**
     * 构建相对时间范围条件
     *
     * @param fieldName 字段名
     * @param config    规则配置
     * @return SQL条件
     */
    private String buildRelativeCondition(String fieldName, RuleConfig config) {
        Integer relativeValue = config.getRelativeValue();
        String relativeUnit = config.getRelativeUnit();

        if (relativeValue == null || StrUtil.isBlank(relativeUnit)) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime targetTime = calculateRelativeTime(now, relativeValue, relativeUnit);

        // relativeValue 为负数表示过去，正数表示未来
        // 过去时间范围：从 targetTime 到 now
        // 未来时间范围：从 now 到 targetTime
        if (relativeValue <= 0) {
            // 过去时间范围：targetTime <= field <= now
            return "(" + fieldName + " >= '" + targetTime.format(DATETIME_FORMATTER) + "' AND " + fieldName + " <= '" + now.format(DATETIME_FORMATTER) + "')";
        } else {
            // 未来时间范围：now <= field <= targetTime
            return "(" + fieldName + " >= '" + now.format(DATETIME_FORMATTER) + "' AND " + fieldName + " <= '" + targetTime.format(DATETIME_FORMATTER) + "')";
        }
    }

    /**
     * 构建绝对时间范围条件
     *
     * @param fieldName 字段名
     * @param config    规则配置
     * @return SQL条件
     */
    private String buildAbsoluteCondition(String fieldName, RuleConfig config) {
        String startTime = config.getStartTime();
        String endTime = config.getEndTime();

        StringBuilder condition = new StringBuilder();

        if (StrUtil.isNotBlank(startTime)) {
            condition.append(fieldName).append(" >= '").append(startTime).append("'");
        }

        if (StrUtil.isNotBlank(endTime)) {
            if (condition.length() > 0) {
                condition.append(" AND ");
            }
            condition.append(fieldName).append(" <= '").append(endTime).append("'");
        }

        String result = condition.toString();
        return StrUtil.isNotBlank(result) ? "(" + result + ")" : null;
    }

    /**
     * 计算相对时间
     *
     * @param baseTime      基准时间
     * @param relativeValue 相对值
     * @param relativeUnit  单位（DAY/MONTH/YEAR）
     * @return 计算后的时间
     */
    private LocalDateTime calculateRelativeTime(LocalDateTime baseTime, Integer relativeValue, String relativeUnit) {
        if (relativeUnit == null) {
            return baseTime;
        }

        return switch (relativeUnit.toUpperCase()) {
            case "DAY" -> baseTime.plusDays(relativeValue);
            case "WEEK" -> baseTime.plusWeeks(relativeValue);
            case "MONTH" -> baseTime.plusMonths(relativeValue);
            case "YEAR" -> baseTime.plusYears(relativeValue);
            default -> baseTime;
        };
    }

    @Override
    public String getRuleType() {
        return DataScopeRuleType.DATE_RANGE_FILTER.name();
    }
}