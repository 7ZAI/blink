package com.blink.base.mapper;

import com.blink.base.entity.BlinkChannelDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.base.dto.req.QueryBlinkChannelReqDTO;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 对接渠道 Mapper 接口
 * </p>
 *
 * @author binblink
 * @since 2024-07-29
 */
@Mapper
public interface BlinkChannelMapper extends BaseMapper<BlinkChannelDO> {

       List<BlinkChannelDO> findBlinkChannelList(QueryBlinkChannelReqDTO reqDTO);

       List<BlinkChannelDO> findAllChannels();
}
