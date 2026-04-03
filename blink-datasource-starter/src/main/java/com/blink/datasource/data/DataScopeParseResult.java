package com.blink.datasource.data;


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.mapping.BoundSql;

import java.util.List;

/**
 * 数据权限范围 解析结果
 * 封装当前用户的上下文信息，用于规则处理
 *
 * @author binblink
 */
@Getter
@Slf4j
public class DataScopeParseResult {

    /**
     * 用户信息（从Redis获取）
     */
    private UserDataScopeInfo userInfo;

    /**
     * 表别名
     */
    private String tableAlias;

    /**
     * 实体类
     */
    private Class<?> entityClass;

    /**
     * 原始sql
     */
    private String originalSql;

    /**
     * 原始sql封装对象
     */
    private BoundSql boundSql;


    /**
     * 构造函数
     *
     * @param userInfo   用户信息
     * @param tableAlias 表别名
     */
    public DataScopeParseResult(UserDataScopeInfo userInfo, String tableAlias, Class<?> entityClass, BoundSql boundSql) {
        this.userInfo = userInfo;
        this.tableAlias = tableAlias;
        this.entityClass = entityClass;
        this.boundSql = boundSql;
        this.originalSql = boundSql.getSql();
    }

    public DataScopeParseResult() {
    }

}