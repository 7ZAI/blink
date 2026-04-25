package com.blink.gateway.admin.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 新增路由分组请求参数
 *
 * @author binblink
 */
@Data
public class AddRouteGroupReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分组标识（业务唯一键）
     */
    @NotBlank(message = "分组标识不能为空")
    private String groupKey;

    /**
     * 分组名称
     */
    @NotBlank(message = "分组名称不能为空")
    private String groupName;

    /**
     * 状态：1启用 0禁用，默认启用
     */
    private Byte status = 1;

    /**
     * 备注说明
     */
    private String remark;
}
