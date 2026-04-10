package com.blink.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 字典类型表
 *
 * @author blink
 * @since 2025-03-07
 */
@Getter
@Setter
@TableName("sys_dict_type")
public class SysDictTypeDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 字典主键id
     */
    @TableId(value = "dict_id", type = IdType.AUTO)
    private Integer dictId;

    /**
     * 字典类型编码（唯一标识）
     */
    @TableField("dict_type")
    private String dictType;

    /**
     * 字典类型名称
     */
    @TableField("dict_name")
    private String dictName;

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
}
