package com.blink.base.mapper;

import com.blink.base.entity.BizLeaveRequestDO;
import com.blink.base.dto.req.QueryLeaveReq;
import com.blink.base.dto.vo.LeaveRequestVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 请假申请 Mapper
 *
 * @author binblink
 */
@Mapper
public interface BizLeaveRequestMapper extends BaseMapper<BizLeaveRequestDO> {

    /**
     * 查询申请人的请假列表
     *
     * @param applicantId 申请人ID
     * @param req         查询条件
     * @return 请假列表
     */
    List<LeaveRequestVO> selectByApplicantId(@Param("applicantId") Integer applicantId,
                                              @Param("req") QueryLeaveReq req);

    /**
     * 查询待审批列表
     *
     * @param loginName 当前用户登录名
     * @param req       查询条件
     * @return 待审批列表
     */
    List<LeaveRequestVO> selectPendingApproval(@Param("loginName") String loginName,
                                                @Param("req") QueryLeaveReq req);

    /**
     * 查询请假详情（含审批记录）
     *
     * @param id 请假申请ID
     * @return 请假详情
     */
    LeaveRequestVO selectDetailById(@Param("id") Integer id);

    /**
     * 管理员查询所有请假列表
     *
     * @param req 查询条件
     * @return 请假列表
     */
    List<LeaveRequestVO> selectAllList(@Param("req") QueryLeaveReq req);
}
