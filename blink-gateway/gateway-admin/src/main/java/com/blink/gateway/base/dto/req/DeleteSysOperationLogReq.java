package com.blink.gateway.base.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 操作日志ID请求DTO
 *
 * @author binblink
 * @since 2024-03-11
 */
@Data
public class DeleteSysOperationLogReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日志ID
     */
    @NotNull(message = "日志ID不能为空")
    private Long logId;

}
