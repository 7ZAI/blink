package com.blink.gateway.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.gateway.admin.entity.GatewayMetricsHistoryDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 网关指标历史记录 Mapper 接口
 *
 * @author binblink
 */
@Mapper
public interface GatewayMetricsHistoryMapper extends BaseMapper<GatewayMetricsHistoryDO> {

    /**
     * 删除指定天数前的历史数据
     *
     * @param beforeTime 截止时间
     * @return 删除记录数
     */
    @Delete("DELETE FROM gateway_metrics_history WHERE collect_time < #{beforeTime}")
    int deleteBeforeTime(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 查询指定实例的历史指标
     *
     * @param instanceId 实例ID
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @return 历史指标列表
     */
    @Select("SELECT * FROM gateway_metrics_history WHERE instance_id = #{instanceId} " +
            "AND collect_time BETWEEN #{startTime} AND #{endTime} ORDER BY collect_time ASC")
    List<GatewayMetricsHistoryDO> selectByInstanceIdAndTimeRange(
            @Param("instanceId") String instanceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}