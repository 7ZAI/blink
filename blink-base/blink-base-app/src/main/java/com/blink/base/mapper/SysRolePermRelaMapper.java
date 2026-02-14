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
}
