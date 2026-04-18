package com.blink.gateway.admin.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 切换实例分组请求参数
 *
 * @author binblink
 */
@Getter
@Setter
public class SwitchInstanceGroupReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例ID
     */
    @NotBlank(message = "实例ID不能为空")
    private String instanceId;

    /**
     * 目标分组标识
     */
    @NotBlank(message = "目标分组不能为空")
    private String targetGroupKey;
}
