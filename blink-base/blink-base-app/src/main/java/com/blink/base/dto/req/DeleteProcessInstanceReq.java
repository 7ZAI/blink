package com.blink.base.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * 删除流程实例请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
@ToString
public class DeleteProcessInstanceReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流程实例ID
     */
    @NotBlank(message = "流程实例ID不能为空")
    private String processInstanceId;

    /**
     * 删除原因
     */
    @NotBlank(message = "删除原因不能为空")
    private String reason;
}