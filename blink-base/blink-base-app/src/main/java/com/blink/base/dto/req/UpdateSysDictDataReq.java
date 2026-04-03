package com.blink.base.dto.req;

import com.blink.base.constants.BaseErrCodeConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * UpdateSysDictDataReq 更新字典数据请求参数对象
 * </p>
 *
 * @author blink
 * @since 2026-03-07
 */
@Data
public class UpdateSysDictDataReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典数据主键id
     */
    @NotNull(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    private Integer dictCode;

    /**
     * 关联字典类型编码
     */
    @NotBlank(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    private String dictType;

    /**
     * 字典标签（显示值）
     */
    @NotBlank(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    private String dictLabel;

    /**
     * 字典键值（实际值）
     */
    @NotBlank(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    private String dictValue;

    /**
     * 样式属性（用于前端显示样式）
     */
    private String cssClass;

    /**
     * 表格回显样式
     */
    private String listClass;

    /**
     * 是否默认：0-否 1-是
     */
    private Boolean isDefault;

    /**
     * 状态：0-启用 1-禁用
     */
    private Boolean status;

    /**
     * 显示顺序
     */
    private Integer orderNum;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 语言标识
     */
    private String locale;
}
