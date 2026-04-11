package com.blink.gateway.admin.dto.rsp;

import com.blink.gateway.admin.dto.vo.HealthDetailVO;
import com.blink.gateway.admin.dto.vo.HttpMetricsVO;
import com.blink.gateway.admin.dto.vo.InstanceInfoVO;
import com.blink.gateway.admin.dto.vo.JvmMetricsVO;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 实例详情响应（包含监控指标）
 *
 * @author binblink
 */
@Data
public class InstanceDetailRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例基本信息
     */
    private InstanceInfoVO instanceInfo;

    /**
     * 健康状态详情
     */
    private HealthDetailVO healthDetail;

    /**
     * JVM 监控指标
     */
    private JvmMetricsVO jvmMetrics;

    /**
     * HTTP 请求统计
     */
    private HttpMetricsVO httpMetrics;
}