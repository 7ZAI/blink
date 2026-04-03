package com.blink.gateway.admin.service;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.GetGatewayInstanceDetailReq;
import com.blink.gateway.admin.dto.req.OfflineGatewayInstanceReq;
import com.blink.gateway.admin.dto.req.OnlineGatewayInstanceReq;
import com.blink.gateway.admin.dto.rsp.GatewayInstanceListRsp;
import com.blink.gateway.admin.dto.vo.GatewayInstanceVO;

/**
 * 网关实例管理服务接口
 *
 * @author binblink
 */
public interface GatewayInstanceService {

    /**
     * 获取网关实例列表
     *
     * @return 实例列表
     */
    ResponseDTO<GatewayInstanceListRsp> getGatewayInstances();

    /**
     * 获取网关实例详情
     *
     * @param req 请求参数
     * @return 实例详情
     */
    ResponseDTO<GatewayInstanceVO> getGatewayInstanceDetail(GetGatewayInstanceDetailReq req);

    /**
     * 下线网关实例
     *
     * @param req 请求参数
     * @return 操作结果
     */
    ResponseDTO<EmptyBody> offlineInstance(OfflineGatewayInstanceReq req);

    /**
     * 上线网关实例
     *
     * @param req 请求参数
     * @return 操作结果
     */
    ResponseDTO<EmptyBody> onlineInstance(OnlineGatewayInstanceReq req);

    /**
     * 同步网关实例状态（定时任务调用）
     */
    void syncInstanceStatus();
}