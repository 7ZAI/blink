package com.blink.base.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * 流程实例ID请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
@ToString
public class ProcessInstanceIdReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流程实例ID
     */
    @NotBlank(message = "流程实例ID不能为空")
    private String processInstanceId;
}