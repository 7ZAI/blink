package com.blink.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.base.dto.vo.SysRoleVO;
import com.blink.base.entity.SysRoleMenuRelaDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 角色关联菜单表 Mapper 接口
 * </p>
 *
 * @author binblink
 * @since 2026-02-14
 */
@Mapper
public interface SysRoleMenuRelaMapper extends BaseMapper<SysRoleMenuRelaDO> {

    int  batchInsert(@Param("list") List<SysRoleMenuRelaDO> list);

    int deleteBatchByMenuIds(List<Integer> deleteList);

    /**
     * 根据菜单ID查询关联的角色列表
     *
     * @param menuId 菜单ID
     * @return 角色列表
     */
    List<SysRoleVO> selectRolesByMenuId(@Param("menuId") Integer menuId);
}
