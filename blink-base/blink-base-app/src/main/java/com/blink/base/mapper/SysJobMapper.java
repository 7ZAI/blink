package com.blink.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.base.entity.SysJobDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定时任务 Mapper
 *
 * @author binblink
 */
@Mapper
public interface SysJobMapper extends BaseMapper<SysJobDO> {
}
