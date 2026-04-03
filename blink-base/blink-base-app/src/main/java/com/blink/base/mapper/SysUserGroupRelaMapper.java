package com.blink.base.mapper;

import com.blink.base.entity.SysUserGroupRelaDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 用户组关系表 多对多 Mapper 接口
 * </p>
 *
 * @author binblink
 * @since 2023-12-15
 */
@Mapper
public interface SysUserGroupRelaMapper extends BaseMapper<SysUserGroupRelaDO> {

    /**
     * 根据用户ID查询部门ID
     *
     * @param userId 用户ID
     * @return 部门ID，不存在返回null
     */
    Integer selectDeptIdByUserId(@Param("userId") Integer userId);
}
