
package com.blink.gateway.base.dto.req;

import com.blink.gateway.base.constants.BaseErrCodeConstant;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * AddSysConfigGroupReqDTO 新增参数分组表请求参数对象
 * </p>
 *
 * @author blink
 * @since 2025-10-14
 */
@Data
public class AddSysConfigGroupReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Integer id;

    /**
     * 分组键名
     */
    @NotBlank(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    private String groupKey;

    /**
     * 分组名称
     */
    @NotBlank(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    private String groupName;


    /**
     * 父分组ID
     */
    private Integer parentId;


    /**
     * 显示顺序
     */
    private Integer orderNum;


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
