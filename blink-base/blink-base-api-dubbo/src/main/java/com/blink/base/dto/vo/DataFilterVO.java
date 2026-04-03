package com.blink.base.dto.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 数据过滤规则VO
 *
 * @author binblink
 */
@Data
public class DataFilterVO implements Serializable {

    @Serial
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
     * 状态 0启用 1禁用
     */
    private Byte status;
}