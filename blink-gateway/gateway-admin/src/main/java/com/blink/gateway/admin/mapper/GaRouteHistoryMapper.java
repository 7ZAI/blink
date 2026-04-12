package com.blink.gateway.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.gateway.admin.entity.GaRouteHistoryDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;

import java.time.LocalDateTime;

/**
 * 网关路由历史审计 Mapper 接口
 *
 * @author binblink
 * @since 2026-04-11
 */
@Mapper
public interface GaRouteHistoryMapper extends BaseMapper<GaRouteHistoryDO> {

    /**
     * 删除指定时间之前的历史记录
     *
     * @param threshold 时间阈值
     * @return 删除记录数
     */
    @Delete("DELETE FROM ga_route_history WHERE operate_time < #{threshold}")
    int deleteByOperateTimeBefore(@Param("threshold") LocalDateTime threshold);
}