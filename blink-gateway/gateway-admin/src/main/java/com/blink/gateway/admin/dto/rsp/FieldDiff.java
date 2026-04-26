package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serial;

/**
 * 字段差异
 *
 * @author binblink
 * @since 2026-04-26
 */
@Data
public class FieldDiff {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字段名称
     */
    private String fieldName;

    /**
     * 旧值（实例版本）
     */
    private String oldValue;

    /**
     * 新值（仓库版本）
     */
    private String newValue;
}