
package com.blink.base.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * DeleteSysConfigReqDTO删除参数配置表请求参数对象
 * </p>
 *
 * @author blink
 * @since 2025-09-05
 */
@Data
public class DeleteSysConfigReqDTO implements Serializable {

    private static final long serialVersionUID = 1L;


    /**
     * 主键ID
     */
    private Integer deleteId;

    /**
     * 批量删除用户Id集合
     */
    private List<Integer> idList;


    /**
     * 是否批量删除标志
     */
    @NotNull
    private boolean isBatchDelete;


}
