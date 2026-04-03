package com.blink.base.dto.rsp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 流程历史节点响应DTO
 *
 * @author binblink
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessHistoryRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 活动实例ID
     */
    private String activityId;

    /**
     * 活动名称
     */
    private String activityName;

    /**
     * 活动类型
     */
    private String activityType;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 受理人
     */
    private String assignee;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 耗时（毫秒）
     */
    private Long durationInMillis;

    /**
     * 审批意见
     */
    private String comment;

    /**
     * 状态（completed-已完成, pending-进行中）
     */
    private String status;
}