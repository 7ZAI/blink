package com.blink.base.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * 导入BPMN XML流程定义请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
@ToString
public class ImportXmlProcessReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流程名称
     */
    @NotBlank(message = "流程名称不能为空")
    private String processName;

    /**
     * BPMN XML内容
     */
    @NotBlank(message = "BPMN XML内容不能为空")
    private String bpmnXmlContent;

    /**
     * 流程描述
     */
    private String description;
}