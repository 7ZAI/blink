package com.blink.gateway.admin.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.gateway.admin.notification.entity.NotificationFailureLogDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知发送失败记录Mapper
 *
 * @author binblink
 * @since 2026-04-28
 */
@Mapper
public interface NotificationFailureLogMapper extends BaseMapper<NotificationFailureLogDO> {
}
