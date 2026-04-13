package com.blink.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 请假申请实体类
 *
 * @author binblink
 */
@Data
@TableName("biz_leave_request")
public class BizLeaveRequestDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 申请人ID
     */
    private Integer applicantId;

    /**
     * 申请人姓名
     */
    private String applicantName;

    /**
     * 部门ID
     */
    private Integer deptId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 请假类型：annual-年假/sick-病假/personal-事假/compensatory-调休/marriage-婚假/maternity-产假
     */
    private String leaveType;

    /**
     * 开始时间
     */
    private LocalDateTime startDate;

    /**
     * 结束时间
     */
    private LocalDateTime endDate;

    /**
     * 请假天数
     */
    private BigDecimal days;

    /**
     * 请假原因
     */
    private String reason;

    /**
     * 状态：draft-草稿/pending-待审批/approved-已通过/rejected-已拒绝/cancelled-已取消
     */
    private String status;

    /**
     * 当前任务节点
     */
    private String currentTask;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 备注
     */
    private String remark;
}
