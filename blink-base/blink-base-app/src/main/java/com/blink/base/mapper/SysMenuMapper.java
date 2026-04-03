package com.blink.base.mapper;

import com.blink.base.dto.req.QueryShowMenuReq;
import com.blink.base.dto.vo.SysMenuVO;
import com.blink.base.entity.SysMenuDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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


       List<SysMenuVO> findSysMenuList(SysMenuDO sysMenuDO);


       List<SysMenuVO> findSysMenuListByRole(QueryShowMenuReq reqDTO);

    /**
     * 清空指定权限关联的所有菜单的perm_id
     *
     * @param permId 权限ID
     */
    void updatePermIdToNullByPermId(@Param("permId") Integer permId);

    /**
     * 批量更新菜单的perm_id
     *
     * @param permId  权限ID
     * @param menuIds 菜单ID列表
     */
    void updatePermIdByMenuIds(@Param("permId") Integer permId, @Param("menuIds") List<Integer> menuIds);

    /**
     * 根据权限ID查询关联的菜单ID列表
     *
     * @param permId 权限ID
     * @return 菜单ID列表
     */
    List<Integer> findMenuIdsByPermId(@Param("permId") Integer permId);
}
