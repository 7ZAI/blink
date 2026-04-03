package com.blink.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.base.entity.SysUserPreferenceDO;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 用户偏好设置 Mapper 接口
 * </p>
 *
 * @author binblink
 */
public interface SysUserPreferenceMapper extends BaseMapper<SysUserPreferenceDO> {

    /**
     * 根据用户ID查询偏好设置
     *
     * @param userId 用户ID
     * @return 偏好设置
     */
    SysUserPreferenceDO selectByUserId(@Param("userId") Integer userId);

}
