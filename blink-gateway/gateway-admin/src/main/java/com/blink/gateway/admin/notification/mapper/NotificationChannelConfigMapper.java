package com.blink.gateway.admin.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.gateway.admin.notification.entity.NotificationChannelConfigDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 通知渠道配置Mapper
 *
 * @author binblink
 * @since 2026-04-28
 */
@Mapper
public interface NotificationChannelConfigMapper extends BaseMapper<NotificationChannelConfigDO> {

    /**
     * 根据渠道类型查询配置
     */
    @Select("SELECT * FROM sys_notification_channel_config WHERE channel_type = #{channelType}")
    NotificationChannelConfigDO selectByChannelType(String channelType);
}
