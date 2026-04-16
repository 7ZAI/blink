package com.blink.gateway.admin.dto.req;

import com.blink.framework.common.data.Page;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 查询告警历史请求
 *
 * @author binblink
 * @since 2026-04-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QueryAlertHistoryReq extends Page {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 状态筛选: FIRING/RESOLVED/ACKNOWLEDGED
     */
    private String status;

    /**
     * 严重程度筛选: INFO/WARNING/ERROR
     */
    private String severity;

    /**
     * 规则 ID 筛选
     */
    private Long ruleId;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 结束时间
     */
    private String endTime;
}