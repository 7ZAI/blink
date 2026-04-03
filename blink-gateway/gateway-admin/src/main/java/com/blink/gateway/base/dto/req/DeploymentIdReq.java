package com.blink.gateway.base.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * 部署ID请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
@ToString
public class DeploymentIdReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 部署ID
     */
    @NotBlank(message = "部署ID不能为空")
    private String deploymentId;

    /**
     * 是否级联删除流程实例
     */
    private Boolean cascade = false;
}