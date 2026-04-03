
package com.blink.gateway.base.dto.req;

import com.blink.gateway.base.constants.BaseErrCodeConstant;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * AddSysConfigReqDTO 新增参数配置表请求参数对象
 * </p>
 *
 * @author blink
 * @since 2025-09-05
 */
@Data
public class AddSysConfigReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Integer id;

    /**
     * 参数键名
     */
    @NotBlank(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    private String configKey;

    /**
     * 参数名称
     */
    @NotBlank(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    private String configName;

    /**
     * 参数值
     */
    @NotBlank(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    private String configValue;


    /**
     * 参数类型：0-字符串 1-数字 2-布尔 3-JSON 4-数组
     */
    private Byte configType;


    /**
     * 参数分组ID
     */
    private Integer groupId;


    /**
     * 参数描述
     */
    private String description;


    /**
     * 是否只读：0-可修改 1-只读
     */
    private Boolean readonly;


    /**
     * 状态：0-禁用 1-启用
     */
    private Boolean status;


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


}
