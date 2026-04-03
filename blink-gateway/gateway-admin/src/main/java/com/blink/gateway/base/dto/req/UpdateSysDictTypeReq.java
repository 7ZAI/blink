package com.blink.gateway.base.dto.req;

import com.blink.gateway.base.constants.BaseErrCodeConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 更新字典类型表请求参数对象
 *
 * @author blink
 * @since 2025-03-07
 */
@Data
public class UpdateSysDictTypeReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典主键id
     */
    @NotNull(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    private Integer dictId;

    /**
     * 字典类型编码（唯一标识）
     */
    @NotBlank(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    private String dictType;

    /**
     * 字典类型名称
     */
    @NotBlank(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    private String dictName;

    /**
     * 状态：0-启用 1-禁用
     */
    private Boolean status;

    /**
     * 显示顺序
     */
    private Integer orderNum;

    /**
     * 更新者
     */
    private String updateBy;

    /**
     * 备注
     */
    private String remark;

    /**
     * 语言标识
     */
    private String locale;
}
