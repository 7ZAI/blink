package com.blink.base.mapper;

import com.blink.base.entity.SysGroupDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 组 Mapper 接口
 * </p>
 *
 * @author binblink
 * @since 2024-01-04
 */
@Mapper
public interface SysGroupMapper extends BaseMapper<SysGroupDO> {

    List<SysGroupDO> findSysGroupList(SysGroupDO reqDTO);

    /**
     * 获取部门及其所有子部门ID列表
     *
     * @param deptId 部门ID
     * @return 部门ID列表（包含子部门）
     */
    List<Integer> selectDeptAndChildrenById(@Param("deptId") Integer deptId);
}
