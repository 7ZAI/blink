package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serial;

/**
 * 差异统计
 *
 * @author binblink
 * @since 2026-04-26
 */
@Data
public class DiffStats {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 新增数量（仓库有但实例没有）
     */
    private Integer addedCount;

    /**
     * 修改数量（两边都有但内容不同）
     */
    private Integer modifiedCount;

    /**
     * 删除数量（实例有但仓库没有，推送后将被删除）
     */
    private Integer deletedCount;

    /**
     * 不变数量（两边一致）
     */
    private Integer unchangedCount;
}