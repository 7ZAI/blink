package com.blink.gateway.base.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * 流程定义ID请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
@ToString
public class ProcessDefinitionIdReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流程定义ID
     */
    @NotBlank(message = "流程定义ID不能为空")
    private String processDefinitionId;
}