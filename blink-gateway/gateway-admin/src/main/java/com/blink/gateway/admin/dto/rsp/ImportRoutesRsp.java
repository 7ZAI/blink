package com.blink.gateway.admin.dto.rsp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 导入路由响应DTO
 *
 * @author binblink
 * @since 2026-04-12
 */
@Getter
@Setter
public class ImportRoutesRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 导入成功数量
     */
    private Integer successCount;

    /**
     * 导入失败数量
     */
    private Integer failedCount;

    /**
     * 失败原因列表
     */
    private List<ImportFailureDetail> failures;

    /**
     * 导入失败详情
     */
    @Getter
    @Setter
    @AllArgsConstructor
    public static class ImportFailureDetail implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 路由ID
         */
        private String routeId;

        /**
         * 失败原因
         */
        private String reason;
    }
}