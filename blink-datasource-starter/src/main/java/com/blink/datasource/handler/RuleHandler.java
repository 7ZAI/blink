package com.blink.datasource.handler;


import com.blink.datasource.data.RuleConfig;
import com.blink.datasource.data.DataScopeParseResult;

/**
 * 规则处理器接口
 * 定义数据过滤规则的处理逻辑
 *
 * @author binblink
 */
public interface RuleHandler {

    /**
     * 处理规则，修改SQL
     *
     * @param sql     原SQL（StringBuilder，可直接修改）
     * @param config  规则配置
     * @param context 上下文
     */
    void apply(StringBuilder sql, RuleConfig config, DataScopeParseResult context);

    /**
     * 获取支持的规则类型
     *
     * @return 规则类型名称
     */
    String getRuleType();
}