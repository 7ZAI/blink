package com.blink.gateway.admin.service;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.CacheCheckReq;
import com.blink.gateway.admin.dto.req.CacheSyncReq;
import com.blink.gateway.admin.dto.rsp.CacheCheckRsp;
import com.blink.gateway.admin.dto.rsp.SyncLogRsp;

/**
 * 缓存状态服务接口
 *
 * @author binblink
 */
public interface CacheStatusService {

    /**
     * 获取网关实例列表
     *
     * @return 实例列表
     */
    ResponseDTO<?> getGatewayInstances();

    /**
     * 执行一致性检查
     *
     * @param req 检查请求
     * @return 检查结果
     */
    ResponseDTO<CacheCheckRsp> checkConsistency(CacheCheckReq req);

    /**
     * 同步数据到网关
     *
     * @param req 同步请求
     * @return 操作结果
     */
    ResponseDTO<EmptyBody> syncData(CacheSyncReq req);

    /**
     * 获取同步日志列表
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 日志列表
     */
    ResponseDTO<SyncLogRsp> getSyncLogs(Integer pageNum, Integer pageSize);
}