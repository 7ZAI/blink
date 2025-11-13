package com.blink.base.mapper;

import com.blink.base.entity.RedisMqDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * redis stream消息发送记录表 Mapper 接口
 * </p>
 *
 * @author binblink
 * @since 2025-11-05
 */
@Mapper
public interface RedisMqMapper extends BaseMapper<RedisMqDO> {


}
