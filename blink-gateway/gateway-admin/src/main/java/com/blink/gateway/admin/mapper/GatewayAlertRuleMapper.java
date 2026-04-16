package com.blink.gateway.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.gateway.admin.entity.GatewayAlertRuleDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 网关告警规则 Mapper
 *
 * @author binblink
 * @since 2026-04-15
 */
@Mapper
public interface GatewayAlertRuleMapper extends BaseMapper<GatewayAlertRuleDO> {

    /**
     * 查询所有启用的规则
     *
     * @return 启用的规则列表
     */
    @Select("SELECT * FROM gateway_alert_rule WHERE enabled = 1")
    List<GatewayAlertRuleDO> selectEnabledRules();

    /**
     * 根据规则类型查询规则
     *
     * @param ruleType 规则类型
     * @return 规则列表
     */
    @Select("SELECT * FROM gateway_alert_rule WHERE rule_type = #{ruleType} AND enabled = 1")
    List<GatewayAlertRuleDO> selectByRuleType(@Param("ruleType") String ruleType);
}