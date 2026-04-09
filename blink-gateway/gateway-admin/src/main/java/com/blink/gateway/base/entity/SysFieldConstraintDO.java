package com.blink.gateway.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 字段约束表 - 用于定义字段的数据类型、长度、精度、正则等约束规则
 * </p>
 *
 * @author binblink
 * @since 2026-03-07
 */
@Getter
@Setter
@TableName("sys_field_constraint")
public class SysFieldConstraintDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 字段约束ID
     */
    @TableId(value = "constraint_id", type = IdType.AUTO)
    private Integer constraintId;

    /**
     * 约束名称（字段名称）
     */
    @TableField("constraint_name")
    private String constraintName;

    /**
     * 约束描述
     */
    @TableField("constraint_description")
    private String constraintDescription;

    /**
     * 数据类型（C-char N-number D-decimal S-string T-time）
     */
    @TableField("data_type")
    private String dataType;

    /**
     * 最大长度
     */
    @TableField("max_length")
    private Integer maxLength;

    /**
     * 数据正则校验模式
     */
    @TableField("data_pattern")
    private String dataPattern;

    /**
     * 数据精度（小数位数）
     */
    @TableField("data_precision")
    private Integer dataPrecision;

    /**
     * 状态：0-启用 1-禁用
     */
    @TableField("status")
    private Boolean status;

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