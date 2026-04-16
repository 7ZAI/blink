package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.util.List;

/**
 * 验证推送结果响应
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class VerifyPushResultRsp {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 推送记录ID
     */
    private Long pushId;

    /**
     * 是否一致
     */
    private Boolean consistent;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 各实例验证结果
     */
    private List<InstanceVerifyResult> instanceResults;

    /**
     * 实例验证结果
     */
    @Data
    public static class InstanceVerifyResult {

        /**
         * 实例ID
         */
        private String instanceId;

        /**
         * 是否一致
         */
        private Boolean consistent;

        /**
         * 路由数量
         */
        private Integer routeCount;

        /**
         * 期望数量
         */
        private Integer expectedCount;

        /**
         * 实际数量
         */
        private Integer actualCount;

        /**
         * 缺失的路由
         */
        private List<String> missingRoutes;

        /**
         * 错误信息
         */
        private String errorMessage;
    }
}
