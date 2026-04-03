package com.blink.gateway.base.dto.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * BatchUpdateSysConfigReq 批量更新系统配置请求参数
 *
 * @author binblink
 * @since 2025-03-10
 */
@Data
public class BatchUpdateSysConfigReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 配置列表
     */
    @NotEmpty(message = "配置列表不能为空")
    @Valid
    private List<UpdateSysConfigReq> configs;
}