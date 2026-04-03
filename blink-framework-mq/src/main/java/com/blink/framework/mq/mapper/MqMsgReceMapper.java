package com.blink.framework.mq.mapper;

import com.blink.framework.mq.entity.MqMsgReceDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 消息消费记录表 Mapper 接口
 * </p>
 *
 * @author binblink
 * @since 2023-11-13
 */
@Mapper
public interface MqMsgReceMapper extends BaseMapper<MqMsgReceDO> {

}
