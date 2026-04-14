package com.blink.gateway.admin.dto.rsp;

import com.blink.gateway.admin.dto.vo.TrafficPointVO;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 查询流量历史响应DTO
 *
 * @author binblink
 * @since 2026-04-14
 */
@Getter
@Setter
public class GetTrafficHistoryRsp implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流量数据点列表
     */
    private List<TrafficPointVO> points;

    /**
     * 总请求数（时间范围内）
     */
    private Long totalRequests;

    /**
     * 峰值 QPS
     */
    private Integer peakQps;
}