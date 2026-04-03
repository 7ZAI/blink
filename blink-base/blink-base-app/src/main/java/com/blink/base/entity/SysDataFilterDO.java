package com.blink.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据权限过滤规则实体
 *
 * @author binblink
 * @since 2023-12-15
 */
@Getter
@Setter
@TableName("sys_data_filter")
public class SysDataFilterDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 数据过滤ID
     */
    @TableId(value = "data_filter_id", type = IdType.AUTO)
    private Integer dataFilterId;

    /**
     * 过滤规则名称
     */
    @TableField("data_filter_name")
    private String dataFilterName;

    /**
     * 过滤规则英文名称
     */
    @TableField("data_filter_en_name")
    private String dataFilterEnName;

    /**
     * 实体类全限定名（如 com.blink.base.entity.SysUserDO）
     */
    @TableField("entity_class")
    private String entityClass;

    /**
     * 对应表名（如 sys_user）
     */
    @TableField("table_name")
    private String tableName;

    /**
     * 规则类型：FIELD_FILTER/CREATOR_FILTER/DATE_RANGE_FILTER/CUSTOM_SQL
     */
    @TableField("rule_type")
    private String ruleType;

    /**
     * 规则配置JSON
     */
    @TableField("rule_config")
    private String ruleConfig;

    /**
     * 状态 0启用 1禁用
     */
    @TableField("status")
    private Byte status;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

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