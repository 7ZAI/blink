
package com.blink.gateway.base.dto.req;

import com.blink.framework.common.data.Page;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * QuerySysMenuPermRelaReqDTO 查询列表菜单权限关系表请求参数对象
 * </p>
 *
 * @author binblink
 * @since 2026-02-11
 */
@Getter
@Setter
public class QuerySysMenuPermRelaReq extends Page {


    /**
     * 菜单id
     */
    private Integer menuId;


    /**
     * 权限id
     */
    private Integer acId;


}
