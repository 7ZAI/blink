package com.blink.base.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 启动流程实例请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
@ToString
public class StartProcessReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流程定义KEY
     */
    @NotBlank(message = "流程定义KEY不能为空")
    private String processDefinitionKey;

    /**
     * 业务KEY
     */
    private String businessKey;

    /**
     * 流程变量
     */
    private Map<String, Object> variables;
}