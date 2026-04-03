package com.blink.datasource.constants;

/**
 * @author binblink
 */
public interface DataSourceConstant {

    //超级管理员标志 0-否 1-是
    Integer SUPER_ADMIN_NO = 0;

    Integer SUPER_ADMIN_YES = 1;



    // ==================== 数据范围权限错误码 ====================

    /**
     * 数据范围规则不存在
     */
    String DATA_SCOPE_RULE_NOT_FOUND = "BUSS0050";

    /**
     * 数据范围规则配置无效
     */
    String DATA_SCOPE_RULE_CONFIG_INVALID = "BUSS0051";

    /**
     * 数据范围SQL片段无效（包含非法字符）
     */
    String DATA_SCOPE_SQL_FRAGMENT_INVALID = "BUSS0052";

    /**
     * 实体类未注册数据范围映射
     */
    String DATA_SCOPE_ENTITY_NOT_REGISTERED = "BUSS0053";


    /***************************错误码---end ********************************/
}
