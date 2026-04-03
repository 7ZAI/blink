package com.blink.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.base.dto.req.QuerySimpleUserReq;
import com.blink.base.dto.req.QuerySysUserReq;
import com.blink.base.dto.vo.SimpleUserVO;
import com.blink.base.dto.vo.SysUserVO;
import com.blink.base.entity.SysUserDO;
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

    /**
     * 查询简化用户列表（用于弹窗选择）
     *
     * @param req 查询条件
     * @return 用户列表
     */
    List<SimpleUserVO> selectSimpleUserList(QuerySimpleUserReq req);

}
