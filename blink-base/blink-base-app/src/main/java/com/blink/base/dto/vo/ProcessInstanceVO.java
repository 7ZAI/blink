package com.blink.base.dto.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 流程实例VO
 *
 * @author binblink
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessInstanceVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
     * 流程定义名称
     */
    private String processDefinitionName;

    /**
     * 业务KEY
     */
    private String businessKey;

    /**
     * 当前节点名称
     */
    private String currentActivityName;

    /**
     * 启动时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 启动人ID
     */
    private String startUserId;

    /**
     * 启动人姓名
     */
    private String startUserName;

    /**
     * 流程状态（running-运行中, completed-已完成, terminated-已终止）
     */
    private String status;

    /**
     * 流程变量
     */
    private Map<String, Object> processVariables;
}