package com.blink.gateway.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.gateway.base.entity.SysDataDictDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 数据字典表 Mapper 接口
 * </p>
 *
 * @author binblink
 */
@Mapper
public interface SysDataDictMapper extends BaseMapper<SysDataDictDO> {

    @Select("select dict_name,data_type,max_length,data_pattern,data_precision from sys_data_dict")
    List<SysDataDictDO> findAllDataDicts();
}
