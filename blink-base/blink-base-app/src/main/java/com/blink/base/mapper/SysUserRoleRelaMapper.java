package com.blink.base.mapper;

import com.blink.base.entity.SysUserRoleRelaDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

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

}
