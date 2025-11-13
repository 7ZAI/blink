package com.blink.base.mapper;

import com.blink.base.entity.SysMenuDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.base.dto.req.QuerySysMenuReqDTO;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 系统菜单 Mapper 接口
 * </p>
 *
 * @author binblink
 * @since 2024-01-05
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenuDO> {


       List<SysMenuDO> findSysMenuList(QuerySysMenuReqDTO reqDTO);
}
