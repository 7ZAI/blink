
package com.blink.base.dto.req;

import com.blink.framework.common.data.PageDTO;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * <p>
 * QuerySysMenuPermRelaReqDTO 查询列表菜单权限关系表请求参数对象
 * </p>
 *
 * @author binblink
 * @since 2026-02-11
 */
@Data
public class QuerySysMenuPermRelaReqDTO extends PageDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;


    /**
     * 菜单id
     */
    private Integer menuId;


    /**
     * 权限id
     */
    private Integer acId;


}
