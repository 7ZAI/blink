package com.blink.base.mapper;

import com.blink.base.entity.SysUserRoleRelaDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 用户角色关系表 多对多 Mapper 接口
 * </p>
 *
 * @author binblink
 * @since 2023-12-15
 */
@Mapper
public interface SysUserRoleRelaMapper extends BaseMapper<SysUserRoleRelaDO> {

    int batchInsert(@Param("list") List<SysUserRoleRelaDO> list);

    /**
     * 根据角色ID列表查询用户ID列表
     *
     * @param roleIds 角色ID列表
     * @return 用户ID列表
     */
    List<Integer> selectUserIdsByRoleIds(@Param("roleIds") List<Integer> roleIds);

    /**
     * 根据角色ID列表查询用户登入名列表
     *
     * @param roleIds 角色ID列表
     * @return 用户登入名列表
     */
    List<String> selectLoginNamesByRoleIds(@Param("roleIds") List<Integer> roleIds);
}
