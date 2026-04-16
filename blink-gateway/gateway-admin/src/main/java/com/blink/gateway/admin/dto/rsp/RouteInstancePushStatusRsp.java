package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 路由实例推送状态响应
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class RouteInstancePushStatusRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 路由ID
     */
    private String routeId;

    /**
     * 总实例数
     */
    private Integer totalInstances;

    /**
     * 已推送实例数
     */
    private Integer pushedInstances;

    /**
     * 推送失败实例数
     */
    private Integer failedInstances;

    /**
     * 未推送实例数
     */
    private Integer notPushedInstances;

    /**
     * 推送状态描述
     * 格式：已推送(3/5)、推送失败(1/5) 等
     */
    private String statusDesc;

    /**
     * 各实例推送详情
     */
    private List<InstancePushDetail> instanceDetails;

    /**
     * 实例推送详情
     */
    @Data
    public static class InstancePushDetail implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 实例ID
         */
        private String instanceId;

        /**
         * 推送状态
         * 0 - 未推送
         * 1 - 已推送
         * 2 - 推送失败
         */
        private Byte pushStatus;

        /**
         * 推送状态描述
         */
        private String pushStatusDesc;

        /**
         * 推送时间
         */
        private String pushTime;

        /**
         * 错误信息
         */
        private String errorMsg;
    }
}
