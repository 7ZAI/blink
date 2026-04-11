package com.blink.gateway.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.gateway.admin.entity.GaRoutePushLogDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 路由推送记录 Mapper
 *
 * @author binblink
 * @since 2026-04-11
 */
@Mapper
public interface GaRoutePushLogMapper extends BaseMapper<GaRoutePushLogDO> {
}