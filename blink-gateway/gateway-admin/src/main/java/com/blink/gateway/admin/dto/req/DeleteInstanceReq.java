package com.blink.gateway.admin.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 删除实例请求参数
 *
 * @author binblink
 */
@Data
public class DeleteInstanceReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例主键 ID
     */
    @NotNull(message = "实例ID不能为空")
    private Integer id;
}