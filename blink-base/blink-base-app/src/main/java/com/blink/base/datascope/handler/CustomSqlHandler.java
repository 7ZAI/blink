package com.blink.base.datascope.handler;

import cn.hutool.core.util.StrUtil;
import com.blink.base.datascope.constants.DataScopeRuleType;
import com.blink.datasource.data.DataScopeParseResult;
import com.blink.datasource.data.RuleConfig;
import com.blink.datasource.handler.RuleHandler;
import com.blink.datasource.utils.CustomSqlValidator;
import com.blink.datasource.utils.DataScopeSqlUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 自定义SQL规则处理器
 * 追加自定义SQL片段作为WHERE条件
 *
 * @author binblink
 */
@Slf4j
public class CustomSqlHandler implements RuleHandler {

    @Override
    public void apply(StringBuilder sql, RuleConfig config, DataScopeParseResult context) {
        String sqlFragment = config.getSqlFragment();

        if (StrUtil.isBlank(sqlFragment)) {
            return;
        }

        // 验证SQL片段安全性
        CustomSqlValidator.validate(sqlFragment);

        // 替换占位符
        String processedFragment = processPlaceholders(sqlFragment, context);

        // 追加WHERE条件
        DataScopeSqlUtil.appendWhereCondition(sql, processedFragment);
    }

    /**
     * 处理SQL片段中的占位符
     * 支持的占位符：
     * - #{currentUserId} - 当前用户ID
     * - #{loginName} - 当前用户登录名
     * - #{currentDeptId} - 当前用户部门ID
     *
     * @param sqlFragment SQL片段
     * @param context     上下文
     * @return 处理后的SQL片段
     */
    private String processPlaceholders(String sqlFragment, DataScopeParseResult context) {
        String result = sqlFragment;

        // 替换当前用户ID
        Integer userId = context.getUserInfo().getUserId();
        if (userId != null) {
            result = result.replace("#{currentUserId}", String.valueOf(userId));
        }

        // 替换当前用户登录名（防止SQL注入：转义单引号）
        String loginName = context.getUserInfo().getLoginName();
        if (StrUtil.isNotBlank(loginName)) {
            String escapedLoginName = loginName.replace("'", "''");
            result = result.replace("#{loginName}", "'" + escapedLoginName + "'");
        }

        // 替换当前部门ID
        Integer deptId = context.getUserInfo().getDeptId();
        if (deptId != null) {
            result = result.replace("#{currentDeptId}", String.valueOf(deptId));
        }

        return result;
    }

    @Override
    public String getRuleType() {
        return DataScopeRuleType.CUSTOM_SQL.name();
    }
}