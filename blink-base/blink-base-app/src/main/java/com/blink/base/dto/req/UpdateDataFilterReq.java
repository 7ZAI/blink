package com.blink.base.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 更新数据过滤规则请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
public class UpdateDataFilterReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据过滤ID
     */
    @NotNull(message = "数据过滤ID不能为空")
    private Integer dataFilterId;

    /**
     * 过滤规则名称
     */
    @NotBlank(message = "过滤规则名称不能为空")
    private String dataFilterName;

    /**
     * 过滤规则英文名称
     */
    private String dataFilterEnName;

    /**
     * 规则配置JSON
     */
    @NotBlank(message = "规则配置不能为空")
    private String ruleConfig;

    /**
     * 状态 0启用 1禁用
     */
    private Byte status;

    /**
     * 备注
     */
    private String remark;
}