package com.blink.gateway.admin.service;

import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.GetTrafficHistoryReq;
import com.blink.gateway.admin.dto.rsp.GetTrafficHistoryRsp;

/**
 * 流量历史查询服务
 *
 * @author binblink
 * @since 2026-04-14
 */
public interface TrafficHistoryService {

    /**
     * 查询流量历史数据
     *
     * @param req 请求参数
     * @return 流量历史数据
     */
    ResponseDTO<GetTrafficHistoryRsp> getTrafficHistory(GetTrafficHistoryReq req);
}