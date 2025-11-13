package com.blink.base.mapper;

import com.blink.base.entity.SysUserGroupRelaDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

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

}
