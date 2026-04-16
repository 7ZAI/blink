package com.blink.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.base.entity.SysJobLogDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定时任务日志 Mapper
 *
 * @author binblink
 */
@Mapper
public interface SysJobLogMapper extends BaseMapper<SysJobLogDO> {
}
