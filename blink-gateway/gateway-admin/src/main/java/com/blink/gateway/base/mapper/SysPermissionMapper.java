package com.blink.gateway.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.gateway.base.dto.req.QueryRolePermissionReq;
import com.blink.gateway.base.dto.req.QuerySysPermissionReq;
import com.blink.gateway.base.dto.vo.SysPermissionVO;
import com.blink.gateway.base.entity.SysPermissionDO;
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
    List<SysPermissionVO> findSysPermissionList(QuerySysPermissionReq reqDTO);

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
    List<SysPermissionDO> findRolesPermissionsWithType(QueryRolePermissionReq reqDTO);

    /**
     * 获取渠道的所有权限
     * @param channelId 渠道id
     * @return 渠道权限列表
     */
    List<SysPermissionDO>  getChannelPermissions(String channelId);

    /**
     * 根据数据过滤规则ID查询关联的权限数量
     * @param dataFilterId 数据过滤规则ID
     * @return 关联的权限数量
     */
    int countByDataFilterId(Integer dataFilterId);
}
