package com.blink.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.base.dto.req.QueryRolePermissionReqDTO;
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

    /**
     * 权限列表查询
     * @param reqDTO 入参
     * @return 权限列表
     */
    List<SysPermissionDO> findSysPermissionList(QuerySysPermissionReqDTO reqDTO);

    /**
     * 根据角色查询权限
     * @param roleIds 角色id集合
     * @return 角色拥有的权限
     */
    List<SysPermissionDO> findRolesPermissions(List<Integer> roleIds);


    /**
     * 根据角色id查询指定类型的权限
     * @param reqDTO
     * @return 角色指定类型权限集合
     */
    List<SysPermissionDO> findRolesPermissionsWithType(QueryRolePermissionReqDTO reqDTO);
}
