package com.blink.base.mapper;

import com.blink.base.entity.SysGroupDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.base.dto.req.QuerySysGroupReqDTO;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 组 Mapper 接口
 * </p>
 *
 * @author binblink
 * @since 2024-01-04
 */
@Mapper
public interface SysGroupMapper extends BaseMapper<SysGroupDO> {
       List<SysGroupDO> findSysGroupList(QuerySysGroupReqDTO reqDTO);
}
