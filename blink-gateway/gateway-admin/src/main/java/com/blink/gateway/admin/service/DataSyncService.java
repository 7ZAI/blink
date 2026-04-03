package com.blink.gateway.admin.service;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.SyncChannelDataReq;

/**
 * 数据同步服务接口
 *
 * @author binblink
 */
public interface DataSyncService {

    /**
     * 同步渠道数据到网关
     *
     * @param req 请求参数
     * @return 操作结果
     */
    ResponseDTO<EmptyBody> syncChannelData(SyncChannelDataReq req);

    /**
     * 同步路由数据到网关
     *
     * @return 操作结果
     */
    ResponseDTO<EmptyBody> syncRouteData();

    /**
     * 同步配置数据到网关
     *
     * @return 操作结果
     */
    ResponseDTO<EmptyBody> syncConfigData();
}
