package com.blink.base.service.flowable;

import com.blink.base.dto.rsp.ProcessHistoryRsp;
import com.blink.framework.common.exception.BlinkException;

import java.util.List;

/**
 * 流程历史服务接口
 * <p>
 * 提供流程历史的查询功能
 * </p>
 *
 * @author binblink
 */
public interface ProcessHistoryService {

    /**
     * 查询流程实例历史
     *
     * @param processInstanceId 流程实例ID
     * @return 历史记录
     * @throws BlinkException 查询失败时抛出
     */
    List<ProcessHistoryRsp> getProcessHistory(String processInstanceId) throws BlinkException;
}