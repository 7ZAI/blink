package com.blink.gateway.base.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * 完成任务请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
@ToString
public class CompleteTaskReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    /**
     * 用户ID
     */
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    /**
     * 审批意见
     */
    private String comment;

    /**
     * 是否通过
     */
    private Boolean approved;

    /**
     * 其他流程变量
     */
    private java.util.Map<String, Object> variables;
}