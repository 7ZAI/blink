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
 * 历史任务VO
 *
 * @author binblink
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricTaskVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务描述
     */
    private String taskDescription;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 流程定义ID
     */
    private String processDefinitionId;

    /**
     * 流程定义KEY
     */
    private String processDefinitionKey;

    /**
     * 流程名称
     */
    private String processName;

    /**
     * 任务受理人
     */
    private String assignee;

    /**
     * 任务受理人姓名
     */
    private String assigneeName;

    /**
     * 任务创建时间
     */
    private LocalDateTime createTime;

    /**
     * 任务结束时间
     */
    private LocalDateTime endTime;

    /**
     * 任务耗时（毫秒）
     */
    private Long durationInMillis;

    /**
     * 删除原因
     */
    private String deleteReason;

    /**
     * 任务优先级
     */
    private Integer priority;
}