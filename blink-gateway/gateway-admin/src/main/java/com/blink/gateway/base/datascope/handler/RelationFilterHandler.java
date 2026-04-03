package com.blink.gateway.base.datascope.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.blink.gateway.base.datascope.constants.DataScopeRuleType;
import com.blink.datasource.data.DataScopeParseResult;
import com.blink.datasource.data.RuleConfig;
import com.blink.datasource.handler.RuleHandler;
import com.blink.datasource.utils.DataScopeSqlUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 关联过滤规则处理器
 * 通过EXISTS子查询实现关联表过滤
 *
 * @author binblink
 */
@Slf4j
public class RelationFilterHandler implements RuleHandler {

    /**
     * SQL标识符合法性校验正则
     * 只允许字母、数字、下划线，且不能以数字开头
     */
    private static final Pattern SQL_IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    @Override
    public void apply(StringBuilder sql, RuleConfig config, DataScopeParseResult context) {
        if (!validateConfig(config)) {
            log.warn("[RelationFilter] 配置校验失败");
            return;
        }

        String matchType = config.getRelationMatchType();

        // 特殊处理：CURRENT_USER_DEPT_CHILDREN 使用直接 IN 条件
        if ("CURRENT_USER_DEPT_CHILDREN".equals(matchType)) {
            String condition = buildDirectInCondition(config, context);
            if (StrUtil.isNotBlank(condition)) {
                DataScopeSqlUtil.appendWhereCondition(sql, condition);
                log.debug("[RelationFilter] 应用部门层级过滤: {}", condition);
            }
            return;
        }

        // 标准处理：EXISTS 子查询
        String existsCondition = buildExistsCondition(config, context);
        if (StrUtil.isNotBlank(existsCondition)) {
            DataScopeSqlUtil.appendWhereCondition(sql, existsCondition);
            log.debug("[RelationFilter] 应用关联过滤: {}", existsCondition);
        }
    }

    /**
     * 构建直接 IN 条件（用于 CURRENT_USER_DEPT_CHILDREN）
     * 直接对 sourceField 进行 IN 过滤，不使用关联表
     *
     * @param config  规则配置
     * @param context 上下文
     * @return IN条件SQL片段
     */
    private String buildDirectInCondition(RuleConfig config, DataScopeParseResult context) {
        // 获取当前用户部门及其子部门ID列表
        List<Integer> deptIds = context.getUserInfo().getDeptIds();
        if (CollUtil.isEmpty(deptIds)) {
            log.warn("[RelationFilter] 当前用户部门及子部门ID列表为空");
            return null;
        }

        String sourceField = config.getSourceField();
        String tableAlias = context.getTableAlias();

        // 校验别名合法性
        if (StrUtil.isNotBlank(tableAlias) && !isValidSqlIdentifier(tableAlias)) {
            log.warn("[RelationFilter] 表别名不合法: {}", tableAlias);
            return null;
        }

        // 构建源字段引用
        String sourceFieldRef = StrUtil.isNotBlank(tableAlias)
                ? tableAlias + "." + sourceField
                : sourceField;

        return sourceFieldRef + " IN " + buildInClause(deptIds);
    }

    /**
     * 校验配置完整性和安全性
     *
     * @param config 规则配置
     * @return true=配置完整且安全
     */
    private boolean validateConfig(RuleConfig config) {
        // 必须有匹配类型
        if (StrUtil.isBlank(config.getRelationMatchType())) {
            return false;
        }

        // 特殊匹配类型 CURRENT_USER_DEPT_CHILDREN 只需要 sourceField
        if ("CURRENT_USER_DEPT_CHILDREN".equals(config.getRelationMatchType())) {
            if (StrUtil.isBlank(config.getSourceField())) {
                log.warn("[RelationFilter] sourceField为空");
                return false;
            }
            if (!isValidSqlIdentifier(config.getSourceField())) {
                log.warn("[RelationFilter] 源字段名不合法: {}", config.getSourceField());
                return false;
            }
            return true;
        }

        // 标准匹配类型需要完整的关联配置
        if (StrUtil.isBlank(config.getRelationTable())
                || StrUtil.isBlank(config.getSourceField())
                || StrUtil.isBlank(config.getRelationSourceField())
                || StrUtil.isBlank(config.getRelationTargetField())) {
            return false;
        }

        // 校验SQL标识符合法性（防止SQL注入）
        if (!isValidSqlIdentifier(config.getRelationTable())) {
            log.warn("[RelationFilter] 关联表名不合法: {}", config.getRelationTable());
            return false;
        }
        if (!isValidSqlIdentifier(config.getSourceField())) {
            log.warn("[RelationFilter] 源字段名不合法: {}", config.getSourceField());
            return false;
        }
        if (!isValidSqlIdentifier(config.getRelationSourceField())) {
            log.warn("[RelationFilter] 关联表源字段名不合法: {}", config.getRelationSourceField());
            return false;
        }
        if (!isValidSqlIdentifier(config.getRelationTargetField())) {
            log.warn("[RelationFilter] 关联表目标字段名不合法: {}", config.getRelationTargetField());
            return false;
        }

        return true;
    }

    /**
     * 校验SQL标识符合法性
     * 只允许字母、数字、下划线，防止SQL注入
     *
     * @param identifier 标识符
     * @return true=合法
     */
    private boolean isValidSqlIdentifier(String identifier) {
        if (StrUtil.isBlank(identifier)) {
            return false;
        }
        return SQL_IDENTIFIER_PATTERN.matcher(identifier).matches();
    }

    /**
     * 构建EXISTS子查询条件
     *
     * @param config  规则配置
     * @param context 上下文
     * @return EXISTS子查询条件
     */
    private String buildExistsCondition(RuleConfig config, DataScopeParseResult context) {
        String relationTable = config.getRelationTable();
        String sourceField = config.getSourceField();
        String relationSourceField = config.getRelationSourceField();

        // 主表别名（仅当上下文有别名时才使用）
        String tableAlias = context.getTableAlias();

        // 校验别名合法性（如果有别名）
        if (StrUtil.isNotBlank(tableAlias) && !isValidSqlIdentifier(tableAlias)) {
            log.warn("[RelationFilter] 表别名不合法: {}", tableAlias);
            return null;
        }

        // 构建源字段引用（有别名时使用别名前缀，无别名时直接使用字段名）
        String sourceFieldRef = StrUtil.isNotBlank(tableAlias)
                ? tableAlias + "." + sourceField
                : sourceField;

        // 获取匹配条件
        String matchCondition = buildMatchCondition(config, context);
        if (StrUtil.isBlank(matchCondition)) {
            log.warn("[RelationFilter] 无法构建匹配条件 | matchType: {}", config.getRelationMatchType());
            return null;
        }

        // 构建 EXISTS 子查询
        return String.format(
                "EXISTS (SELECT 1 FROM %s r WHERE r.%s = %s AND %s)",
                relationTable,
                relationSourceField,
                sourceFieldRef,
                matchCondition
        );
    }

    /**
     * 构建匹配条件
     *
     * @param config  规则配置
     * @param context 上下文
     * @return 匹配条件SQL片段
     */
    private String buildMatchCondition(RuleConfig config, DataScopeParseResult context) {
        String matchType = config.getRelationMatchType();
        String targetField = config.getRelationTargetField();
        List<Integer> matchValues = config.getRelationMatchValues();

        return switch (matchType) {
            case "CURRENT_USER" -> {
                Integer userId = context.getUserInfo().getUserId();
                if (userId == null) {
                    log.warn("[RelationFilter] 当前用户ID为空");
                    yield null;
                }
                yield "r." + targetField + " = " + userId;
            }
            case "CURRENT_DEPT" -> {
                Integer deptId = context.getUserInfo().getDeptId();
                if (deptId == null) {
                    log.warn("[RelationFilter] 当前用户部门ID为空");
                    yield null;
                }
                yield "r." + targetField + " = " + deptId;
            }
            case "CURRENT_DEPT_CHILDREN" -> {
                // 当前用户部门及其子部门ID列表
                List<Integer> deptIds = context.getUserInfo().getDeptIds();
                if (CollUtil.isEmpty(deptIds)) {
                    log.warn("[RelationFilter] 当前用户部门及子部门ID列表为空");
                    yield null;
                }
                yield "r." + targetField + " IN " + buildInClause(deptIds);
            }
            case "CURRENT_USER_DEPT_CHILDREN" -> {
                // 用于部门表关联用户场景：筛选有当前用户部门及子部门用户的部门
                // 关联表的targetField是user_id，需要找出这些用户所属的部门
                List<Integer> deptIds = context.getUserInfo().getDeptIds();
                if (CollUtil.isEmpty(deptIds)) {
                    log.warn("[RelationFilter] 当前用户部门及子部门ID列表为空");
                    yield null;
                }
                // 查询关联表中的user_id是否属于当前用户的部门或子部门
                yield "r." + targetField + " IN (SELECT user_id FROM sys_user WHERE group_id IN " + buildInClause(deptIds) + ")";
            }
            case "CURRENT_ROLE" -> {
                List<Integer> roleIds = context.getUserInfo().getRoleIds();
                if (CollUtil.isEmpty(roleIds)) {
                    log.warn("[RelationFilter] 当前用户角色ID列表为空");
                    yield null;
                }
                yield "r." + targetField + " IN " + buildInClause(roleIds);
            }
            case "USER_LIST", "DEPT_LIST", "ROLE_LIST" -> {
                if (CollUtil.isEmpty(matchValues)) {
                    log.warn("[RelationFilter] 匹配值列表为空 | matchType: {}", matchType);
                    yield null;
                }
                yield "r." + targetField + " IN " + buildInClause(matchValues);
            }
            default -> {
                log.warn("[RelationFilter] 未知的匹配类型: {}", matchType);
                yield null;
            }
        };
    }

    /**
     * 构建IN子句
     * 注意：空列表会返回 null，调用方需要处理
     *
     * @param values 值列表
     * @return IN子句字符串，空列表返回 null
     */
    private String buildInClause(List<Integer> values) {
        if (CollUtil.isEmpty(values)) {
            // 返回 null 而不是无效的 "()" 语法
            // 调用方 buildMatchCondition 已经检查了空列表
            return null;
        }

        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(values.get(i));
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String getRuleType() {
        return DataScopeRuleType.RELATION_FILTER.name();
    }
}