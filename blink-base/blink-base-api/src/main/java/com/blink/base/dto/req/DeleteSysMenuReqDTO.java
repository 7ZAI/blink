package com.blink.base.dto.req;

import com.blink.base.dto.constant.BaseAppConstant;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * DeleteSysMenuReqDTO删除系统菜单请求参数对象
 * </p>
 *
 * @author binblink
 * @since 2024-01-05
 */
@Data
public class DeleteSysMenuReqDTO implements Serializable {

    private static final long serialVersionUID = 1L;


    /**
     * 菜单id
     */
    private Integer deleteId;

    /**
     * 批量删除用户Id集合
     */
    private List<Integer> idList;


    /**
     * 是否批量删除标志
     */
    @NotNull(message = BaseAppConstant.PARAMETER_NOT_NULL)
    private boolean isBatchDelete;


}
