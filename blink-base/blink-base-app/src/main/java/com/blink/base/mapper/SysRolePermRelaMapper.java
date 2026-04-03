package com.blink.base.mapper;

import com.blink.base.entity.SysRolePermRelaDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 角色权限关系表 Mapper 接口
 * </p>
 *
 * @author binblink
 * @since 2024-01-14
 */
@Mapper
public interface SysRolePermRelaMapper extends BaseMapper<SysRolePermRelaDO> {

    int  batchInsert(@Param("list") List<SysRolePermRelaDO> list);

    int deleteBatchByPermIds(List<Integer> deleteList);

    /**
     * 根据角色ID查询数据权限过滤规则ID列表
     *
     * @param roleId 角色ID
     * @return 数据过滤规则ID列表
     */
    List<Integer> selectDataFilterIdsByRoleId(@Param("roleId") Integer roleId);

    /**
     * 删除角色的接口权限关联（ac_type=1）
     *
     * @param roleId 角色ID
     */
    void deleteApiPermissionsByRoleId(@Param("roleId") Integer roleId);

    /**
     * 删除角色列表中指定权限的关联
     *
     * @param roleIds 角色ID列表
     * @param permId 权限ID
     */
    void deleteByRoleIdsAndPermId(@Param("roleIds") List<Integer> roleIds, @Param("permId") Integer permId);

    /**
     * 批量插入角色权限关联（如果不存在）
     *
     * @param list 角色权限关联列表
     */
    int batchInsertIgnore(@Param("list") List<SysRolePermRelaDO> list);
}
