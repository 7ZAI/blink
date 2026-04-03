package com.blink.datasource.interceptor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import com.blink.datasource.annotation.DataScope;
import com.blink.datasource.component.DataScopeEntityScanner;
import com.blink.datasource.constants.DataSourceConstant;
import com.blink.datasource.data.DataScopeParseResult;
import com.blink.datasource.data.RuleConfig;
import com.blink.datasource.data.RuleMerge;
import com.blink.datasource.data.UserDataScopeInfo;
import com.blink.datasource.handler.RuleHandler;
import com.blink.datasource.utils.DataScopeSqlUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.*;
import java.util.function.Supplier;

/**
 * 数据范围权限拦截器
 * 拦截MyBatis查询，自动应用数据过滤规则
 *
 * @author binblink
 */
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
@Slf4j
public class DataScopeInterceptor implements Interceptor {


    private final List<RuleHandler> ruleHandlers;

    private final Supplier<UserDataScopeInfo> userInfoSupplier;

    private final RuleMerge ruleMerge;

    public DataScopeInterceptor(List<RuleHandler> ruleHandlers, Supplier<UserDataScopeInfo> userInfoSupplier, RuleMerge ruleMerge) {
        this.ruleHandlers = ruleHandlers;
        this.userInfoSupplier = userInfoSupplier;
        this.ruleMerge = ruleMerge;
    }

    /**
     * 拦截SQL执行，应用数据过滤规则
     *
     * @param invocation 调用信息
     * @return 执行结果
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {

//        PageHelper.clearPage();
        UserDataScopeInfo userInfo = userInfoSupplier.get();

        // 解析并判断 是否要拦截
        DataScopeParseResult dsParseResult = shouldBeIntercepted(userInfo, invocation);

        if (dsParseResult == null) {
            return invocation.proceed();
        }

        StringBuilder sqlBuilder = new StringBuilder(dsParseResult.getOriginalSql());
        // 获取并合并过滤规则
        List<RuleConfig> mergedRules = getMergedRules(userInfo.getRuleConfigs(), dsParseResult.getEntityClass().getName());

        // 依次执行规则处理器修改SQL
        for (RuleConfig rule : mergedRules) {
            RuleHandler handler = getHandler(rule.getRuleType());
            if (handler != null) {
                log.debug("DataScopeInterceptor应用规则: {}", rule.getRuleType());
                handler.apply(sqlBuilder, rule, dsParseResult);
            }
        }

        // 替换原SQL
        if (!sqlBuilder.toString().equals(dsParseResult.getOriginalSql())) {
            ReflectUtil.setFieldValue(dsParseResult.getBoundSql(), "sql", sqlBuilder.toString());
            log.debug("DataScopeInterceptor修改SQL: {}", sqlBuilder);
        } else {
            log.debug("DataScopeInterceptor未修改SQL");
        }

        return invocation.proceed();
    }

    /**
     * 获取指定规则类型的处理器
     *
     * @param ruleType 规则类型
     * @return 处理器，未找到返回null
     */
    private RuleHandler getHandler(String ruleType) {
        if (ruleHandlers == null || ruleType == null) {
            return null;
        }

        RuleHandler handler = ruleHandlers.stream()
                .filter(h -> h.getRuleType() != null && h.getRuleType().equals(ruleType))
                .findFirst()
                .orElse(null);

        if (handler == null) {
            log.warn("未找到规则类型 [{}] 对应的处理器，请检查是否已实现该处理器", ruleType);
        }

        return handler;
    }

    /**
     * 是否应该执行数据过滤
     *
     * @return DataScopeParseResult 需要拦截时返回解析结果，不需要拦截返回 null
     */
    private DataScopeParseResult shouldBeIntercepted(UserDataScopeInfo userInfo, Invocation invocation) {

        //1. 获取登入用户信息 检查数据过滤权限
        if (ObjectUtil.isNull(userInfo)) {
            log.debug("DataScopeInterceptor跳过: 无用户上下文");
            return null;
        }
        // 用户没有配置过滤权限
        if (CollUtil.isEmpty(userInfo.getRuleConfigs())) {
            return null;
        }

        // 用户：超管跳过所有过滤
        if (DataSourceConstant.SUPER_ADMIN_YES.equals(userInfo.getSuperFlag())) {
            log.debug("DataScopeInterceptor跳过: 超级管理员");
            return null;
        }

        // 获取 StatementHandler 和 MappedStatement 信息
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");

        // 只对 SELECT 语句应用数据过滤
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
        if (!SqlCommandType.SELECT.equals(sqlCommandType)) {
            log.debug("DataScopeInterceptor跳过: 非SELECT语句 [{}]", sqlCommandType);
            return null;
        }

        //  获取原始SQL
        BoundSql boundSql = statementHandler.getBoundSql();
        String originalSql = boundSql.getSql();

        log.debug("DataScopeInterceptor拦截SQL: {}", originalSql);

        //  解析SQL获取表名
        Set<String> tableNames = DataScopeSqlUtil.extractTableNames(originalSql);


        // 获取Mapper方法上的@DataScope注解
        DataScope dataScope = getDataScopeAnnotation(mappedStatement);
        if (dataScope != null && !dataScope.enabled()) {
            log.debug("DataScopeInterceptor跳过: 注解disabled");
            return null;
        }
        Class<?> entityClass = null;
        // 根据表数量决定处理策略
        String tableAlias = null;
        // 多表查询 必须指定别名
        if (tableNames.size() > 1) {
            // JOIN查询：必须有@DataScope注解指定实体和别名
            if (dataScope == null || dataScope.entity() == Void.class) {
                log.debug("DataScopeInterceptor跳过: JOIN查询未指定@DataScope注解");
                return null;
            }
            entityClass = dataScope.entity();
            tableAlias = dataScope.tableAlias();
            log.debug("DataScopeInterceptor处理JOIN查询: entity={}, alias={}", entityClass.getName(), tableAlias);
        } else if (tableNames.size() == 1) {
            // 单表查询：自动识别
            String tableName = tableNames.iterator().next();

            entityClass = DataScopeEntityScanner.getEntityClass(tableName);

            log.debug("DataScopeInterceptor处理单表查询: table={}, entity={}", tableName, entityClass);
        } else {
            // 无表名（异常情况），跳过
            log.debug("DataScopeInterceptor跳过: 无法提取表名");
            return null;
        }

        //表名无对应实体
        if (ObjectUtil.isNull(entityClass)) {
            log.debug("DataScopeInterceptor跳过: 实体类未注册 {}", tableNames);
            return null;
        }
        //  检查实体类是否标记了 @DataScopeEntity 注解 //实体类未注册为过滤实体，跳过过滤
        if (!DataScopeEntityScanner.isDataScopeEntity(entityClass)) {
            log.debug("DataScopeInterceptor跳过: 实体类未标记@DataScopeEntity {}", entityClass.getName());
            return null;
        }

        return new DataScopeParseResult(userInfo, tableAlias, entityClass, boundSql);
    }


    private List<RuleConfig> getMergedRules(List<RuleConfig> roleRules, String entityClass) {

        // 先按实体类过滤规则
        List<RuleConfig> filteredRules = new ArrayList<>();
        for (RuleConfig rule : roleRules) {
            if (entityClass.equals(rule.getEntityClass())) {
                filteredRules.add(rule);
            }
        }

        if (filteredRules.isEmpty()) {
            log.debug("DataScopeInterceptor: 未找到实体类 [{}] 对应的过滤规则", entityClass);
            return Collections.emptyList();
        }

        // 按类型分组
        Map<String, List<RuleConfig>> rulesByType = new HashMap<>();

        for (RuleConfig rule : filteredRules) {
            String ruleType = rule.getRuleType();
            rulesByType.computeIfAbsent(ruleType, k -> new ArrayList<>()).add(rule);
        }

        if (rulesByType.isEmpty()) {
            return Collections.emptyList();
        }

        // 按类型合并规则
        List<RuleConfig> mergedRules = new ArrayList<>();
        for (Map.Entry<String, List<RuleConfig>> entry : rulesByType.entrySet()) {
            List<RuleConfig> sameTypeRules = entry.getValue();
            if (sameTypeRules.size() == 1) {
                // 只有一个规则，直接使用
                mergedRules.add(sameTypeRules.get(0));
            } else {
                // 多个规则，合并
                RuleConfig merged = ruleMerge.merge(sameTypeRules);
                if (merged != null) {
                    mergedRules.add(merged);
                }
            }
        }

        return mergedRules;
    }

    /**
     * 获取Mapper方法上的@DataScope注解
     *
     * @param mappedStatement MappedStatement
     * @return DataScope注解，未找到返回null
     */
    private DataScope getDataScopeAnnotation(MappedStatement mappedStatement) {
        try {
            // 获取Mapper类名和方法名
            String id = mappedStatement.getId();
            String className = id.substring(0, id.lastIndexOf("."));
            String methodName = id.substring(id.lastIndexOf(".") + 1);

            // 通过反射获取方法上的注解
            Class<?> mapperClass = Class.forName(className);
            for (Method method : mapperClass.getMethods()) {
                if (method.getName().equals(methodName)) {
                    return method.getAnnotation(DataScope.class);
                }
            }
        } catch (Exception e) {
            log.debug("获取@DataScope注解失败: {}", e.getMessage());
        }
        return null;
    }
}