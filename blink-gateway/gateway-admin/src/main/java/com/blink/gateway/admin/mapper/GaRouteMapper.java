package com.blink.gateway.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.gateway.admin.entity.GaRouteDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 网关路由配置 Mapper 接口
 *
 * @author binblink
 * @since 2026-04-11
 */
@Mapper
public interface GaRouteMapper extends BaseMapper<GaRouteDO> {

}