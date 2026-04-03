package com.blink.gateway.admin.dto.rsp;

import com.blink.gateway.admin.dto.vo.GatewayMetricsVO;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 网关指标响应DTO
 *
 * @author binblink
 */
@Data
public class GatewayMetricsRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 指标列表
     */
    private List<GatewayMetricsVO> metricsList;
}