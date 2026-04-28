package com.blink.gateway.admin.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 获取实例配置文件请求
 *
 * @author binblink
 * @since 2026-04-26
 */
@Data
public class GetInstanceConfigReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例ID（必填）
     */
    @NotBlank(message = "实例ID不能为空")
    private String instanceId;
}
