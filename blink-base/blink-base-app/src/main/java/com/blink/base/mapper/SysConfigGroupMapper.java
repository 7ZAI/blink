package com.blink.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.base.dto.req.QuerySysConfigGroupReqDTO;
import com.blink.base.entity.SysConfigGroupDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 参数分组表 Mapper 接口
 * </p>
 *
 * @author blink
 * @since 2025-10-14
 */
@Mapper
public interface SysConfigGroupMapper extends BaseMapper<SysConfigGroupDO> {


    List<SysConfigGroupDO> findSysConfigGroupList(QuerySysConfigGroupReqDTO reqDTO);

    List<Integer> findAllSonIdByParentId(Integer parentId);
}
