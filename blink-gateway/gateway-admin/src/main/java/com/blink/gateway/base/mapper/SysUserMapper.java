package com.blink.gateway.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.gateway.base.dto.req.QuerySysUserReq;
import com.blink.gateway.base.dto.vo.SysUserVO;
import com.blink.gateway.base.entity.SysUserDO;
import com.blink.datasource.annotation.DataScope;
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

    @DataScope(entity = SysUserDO.class, tableAlias = "su")
    List<SysUserVO> findSysUserList(QuerySysUserReq reqDTO);

    @DataScope(entity = SysUserDO.class, tableAlias = "su")
    SysUserVO findUserDetail(QuerySysUserReq reqDTO);

    List<SysUserDO> selectUserListByIds(List<Integer> userIdList);

}
