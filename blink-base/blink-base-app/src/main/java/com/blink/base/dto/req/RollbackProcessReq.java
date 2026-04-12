package com.blink.base.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * 回退流程请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
@ToString
public class RollbackProcessReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流程实例ID
     */
    @NotBlank(message = "流程实例ID不能为空")
    private String processInstanceId;

    /**
     * 目标节点ID
     */
    @NotBlank(message = "目标节点ID不能为空")
    private String targetActivityId;

    /**
     * 回退原因
     */
    @NotBlank(message = "回退原因不能为空")
    private String reason;
}