package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 实例摘要响应
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class InstanceSummaryRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例ID
     */
    private String instanceId;

    /**
     * 主机地址
     */
    private String host;

    /**
     * 端口
     */
    private Integer port;

    /**
     * 状态：ONLINE/OFFLINE
     */
    private String status;

    /**
     * 健康状态
     */
    private String healthStatus;

    /**
     * 熔断器汇总
     */
    private CircuitBreakerSummary summary;

    /**
     * 熔断器汇总内部类
     */
    @Data
    public static class CircuitBreakerSummary implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Integer total;
        private Integer open;
        private Integer closed;
        private Integer halfOpen;
    }
}
