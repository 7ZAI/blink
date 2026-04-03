package com.blink.gateway.admin.mapper;

import com.blink.gateway.admin.entity.RedisMqDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;

/**
 * redis stream消息发送记录表 Mapper 接口
 *
 * @author binblink
 * @since 2025-11-05
 */
@Mapper
public interface RedisMqMapper extends BaseMapper<RedisMqDO> {


}