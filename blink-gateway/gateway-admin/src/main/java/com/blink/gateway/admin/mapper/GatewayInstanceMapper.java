package com.blink.gateway.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.gateway.admin.entity.GatewayInstanceDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 网关实例 Mapper 接口
 *
 * @author binblink
 */
@Mapper
public interface GatewayInstanceMapper extends BaseMapper<GatewayInstanceDO> {

}
