package com.blink.gateway.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.gateway.admin.entity.GaChannelDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 对接渠道 Mapper 接口
 *
 * @author binblink
 */
@Mapper
public interface GaChannelMapper extends BaseMapper<GaChannelDO> {

    /**
     * 查询所有渠道
     *
     * @return 渠道列表
     */
    List<GaChannelDO> findAllChannels();
}
