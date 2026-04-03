package com.blink.gateway.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 字典数据表
 * </p>
 *
 * @author blink
 * @since 2026-03-07
 */
@Getter
@Setter
@TableName("sys_dict_data")
public class SysDictDataDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 字典数据主键id
     */
    @TableId(value = "dict_code", type = IdType.AUTO)
    private Integer dictCode;

    /**
     * 关联字典类型编码
     */
    @TableField("dict_type")
    private String dictType;

    /**
     * 字典标签（显示值）
     */
    @TableField("dict_label")
    private String dictLabel;

    /**
     * 字典键值（实际值）
     */
    @TableField("dict_value")
    private String dictValue;

    /**
     * 样式属性（用于前端显示样式）
     */
    @TableField("css_class")
    private String cssClass;

    /**
     * 表格回显样式
     */
    @TableField("list_class")
    private String listClass;

    /**
     * 是否默认：0-否 1-是
     */
    @TableField("is_default")
    private Boolean isDefault;

    /**
     * 状态：0-启用 1-禁用
     */
    @TableField("status")
    private Boolean status;

    /**
     * 显示顺序
     */
    @TableField("order_num")
    private Integer orderNum;

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

    /**
     * 语言标识
     */
    @TableField("locale")
    private String locale;
}
