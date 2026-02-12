package com.blink.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.base.dto.req.QuerySysMenuPermRelaReqDTO;
import com.blink.base.entity.SysMenuPermRelaDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 菜单权限关系表 Mapper 接口
 * </p>
 *
 * @author binblink
 * @since 2026-02-11
 */
@Mapper
public interface SysMenuPermRelaMapper extends BaseMapper<SysMenuPermRelaDO> {


    List<SysMenuPermRelaDO> findSysMenuPermRelaList(QuerySysMenuPermRelaReqDTO reqDTO);
}
