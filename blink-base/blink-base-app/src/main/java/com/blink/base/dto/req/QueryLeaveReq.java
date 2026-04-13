package com.blink.base.dto.req;

import com.blink.framework.common.data.Page;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询请假申请请求
 *
 * @author binblink
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QueryLeaveReq extends Page {

    /**
     * 状态：draft/pending/approved/rejected/cancelled
     */
    private String status;

    /**
     * 请假类型
     */
    private String leaveType;

    /**
     * 申请人ID（管理员查询时使用）
     */
    private Integer applicantId;

    /**
     * 申请人姓名（模糊查询）
     */
    private String applicantName;

    /**
     * 部门ID
     */
    private Integer deptId;

    /**
     * 开始时间-起
     */
    private String startDateBegin;

    /**
     * 开始时间-止
     */
    private String startDateEnd;
}
