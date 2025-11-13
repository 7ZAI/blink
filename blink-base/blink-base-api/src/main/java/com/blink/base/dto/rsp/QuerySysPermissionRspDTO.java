package com.blink.base.dto.rsp;

import lombok.Data;
import java.io.Serializable;
import com.blink.framework.common.data.PageDTO;
import java.time.LocalDateTime;

/**
 * <p>
 * QuerySysPermissionRspDTO 新增权限菜单请求参数对象
 * </p>
 *
 * @author binblink
 * @since 2024-01-13
 */
@Data
public class QuerySysPermissionRspDTO extends PageDTO implements Serializable {

  private static final long serialVersionUID = 1L;


    /**
     * 权限id
     */
    private Integer acId;


    /**
     * 权限名称
     */
    private String acName;


    /**
     * 权限英文名称
     */
    private String acEnName;


    /**
     * 权限标识
     */
    private String acIdentity;


    /**
     * 权限类型 0 菜单权限 1数据权限 2功能权限 3接口权限
     */
    private Byte acType;


    /**
     * 权限图标
     */
    private String icon;


    /**
     * 权限地址
     */
    private String url;


    /**
     * 状态 0启动 1禁用 2隐藏
     */
    private Byte status;


    /**
     * 父权限id
     */
    private Integer parentId;


    /**
     * 数据过滤器id
     */
    private Integer dataFilterId;


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


}
