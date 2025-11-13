package com.blink.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.base.dto.req.QuerySysPermissionReqDTO;
import com.blink.base.entity.SysPermissionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 权限菜单 Mapper 接口
 * </p>
 *
 * @author binblink
 * @since 2024-01-13
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermissionDO> {


    List<SysPermissionDO> findSysPermissionList(QuerySysPermissionReqDTO reqDTO);

    List<SysPermissionDO> findRolesPermissions(List<Integer> roleIds);
}
