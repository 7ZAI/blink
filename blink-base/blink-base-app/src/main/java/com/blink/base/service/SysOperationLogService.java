package com.blink.base.service;

import com.blink.base.dto.req.QueryOperationLogReq;
import com.blink.base.dto.rsp.OperationLogDetailRsp;
import com.blink.base.dto.rsp.OperationLogRsp;
import com.blink.base.entity.SysOperationLogDO;
import com.blink.framework.common.exception.BlinkException;

import java.time.LocalDate;

/**
 * 操作日志服务接口
 *
 * @author binblink
 */
public interface SysOperationLogService {

    /**
     * 分页查询操作日志列表
     *
     * @param req 查询参数
     * @return 日志列表
     * @throws BlinkException 业务异常
     */
    OperationLogRsp getOperationLogList(QueryOperationLogReq req) throws BlinkException;

    /**
     * 查询操作日志详情
     *
     * @param logId 日志ID
     * @return 日志详情
     * @throws BlinkException 业务异常
     */
    OperationLogDetailRsp getOperationLogDetail(Long logId) throws BlinkException;

    /**
     * 异步保存操作日志
     *
     * @param logDO 日志实体
     */
    void asyncSaveLog(SysOperationLogDO logDO);

    /**
     * 归档日志
     * <p>
     * 将指定日期之前的日志归档到历史表
     *
     * @param beforeDate 归档截止日期（不包含该日期）
     * @return 归档数量
     * @throws BlinkException 业务异常
     */
    int archiveLogs(LocalDate beforeDate) throws BlinkException;

}
