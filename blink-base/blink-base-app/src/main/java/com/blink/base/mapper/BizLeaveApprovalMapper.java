package com.blink.base.mapper;

import com.blink.base.entity.BizLeaveApprovalDO;
import com.blink.base.dto.vo.LeaveApprovalVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 请假审批记录 Mapper
 *
 * @author binblink
 */
@Mapper
public interface BizLeaveApprovalMapper extends BaseMapper<BizLeaveApprovalDO> {

    /**
     * 根据请假申请ID查询审批记录
     *
     * @param leaveRequestId 请假申请ID
     * @return 审批记录列表
     */
    List<LeaveApprovalVO> selectByLeaveRequestId(@Param("leaveRequestId") Integer leaveRequestId);
}
