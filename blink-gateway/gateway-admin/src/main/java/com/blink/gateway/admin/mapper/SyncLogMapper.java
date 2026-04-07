package com.blink.gateway.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.gateway.admin.entity.SyncLogDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 同步日志 Mapper
 *
 * @author binblink
 */
@Mapper
public interface SyncLogMapper extends BaseMapper<SyncLogDO> {
}