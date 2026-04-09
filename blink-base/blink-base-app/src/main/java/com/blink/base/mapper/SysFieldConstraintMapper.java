package com.blink.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.base.entity.SysFieldConstraintDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 字段约束表 Mapper 接口
 * </p>
 *
 * @author binblink
 * @since 2026-03-07
 */
@Mapper
public interface SysFieldConstraintMapper extends BaseMapper<SysFieldConstraintDO> {

    /**
     * 查询所有字段约束
     *
     * @return 字段约束列表
     */
    List<SysFieldConstraintDO> findAllFieldConstraints();
}