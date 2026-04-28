package com.blink.gateway.admin.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 保存实例配置文件内容请求
 * 直接保存 YAML 内容
 *
 * @author binblink
 * @since 2026-04-26
 */
@Data
public class SaveInstanceConfigContentReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例ID（必填）
     */
    @NotBlank(message = "实例ID不能为空")
    private String instanceId;

    /**
     * 配置内容（YAML 格式）
     */
    @NotBlank(message = "配置内容不能为空")
    private String content;

    /**
     * 备注
     */
    private String remark;
}
