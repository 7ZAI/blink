package com.blink.gateway.base.mapper;

import com.blink.gateway.base.entity.SysDataFilterDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 数据权限过滤规则 Mapper 接口
 *
 * @author binblink
 */
@Mapper
public interface SysDataFilterMapper extends BaseMapper<SysDataFilterDO> {

    /**
     * 根据用户ID查询该用户拥有的所有数据过滤规则
     *
     * @param userId 用户ID
     * @return 数据过滤规则列表
     */
    List<SysDataFilterDO> selectByUserId(@Param("userId") Integer userId);
}