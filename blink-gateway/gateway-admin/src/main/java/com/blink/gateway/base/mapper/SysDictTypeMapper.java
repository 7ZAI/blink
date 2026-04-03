package com.blink.gateway.base.mapper;

import com.blink.gateway.base.entity.SysDictTypeDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.gateway.base.dto.req.QuerySysDictTypeReq;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 字典类型表 Mapper 接口
 *
 * @author blink
 * @since 2025-03-07
 */
@Mapper
public interface SysDictTypeMapper extends BaseMapper<SysDictTypeDO> {

    /**
     * 查询字典类型列表
     *
     * @param reqDTO 查询条件
     * @return 字典类型列表
     */
    List<SysDictTypeDO> findSysDictTypeList(QuerySysDictTypeReq reqDTO);
}
