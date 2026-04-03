package com.blink.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.blink.datasource.annotation.DataScopeEntity;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 权限菜单
 *
 * @author binblink
 */
@Getter
@Setter
@TableName("sys_permission")
@DataScopeEntity(name = "权限菜单", enName = "SysPermission")
public class SysPermissionDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 权限id
     */
    @TableId(value = "ac_id", type = IdType.AUTO)
    private Integer acId;

    /**
     * 权限名称
     */
    @TableField("ac_name")
    private String acName;

    /**
     * 权限英文名称
     */
    @TableField("ac_en_name")
    private String acEnName;

    /**
     * 权限标识
     */
    @TableField("ac_identity")
    private String acIdentity;

    /**
     * 权限类型  1接口权限 2数据权限
     */
    @TableField("ac_type")
    private Byte acType;

    /**
     * 权限地址
     */
    @TableField("url")
    private String url;


    /**
     * 数据过滤器id
     */
    @TableField("data_filter_id")
    private Integer dataFilterId;

    /**
     * 创建者
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
