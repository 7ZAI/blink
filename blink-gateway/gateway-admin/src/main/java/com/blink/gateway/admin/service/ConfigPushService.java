package com.blink.gateway.admin.service;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.GetConfigHistoryReq;
import com.blink.gateway.admin.dto.req.PushConfigReq;
import com.blink.gateway.admin.dto.req.RollbackConfigReq;
import com.blink.gateway.admin.dto.rsp.ConfigHistoryRsp;

/**
 * 配置推送服务接口
 *
 * @author binblink
 */
public interface ConfigPushService {

    /**
     * 推送配置到 Nacos
     *
     * @param req 请求参数
     * @return 操作结果
     */
    ResponseDTO<EmptyBody> pushConfigToNacos(PushConfigReq req);

    /**
     * 获取配置历史列表
     *
     * @param req 请求参数
     * @return 配置历史列表
     */
    ResponseDTO<ConfigHistoryRsp> getConfigHistory(GetConfigHistoryReq req);

    /**
     * 回滚配置到指定版本
     *
     * @param req 请求参数
     * @return 操作结果
     */
    ResponseDTO<EmptyBody> rollbackConfig(RollbackConfigReq req);
}