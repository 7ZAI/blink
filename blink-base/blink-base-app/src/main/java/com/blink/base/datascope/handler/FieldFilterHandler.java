package com.blink.base.datascope.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.blink.base.datascope.constants.DataScopeRuleType;
import com.blink.datasource.data.DataScopeParseResult;
import com.blink.datasource.data.RuleConfig;
import com.blink.datasource.handler.RuleHandler;
import com.blink.datasource.utils.DataScopeSqlUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 字段过滤规则处理器
 * 从SELECT中排除指定字段
 *
 * @author binblink
 */
@Slf4j
public class FieldFilterHandler implements RuleHandler {



    @Override
    public void apply(StringBuilder sql, RuleConfig config, DataScopeParseResult context) {
        List<String> excludeFields = config.getExcludeFields();
        if (CollUtil.isEmpty(excludeFields)) {
            return;
        }

        String tableAlias = context.getTableAlias();
        String originalSql = sql.toString();
        String upperSql = originalSql.toUpperCase().trim();

        // 只处理SELECT语句
        if (!upperSql.startsWith("SELECT")) {
            return;
        }

        // 跳过COUNT查询（PageHelper生成的count查询）
        if (upperSql.contains("COUNT(")) {
            log.debug("FieldFilterHandler跳过COUNT查询: {}", originalSql);
            return;
        }

        // 提取并过滤SELECT字段
        String selectPart = DataScopeSqlUtil.extractSelectPart(originalSql);
        if (StrUtil.isBlank(selectPart) || "*".equals(selectPart.trim())) {
            return;
        }

        // 解析原始字段列表（用于比较）
        List<String> originalFields = DataScopeSqlUtil.parseSelectFields(selectPart);
        List<String> filteredFields = new ArrayList<>();

        for (String field : originalFields) {
            String fieldName = DataScopeSqlUtil.extractFieldName(field);
            String fullName = StrUtil.isNotBlank(tableAlias)
                    ? tableAlias + "." + fieldName
                    : fieldName;

            if (!excludeFields.contains(fieldName) && !excludeFields.contains(fullName)) {
                filteredFields.add(field);
            }
        }

        // 如果没有字段被过滤，不修改SQL
        if (filteredFields.size() == originalFields.size()) {
            return;
        }

        // 构建过滤后的SELECT部分
        String filteredSelect = String.join(", ", filteredFields);

        // 替换SELECT部分
        int selectEndIndex = upperSql.indexOf(" FROM ");
        if (selectEndIndex > 0) {
            sql.replace(7, selectEndIndex, " " + filteredSelect);
            log.debug("FieldFilterHandler修改SQL: {}", sql);
        }
    }



    @Override
    public String getRuleType() {
        return DataScopeRuleType.FIELD_FILTER.name();
    }
}