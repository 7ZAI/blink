package com.blink.base.mapper;

import com.blink.base.entity.SysConfigDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.base.dto.req.QuerySysConfigReqDTO;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 参数配置表 Mapper 接口
 * </p>
 *
 * @author blink
 * @since 2025-09-05
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfigDO> {


       List<SysConfigDO> findSysConfigList(QuerySysConfigReqDTO reqDTO);
 }
