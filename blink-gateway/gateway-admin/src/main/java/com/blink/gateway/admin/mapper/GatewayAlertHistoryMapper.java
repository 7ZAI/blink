package com.blink.gateway.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.gateway.admin.entity.GatewayAlertHistoryDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 网关告警历史 Mapper
 *
 * @author binblink
 * @since 2026-04-15
 */
@Mapper
public interface GatewayAlertHistoryMapper extends BaseMapper<GatewayAlertHistoryDO> {

    /**
     * 查询指定规则当前触发中的告警
     *
     * @param ruleId 规则 ID
     * @return 触发中的告警
     */
    @Select("SELECT * FROM gateway_alert_history WHERE rule_id = #{ruleId} AND status = 'FIRING' ORDER BY fired_time DESC LIMIT 1")
    GatewayAlertHistoryDO selectFiringByRuleId(@Param("ruleId") Long ruleId);

    /**
     * 查询所有触发中的告警
     *
     * @return 触发中的告警列表
     */
    @Select("SELECT * FROM gateway_alert_history WHERE status = 'FIRING' ORDER BY fired_time DESC")
    List<GatewayAlertHistoryDO> selectFiringAlerts();

    /**
     * 更新告警状态为已确认
     *
     * @param id 告警 ID
     * @param acknowledgedBy 确认人 ID
     * @return 更新行数
     */
    @Update("UPDATE gateway_alert_history SET status = 'ACKNOWLEDGED', acknowledged_time = NOW(), acknowledged_by = #{acknowledgedBy} WHERE id = #{id}")
    int acknowledge(@Param("id") Long id, @Param("acknowledgedBy") Integer acknowledgedBy);

    /**
     * 更新告警状态为已恢复
     *
     * @param id 告警 ID
     * @return 更新行数
     */
    @Update("UPDATE gateway_alert_history SET status = 'RESOLVED', resolved_time = NOW() WHERE id = #{id}")
    int resolve(@Param("id") Long id);

    /**
     * 查询指定时间范围内的告警历史
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param status 状态筛选 (可选)
     * @return 告警历史列表
     */
    @Select("<script>" +
            "SELECT * FROM gateway_alert_history " +
            "WHERE fired_time BETWEEN #{startTime} AND #{endTime} " +
            "<if test='status != null and status != \"\"'>" +
            "AND status = #{status} " +
            "</if>" +
            "ORDER BY fired_time DESC" +
            "</script>")
    List<GatewayAlertHistoryDO> selectByTimeRangeAndStatus(
            @Param("startTime") String startTime,
            @Param("endTime") String endTime,
            @Param("status") String status);
}