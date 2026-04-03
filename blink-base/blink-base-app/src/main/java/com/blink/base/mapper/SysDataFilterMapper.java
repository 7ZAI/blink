package com.blink.base.mapper;

import com.blink.base.dto.vo.DataFilterVO;
import com.blink.base.entity.SysDataFilterDO;
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

    /**
     * 根据角色ID列表查询数据过滤权限
     *
     * @param roleIds 角色ID列表
     * @return 数据过滤权限列表
     */
    List<DataFilterVO> selectDataFiltersByRoleIds(@Param("roleIds") List<Integer> roleIds);
}