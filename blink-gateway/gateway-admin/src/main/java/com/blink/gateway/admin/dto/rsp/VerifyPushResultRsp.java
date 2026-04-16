package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.util.List;
import java.util.Map;

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
     * 验证结果
     * 0 - 一致
     * 1 - 部分不一致
     * 2 - 完全不一致
     */
    private Integer verifyResult;

    /**
     * 结果摘要
     */
    private String summary;

    /**
     * 各实例验证详情
     */
    private List<InstanceVerifyDetail> instanceDetails;

    /**
     * 实例验证详情
     */
    @Data
    public static class InstanceVerifyDetail {

        /**
         * 实例ID
         */
        private String instanceId;

        /**
         * 验证结果
         * 0 - 一致
         * 1 - 不一致
         */
        private Integer result;

        /**
         * 推送的路由数量
         */
        private Integer pushedCount;

        /**
         * 实际路由数量
         */
        private Integer actualCount;

        /**
         * 匹配的路由数量
         */
        private Integer matchedCount;

        /**
         * 缺失的路由（推送了但实例没有）
         */
        private List<RouteDiff> missingRoutes;

        /**
         * 多余的路由（实例有但未推送）
         */
        private List<RouteDiff> extraRoutes;

        /**
         * 配置不一致的路由
         */
        private List<RouteDiff> mismatchedRoutes;
    }

    /**
     * 路由差异
     */
    @Data
    public static class RouteDiff {

        /**
         * 路由ID
         */
        private String routeId;

        /**
         * 差异类型
         * MISSING - 缺失
         * EXTRA - 多余
         * MISMATCH - 配置不一致
         */
        private String diffType;

        /**
         * 描述
         */
        private String description;

        /**
         * 推送的配置
         */
        private Map<String, Object> pushedConfig;

        /**
         * 实际配置
         */
        private Map<String, Object> actualConfig;
    }
}
