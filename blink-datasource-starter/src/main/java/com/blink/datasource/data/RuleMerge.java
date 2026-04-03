package com.blink.datasource.data;

import java.util.List;

/**
 * @author binblink
 */
public interface RuleMerge {

    /**
     * 合并同一类型的多个规则配置
     * 多角色对同一实体有多个相同类型的规则时，取并集（最宽松）
     *
     * @param sameTypeRules 同类型规则列表
     * @return 合并后的规则配置
     */
     RuleConfig merge(List<RuleConfig> sameTypeRules);


}
