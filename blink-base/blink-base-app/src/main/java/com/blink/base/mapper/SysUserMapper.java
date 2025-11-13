package com.blink.base.mapper;

import com.blink.base.dto.req.QuerySysUserReqDTO;
import com.blink.base.dto.vo.SysUserVO;
import com.blink.base.entity.SysUserDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 系统用户 Mapper 接口
 * </p>
 *
 * @author binblink
 * @since 2023-12-15
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUserDO> {

     List<SysUserVO> findSysUserList(QuerySysUserReqDTO reqDTO);

}
