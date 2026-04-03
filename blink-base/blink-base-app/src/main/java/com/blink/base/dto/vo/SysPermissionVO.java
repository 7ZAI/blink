package com.blink.base.dto.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author binblink
 */
@Data
public class SysPermissionVO implements Serializable {

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
     * 权限类型 1接口权限 2数据权限
     */
    private Byte acType;

    /**
     * 权限地址
     */
    private String url;

    /**
     * 数据过滤器id
     */
    private Integer dataFilterId;

    /**
     * 数据过滤器名称
     */
    private String dataFilterName;

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
     * 关联的菜单ID列表
     */
    private List<Integer> menuIds;

}
