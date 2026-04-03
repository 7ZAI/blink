package com.blink.gateway.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 参数配置表
 * </p>
 *
 * @author blink
 * @since 2025-09-05
 */
@Getter
@Setter
@TableName("sys_config")
public class SysConfigDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 参数键名
     */
    @TableField("config_key")
    private String configKey;

    /**
     * 参数名称
     */
    @TableField("config_name")
    private String configName;

    /**
     * 参数值
     */
    @TableField("config_value")
    private String configValue;

    /**
     * 参数类型：0-字符串 1-数字 2-布尔 3-JSON 4-数组
     */
    @TableField("config_type")
    private Byte configType;

    /**
     * 参数分组ID
     */
    @TableField("group_id")
    private Integer groupId;

    /**
     * 参数描述
     */
    @TableField("description")
    private String description;

    /**
     * 是否只读：0-可修改 1-只读
     */
    @TableField("readonly")
    private Boolean readonly;

    /**
     * 状态：0-禁用 1-启用
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

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;
}
