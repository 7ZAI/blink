package com.blink.gateway.admin.service;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.DeleteInstanceReq;
import com.blink.gateway.admin.dto.req.GetGatewayInstanceDetailReq;
import com.blink.gateway.admin.dto.req.GetInstanceDetailReq;
import com.blink.gateway.admin.dto.req.OfflineGatewayInstanceReq;
import com.blink.gateway.admin.dto.req.OnlineGatewayInstanceReq;
import com.blink.gateway.admin.dto.req.QueryInstanceReq;
import com.blink.gateway.admin.dto.req.SaveInstanceReq;
import com.blink.gateway.admin.dto.rsp.GatewayInstanceListRsp;
import com.blink.gateway.admin.dto.rsp.InstanceDetailRsp;
import com.blink.gateway.admin.dto.rsp.QueryInstanceListRsp;
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
     * 下线网关实例（通过Nacos设置enabled=false）
     *
     * @param req 请求参数
     * @return 操作结果
     */
    ResponseDTO<EmptyBody> offlineInstance(OfflineGatewayInstanceReq req);

    /**
     * 上线网关实例（通过Nacos设置enabled=true）
     *
     * @param req 请求参数
     * @return 操作结果
     */
    ResponseDTO<EmptyBody> onlineInstance(OnlineGatewayInstanceReq req);

    /**
     * 同步网关实例状态（定时任务调用）
     */
    void syncInstanceStatus();

    /**
     * 手动刷新实例状态（从Nacos实时获取并同步到数据库）
     *
     * @return 同步结果
     */
    ResponseDTO<EmptyBody> refreshInstanceStatus();

    /**
     * 分页查询实例列表（从数据库）
     *
     * @param req 查询请求参数
     * @return 实例列表响应
     */
    ResponseDTO<QueryInstanceListRsp> queryInstanceList(QueryInstanceReq req);

    /**
     * 保存实例（新增/编辑）
     *
     * @param req 保存请求参数
     * @return 操作结果
     */
    ResponseDTO<EmptyBody> saveInstance(SaveInstanceReq req);

    /**
     * 删除实例
     *
     * @param req 删除请求参数
     * @return 操作结果
     */
    ResponseDTO<EmptyBody> deleteInstance(DeleteInstanceReq req);

    /**
     * 获取实例详情（含监控指标）
     *
     * @param req 请求参数
     * @return 实例详情响应
     */
    ResponseDTO<InstanceDetailRsp> getInstanceDetailWithMetrics(GetInstanceDetailReq req);
}