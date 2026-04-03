package com.blink.gateway.base.dto.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据过滤规则VO
 *
 * @author binblink
 */
@Getter
@Setter
public class DataFilterVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据过滤ID
     */
    private Integer dataFilterId;

    /**
     * 过滤规则名称
     */
    private String dataFilterName;

    /**
     * 过滤规则英文名称
     */
    private String dataFilterEnName;

    /**
     * 实体类全限定名
     */
    private String entityClass;

    /**
     * 对应表名
     */
    private String tableName;

    /**
     * 规则类型
     */
    private String ruleType;

    /**
     * 规则配置JSON
     */
    private String ruleConfig;

    /**
     * 状态 0启用 1禁用
     */
    private Byte status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}