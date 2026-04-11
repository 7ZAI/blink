package com.blink.gateway.admin.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 获取实例详情请求参数
 *
 * @author binblink
 */
@Data
public class GetInstanceDetailReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例主键 ID
     */
    @NotNull(message = "实例ID不能为空")
    private Integer id;
}