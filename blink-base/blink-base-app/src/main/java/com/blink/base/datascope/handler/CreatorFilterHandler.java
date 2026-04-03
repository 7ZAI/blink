package com.blink.base.datascope.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.blink.base.mapper.SysUserRoleRelaMapper;
import com.blink.base.datascope.constants.DataScopeRuleType;
import com.blink.datasource.data.DataScopeParseResult;
import com.blink.datasource.data.RuleConfig;
import com.blink.datasource.handler.RuleHandler;
import com.blink.datasource.utils.DataScopeSqlUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;

/**
 * 用户过滤规则处理器
 * 根据用户字段过滤数据，支持用户ID和登入名两种匹配方式
 *
 * @author binblink
 */
@Slf4j
public class CreatorFilterHandler implements RuleHandler {

    private final SysUserRoleRelaMapper sysUserRoleRelaMapper;

    /**
     * 登入名字段名集合
     */
    private static final Set<String> LOGIN_NAME_FIELDS = Set.of(
            "create_by", "update_by", "creator", "updater"
    );

    /**
     * 构造函数
     *
     * @param sysUserRoleRelaMapper 用户角色关系Mapper
     */
    public CreatorFilterHandler(SysUserRoleRelaMapper sysUserRoleRelaMapper) {
        this.sysUserRoleRelaMapper = sysUserRoleRelaMapper;
    }

    @Override
    public void apply(StringBuilder sql, RuleConfig config, DataScopeParseResult context) {
        String field = config.getField();
        String matchType = config.getMatchType();

        if (StrUtil.isBlank(field) || StrUtil.isBlank(matchType)) {
            return;
        }

        // 构建字段名（带别名）
        String fieldName = buildFieldName(field, context.getTableAlias());

        // 判断是否是登入名字段
        boolean isLoginNameField = isLoginNameField(field);

        // 根据匹配类型构建条件
        String condition = buildMatchCondition(fieldName, matchType, config, context, isLoginNameField);
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
     * 判断是否是登入名字段
     *
     * @param field 字段名
     * @return true=登入名字段
     */
    private boolean isLoginNameField(String field) {
        return LOGIN_NAME_FIELDS.contains(field.toLowerCase());
    }

    /**
     * 构建匹配条件
     *
     * @param fieldName         字段名
     * @param matchType         匹配类型
     * @param config            规则配置
     * @param context           上下文
     * @param isLoginNameField  是否是登入名字段
     * @return SQL条件
     */
    private String buildMatchCondition(String fieldName, String matchType, RuleConfig config,
                                        DataScopeParseResult context, boolean isLoginNameField) {
        switch (matchType) {
            case "CURRENT_USER":
                if (isLoginNameField) {
                    // 登入名字段：使用 loginName
                    String loginName = context.getUserInfo().getLoginName();
                    if (StrUtil.isBlank(loginName)) {
                        return null;
                    }
                    return fieldName + " = '" + escapeSql(loginName) + "'";
                } else {
                    // 用户ID字段：使用 userId
                    return fieldName + " = " + context.getUserInfo().getUserId();
                }

            case "USER_LIST":
                if (isLoginNameField) {
                    // 登入名字段：使用 loginNames
                    List<String> loginNames = config.getLoginNames();
                    if (CollUtil.isEmpty(loginNames)) {
                        return null;
                    }
                    return fieldName + " IN " + buildStringInClause(loginNames);
                } else {
                    // 用户ID字段：使用 userIds
                    List<Integer> userIds = config.getUserIds();
                    if (CollUtil.isEmpty(userIds)) {
                        return null;
                    }
                    return fieldName + " IN " + buildInClause(userIds);
                }

            case "ROLE_USER":
                List<Integer> roleIds = config.getRoleIds();
                if (CollUtil.isEmpty(roleIds)) {
                    return null;
                }
                if (isLoginNameField) {
                    // 登入名字段：查询角色下的用户登入名
                    List<String> loginNamesByRole = sysUserRoleRelaMapper.selectLoginNamesByRoleIds(roleIds);
                    if (CollUtil.isEmpty(loginNamesByRole)) {
                        log.warn("[CreatorFilter] 角色下未找到用户 | roleIds: {}", roleIds);
                        return null;
                    }
                    return fieldName + " IN " + buildStringInClause(loginNamesByRole);
                } else {
                    // 用户ID字段：查询角色下的用户ID
                    List<Integer> userIdsByRole = sysUserRoleRelaMapper.selectUserIdsByRoleIds(roleIds);
                    if (CollUtil.isEmpty(userIdsByRole)) {
                        log.warn("[CreatorFilter] 角色下未找到用户 | roleIds: {}", roleIds);
                        return null;
                    }
                    return fieldName + " IN " + buildInClause(userIdsByRole);
                }

            default:
                return null;
        }
    }

    /**
     * 构建IN子句（数字类型）
     *
     * @param ids ID列表
     * @return IN子句字符串
     */
    private String buildInClause(List<Integer> ids) {
        if (CollUtil.isEmpty(ids)) {
            return "()";
        }

        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(ids.get(i));
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * 构建IN子句（字符串类型）
     *
     * @param values 字符串列表
     * @return IN子句字符串
     */
    private String buildStringInClause(List<String> values) {
        if (CollUtil.isEmpty(values)) {
            return "()";
        }

        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("'").append(escapeSql(values.get(i))).append("'");
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * SQL字符串转义，防止SQL注入
     *
     * @param value 原始字符串
     * @return 转义后的字符串
     */
    private String escapeSql(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", "''");
    }

    @Override
    public String getRuleType() {
        return DataScopeRuleType.CREATOR_FILTER.name();
    }
}