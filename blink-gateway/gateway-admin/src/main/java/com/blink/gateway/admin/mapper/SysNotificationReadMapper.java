package com.blink.gateway.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.gateway.admin.entity.SysNotificationReadDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息读取状态Mapper
 *
 * @author binblink
 * @since 2026-04-06
 */
@Mapper
public interface SysNotificationReadMapper extends BaseMapper<SysNotificationReadDO> {
}