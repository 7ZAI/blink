package com.blink.gateway.admin.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 保存实例请求参数（新增/编辑）
 *
 * @author binblink
 */
@Data
public class SaveInstanceReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID（编辑时必填）
     */
    private Integer id;

    /**
     * 服务ID
     */
    @NotBlank(message = "服务ID不能为空")
    private String serviceId;

    /**
     * 主机地址
     */
    @NotBlank(message = "主机地址不能为空")
    private String host;

    /**
     * 端口
     */
    @NotNull(message = "端口不能为空")
    private Integer port;

    /**
     * 元数据（JSON 格式，可选）
     */
    private String metadata;
}