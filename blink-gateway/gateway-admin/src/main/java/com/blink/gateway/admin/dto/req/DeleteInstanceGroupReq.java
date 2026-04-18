package com.blink.gateway.admin.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 删除实例分组请求参数
 *
 * @author binblink
 */
@Data
public class DeleteInstanceGroupReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分组ID
     */
    @NotNull(message = "分组ID不能为空")
    private Integer groupId;
}
