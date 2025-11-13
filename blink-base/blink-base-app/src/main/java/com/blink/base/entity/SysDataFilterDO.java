package com.blink.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 数据范围权限类型
 * </p>
 *
 * @author binblink
 * @since 2023-12-15
 */
@Getter
@Setter
@TableName("sys_data_filter")
public class SysDataFilterDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据过滤id
     */
    @TableId(value = "data_ac_id", type = IdType.AUTO)
    private Integer dataAcId;

    /**
     * 所属权限名称
     */
    @TableField("ac_name")
    private Integer acName;

    /**
     * 所属权限id
     */
    @TableField("ac_id")
    private Integer acId;

    /**
     * 所属权限url
     */
    @TableField("ac_url")
    private Integer acUrl;

    /**
     * 数据过滤名称
     */
    @TableField("data_filter_name")
    private String dataFilterName;

    /**
     * 数据过滤英文名称
     */
    @TableField("data_filter_name_en_name")
    private String dataFilterNameEnName;

    /**
     * 数据过滤类型 0 字段过滤 1条件过滤 3日期过滤
     */
    @TableField("fliter_type")
    private Integer fliterType;

    /**
     * 过滤表达式
     */
    @TableField("fliter_expression")
    private String fliterExpression;

    /**
     * 状态 0启动 1禁用
     */
    @TableField("status")
    private Integer status;

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

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;


}
