package com.blink.gateway.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.gateway.admin.entity.GatewayTrafficHistoryDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 网关流量历史记录 Mapper 接口
 *
 * @author binblink
 * @since 2026-04-14
 */
@Mapper
public interface GatewayTrafficHistoryMapper extends BaseMapper<GatewayTrafficHistoryDO> {

    /**
     * 删除指定天数前的历史数据
     *
     * @param beforeTime 截止时间
     * @param granularity 粒度（MINUTE/HOUR）
     * @return 删除记录数
     */
    @Delete("DELETE FROM gateway_traffic_history WHERE time_bucket < #{beforeTime} AND granularity = #{granularity}")
    int deleteBeforeTime(@Param("beforeTime") LocalDateTime beforeTime, @Param("granularity") String granularity);

    /**
     * 查询指定时间范围和粒度的流量数据
     *
     * @param startTime   开始时间
     * @param endTime     结束时间
     * @param granularity 粒度（MINUTE/HOUR）
     * @return 流量历史列表
     */
    @Select("SELECT * FROM gateway_traffic_history WHERE time_bucket BETWEEN #{startTime} AND #{endTime} " +
            "AND granularity = #{granularity} ORDER BY time_bucket ASC")
    List<GatewayTrafficHistoryDO> selectByTimeRangeAndGranularity(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("granularity") String granularity);
}