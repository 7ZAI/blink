package com.blink.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.base.dto.req.QueryUserRolesReq;
import com.blink.base.dto.vo.SysRoleVO;
import com.blink.base.entity.SysRoleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 系统角色 Mapper 接口
 * </p>
 *
 * @author binblink
 * @since 2024-01-03
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRoleDO> {


    List<SysRoleVO> findSysRoleList(SysRoleDO sysRoleDO);

    List<SysRoleDO> findSysRolesByUser(QueryUserRolesReq queryUserRolesReq);
}
