package com.blink.base.dto.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 实体字段VO
 *
 * @author binblink
 */
@Getter
@Setter
public class EntityFieldVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 字段名（Java属性名）
     */
    private String fieldName;

    /**
     * 列名（数据库列名）
     */
    private String columnName;

    /**
     * 字段类型
     */
    private String fieldType;
}