package com.blink.datasource.utils;

import com.blink.datasource.constants.DataSourceConstant;
import com.blink.framework.common.exception.BlinkException;

import java.util.regex.Pattern;

/**
 * 自定义SQL片段验证器
 * 防止SQL注入
 *
 * @author binblink
 */
public class CustomSqlValidator {

    private CustomSqlValidator() {
    }

    /**
     * 禁止的关键字和符号模式
     * 包括：语句分隔符、DDL/DML操作、UNION注入、存储过程、时间盲注等
     */
    private static final Pattern FORBIDDEN_PATTERN = Pattern.compile(
            ";|\\bSELECT\\b|\\bINSERT\\b|\\bUPDATE\\b|\\bDELETE\\b|\\bDROP\\b|\\bTRUNCATE\\b|" +
            "\\bUNION\\b|\\bEXEC\\b|\\bEXECUTE\\b|\\bWAITFOR\\b|\\bBENCHMARK\\b|\\bSLEEP\\b|" +
            "--|/\\*|\\*/|xp_|sp_|\\bINTO\\b|\\bOUTFILE\\b|\\bLOAD_FILE\\b|\\bINFORMATION_SCHEMA\\b",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 允许的安全字符模式（白名单）
     * 只允许：字母、数字、下划线、空格、比较运算符、逻辑运算符、括号、引号、算术运算符
     */
    private static final Pattern SAFE_PATTERN = Pattern.compile(
            "^[\\w\\s.=<>!(),'\"+_\\-*/%#{}]+$",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 验证SQL片段是否安全
     * 采用白名单+黑名单双重验证
     *
     * @param sqlFragment SQL片段
     * @throws BlinkException 包含非法字符时抛出业务异常
     */
    public static void validate(String sqlFragment) {
        if (sqlFragment == null || sqlFragment.trim().isEmpty()) {
            return;
        }

        // 1. 白名单校验：只允许安全字符
        if (!SAFE_PATTERN.matcher(sqlFragment).matches()) {
            BlinkException.throwBusinessException(DataSourceConstant.DATA_SCOPE_SQL_FRAGMENT_INVALID);
        }

        // 2. 黑名单校验：检查禁止的关键字
        if (FORBIDDEN_PATTERN.matcher(sqlFragment).find()) {
            BlinkException.throwBusinessException(DataSourceConstant.DATA_SCOPE_SQL_FRAGMENT_INVALID);
        }
    }
}