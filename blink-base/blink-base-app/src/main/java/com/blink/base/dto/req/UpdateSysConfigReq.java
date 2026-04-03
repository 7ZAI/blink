
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
 * UpdateSysConfigReqDTO 更新参数配置表请求参数对象
 * </p>
 *
 * @author blink
 * @since 2025-09-05
 */
@Data
public class UpdateSysConfigReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;


    /**
     * 主键ID
     */
    @NotNull(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    private Integer id;


    /**
     * 参数键名
     */
    @NotBlank(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    private String configKey;

    /**
     * 参数值
     */
    @NotBlank(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    private String configValue;


    /**
     * 参数名称
     */
    private String configName;


    /**
     * 参数类型：0-字符串 1-数字 2-布尔 3-JSON 4-数组
     */
    private Byte configType;


    /**
     * 参数描述
     */
    private String description;


    /**
     * 更新者
     */
    private String updateBy;


    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


}
