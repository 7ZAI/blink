package com.blink.datasource.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.util.TablesNamesFinder;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据范围SQL处理工具类
 * 提供SQL解析、字段过滤、条件追加等通用方法
 *
 * @author binblink
 */
@Slf4j
public class DataScopeSqlUtil {

    private DataScopeSqlUtil() {
    }

    /**
     * 从SQL中提取表名集合
     *
     * @param sql SQL语句
     * @return 表名集合
     */
    public static Set<String> extractTableNames(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
            List<String> tableList = tablesNamesFinder.getTableList(statement);
            return new HashSet<>(tableList);
        } catch (JSQLParserException e) {
            log.warn("SQL解析失败: {}", e.getMessage());
        }
        return new HashSet<>();
    }

    /**
     * 提取SELECT部分
     *
     * @param sql 完整SQL
     * @return SELECT字段部分字符串
     */
    public static String extractSelectPart(String sql) {
        try {
            Select select = (Select) CCJSqlParserUtil.parse(sql);
            SelectBody selectBody = select.getSelectBody();
            if (selectBody instanceof PlainSelect) {
                PlainSelect plainSelect = (PlainSelect) selectBody;
                return plainSelect.getSelectItems().stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(", "));
            }
            return "*";
        } catch (JSQLParserException e) {
            log.warn("提取SELECT部分失败: {}", e.getMessage());
            return "*";
        }
    }

    /**
     * 提取FROM及之后部分
     *
     * @param sql 完整SQL
     * @return FROM及之后的字符串
     */
    public static String extractFromPart(String sql) {
        int fromIndex = sql.toUpperCase().indexOf(" FROM ");
        if (fromIndex > 0) {
            return sql.substring(fromIndex);
        }
        return "";
    }

    /**
     * 过滤SELECT字段，排除指定字段
     *
     * @param selectPart    SELECT部分字符串
     * @param excludeFields 需要排除的字段列表
     * @param tableAlias    表别名（可为空）
     * @return 过滤后的SELECT字段字符串
     */
    public static String filterFields(String selectPart, List<String> excludeFields, String tableAlias) {
        if (CollUtil.isEmpty(excludeFields)) {
            return selectPart;
        }

        List<String> originalFields = parseSelectFields(selectPart);
        List<String> filteredFields = new ArrayList<>();

        for (String field : originalFields) {
            String fieldName = extractFieldName(field);
            String fullName = StrUtil.isNotBlank(tableAlias)
                    ? tableAlias + "." + fieldName
                    : fieldName;

            if (!excludeFields.contains(fieldName) && !excludeFields.contains(fullName)) {
                filteredFields.add(field);
            }
        }

        return String.join(", ", filteredFields);
    }

    /**
     * 解析SELECT字段列表
     *
     * @param selectPart SELECT部分字符串
     * @return 字段列表
     */
    public static List<String> parseSelectFields(String selectPart) {
        if (StrUtil.isBlank(selectPart) || "*".equals(selectPart.trim())) {
            return Collections.singletonList("*");
        }

        return Arrays.stream(selectPart.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    /**
     * 从字段表达式中提取字段名
     *
     * @param fieldExpression 字段表达式（如 "u.user_id", "user_id AS id", "COUNT(*)"）
     * @return 字段名
     */
    public static String extractFieldName(String fieldExpression) {
        String trimmed = fieldExpression.trim();

        // 处理别名：user_id AS id -> user_id
        if (trimmed.toUpperCase().contains(" AS ")) {
            trimmed = trimmed.split("(?i) AS ")[0].trim();
        }

        // 处理表别名：u.user_id -> user_id
        if (trimmed.contains(".")) {
            trimmed = trimmed.substring(trimmed.lastIndexOf(".") + 1);
        }

        return trimmed;
    }

    /**
     * 追加WHERE条件到SQL中
     *
     * @param sql       SQL字符串
     * @param condition 条件表达式
     */
    public static void appendWhereCondition(StringBuilder sql, String condition) {
        String upperSql = sql.toString().toUpperCase();

        if (upperSql.contains(" WHERE ")) {
            // 已有WHERE，追加AND
            int insertIndex = findWhereInsertPosition(sql);
            sql.insert(insertIndex, " AND " + condition);
        } else {
            // 无WHERE，添加WHERE
            int insertIndex = findFromEndPosition(sql);
            sql.insert(insertIndex, " WHERE " + condition);
        }
    }

    /**
     * 查找WHERE条件插入位置
     *
     * @param sql SQL字符串
     * @return 插入位置
     */
    private static int findWhereInsertPosition(StringBuilder sql) {
        String upperSql = sql.toString().toUpperCase();
        int groupByIndex = upperSql.indexOf(" GROUP BY ");
        int havingIndex = upperSql.indexOf(" HAVING ");
        int orderByIndex = upperSql.indexOf(" ORDER BY ");
        int limitIndex = upperSql.indexOf(" LIMIT ");

        int minIndex = sql.length();
        if (groupByIndex > 0) {
            minIndex = Math.min(minIndex, groupByIndex);
        }
        if (havingIndex > 0) {
            minIndex = Math.min(minIndex, havingIndex);
        }
        if (orderByIndex > 0) {
            minIndex = Math.min(minIndex, orderByIndex);
        }
        if (limitIndex > 0) {
            minIndex = Math.min(minIndex, limitIndex);
        }

        return minIndex;
    }

    /**
     * 查找FROM子句结束位置
     *
     * @param sql SQL字符串
     * @return 结束位置
     */
    private static int findFromEndPosition(StringBuilder sql) {
        String upperSql = sql.toString().toUpperCase();
        int groupByIndex = upperSql.indexOf(" GROUP BY ");
        int orderByIndex = upperSql.indexOf(" ORDER BY ");
        int limitIndex = upperSql.indexOf(" LIMIT ");

        int minIndex = sql.length();
        if (groupByIndex > 0) {
            minIndex = Math.min(minIndex, groupByIndex);
        }
        if (orderByIndex > 0) {
            minIndex = Math.min(minIndex, orderByIndex);
        }
        if (limitIndex > 0) {
            minIndex = Math.min(minIndex, limitIndex);
        }

        return minIndex;
    }
}