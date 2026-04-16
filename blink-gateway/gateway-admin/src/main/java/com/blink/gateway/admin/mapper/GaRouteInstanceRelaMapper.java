package com.blink.gateway.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.gateway.admin.entity.GaRouteInstanceRelaDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 路由实例关联 Mapper 接口
 *
 * @author binblink
 * @since 2026-04-16
 */
@Mapper
public interface GaRouteInstanceRelaMapper extends BaseMapper<GaRouteInstanceRelaDO> {

    /**
     * 根据路由ID查询所有实例推送状态
     *
     * @param routeId 路由ID
     * @return 实例推送状态列表
     */
    @Select("SELECT * FROM ga_route_instance_rela WHERE route_id = #{routeId}")
    List<GaRouteInstanceRelaDO> selectByRouteId(@Param("routeId") String routeId);

    /**
     * 根据实例ID查询所有路由推送状态
     *
     * @param instanceId 实例ID
     * @return 路由推送状态列表
     */
    @Select("SELECT * FROM ga_route_instance_rela WHERE instance_id = #{instanceId}")
    List<GaRouteInstanceRelaDO> selectByInstanceId(@Param("instanceId") String instanceId);

    /**
     * 根据推送记录ID查询所有实例推送状态
     *
     * @param pushId 推送记录ID
     * @return 实例推送状态列表
     */
    @Select("SELECT * FROM ga_route_instance_rela WHERE push_id = #{pushId}")
    List<GaRouteInstanceRelaDO> selectByPushId(@Param("pushId") Long pushId);

    /**
     * 统计路由在各状态下的实例数量
     *
     * @param routeId 路由ID
     * @param pushStatus 推送状态
     * @return 实例数量
     */
    @Select("SELECT COUNT(*) FROM ga_route_instance_rela WHERE route_id = #{routeId} AND push_status = #{pushStatus}")
    int countByRouteIdAndStatus(@Param("routeId") String routeId, @Param("pushStatus") Byte pushStatus);
}
