package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 查询网关统计数据请求参数
 *
 * @author binblink
 */
@Data
public class QueryStatisticsReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 时间范围（可选）
     */
    private String timeRange;
}
